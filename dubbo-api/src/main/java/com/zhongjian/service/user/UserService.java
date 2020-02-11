package com.zhongjian.service.user;

import com.zhongjian.dto.user.query.UserQueryDTO;
import com.zhongjian.dto.user.result.UserCopResultDTO;
import com.zhongjian.dto.user.result.UserResultDTO;

import java.util.List;

public interface UserService {
	/**
	 * 根据token获取uid
	 * @param loginToken
	 * @return
	 */
	Integer getUidByLoginToken(String loginToken);

	/**
	 * 根据uid获取user信息
	 * @param id
	 * @return
	 */
	UserResultDTO getUserBeanById(Integer id);


	/**
	 * 根据uid查询优惠卷信息.
	 * @param userQueryDTO
	 * @return
	 */
	List<UserCopResultDTO> getCouponByUid(UserQueryDTO userQueryDTO);


	/**
	 * 根据uid查询优惠卷信息.
	 * @param userQueryDTO
	 * @return
	 */
	List<UserCopResultDTO> getCVCouponByUid(UserQueryDTO userQueryDTO);

	
	String deCoderPhone(String enCoderPhone);
	
	Boolean bindFlOpenId(Integer uid,String openId);
	

}
