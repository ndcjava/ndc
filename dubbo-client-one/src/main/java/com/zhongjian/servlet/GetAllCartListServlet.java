package com.zhongjian.servlet;

import com.zhongjian.common.GsonUtil;
import com.zhongjian.common.ResponseHandle;
import com.zhongjian.common.SpringContextHolder;
import com.zhongjian.dto.common.CommonMessageEnum;
import com.zhongjian.dto.common.ResultUtil;
import com.zhongjian.executor.ThreadPoolExecutorSingle;
import com.zhongjian.service.cart.shopown.CartShopownService;
import org.apache.log4j.Logger;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @Author: ldd
 */
@WebServlet(value = "/v1/cart/getAllCartList", asyncSupported = true)
public class GetAllCartListServlet extends HttpServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger(GetAllCartListServlet.class);

	private CartShopownService cartShopownService = (CartShopownService) SpringContextHolder
			.getBean(CartShopownService.class);

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

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
						result = GetAllCartListServlet.this.handle(uid);
						// 返回数据
						ResponseHandle.wrappedResponse(asyncContext.getResponse(), result);
					} catch (Exception e) {
						try {
							ResponseHandle.wrappedResponse(asyncContext.getResponse(), result);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						log.error("fail cart/getAllCartList : " + e.getMessage());
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

	private String handle(Integer uid) {
		if (uid == 0) {
			return GsonUtil.GsonString(ResultUtil.getFail(CommonMessageEnum.USER_IS_NULL));
		}
		cartShopownService.deleteGoodsOnShelves(uid, false);
		return GsonUtil.GsonString(cartShopownService.queryList(uid));
	}
}
