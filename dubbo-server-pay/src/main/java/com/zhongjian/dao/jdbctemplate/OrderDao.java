package com.zhongjian.dao.jdbctemplate;
import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.zhongjian.util.DateUtil;



@Repository
public class OrderDao {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	public boolean addOrder(BigDecimal totalPrice,String orderNo) {
		String sql = "INSERT INTO `hm_fl_order` (order_no,total_price,query_time,num) values (?,?,?,?)";
		return jdbcTemplate.update(sql, orderNo,totalPrice,DateUtil.getCurrentTime() + 10,0) > 0 ?true:false;
	}
}
