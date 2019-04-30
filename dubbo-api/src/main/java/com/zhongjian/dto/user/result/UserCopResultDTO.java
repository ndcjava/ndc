package com.zhongjian.dto.user.result;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.google.gson.annotations.Expose;

/**
 * @Author: ldd
 */
@Data
public class UserCopResultDTO implements Serializable {

    private static final long serialVersionUID = -1728911295677881024L;


    /**
     * 主键id
     */
    private Integer id;

    /**
     * 满减值
     */
    @Expose(serialize =  false)
    private BigDecimal payFull;

    /**
     * 优惠金额
     */
    private BigDecimal coupon;

    /**
     * 开始时间
     */
    @Expose(serialize =  false)
    private Integer starttime;

    /**
     * 结束时间
     */
    @Expose(serialize =  false)
    private Integer endtime;

    /**
     * 开始时间
     */
    private String start_Time;

    /**
     * 结束时间
     */
    private String end_Time;



    /**
     * 优惠券类型0满减券1配送券2全场通用
     */
    private Integer type;

    /**
     * 菜场id
     */
    private String marketId;

    /**
     * 状态
     */
    private Integer state;

    /**
     * 内容
     */
    private String content;




}