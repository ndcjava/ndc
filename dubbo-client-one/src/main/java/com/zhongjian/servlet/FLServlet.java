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
import java.util.TreeMap;

@WebServlet(value = "/v1/notify/flapp", asyncSupported = false)
public class FLServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger(FLServlet.class);

	private PropUtil propUtil = (PropUtil) SpringContextHolder.getBean(PropUtil.class);
	
	private OrderService orderService = (OrderService) SpringContextHolder.getBean(OrderService.class);

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		log.info("丰联回调开始");
		InputStream is = null;
		try {
			is = request.getInputStream();
			String xml = PayCommonUtil.inputStream2String(is, "UTF-8");
			log.info(xml);
			TreeMap<String, String> notifyMap = new TreeMap<String, String>(PayCommonUtil.xmlToMap(xml));
			log.info(notifyMap);
		} catch (Exception e) {
			log.error("微信异步通知发生异常，请注意处理 " + e);
			response.getWriter().print("failure");
		}

	}

}