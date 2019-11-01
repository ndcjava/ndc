package com.zhongjian.dao.entity.order.sf;

import java.util.List;

import lombok.Data;

@Data
public class SFOrderDetail {

	private Integer total_price;
	
	private Integer product_type;
	
	private Integer weight_gram;
	
	private Integer product_num;
	
	private Integer product_type_num;
	
	private List<SFProductDetail> product_detail;
}
