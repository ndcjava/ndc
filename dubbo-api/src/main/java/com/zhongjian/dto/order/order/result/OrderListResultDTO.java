package com.zhongjian.dto.order.order.result;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: ldd
 */
@Data
public class OrderListResultDTO implements Serializable{

    private static final long serialVersionUID = -2548785476444048539L;

    /**
     * 订单id
     */
    private Integer oid;

    /**
     * 商户名称
     */
    private String sname;

    /**
     * 订单价格
     */
    private String totalPrice;

    /**
     *商品订单原价，当实际价格与原价相同，显示为空
     */
    private String orderTotal;

    /**
     * 商品订单实际价格
     */
    private String orderPayment;
}
