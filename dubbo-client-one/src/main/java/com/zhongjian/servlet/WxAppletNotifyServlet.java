package com.zhongjian.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zhongjian.util.PayCommonUtil;

import org.apache.log4j.Logger;

import com.zhongjian.common.SpringContextHolder;
import com.zhongjian.component.PropUtil;
import com.zhongjian.service.order.OrderService;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.TreeMap;

@WebServlet(value = "/v1/notify/wxapplet", asyncSupported = false)
public class WxAppletNotifyServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger(WxAppletNotifyServlet.class);

	private PropUtil propUtil = (PropUtil) SpringContextHolder.getBean(PropUtil.class);

	private OrderService orderService = (OrderService) SpringContextHolder.getBean(OrderService.class);
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		InputStream is = null;
		try {
			is = request.getInputStream();
			String xml = PayCommonUtil.inputStream2String(is, "UTF-8");
			TreeMap<String, String> notifyMap = new TreeMap<String, String>(PayCommonUtil.xmlToMap(xml));
			//验签
			String transSign = notifyMap.get("sign");
			notifyMap.remove("sign");
			String localSign = PayCommonUtil.createSign("UTF-8", notifyMap, propUtil.getWxAppletKey());
			if (!transSign.equals(localSign)) {
				response.getWriter().write(
						"<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[sign]]></return_msg></xml>");
				log.error("wechat sign is wrong!");
				return;
			}
			String returnCode = notifyMap.get("return_code");
			String resultCode = notifyMap.get("result_code");
			if (returnCode.equals("SUCCESS") && resultCode.equals("SUCCESS")) {
				String tradeNo = notifyMap.get("out_trade_no");
				String totalAmount = notifyMap.get("total_fee");
				BigDecimal totalAmountBigDecimal = new BigDecimal(totalAmount);
				String transTotalAmount = totalAmountBigDecimal.divide(new BigDecimal(100)).setScale(2, BigDecimal.ROUND_HALF_UP).toString();
				if (orderService.handleROrder(tradeNo, transTotalAmount,"applets")) {
					response.getWriter().write(
							"<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>");
						return;
				}else {
					response.getWriter().write(
							"<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[SEVERERR]]></return_msg></xml>");
					return;
				}
				
			} else {
				response.getWriter().write(
						"<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[SEVERERR]]></return_msg></xml>");
				return;
			}
		} catch (Exception e) {
			log.error("微信异步通知发生异常，请注意处理 " + e);
			response.getWriter().print("failure");
		}

	}

}