package com.zhongjian.dto.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Project Name: kc-risk
 * Package Name: cn.kingcar.risk.biz.common.constant.enums.response
 * Function: 公共响应消息枚举
 * user: San
 * Date:2018/2/5
 */
@AllArgsConstructor
public enum CommonMessageEnum {

    /**
     * 响应消息枚举列表
     */
    SUCCESS("通用成功", 10000),
    FAIL("通用失败", 10001),
    PARAM_LOST("参数缺失", 10008),
    USER_IS_NULL("用户不存在", 11001),
    USER_INTEGRAL_ERR("积分不够", 11008),
    USER_COUPON_ERR("优惠券已失效", 11009),
    USER_CLICK_FARMING("您的行为可能涉及恶意刷单", 11010),
    SHOP_CHANGE("商户状态变化", 12001),
    SHOP_CHANGE_OPEN("商户状态变化为开张", 12000),
    SHOP_CHANGE_ADVANCE("商户状态变化为预约", 12002),
    ORDER_CHANGE("订单状态变化", 13001),
    ORDER_ALREADYCREATE("您已生成订单", 13002),
    ORDER_ALREADYCANCEL("订单已取消", 13003),
    ORDER_ADDRESSFAIL("订单地址检验失败", 13004),
    SERVERERR("服务异常", 20000),
    NO_PERMISION("您没有权限执行此操作", 50000),
    ;

    private @Getter
    String msg;
    private @Getter
    int code;

}
