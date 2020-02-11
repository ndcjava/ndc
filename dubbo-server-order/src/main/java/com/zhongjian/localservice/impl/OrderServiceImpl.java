package com.zhongjian.localservice.impl;

import com.csii.upay.api.factory.CSIIBeanFactory;
import com.csii.upay.api.net.Client;
import com.csii.upay.api.request.IQSRRequest;
import com.csii.upay.api.response.IQSRResponse;
import com.mysql.cj.x.protobuf.MysqlxCrud.Order;
import com.zhongjian.commoncomponent.PropUtil;
import com.zhongjian.dao.entity.order.rider.OrderRiderOrderBean;
import com.zhongjian.dao.framework.impl.HmBaseService;
import com.zhongjian.dao.jdbctemplate.CVOrderDao;
import com.zhongjian.dao.jdbctemplate.OrderDao;
import com.zhongjian.localservice.OrderService;
import com.zhongjian.task.OrderTask;
import com.zhongjian.util.DateUtil;
import com.zhongjian.util.DistributedLock;
import com.zhongjian.util.HttpConnectionPoolUtil;
import com.zhongjian.util.LogUtil;

import org.apache.http.client.methods.HttpPost;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.time.LocalTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service("localOrderService")
public class OrderServiceImpl extends HmBaseService<OrderRiderOrderBean, Integer> implements OrderService {


    @Resource
    private com.zhongjian.service.order.OrderService orderService;
    
    @Resource
    private CVOrderDao cvOrderDao;
    
    @Resource
    private PropUtil propUtil;
    
    @Resource
    private DistributedLock zkLock;
    
    @Resource
    private OrderTask orderTask;
    
    @Resource
    private OrderDao orderDao;
    
    
    @Override
    public void todoSth() {

        List<Integer> ids = this.dao.executeListMethod(null, "findRiderOrder", Integer.class);
       
        LogUtil.info("定时任务" , DateUtil.formateDate(new Date(),DateUtil.DateMode_4) + ":处理超时订单任务开始");
        for (Integer id : ids) {
            orderService.cancelOrder(id);
        }
        LogUtil.info("定时任务" , DateUtil.formateDate(new Date(),DateUtil.DateMode_4) + ":处理超时订单任务结束");
    }

	@Override
	public boolean changeDelieverModel(Integer uoid) {
		if (cvOrderDao.changeModelToTwo(uoid)) {
			return true;
		} 
		if (cvOrderDao.getDeliverModelByUoid(uoid) == 2) {
			return true;
		}
		return false;
	}

	
	
	
	@Override
	@Transactional
	public void distributeOrder(Integer uoid,boolean isFirst,int time) {
		//派单的前置条件rid等于0
		if (time > 8) {
			//超过八次直接拒绝（菜场无骑手）
			return;
		}
		if (!isFirst) {
			try {
				Thread.sleep(60 * 5 * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (cvOrderDao.getRidFromCVOrder(uoid) != 0) {
			return;
		}
		Integer marketId = 0;
		// 查出makertid 
		marketId = cvOrderDao.getMarketIdByCVOrder(uoid);
		if (marketId == 0) {
			return;
		}
		//派单逻辑
		Integer rid = orderService.getRidFormMarket(marketId, "least");
		if (rid != -1) {
			cvOrderDao.updateRidOfHmCVOrder(rid, uoid);
			cvOrderDao.deleteWaitdeliverOrder(uoid);
			cvOrderDao.addOrUpdateRiderNum(rid);
			orderTask.handleCVOrderPushRider(rid);
		}else {
		//重新派单
				distributeOrder(uoid,false,time ++);
		}
		
	}
	
	public void changeAndDistributeOrder(Integer uoid) {
		if (this.changeDelieverModel(uoid)) {
			this.lockDistributeOrder(uoid);
		}
	}

	@Override
	public Integer getDelieverModel(Integer uoid) {
		return cvOrderDao.getDeliverModelByUoid(uoid);
	}

	@Override
	public List<Integer> queryWaitdeliverOrderList() {
		return cvOrderDao.queryWaitdeliverOrderList(propUtil.getDatacenterId());
	}

	@Override
	public void lockDistributeOrder(Integer uoid) {
		String lockName = null;
		try {
			//zookeeper加锁(针对uid加锁)
			try {
				lockName = zkLock.lock(uoid + "");
			} catch (Exception e) {
				e.printStackTrace();
			}
			this.distributeOrder(uoid,true,1);
		} finally {
			//zookeeper解锁
			try {
				zkLock.unlock(lockName);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void checkCVOrder(Integer orderId) {
		//人工智能自行纠错
		Map<String, Object> cvOrderDetail = cvOrderDao.getCVOrderDetail(orderId);
		BigDecimal integralPrice = (BigDecimal) cvOrderDetail.get("integralPrice");
		BigDecimal vipRelief = (BigDecimal) cvOrderDetail.get("vip_relief");
		BigDecimal couponPrice = (BigDecimal) cvOrderDetail.get("coupon_price");
		BigDecimal originalPrice = (BigDecimal) cvOrderDetail.get("originalPrice");
		BigDecimal totalPrice = (BigDecimal) cvOrderDetail.get("totalPrice");
		BigDecimal deliverFee = (BigDecimal) cvOrderDetail.get("deliver_fee");
		BigDecimal checkTotalPrice = originalPrice.subtract(vipRelief).subtract(couponPrice).subtract(integralPrice).add(deliverFee).setScale(2, BigDecimal.ROUND_HALF_UP);
		totalPrice = totalPrice.setScale(2, BigDecimal.ROUND_HALF_UP);
		if (totalPrice.compareTo(checkTotalPrice) != 0) {
			//有错误
			cvOrderDao.setCVOrderError(orderId);
		}
		
	}

	@Override
	public void createFalseRorder(Integer marketid, Integer uid, Integer addressid) {
		orderService.createFalseRorder(marketid, uid, addressid);		
	}

	@Override
	public void finishRorder(Integer roid, String login_token) {
		HttpPost httpPost = new HttpPost(propUtil.getOrderFinishUrl());
		httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
		HashMap hashMap = new HashMap();
		hashMap.put("login_token", login_token);
		hashMap.put("roid", String.valueOf(roid));
		hashMap.put("status", "2");
		HttpConnectionPoolUtil.post(propUtil.getOrderFinishUrl(), httpPost, hashMap);
		
	}

	@Override
	public void withdraw() throws URISyntaxException {
		int hour = LocalTime.now().getHour();
		if (hour < 20 && hour > 8) {
			orderService.withdraw();
		}
		//判断时间是否在早上8-晚上20
		//是则继续执行
		//找寻一个商户拿到login_token
		//查看商户余额，超过100发起提现,取三分之一整数作为money
		//查看提现列表为空则添加账号获取bankid
		//拿到login_token,bankid,money可以发起提现了！
	}

	@Override
	public void toHandleFlOrder() {
		List<Map<String, Object>> flOrders = orderDao.getHmFlOrder();
		Client client = CSIIBeanFactory.getInstance().getDefaultClient();
		IQSRResponse iqsrResponse = null;
       for (Iterator<Map<String, Object>> iterator = flOrders.iterator(); iterator.hasNext();) {
    	   Map<String, Object> map = (Map<String, Object>) iterator.next();
    	   String orderNo = (String) map.get("order_no");
    	   BigDecimal totalPrice = (BigDecimal) map.get("total_price");
    	   orderDao.updateFlOrderNum((Integer) map.get("id"));
    		IQSRRequest iqsrRequest = new IQSRRequest();
    		iqsrRequest.setMerchantId(propUtil.getMerchantId());
    		iqsrRequest.setSubMerchantId(propUtil.getSubMerchantId());
    		iqsrRequest.setMerSeqNo(orderNo);
    		iqsrRequest.setMerTransDate(new Date());
    		iqsrRequest.setMerTransAmt(totalPrice);
    		
    		try {
    			iqsrResponse = client.post(iqsrRequest);
    			if ("000000".equals(iqsrResponse.getRespCode())) {
    				if ("0001".equals(iqsrResponse.getTransStatus())) {
						orderService.handleROrder(orderNo, totalPrice.toPlainString(), "flpay");
					}
    			}
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	   
		}
	}
	

}
