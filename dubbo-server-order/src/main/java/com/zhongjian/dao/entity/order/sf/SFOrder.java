package com.zhongjian.dao.entity.order.sf;

import lombok.Data;

@Data
public class SFOrder {

	private String dev_id;
	
	private String shop_id;
	
	private Integer shop_type;
	
	private String order_source;
	
	private String shop_order_id;
	
	private Integer lbs_type;
	
	private Integer pay_type;
	
	private Integer order_time;
	
	private Integer is_appoint;
	
	private Integer expect_time;
	
	private Integer push_time;
	
	private Integer version;
	
	private Integer return_flag;
	
	private Integer is_insured;
	
	private SFReceive receive;
	
	private SFOrderDetail order_detail;
	
}
