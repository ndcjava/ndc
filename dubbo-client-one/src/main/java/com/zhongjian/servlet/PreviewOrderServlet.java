package com.zhongjian.servlet;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zhongjian.dto.cart.address.query.CartAddressQueryDTO;
import com.zhongjian.dto.cart.address.result.CartAddressResultDTO;
import com.zhongjian.dto.common.CommonMessageEnum;
import com.zhongjian.dto.common.ResultDTO;
import com.zhongjian.dto.common.ResultUtil;
import com.zhongjian.dto.order.order.query.OrderStatusQueryDTO;
import com.zhongjian.exception.NDCException;

import org.apache.log4j.Logger;

import com.zhongjian.common.FormDataUtil;
import com.zhongjian.common.GsonUtil;
import com.zhongjian.common.ResponseHandle;
import com.zhongjian.common.SpringContextHolder;
import com.zhongjian.executor.ThreadPoolExecutorSingle;
import com.zhongjian.service.address.AddressService;
import com.zhongjian.service.order.OrderService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@WebServlet(value = "/v1/order/previeworder", asyncSupported = true)
public class PreviewOrderServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger(PreviewOrderServlet.class);

	private OrderService orderService = (OrderService) SpringContextHolder.getBean(OrderService.class);

	private AddressService addressServie = (AddressService) SpringContextHolder.getBean(AddressService.class);
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
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
						//sid逗号隔开
						//type默认0 
						//extra在type为2是必传
						//isselfmention默认0
						//status商户状态
						Integer uid = (Integer) request.getAttribute("uid");
						String sid = formData.get("sid");
						String[] sidsString = sid.split(",");
						Integer[] sids = new Integer[sidsString.length];//sid数组传给service
						for (int i = 0; i < sidsString.length; i++) {
							sids[i] = Integer.valueOf(sidsString[i]);
						}
						String type = formData.get("type")==null?"0":formData.get("type");
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
						String isSelfMention = formData.get("isselfmention") == null?"0":formData.get("isselfmention");
						Integer status = Integer.valueOf(formData.get("status"));
						if (status == 1) {
							 result = GsonUtil.GsonString(ResultUtil.getFail(CommonMessageEnum.SHOP_CHANGE));
							 ResponseHandle.wrappedResponse(asyncContext.getResponse(), result);
							 asyncContext.complete();
							 return;
						}
						result = PreviewOrderServlet.this.handle(uid,sids,type,extra,isSelfMention,status);
						ResponseHandle.wrappedResponse(asyncContext.getResponse(), result);
					} catch (Exception e) {
						e.printStackTrace();
						try {
							ResponseHandle.wrappedResponse(asyncContext.getResponse(), result);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						log.error("fail previeworder: " + e.getMessage());
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
			Integer status) throws NDCException {
		if (uid == 0) {
			return GsonUtil.GsonString(ResultUtil.getFail(CommonMessageEnum.USER_IS_NULL));
		}
		//check shop status
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
		}else if (jungleString.equals("2")) {
			return GsonUtil.GsonString(ResultUtil.getFail(CommonMessageEnum.SHOP_CHANGE_ADVANCE));
		}else if (jungleString.equals("3")){
			return GsonUtil.GsonString(ResultUtil.getFail(CommonMessageEnum.SHOP_CHANGE_OPEN));
		}
		String marketIdString = splits[1];
		Integer isAppointment = 0;
		if (status == 2) {
			isAppointment = 1;
		}
		//请求服务获取预览订单数据--start
		String serverTime = orderService.previewOrderTime(isAppointment);
		CartAddressQueryDTO cartAddressQueryDTO = new CartAddressQueryDTO();
		cartAddressQueryDTO.setId(0);
		cartAddressQueryDTO.setUid(uid);
		cartAddressQueryDTO.setMarketId(Integer.valueOf(marketIdString));
		CartAddressResultDTO address = addressServie.previewOrderAddress(cartAddressQueryDTO);
		Map<String, Object> orderDetail = orderService.previewOrCreateOrder(uid, sids, type, extra, isSelfMention, false, null, null, isAppointment,null);
		//请求服务获取预览订单数据--end
		orderDetail.put("serverTime", serverTime);
		orderDetail.put("address", address);
		return GsonUtil.GsonString(ResultUtil.getSuccess(orderDetail));
	}
}