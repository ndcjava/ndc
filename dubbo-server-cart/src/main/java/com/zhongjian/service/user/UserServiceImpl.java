package com.zhongjian.service.user;

import com.zhongjian.commoncomponent.PropUtil;
import com.zhongjian.dao.entity.cart.user.UserBean;
import com.zhongjian.dao.framework.impl.HmBaseService;
import com.zhongjian.dao.jdbctemplate.CouponDao;
import com.zhongjian.dao.jdbctemplate.UserDao;
import com.zhongjian.dto.user.query.UserQueryDTO;
import com.zhongjian.dto.user.result.UserCopResultDTO;
import com.zhongjian.dto.user.result.UserResultDTO;
import com.zhongjian.util.RsaEncoder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @Author: ldd
 */
@Service("userService")
public class UserServiceImpl extends HmBaseService<UserBean, Integer> implements UserService {

	@Autowired
	private CouponDao couponDao;
	
	@Autowired
	private UserDao UserDao;
	
	@Autowired
	private PropUtil propUtil;

	@Override
	public Integer getUidByLoginToken(String loginToken) {

		Integer findUserByLoginToken = this.dao.executeSelectOneMethod(loginToken, "findUserByLoginToken",
				Integer.class);

		if (null == findUserByLoginToken) {
			return 0;
		}
		return findUserByLoginToken;
	}

	@Override
	public UserResultDTO getUserBeanById(Integer id) {

		UserResultDTO findUserById = this.dao.executeSelectOneMethod(id, "findUserById", UserResultDTO.class);

		return findUserById;
	}

	@Override
	public List<UserCopResultDTO> getCouponByUid(UserQueryDTO userQueryDTO) {

		List<UserCopResultDTO> findCouponByUid = this.dao.executeListMethod(userQueryDTO.getUid(),
				"findCouponByUidModelOne", UserCopResultDTO.class);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Iterator<UserCopResultDTO> iterator = findCouponByUid.iterator();
		while (iterator.hasNext()) {
			UserCopResultDTO userCopResultDTO = iterator.next();
			Map<String, Object> coupInfo = couponDao.getCoupInfo(userCopResultDTO.getMongoId());
			Integer type = (Integer) coupInfo.get("type");
			// 是否达到满减
			// state设置
			if ((new BigDecimal(userQueryDTO.getPrice()).compareTo((BigDecimal) coupInfo.get("pay_full")) >= 0
					&& type == 0) || type == 1) {
				userCopResultDTO.setState(0);
				userCopResultDTO.setReason("");
			} else {
				userCopResultDTO.setState(1);
				userCopResultDTO.setReason("未达到满减");
			}
			// type设置
			userCopResultDTO.setType(type);
			// content设置
			if (type == 0) {
				userCopResultDTO.setContent("满" + coupInfo.get("pay_full") + "元可用(每天限用一张)");
			} else {
				userCopResultDTO.setContent("抵消该单配送费(每天限用一张)");
			}

			String startTime = sdf.format(new Date((long) userCopResultDTO.getStarttime() * 1000));
			String endTime = sdf.format(new Date((long) userCopResultDTO.getEndtime() * 1000));
			userCopResultDTO.setEndtime(null);
			userCopResultDTO.setStarttime(null);
			userCopResultDTO.setMongoId(null);
			// time设置
			userCopResultDTO.setStart_time(startTime);
			userCopResultDTO.setEnd_time(endTime);

			// 去掉不符合市场的
			List<Integer> mids = (List<Integer>) coupInfo.get("marketid");
			Boolean flag = true;
			if (mids != null) {
				for (Integer mid : mids) {
					if (userQueryDTO.getMarketId().equals(mid)) {
						flag = false;
						break;
					}
				}
				if (flag) {
					iterator.remove();
				}
			} else {
				continue;
			}
		}
		return findCouponByUid;
	}

	@Override
	public List<UserCopResultDTO> getCVCouponByUid(UserQueryDTO userQueryDTO) {

		List<UserCopResultDTO> findCouponByUid = this.dao.executeListMethod(userQueryDTO.getUid(),
				"findCouponByUidModelTWO", UserCopResultDTO.class);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Iterator<UserCopResultDTO> iterator = findCouponByUid.iterator();
		while (iterator.hasNext()) {
			UserCopResultDTO userCopResultDTO = iterator.next();
			Map<String, Object> coupInfo = couponDao.getCoupInfo(userCopResultDTO.getMongoId());
			Integer type = (Integer) coupInfo.get("type");
			// 是否达到满减
			// state设置
			if ((new BigDecimal(userQueryDTO.getPrice()).compareTo((BigDecimal) coupInfo.get("pay_full")) >= 0
					&& type == 0)) {
				userCopResultDTO.setState(0);
				userCopResultDTO.setReason("");
			} else {
				userCopResultDTO.setState(1);
				userCopResultDTO.setReason("未达到满减");
			}
			// type设置
			userCopResultDTO.setType(type);
			// content设置
			if (type == 0) {
				userCopResultDTO.setContent("满" + coupInfo.get("pay_full") + "元可用(每天限用一张)");
			}
			String startTime = sdf.format(new Date((long) userCopResultDTO.getStarttime() * 1000));
			String endTime = sdf.format(new Date((long) userCopResultDTO.getEndtime() * 1000));
			userCopResultDTO.setEndtime(null);
			userCopResultDTO.setStarttime(null);
			userCopResultDTO.setMongoId(null);
			// time设置
			userCopResultDTO.setStart_time(startTime);
			userCopResultDTO.setEnd_time(endTime);
		}
		return findCouponByUid;

	}

	@Override
	public String deCoderPhone(String enCoderPhone) {
		return RsaEncoder.decrypt(enCoderPhone, propUtil.getFlPriveteKey());
	}

	@Override
	public Boolean bindFlOpenId(Integer uid, String openId) {
		return UserDao.updateOpenIdByUid(uid, openId);
	}
}
