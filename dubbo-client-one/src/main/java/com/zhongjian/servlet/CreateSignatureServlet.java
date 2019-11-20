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
import org.apache.log4j.Logger;

import com.zhongjian.common.FormDataUtil;
import com.zhongjian.common.GsonUtil;
import com.zhongjian.common.ResponseHandle;
import com.zhongjian.common.SpringContextHolder;
import com.zhongjian.executor.ThreadPoolExecutorSingle;
import com.zhongjian.service.order.OrderService;
import com.zhongjian.service.pay.GenerateSignatureService;
import com.zhongjian.service.user.UserService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet(value = "/v1/pay/createsign", asyncSupported = true)
public class CreateSignatureServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger(CreateSignatureServlet.class);

	private OrderService orderService = (OrderService) SpringContextHolder.getBean(OrderService.class);

	private UserService userService = (UserService) SpringContextHolder.getBean(UserService.class);
	
	private GenerateSignatureService generateSignatureService = (GenerateSignatureService) SpringContextHolder
			.getBean(GenerateSignatureService.class);

	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Map<String, String> formData = FormDataUtil.getFormData(request);
		AsyncContext asyncContext = request.startAsync();
		ServletInputStream inputStream = request.getInputStream();
		
		inputStream.setReadListener(new ReadListener() {
			@Override
			public void onDataAvailable() throws IOException {
			}
			
			private String getRealIp(HttpServletRequest request) {
				String ip = request.getHeader("x-forwarded-for");
				if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
					ip = request.getHeader("Proxy-Client-IP");
				}
				if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
					ip = request.getHeader("WL-Proxy-Client-IP");
				}
				if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
					ip = request.getRemoteAddr();
				}
				return ip;
			}
			
			@Override
			public void onAllDataRead() {
				ThreadPoolExecutorSingle.executor.execute(() -> {
					String result =  GsonUtil.GsonString(ResultUtil.getFail(CommonMessageEnum.SERVERERR));
					try {
					Integer uid = (Integer) request.getAttribute("uid");
					String business = formData.get("business") == null?"RIO":formData.get("business");
					Integer orderid = Integer.valueOf(formData.get("roid"));
					// 0 支付宝 1微信 2微信小程序
					String payType = formData.get("type");
					String realIp = this.getRealIp(request);
					result = CreateSignatureServlet.this.handle(uid, business, orderid, payType, realIp);
					// 返回数据
					if ("flpay".equals(payType)) {
						Map<String, Object> data = GsonUtil.GsonToMaps(result);
						request.setAttribute("Plain", data.get("Plain"));
						request.setAttribute("Signature", data.get("Signature"));
						request.getRequestDispatcher("/fl.jsp").forward(request, asyncContext.getResponse());
					}else {
						ResponseHandle.wrappedResponse(asyncContext.getResponse(), result);
					}
					
						
					} catch (IOException e) {
						try {
							ResponseHandle.wrappedResponse(asyncContext.getResponse(), result);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						log.error("fail createsign: " + e.getMessage());
					} catch (ServletException e) {
						e.printStackTrace();
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

	private String handle(Integer uid, String business, Integer orderId, String payType,
			String realIp) {
		if (uid == 0) {
			return GsonUtil.GsonString(ResultUtil.getFail(CommonMessageEnum.USER_IS_NULL));
		}
		//线上地址校验
		if (!orderService.orderCheck(orderId, business)) {
			return GsonUtil.GsonString(ResultUtil.getFail(CommonMessageEnum.ORDER_ADDRESSFAIL));
		}
		
		Map<String, Object> orderDetail = orderService.getOutTradeNoAndAmount(uid, orderId, business);
		if (orderDetail == null) {
			return GsonUtil.GsonString(ResultUtil.getFail(CommonMessageEnum.ORDER_CHANGE));
		} else {
			String out_trade_no = (String) orderDetail.get("out_trade_no");
			String totalPrice = String.valueOf(orderDetail.get("totalPrice"));
			String body = (String) orderDetail.get("body");
			String subject = (String) orderDetail.get("subject");
			HashMap<String, Object> result = new HashMap<>();
			result.put("roid", String.valueOf(orderId));
			result.put("riderSn", out_trade_no);
			result.put("type", payType);
			if ("aliwap".equals(payType)) {
				//支付宝
				String sign = generateSignatureService.getAliSignature(out_trade_no, totalPrice,subject);
				result.put("sign", sign);
				ResultDTO<Object> success = ResultUtil.getSuccess(result);
				success.setFlag(null);
				success.setTotal(null);
				return  GsonUtil.GsonString(success);
			} else if ("wechat".equals(payType)) {
				//微信app支付
				result.put("sign", generateSignatureService.getWxAppSignature(out_trade_no, totalPrice, "", realIp, 0,body));
				ResultDTO<Object> success = ResultUtil.getSuccess(result);
				success.setFlag(null);
				success.setTotal(null);
				return GsonUtil.GsonString(success);
			} else if ("applets".equals(payType)) {
				//小程序支付
				String appletsOpenid = userService.getUserBeanById(uid).getAppletsOpenid();
				if (appletsOpenid == null) {
					return GsonUtil.GsonString(ResultUtil.getFail(null));
				}
				result.put("sign", generateSignatureService.getWxAppSignature(out_trade_no, totalPrice, appletsOpenid, realIp, 1,body));
				ResultDTO<Object> success = ResultUtil.getSuccess(result);
				success.setFlag(null);
				success.setTotal(null);
				return GsonUtil.GsonString(success);
				//丰联支付
			}else if ("flpay".equals(payType)) {
				return GsonUtil.GsonString(generateSignatureService.getFlPayData(out_trade_no, totalPrice, subject));
				
			}else {
				//微信银行支付
				return generateSignatureService.getYinHangWxApp(out_trade_no, totalPrice, realIp,body);
			}
		}
	}



}