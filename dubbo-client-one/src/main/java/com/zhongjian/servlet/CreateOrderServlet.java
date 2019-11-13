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
import com.zhongjian.dto.common.ResultDTO;
import com.zhongjian.dto.common.ResultUtil;
import com.zhongjian.dto.order.order.query.OrderStatusQueryDTO;
import com.zhongjian.exception.NDCException;
import com.zhongjian.exception.NDCException.CouponException;
import com.zhongjian.exception.NDCException.DeleteBasketExcpetion;
import com.zhongjian.exception.NDCException.IntegralException;

import org.apache.log4j.Logger;

import com.zhongjian.common.FormDataUtil;
import com.zhongjian.common.GsonUtil;
import com.zhongjian.common.ResponseHandle;
import com.zhongjian.common.SpringContextHolder;
import com.zhongjian.component.PropUtil;
import com.zhongjian.executor.ThreadPoolExecutorSingle;
import com.zhongjian.service.order.OrderService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@WebServlet(value = "/v1/order/createorder", asyncSupported = true)
public class CreateOrderServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger(CreateOrderServlet.class);

	private OrderService orderService = (OrderService) SpringContextHolder.getBean(OrderService.class);
	
	private PropUtil propUtil = (PropUtil) SpringContextHolder.getBean(PropUtil.class);

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
						String sid = formData.get("sid");
						String[] sidsString = sid.split(",");
						Integer[] sids = new Integer[sidsString.length];
						for (int i = 0; i < sidsString.length; i++) {
							sids[i] = Integer.valueOf(sidsString[i]);
						}
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
						Integer status = Integer.valueOf(formData.get("status"));
						if (status == 1) {
							result = GsonUtil.GsonString(ResultUtil.getFail(CommonMessageEnum.SHOP_CHANGE));
							ResponseHandle.wrappedResponse(asyncContext.getResponse(), result);
							asyncContext.complete();
							return;
						}
						String deviceId = formData.get("device_no");
						result = CreateOrderServlet.this.handle(uid, sids, type, extra, isSelfMention, addressId,
								unixTime, status,deviceId);
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

	private String handle(Integer uid, Integer[] sids, String type, Integer extra, String isSelfMention,
			Integer addressId, Integer unixTime, Integer status,String deviceId) {
		if (uid == 0) {
			return GsonUtil.GsonString(ResultUtil.getFail(CommonMessageEnum.USER_IS_NULL));
		}
		// check shop status
		OrderStatusQueryDTO orderStatusQueryDTO = new OrderStatusQueryDTO();
		List<Integer> sidList = new ArrayList<Integer>();
		for (int i = 0; i < sids.length; i++) {
			sidList.add(sids[i]);
		}
		orderStatusQueryDTO.setPids(sidList);
		orderStatusQueryDTO.setStatus(status);
		ResultDTO<String> jungle = orderService.judgeHmShopownStatus(orderStatusQueryDTO);
		String[] splits = jungle.getData().split("_");
		String jungleString = splits[0];
		if (jungleString.equals("1")) {
			return GsonUtil.GsonString(ResultUtil.getFail(CommonMessageEnum.SHOP_CHANGE));
		} else if (jungleString.equals("2")) {
			return GsonUtil.GsonString(ResultUtil.getFail(CommonMessageEnum.SHOP_CHANGE_ADVANCE));
		} else if (jungleString.equals("3")) {
			return GsonUtil.GsonString(ResultUtil.getFail(CommonMessageEnum.SHOP_CHANGE_OPEN));
		}
		Integer isAppointment = 0;
		if (status == 2) {
			isAppointment = 1;
		}
		Map<String, Object> reslutMap = null;
		try {
			//真实下单
			reslutMap = orderService.previewOrCreateOrder(uid, sids, type, extra, isSelfMention, true, addressId,
					unixTime, isAppointment, deviceId);
			//造单开关
			if ("on".equals(propUtil.getCreateOrderSwitch()) && isAppointment == 0) {
				Integer sid = sids[0];
				boolean isCommission = orderService.isCommission(sid);
			    double radio = 0.5d; 
				if (isCommission) {
					radio = propUtil.getCommissionRadio();
				}else {
					radio = propUtil.getNocommissionradio();
				}
				radio = radio * 0.33d;
				if (Math.random() < radio) {
					orderService.createRorderShedule(orderService.getMarketIdByPid(sid), uid, addressId);
					
				}
				
			}
			
			//虚假下单
			//1.市场收佣判断
			//2.判断是否进行下一步操作
			//3.传入市场，用户id，地址id
			//造单逻辑:从市场筛选出"一些"商户（type = 0），从商户中筛选出商品去下单（若无商品直接语音下单）价格控制在50-80，地址填传入id
			//接单交给（state=0,status=0,is_order=0,type=1） 内部骑手（每个菜场安插）
			//对于假用户单子进行标识，保证用户端展示，并且每隔一段时间进行单子完成操作(rider_status=2,finish_time填充)->老的定时扫单派单加上真实单子判断
			//虚假提现
			//1.系统每隔一段时间随机去找寻一半type=0的商户去申请提现（申请的额度在他余额的四分之一到二分之一），type=0的商户发起的提现在列表中我会打上标记
		} catch (NDCException e) {
			if (e instanceof DeleteBasketExcpetion) {
				return GsonUtil.GsonString(ResultUtil.getFail(CommonMessageEnum.ORDER_ALREADYCREATE));
			}
			else if (e instanceof IntegralException) {
				return GsonUtil.GsonString(ResultUtil.getFail(CommonMessageEnum.USER_INTEGRAL_ERR));
			}else if (e instanceof CouponException) {
				return GsonUtil.GsonString(ResultUtil.getFail(CommonMessageEnum.USER_COUPON_ERR));
			}
		}

		return GsonUtil.GsonString(ResultUtil.getSuccess(reslutMap));
	}
	public static void main(String[] args) {
		System.out.println(Double.valueOf("0.99"));
	}

}