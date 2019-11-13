package com.zhongjian.exception;

@SuppressWarnings("serial")
public class NDCException extends Exception {

	// 购物车清除异常
	public static class DeleteBasketExcpetion extends NDCException {

	}

	// 积分变动异常
	public static class IntegralException extends NDCException {

	}

	// 优惠券变动异常
	public static class CouponException extends NDCException {

	}
	
	// 刷单异常
	public static class ClickException extends NDCException {

	}

}
