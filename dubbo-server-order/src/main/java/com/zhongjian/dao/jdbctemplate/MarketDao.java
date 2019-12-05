package com.zhongjian.dao.jdbctemplate;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MarketDao {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	public Map<String, Object> getMarketActivtiy(Integer marketId) {
		String sql = "SELECT type,rule,up_limit from hm_market_activity where marketid = ? and `status` = 1";
		Map<String, Object> resMap = null;
		try {
			resMap = jdbcTemplate.queryForMap(sql, marketId);
		} catch (EmptyResultDataAccessException e) {
		}
		return resMap;
	}
	
	public Map<String, Object> getStartAndEnd(Integer marketId) {
		String sql = "select starttime,endtime from hm_market where id = ?";
		Map<String, Object> resMap = null;
		try {
			resMap = jdbcTemplate.queryForMap(sql, marketId);
		} catch (EmptyResultDataAccessException e) {
		}
		return resMap;
	}
	
	public Integer isMarketCanBy(Integer marketId) {
		String sql = "SELECT can_buy from hm_market where id = ?";
		return jdbcTemplate.queryForObject(sql, new Object[] { marketId }, Integer.class);
	}

}
