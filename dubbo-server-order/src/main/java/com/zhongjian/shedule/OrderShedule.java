package com.zhongjian.shedule;



import java.net.URISyntaxException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.zhongjian.commoncomponent.TaskBase;
import com.zhongjian.localservice.OrderService;

@Component
public class OrderShedule extends TaskBase implements ApplicationListener<ContextRefreshedEvent> {

@Autowired
private OrderService orderService;



void flOrderShedule() {
	// 定时执行任务，每隔1分钟钟执行一次
	executeShedule(new Runnable() {
		@Override
		public void run() {
			orderService.toHandleFlOrder();
		}
	}, 0, 60);
}

	void orderShedule() {
		// 定时执行任务，每隔5分钟钟执行一次
		executeShedule(new Runnable() {
			@Override
			public void run() {
				orderService.todoSth();
			}
		}, 0, 300);
	}
	
	void withdraw() {
		// 定时执行任务，每天执行一次
		executeShedule(new Runnable() {
			@Override
			public void run() {
				try {
					orderService.withdraw();
				} catch (URISyntaxException e) {
				}
			}
		}, 0, 14 * 60);
	}

	//延时处理订单
	public void delayHandleCVOrder(Integer orderId,Integer time) {
		if (orderService.getDelieverModel(orderId) == 2) {
			orderService.lockDistributeOrder(orderId);
			return;
		}
		// 延时60s把订单改为平台处理
		executeDelayShedule(new Runnable() {
			@Override
			public void run() {
				orderService.changeAndDistributeOrder(orderId);
			}
		},  time);
	}
	

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		this.orderShedule();
//		this.withdraw();
	}
	
	
	//检查订单是否有金额错误
	public void checkCVOrder(Integer orderId) {
		// 延时60s把订单改为平台处理
		executeDelayShedule(new Runnable() {
			@Override
			public void run() {
				orderService.checkCVOrder(orderId);
			}
		},  5);
	}
	
	//延时处理造假订单
	public void delayCreateOrder(Integer marketid, Integer uid, Integer addressid) {
		// 延时60s把订单改为平台处理
		executeDelayShedule(new Runnable() {
			@Override
			public void run() {
				orderService.createFalseRorder(marketid, uid, addressid);
			}
		},   24 * 60 * 60);
	}
	
	//延时处理造假订单
	public void delayFinishOrder(Integer roid ,String login_token) {
		// 延时60s把订单改为平台处理
		executeDelayShedule(new Runnable() {
			@Override
			public void run() {
				orderService.finishRorder(roid, login_token);
			}
		},  48 * 60);
	}
	
}
