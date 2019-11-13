package com.zhongjian.servlet;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zhongjian.dto.common.CommonMessageEnum;
import com.zhongjian.dto.common.ResultUtil;
import com.zhongjian.exception.NDCException;
import com.zhongjian.exception.NDCException.ClickException;
import com.zhongjian.exception.NDCException.CouponException;
import com.zhongjian.exception.NDCException.DeleteBasketExcpetion;
import com.zhongjian.exception.NDCException.IntegralException;

import org.apache.log4j.Logger;

import com.zhongjian.common.FormDataUtil;
import com.zhongjian.common.GsonUtil;
import com.zhongjian.common.ResponseHandle;
import com.zhongjian.common.SpringContextHolder;
import com.zhongjian.executor.ThreadPoolExecutorSingle;
import com.zhongjian.service.order.CVOrderService;

import java.io.IOException;
import java.util.Map;

@WebServlet(value = "/v1/order/createcvorder", asyncSupported = true)
public class CreateCVOrderServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger(CreateCVOrderServlet.class);

	private CVOrderService cvOrderService = (CVOrderService) SpringContextHolder.getBean(CVOrderService.class);

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Map<String, String> formData = FormDataUtil.getFormData(request);
		AsyncContext asyncContext = request.startAsync();
		ServletInputStream inputStream = request.getInputStream();
		inputStream.setReadListener(new ReadListener() {
			@Override
			public void onDataAvailable() throws IOException {
			}

			@Override
			public void onAllDataRead() {
				ThreadPoolExecutorSingle.executor.execute(() -> {
					String result = GsonUtil.GsonString(ResultUtil.getFail(CommonMessageEnum.SERVERERR));
					try {
						Integer uid = (Integer) request.getAttribute("uid");
						Integer sid = Integer.valueOf(formData.get("sid"));
						String type = formData.get("type");
						String extraString = formData.get("extra");
						if ("2".equals(type) && extraString == null) {
							result = GsonUtil.GsonString(ResultUtil.getFail(CommonMessageEnum.PARAM_LOST));
							ResponseHandle.wrappedResponse(asyncContext.getResponse(), result);
							asyncContext.complete();
							return;
						}
						Integer extra = null;
						if ("2".equals(type)) {
							extra = Integer.valueOf(extraString);
						}
						String isSelfMention = formData.get("isselfmention");
						Integer addressId = Integer.valueOf(formData.get("addressid"));
						Integer unixTime = Integer.valueOf(formData.get("unixtime"));
						result = CreateCVOrderServlet.this.handle(uid, sid, type, extra, isSelfMention, addressId,
								unixTime);
						ResponseHandle.wrappedResponse(asyncContext.getResponse(), result);
					} catch (Exception e) {
						e.printStackTrace();
						try {
							ResponseHandle.wrappedResponse(asyncContext.getResponse(), result);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						log.error("fail createorder: " + e.getMessage());
					}
					asyncContext.complete();
				});
			}

			@Override
			public void onError(Throwable t) {
				asyncContext.complete();
			}
		});

	}

	private String handle(Integer uid, Integer sid, String type, Integer extra, String isSelfMention,
			Integer addressId, Integer unixTime) {
		if (uid == 0) {
			return GsonUtil.GsonString(ResultUtil.getFail(CommonMessageEnum.USER_IS_NULL));
		}
	
		if (!cvOrderService.judgeHmShopownStatus(sid)) {
			return GsonUtil.GsonString(ResultUtil.getFail(CommonMessageEnum.SHOP_CHANGE));
		}
		Map<String, Object> reslutMap = null;
		try {
			reslutMap = cvOrderService.createOrder(uid, sid, type, extra, isSelfMention, addressId, unixTime);
		} catch (NDCException e) {
			if (e instanceof DeleteBasketExcpetion) {
				return GsonUtil.GsonString(ResultUtil.getFail(CommonMessageEnum.ORDER_ALREADYCREATE));
			}
			else if (e instanceof IntegralException) {
				return GsonUtil.GsonString(ResultUtil.getFail(CommonMessageEnum.USER_INTEGRAL_ERR));
			}else if (e instanceof CouponException) {
				return GsonUtil.GsonString(ResultUtil.getFail(CommonMessageEnum.USER_COUPON_ERR));
			}else if (e instanceof ClickException) {
				return GsonUtil.GsonString(ResultUtil.getFail(CommonMessageEnum.USER_CLICK_FARMING));
			}
			
		}

		return GsonUtil.GsonString(ResultUtil.getSuccess(reslutMap));
	}

}