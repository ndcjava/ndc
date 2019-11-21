package com.zhongjian.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zhongjian.util.XmlUtil;

import org.apache.log4j.Logger;

import com.zhongjian.common.SpringContextHolder;
import com.zhongjian.service.order.OrderService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@WebServlet(value = "/v1/notify/flapp", asyncSupported = false)
public class FLServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger(FLServlet.class);

	
	private OrderService orderService = (OrderService) SpringContextHolder.getBean(OrderService.class);

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		log.info("丰联回调开始");
		InputStream is = null;
		try {
			is = request.getInputStream();
		    Map<String, String> orderDetail = XmlUtil.flGetOrderNo(is);
		    log.info(orderDetail);
//			orderService.handleROrder(orderDetail.get("out_trade_no"), orderDetail.get("total"),"flpay");
			response.getWriter().print("success");
		} catch (Exception e) {
			log.error("丰联回调发生异常，请注意处理 " + e);
			response.getWriter().print("failure");
		}

	}

}