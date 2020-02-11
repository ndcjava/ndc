package com.zhongjian.dao.jdbctemplate;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.zhongjian.dao.MongoDBDaoBase;
import com.zhongjian.dto.cart.storeActivity.result.CartStoreActivityResultDTO;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.zhongjian.util.DateUtil;

@Repository
public class OrderDao extends MongoDBDaoBase {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	// activity ----start
	public CartStoreActivityResultDTO getStoreActivtiy(Integer sid, BigDecimal amount) {
		String sql = "SELECT full,reduce,discount,type from hm_store_activity where sid="
				+ " ? and full <= ? and `enable` = 1 and is_delete = 0 AND examine = 2 ORDER BY full DESC LIMIT 1";
		RowMapper<CartStoreActivityResultDTO> rowMapper = new BeanPropertyRowMapper<>(CartStoreActivityResultDTO.class);
		CartStoreActivityResultDTO cartStoreActivityResultDTO = null;
		try {
			cartStoreActivityResultDTO = jdbcTemplate.queryForObject(sql, rowMapper, sid, amount);
		} catch (EmptyResultDataAccessException e) {
			// cartStoreActivityResultDTO = null;
		}
		return cartStoreActivityResultDTO;
	}

	// activity ----end

	public List<Map<String, Object>> getBasketByUidAndSid(Integer sid, Integer uid) {
		String sql = "SELECT hm_goods.price,hm_basket.amount,hm_basket.gid,hm_basket.unitPrice,"
				+ "hm_goods.gname,hm_goods.unit,hm_shopown.sname,"
				+ "hm_shopown.unFavorable,hm_shopown.marketid,hm_shopown.pid,"
				+ "hm_basket.price basketprice,hm_basket.remark from hm_basket LEFT JOIN hm_goods ON hm_basket.gid = "
				+ "hm_goods.id INNER JOIN hm_shopown on  hm_shopown.pid = hm_basket.sid where "
				+ "hm_basket.uid = ? and hm_basket.sid = ?";
		List<Map<String, Object>> resultList = jdbcTemplate.queryForList(sql, new Object[] { uid, sid });
		return resultList;
	}
	
	public List<Map<String, Object>> getHmFlOrder() {
		String sql = "SELECT order_no, total_price FROM hm_fl_order WHERE num < 3 AND query_time > 0";
		List<Map<String, Object>> resultList = jdbcTemplate.queryForList(sql);
		return resultList;
	}

	// delete baskets
	public boolean deleteBasketBySid(Integer[] sids, Integer uid) {
		StringBuilder sidString = new StringBuilder();
		for (Integer sid : sids) {
			if (sidString.length() == 0) {
				sidString.append(sid);
			} else {
				sidString.append("," + sid);
			}
		}
		String sql = "DELETE FROM hm_basket WHERE sid in (" + sidString + ") AND uid = ?";
		return jdbcTemplate.update(sql, uid) > 0 ? true : false;
	}

	public boolean checkFirstOrderByUid(Integer uid) {
		String sql = "SELECT COUNT(1) FROM hm_rider_order where uid = ? AND ctime  > ? AND (pay_status = 0 or pay_status = 1) AND is_delete = 0";
		Integer num = jdbcTemplate.queryForObject(sql, new Object[] { uid, DateUtil.getTodayZeroTime() },
				Integer.class);
		return num > 0 ? false : true;
	}

	public boolean checkFirstPayOrderByUid(Integer uid) {
		String sql = "SELECT COUNT(1) FROM hm_rider_order where uid = ? AND ctime  > ? AND  pay_status = 1 and is_delete = 0";
		Integer num = jdbcTemplate.queryForObject(sql, new Object[] { uid, DateUtil.getTodayZeroTime() },
				Integer.class);
		return num > 0 ? true : false;
	}

	public Integer checkFirstToPayOrderByUid(Integer uid) {
		String sql = "SELECT id FROM hm_rider_order where uid = ? AND ctime  > ? AND  pay_status = 0 AND is_delete = 0";
		Integer orderId = null;
		try {
			orderId = jdbcTemplate.queryForObject(sql, new Object[] { uid, DateUtil.getTodayZeroTime() },
					Integer.class);
		} catch (Exception e) {
		}
		return orderId;
	}

	public Integer checkToPayNum(Integer uid) {
		String sql = "SELECT COUNT(1) FROM hm_rider_order where uid = ? AND ctime  > ? AND  pay_status = 0 AND is_delete = 0";
		Integer num = jdbcTemplate.queryForObject(sql, new Object[] { uid, DateUtil.getTodayZeroTime() },
				Integer.class);
		return num;
	}

	public boolean checkCouponOrderByUid(Integer uid) {
		String sql = "SELECT COUNT(1) FROM hm_rider_order where uid = ? AND ctime > ? AND (pay_status = 0 or pay_status = 1) AND couponid is not null AND is_delete = 0";
		Integer num = jdbcTemplate.queryForObject(sql, new Object[] { uid, DateUtil.getTodayZeroTime() },
				Integer.class);
		return num > 0 ? false : true;
	}

	// 查看优惠券数量
	public Integer getCouponsNumCanUse(Integer uid) {
		String sql = "SELECT COUNT(1) from hm_user_coupon where uid = ? and state = 0 and model = 1 and is_active = 1";
		Integer num = jdbcTemplate.queryForObject(sql, new Object[] { uid }, Integer.class);
		return num;
	}

	// 查看优惠券
	public Map<String, Object> getCouponInfo(Integer uid, Integer couponId) {
		String sql = "SELECT uc.price,uc.coupon mongoid from hm_user_coupon uc where"
				+ " uc.uid = ? and uc.id = ? and uc.state = 0";
		Map<String, Object> resMap = null;
		try {
			resMap = jdbcTemplate.queryForMap(sql, uid, couponId);
		} catch (EmptyResultDataAccessException e) {
			return resMap;
		}
		String couponMongoId = (String) resMap.get("mongoid");
		MongoCollection<Document> collection = getCollection("nidcai", "coupon");
		Document filter = new Document();
		filter.append("_id", new ObjectId(couponMongoId));
		List<Document> results = new ArrayList<Document>();
		FindIterable<Document> iterables = collection.find(filter);
		MongoCursor<Document> cursor = iterables.iterator();
		while (cursor.hasNext()) {
			results.add(cursor.next());
		}
		for (Iterator<Document> iterator = results.iterator(); iterator.hasNext();) {
			Document document = iterator.next();
			resMap.put("pay_full", new BigDecimal((Double) document.get("payFull")));
			resMap.put("type", document.get("type"));
			Object ext = document.get("ext");
			if (ext == null) {
				resMap.put("ext", 0);
			} else {
				resMap.put("ext", 1);
			}
			break;
		}
		return resMap;
	}

	// 更改优惠券状态为已使用
	public boolean changeCouponToOne(Integer couponId) {
		String sql = "update hm_user_coupon set state = 1 where id = ? and state = 0";
		return jdbcTemplate.update(sql, couponId) > 0 ? true : false;
	}

	// 更改优惠券状态为未使用
	public boolean changeCouponToZero(Integer couponId) {
		String sql = "update hm_user_coupon set state = 0 where id = ? and state = 1";
		return jdbcTemplate.update(sql, couponId) > 0 ? true : false;
	}
	// 优惠券-----end

	// hm_cart添加
	public void addHmCart(Map<String, Object> map) {
		String sql = "INSERT INTO `hm_cart` (pid,gid,gname,unit,uid,price,amount,oid,sid,status,ctime,remark) VALUES (null,?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		jdbcTemplate.update(sql, map.get("gid"), map.get("gname"), map.get("unit"), map.get("uid"), map.get("price"),
				map.get("amount"), map.get("oid"), map.get("sid"), map.get("status"), map.get("ctime"),
				map.get("remark") == null ? "" : map.get("remark"));
	}

	// hm_order_detail添加
	public void addOrderDetail(Map<String, Object> map, String smallOrderSn) {
		String sql = "INSERT INTO `hm_order_detail` (order_sn,amount,gid,price,oid) VALUES ( ?, ?, ?, ?, ?)";
		jdbcTemplate.update(sql, smallOrderSn, map.get("amount"), map.get("gid"), map.get("price"), map.get("oid"));
	}

	// hm_order添加
	public Integer addHmOrder(Map<String, Object> map) {
		String sql = "INSERT INTO `hm_order` (order_sn,pid,uid,marketid,total,payment,actual_achieve,integral,"
				+ "pay_status,order_status,ctime,is_appointment,roid,type,couponid,pay_time,cartids,remark,test) "
				+ "VALUES (?, ?, ?, ?, ?, ?,?, 0, ?, ?,  ?, ?, ?,null,null,null,null,?,null)";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
				ps.setString(1, (String) map.get("order_sn"));
				ps.setInt(2, (int) map.get("pid"));
				ps.setInt(3, (int) map.get("uid"));
				ps.setInt(4, (int) map.get("marketid"));
				ps.setBigDecimal(5, (BigDecimal) map.get("total"));
				ps.setBigDecimal(6, (BigDecimal) map.get("payment"));
				ps.setBigDecimal(7, (BigDecimal) map.get("actual_achieve"));
				ps.setInt(8, (Integer) map.get("pay_status"));
				ps.setInt(9, (Integer) map.get("order_status"));
				ps.setInt(10, (int) map.get("ctime"));
				ps.setInt(11, (int) map.get("is_appointment"));
				ps.setInt(12, (int) map.get("roid"));
				ps.setString(13, (String) map.get("remark"));
				return ps;
			}
		}, keyHolder);
		return keyHolder.getKey().intValue();
	}

	// hm_rider_order添加
	public Integer addHmRiderOrder(Map<String, Object> map) {
		final String sql = "INSERT INTO hm_rider_order (rider_sn,order_sn,uid,marketid,rid,pay_status,address_id,rider_pay,"
				+ "couponid,type_pay,totalPrice,pay_time,service_time,order_time,finish_time,ctime,integral,is_appointment,end_time,"
				+ "original_price,out_trade_no,coupon_price,market_activity_price,store_activity_price,vip_relief,remark,test,rider_status,self_sufficiency,deviceId,commission_price) VALUES (?,?,?,"
				+ "?,?,?,?,?,?,null,?,?,?,null,null,?,?,?,null,?,?,?,?,?,?,null,null,?,?,?,?)";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
				ps.setString(1, (String) map.get("rider_sn"));
				ps.setString(2, (String) map.get("order_sn"));
				ps.setInt(3, (Integer) map.get("uid"));
				ps.setInt(4, (Integer) map.get("marketid"));
				ps.setObject(5, (Integer) map.get("rid"), java.sql.Types.INTEGER);
				ps.setInt(6, (Integer) map.get("pay_status"));
				ps.setInt(7, (Integer) map.get("address_id"));
				ps.setBigDecimal(8, (BigDecimal) map.get("rider_pay"));
				ps.setObject(9, (Integer) map.get("couponid"));
				ps.setBigDecimal(10, (BigDecimal) map.get("totalPrice"));
				ps.setObject(11, (Integer) map.get("pay_time"), java.sql.Types.INTEGER);
				ps.setInt(12, (Integer) map.get("service_time"));
				ps.setInt(13, (Integer) map.get("ctime"));
				ps.setInt(14, (int) map.get("integral"));
				ps.setInt(15, (Integer) map.get("is_appointment"));
				ps.setBigDecimal(16, (BigDecimal) map.get("original_price"));
				ps.setString(17, (String) map.get("out_trade_no"));
				ps.setBigDecimal(18, (BigDecimal) map.get("coupon_price"));
				ps.setBigDecimal(19, (BigDecimal) map.get("market_activity_price"));
				ps.setBigDecimal(20, (BigDecimal) map.get("store_activity_price"));
				ps.setBigDecimal(21, (BigDecimal) map.get("vip_relief"));
				ps.setInt(22, (Integer) map.get("rider_status"));
				ps.setInt(23, (Integer) map.get("self_sufficiency"));
				ps.setString(24, (String) map.get("deviceId"));
				ps.setBigDecimal(25, (BigDecimal) map.get("commission_price"));
				return ps;
			}
		}, keyHolder);
		return keyHolder.getKey().intValue();
	}

	public List<Map<String, Object>> getRidOrderNumByMarketId(Integer marketId, Integer todayTimeZone) {
		String sql = " SELECT rid,count(rid) sum FROM hm_rider_order"
				+ " where rid IN (SELECT rid FROM hm_rider_user where state = 1 "
				+ "AND status=1 AND is_order = 1 AND marketid = ? ) AND rider_status in (0,1) "
				+ "AND ctime > ? AND is_delete = 0 GROUP BY rid ORDER BY sum";
		List<Map<String, Object>> res = jdbcTemplate.queryForList(sql, marketId, todayTimeZone);
		return res;
	}

	public List<Map<String, Object>> getRidsByMarketId(Integer marketId) {
		String sql = "SELECT rid,0 as sum  FROM hm_rider_user where state = 1 "
				+ "AND status=1 AND is_order = 1 AND marketid = ?";
		List<Map<String, Object>> res = jdbcTemplate.queryForList(sql, marketId);
		return res;
	}

	public Map<String, Object> getDetailByOrderId(Integer uid, Integer orderId) {
		String sql = "select out_trade_no,totalPrice from  hm_rider_order where id = ? and uid = ? and pay_status = 0";
		Map<String, Object> resMap = null;
		try {
			resMap = jdbcTemplate.queryForMap(sql, orderId, uid);
		} catch (EmptyResultDataAccessException e) {
		}
		return resMap;
	}

	public Map<String, Object> getDetailByOrderId(Integer orderId) {
		String sql = "select uid,rid,couponid,integral from hm_rider_order where id = ?";
		Map<String, Object> resMap = null;
		try {
			resMap = jdbcTemplate.queryForMap(sql, orderId);
		} catch (EmptyResultDataAccessException e) {
		}
		return resMap;
	}

	public Map<String, Object> getROrderIdByOutTradeNo(String outTradeNo) {
		String sql = "select id,marketid,address_id,rider_sn,uid from hm_rider_order where out_trade_no = ?";
		Map<String, Object> resMap = null;
		try {
			resMap = jdbcTemplate.queryForMap(sql, outTradeNo);
		} catch (EmptyResultDataAccessException e) {
		}
		return resMap;
	}

	public boolean updateROStatusToSuccess(String outTradeNo, Integer unixTime, String payType,
			String newOrderTradeNo) {
		String sql = "update hm_rider_order set pay_status = 1,pay_time = ?,type_pay = ?,out_trade_no = ? where out_trade_no = ? and pay_status = 0";
		return jdbcTemplate.update(sql, unixTime, payType, newOrderTradeNo, outTradeNo) > 0 ? true : false;
	}

	public boolean updateROStatusToTimeout(Integer orderId) {
		String sql = "update hm_rider_order set pay_status = 2 where id = ? and pay_status = 0";
		return jdbcTemplate.update(sql, orderId) > 0 ? true : false;
	}

	public boolean updateOStatus(List<Integer> orderIds, Integer payStatus, Integer unixTime) {
		StringBuilder orderIdsString = new StringBuilder();
		for (Iterator iterator = orderIds.iterator(); iterator.hasNext();) {
			Integer orderId = (Integer) iterator.next();
			if (orderIdsString.length() == 0) {
				orderIdsString.append(orderId);
			} else {
				orderIdsString.append("," + orderId);
			}

		}
		String sql = "";
		if (payStatus == 1) {
			sql = "update hm_order set pay_status = ?,pay_time = ? where id in (" + orderIdsString.toString() + ")";
			return jdbcTemplate.update(sql, payStatus, unixTime) > 0 ? true : false;
		} else {
			sql = "update hm_order set pay_status = ? where id in (" + orderIdsString.toString() + ")";
			return jdbcTemplate.update(sql, payStatus) > 0 ? true : false;
		}

	}

	public List<Integer> getOrderIdsByRoid(Integer roid) {
		String sql = "select id from hm_order where roid = ?";
		return jdbcTemplate.queryForList(sql, Integer.class, roid);
	}

	public List<Map<String, Object>> getOrderDetailByRoid(Integer roid) {
		String sql = "select order_sn,is_appointment,pid from hm_order where roid = ?";
		List<Map<String, Object>> resultList = jdbcTemplate.queryForList(sql, new Object[] { roid });
		return resultList;
	}

	public void updateroRider(Integer rid, Integer rorderId) {
		String sql = "update hm_rider_order set rid = ? where id = ?";
		jdbcTemplate.update(sql, rid, rorderId);
	}

	// 查看优惠券数量
	public Double getVipRelief(Integer uid, Integer todayTimeZone) {
		String sql = "SELECT SUM(vip_relief) FROM hm_rider_order WHERE uid = ? AND (pay_status = 0 OR pay_status = 1) AND ctime > ? AND is_delete = 0";
		Double num = jdbcTemplate.queryForObject(sql, new Object[] { uid, todayTimeZone }, Double.class);
		return num;
	}

	public Map<String, Object> getMarketAdressByOrderId(Integer orderId) {
		String sql = "SELECT longitude,latitude from hm_market hm,"
				+ "hm_rider_order hro where hm.id=hro.marketid and hro.id = ?";
		Map<String, Object> resMap = null;
		try {
			resMap = jdbcTemplate.queryForMap(sql, orderId);
		} catch (EmptyResultDataAccessException e) {
		}
		return resMap;
	}

	// 获取菜场订单用户地址
	public Map<String, Object> getOrderAddress(Integer orderId) {
		String sql = "SELECT longitude,latitude from hm_address ha,"
				+ "hm_rider_order hro where ha.id=hro.address_id and hro.id = ?";
		Map<String, Object> resMap = null;
		try {
			resMap = jdbcTemplate.queryForMap(sql, orderId);
		} catch (EmptyResultDataAccessException e) {
		}
		return resMap;
	}

	// 获取商户品类收佣比率
	public BigDecimal getSratio(Integer pid) {
		String sql = "SELECT hm_household_category.ratio FROM hm_household_category,hm_shopown WHERE hm_household_category.id =  hm_shopown.household_category AND hm_shopown.pid = ?";
		return jdbcTemplate.queryForObject(sql, new Object[] { pid }, BigDecimal.class);
	}

	// 查看是否是free
	public Boolean isFree(Integer pid) {
		String sql = "SELECT is_free from hm_shopown where pid = ?";
		Integer isFree = jdbcTemplate.queryForObject(sql, new Object[] { pid }, Integer.class);
		return isFree == 0 ? false : true;
	}

	// 查询自由比率
	public BigDecimal getFSratio(Integer pid) {
		String sql = "SELECT free_ratio from hm_shopown where pid = ?";
		return jdbcTemplate.queryForObject(sql, new Object[] { pid }, BigDecimal.class);
	}

	// 获取菜场比率(默认为1，后期翻倍亦可)
	public BigDecimal getMratio(Integer pid) {
		String sql = "SELECT hm_market.ratio from hm_market ,hm_shopown WHERE hm_market.id = hm_shopown.marketid AND hm_shopown.pid = ? AND hm_market.is_ratio_start = 1";
		BigDecimal mRatio = null;
		try {
			mRatio = jdbcTemplate.queryForObject(sql, new Object[] { pid }, BigDecimal.class);
		} catch (Exception e) {
		}
		return mRatio;
	}

	// 获取平台比率(默认为1，后期翻倍亦可)
	public BigDecimal getRatio() {
		String sql = "SELECT ratio from hm_ratio";
		BigDecimal ratio = jdbcTemplate.queryForObject(sql, BigDecimal.class);
		return ratio;
	}
	//推迟预约时间
	public void updateServiceTime(Integer orderId,Integer updateServiceTime) {
		int servieTime = updateServiceTime;
		String sql = "update hm_rider_order set service_time = ? where id = ?";
		jdbcTemplate.update(sql, servieTime,orderId);
	}

	//查询预约时间
	public Integer getServiceTime(Integer orderId) {
		String sql = "select service_time from hm_rider_order where id = ?";
		return jdbcTemplate.queryForObject(sql, new Object[] { orderId }, Integer.class);
	}
	
	//根据订单id查询菜场关门时间
	public String getEndTimeByOrderId(Integer orderId) {
		String sql = "SELECT hm_market.endtime from hm_rider_order,hm_market WHERE hm_rider_order.marketid = hm_market.id AND hm_rider_order.id = ?";
		return jdbcTemplate.queryForObject(sql, new Object[] { orderId }, String.class);
	}
	
	public Integer queryCouponLevel(Integer couponid) {
		String sql = "select level from hm_user_coupon where id = ?";
		return jdbcTemplate.queryForObject(sql, new Object[] { couponid }, Integer.class);
	}
	
	public void activeCoupon(Integer uid,Integer level) {
		int endTime = DateUtil.getCurrentTime() + 7 * 24 * 3600;
		String sql = "update hm_user_coupon set end_time = ?,is_active = 1 where uid = ? and level = ?";
		jdbcTemplate.update(sql, endTime,uid,level);
	}
	
	
	//防刷单判断
	public boolean marketClickfarming (String deviceId) {
		String sql = "SELECT COUNT(1) FROM hm_rider_order WHERE deviceId = ? AND ctime > ? AND (pay_status = 1 or pay_status = 0) AND is_delete = 0 AND market_activity_price IS NOT NULL";
		Integer num = jdbcTemplate.queryForObject(sql, new Object[] { deviceId,DateUtil.getTodayZeroTime() }, Integer.class);
		return num > 0?true:false;
	}
	
	public boolean couponClickfarming (String deviceId) {
		String sql = "SELECT COUNT(1) FROM hm_rider_order WHERE deviceId = ? AND ctime > ? AND (pay_status = 1 or pay_status = 0) AND is_delete = 0 AND coupon_price IS NOT NULL";
		Integer num = jdbcTemplate.queryForObject(sql, new Object[] { deviceId,DateUtil.getTodayZeroTime() }, Integer.class);
		return num > 0?true:false;
	}
	
////-----------------------------------------------------------------------------------------------
//	public List<Integer> selectOrder1() {
//		String sql = " SELECT hr.id from hm_rider_order hr,hm_order ho"
//				+ " where hr.id = ho.roid and hr.ctime < 1546272000 and hr.pay_status =1 AND hr.totalPrice != 0 AND (hr.integral IS  NULL OR hr.integral = 0)";
//		
//		return jdbcTemplate.queryForList(sql, Integer.class);
//	}
//	
//	public Map<String, Object> TotalPriceAndRiderPay(Integer roid) {
//		String sql = "SELECT totalPrice,rider_pay from hm_rider_order where id = ?";
//		Map<String, Object> resMap = null;
//		try {
//			resMap = jdbcTemplate.queryForMap(sql, roid);
//		} catch (EmptyResultDataAccessException e) {
//		}
//		return resMap;
//	}
//	
//	public Map<String, Object> getPayment(Integer roid) {
//		Map<String, Object> resMap = null;
//		String sql = "SELECT SUM(payment) payment,SUM(total) total from hm_order where roid = ?";
//		try {
//			resMap = jdbcTemplate.queryForMap(sql, roid);
//		} catch (EmptyResultDataAccessException e) {
//		}
//		return resMap;
//	}
//	
//	public void updateMarketAPrice(Integer roid,BigDecimal marketAprice) {
//		String sql = "update hm_rider_order set market_activity_price = ? where id = ?";
//		jdbcTemplate.update(sql, marketAprice,roid);
//	}
//	
//	
//	
//	//查询totalprice为null的订单
//	public List<Integer> selectOrder2() {
//		String sql = "SELECT id from hm_rider_order hr where  hr.totalPrice IS NULL and hr.ctime < 1546272000";
//		return jdbcTemplate.queryForList(sql, Integer.class);
//	}
//	
//	//查询totalprice为null的订单
//	public List<Integer> selectOrder3() {
//		String sql = "SELECT id from hm_rider_order where uid in(SELECT uid from hm_rider_order GROUP BY uid HAVING  COUNT(1) > 2) and ctime < 1560096000";
//		return jdbcTemplate.queryForList(sql, Integer.class);
//	}
//	
//	public void updateRiderOrder(Integer roid,Integer type) {
//		String sql = "";
//		if (type == 0) {
//			sql = "update hm_rider_order set id = id * 1000000 ,pay_time = pay_time + 2592000,service_time = service_time + 2592000,order_time= order_time + 2592000,finish_time =finish_time + 2592000,ctime = ctime + 2592000  where id =?";
//		}else {
//			sql = "delete from hm_rider_order where id =?";
//		}
//		 
//		jdbcTemplate.update(sql,roid);
//	}
//	
//	public void updateOrder(Integer roid ,Integer type) {
//		String sql = "";
//		if(type == 0) {
//		sql = "update hm_order set ctime = ctime + 2592000,roid=roid*1000000,id=id*1000000 where roid = ?";	
//		}else {
//			sql = "delete from hm_order where roid = ?";
//		}
//		
//		jdbcTemplate.update(sql,roid);
//	}
//	
//	public void updateCart(Integer oid ,Integer type) {
//		String sql = "";
//		if(type == 0) {
//		sql = "update hm_cart set cid = cid * 1000000,oid = oid * 1000000 where oid = ?";	
//		}else {
//			sql = "delete from hm_cart where oid = ?";
//		}
//		
//		jdbcTemplate.update(sql,oid);
//	}
//	
//	public List<Integer> getOidOfRoid(Integer roid) {
//		String sql = "SELECT id FROM hm_order where roid = ?";
//		return jdbcTemplate.queryForList(sql, Integer.class,roid);
//	}
//	
//	public List<Integer> getAllRiderOrder() {
//		String sql = "SELECT id from hm_rider_order";
//		return jdbcTemplate.queryForList(sql, Integer.class);
//	}
//	
//	public List<Integer> getAllOrder() {
//		String sql = "SELECT id from hm_order";
//		return jdbcTemplate.queryForList(sql, Integer.class);
//	}
//	
//	public List<Integer> getAllCart() {
//		String sql = "SELECT cid from hm_cart";
//		return jdbcTemplate.queryForList(sql, Integer.class);
//	}
//	
//	public List<Integer> getCidByOid(Integer oid) {
//		String sql = "SELECT cid from hm_cart where oid = ?";
//		return jdbcTemplate.queryForList(sql, Integer.class,oid);
//	}
//	
//	public void deleteCarts(String carts) {
//		String sql = "delete from hm_cart where cid in " + carts;
//		jdbcTemplate.update(sql);
//	}
//	
//	public void deleteOrders(String orders) {
//		String sql = "delete from hm_order where id in " + orders;
//		jdbcTemplate.update(sql);
//	}
//	
//	public void deleteROrders(String rorders) {
//		String sql = "delete from hm_rider_order where id in " + rorders;
//		jdbcTemplate.update(sql);
//	}

	public List<Integer> getStore(Integer marketid) {
		String sql = "select pid from hm_shopown where marketid = ? and type = 0 and examine = 1 and is_show = 1";
		return jdbcTemplate.queryForList(sql, Integer.class, marketid);
	}
	
	public List<Integer> getStore() {
		String sql = "select pid from hm_shopown where type = 0 and examine = 1 and is_show = 1";
		return jdbcTemplate.queryForList(sql, Integer.class);
	}

	public String getStoreToken(Integer pid) {
		String sql = "select login_token from hm_shopown where pid = ?";
		return jdbcTemplate.queryForObject(sql, String.class, pid);
	}
	
	public String getPhone(Integer pid) {
		String sql = "select phone from hm_shopown where pid = ?";
		return jdbcTemplate.queryForObject(sql, String.class, pid);
	}
	
	public BigDecimal getBalanceByPid(Integer pid) {
		String sql = "select balance from hm_shopown where pid = ?";
		return jdbcTemplate.queryForObject(sql, BigDecimal.class, pid);
	}
	
	public List<Map<String, Object>> getGoods(Integer sid) {
		String sql = "select id,gname,price,unit from hm_goods where state = 0 and is_delete = 0 and pid = ? and price BETWEEN 30 AND 60";
		List<Map<String, Object>> res = jdbcTemplate.queryForList(sql,sid);
		return res;
	}
	public boolean updateROrderTime(Integer unixTime, Integer roid) {
		String sql = "update hm_rider_order set order_time = ?,rider_status = 1 where id = ?";
		return jdbcTemplate.update(sql,unixTime,roid) > 0 ? true : false;
	}
	
	public boolean updateROrderDelete( Integer roid) {
		String sql = "update hm_rider_order set is_delete = 1 where id = ?";
		return jdbcTemplate.update(sql,roid) > 0 ? true : false;
	}
	
	public boolean updateOrderDelete( Integer oid) {
		String sql = "update hm_order set is_delete = 1 where id = ?";
		return jdbcTemplate.update(sql,oid) > 0 ? true : false;
	}
	
	public Integer getMarketId(Integer sid) {
		String sql = "select marketid from hm_shopown where pid = ?";
		return jdbcTemplate.queryForObject(sql, Integer.class,sid);
	}
	
	public List<Integer> getRids(Integer marketId) {
		String sql = "SELECT rid  FROM hm_rider_user where login_token is NOT NULL";
		List<Integer> res = jdbcTemplate.queryForList(sql,Integer.class);
		return res;
	}
	
	public String getLoginTokenByRid(Integer rid) {
		String sql = "SELECT login_token FROM hm_rider_user where rid = ?";
		return jdbcTemplate.queryForObject(sql, String.class,rid);
	}
	
}
