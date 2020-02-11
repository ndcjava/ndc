package com.zhongjian.dao.jdbctemplate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserDao {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	public boolean updateOpenIdByUid(Integer uid,String openId) {
		String sql = "update hm_user set fl_openid = ? where id = ?";
		return jdbcTemplate.update(sql, openId,uid) > 0 ? true : false;
	}
}
