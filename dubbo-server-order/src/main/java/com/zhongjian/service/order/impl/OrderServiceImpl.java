package com.zhongjian.service.order.impl;

import com.zhongjian.common.IdWorkers;
import com.zhongjian.common.constant.FinalDatas;
import com.zhongjian.commoncomponent.PropUtil;
import com.zhongjian.dao.entity.order.address.OrderAddressBean;
import com.zhongjian.dao.entity.order.address.OrderAddressOrderBean;
import com.zhongjian.dao.entity.order.shopown.OrderShopownBean;
import com.zhongjian.dao.framework.impl.HmBaseService;
import com.zhongjian.dao.jdbctemplate.AddressDao;
import com.zhongjian.dao.jdbctemplate.CVOrderDao;
import com.zhongjian.dao.jdbctemplate.IntegralVipDao;
import com.zhongjian.dao.jdbctemplate.MarketDao;
import com.zhongjian.dao.jdbctemplate.OrderDao;
import com.zhongjian.dto.cart.storeActivity.result.CartStoreActivityResultDTO;
import com.zhongjian.dto.common.ResultDTO;
import com.zhongjian.dto.order.order.query.OrderStatusQueryDTO;
import com.zhongjian.exception.NDCException;
import com.zhongjian.service.order.OrderService;
import com.zhongjian.shedule.OrderShedule;
import com.zhongjian.task.AddressTask;
import com.zhongjian.task.OrderTask;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.zhongjian.util.DateUtil;
import com.zhongjian.util.DistanceUtils;
import com.zhongjian.util.RandomUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service("orderService")
public class OrderServiceImpl extends HmBaseService<OrderShopownBean, Integer> implements OrderService {

	@Autowired
	private OrderDao orderDao;

	@Autowired
	private CVOrderDao cvOrderDao;

	@Autowired
	private AddressDao addressDao;

	@Autowired
	private IntegralVipDao integralVipDao;

	@Autowired
	private MarketDao marketDao;

	@Autowired
	private AddressTask addressTask;

	@Autowired
	private OrderTask orderTask;

	@Autowired
	private OrderShedule orderShedule;

	@Autowired
	private PropUtil propUtil;

	@Autowired
	private IdWorkers idWorkers;

	@SuppressWarnings("unchecked")
	@Override
	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> previewOrCreateOrder(Integer uid, Integer[] sids, String type, Integer extra,
			String isSelfMention, boolean toCreateOrder, Integer addressId, Integer unixTime, Integer isAppointment)
			throws NDCException {

		Integer isVIp = 0;
		String vipFavour = "";

		Integer integralCanUse = 0;
		Integer couponCanUse = 0;
		// 积分描述
		String integralContent = "";
		String integralPriceString = "";
		// 优惠券描述
		String couponContent = "";
		Integer marketId = 0;
		// 该单应付总价
		BigDecimal needPay = BigDecimal.ZERO;
		// 该单应付总价
		String needPayString = "";
		// 各商户总价和
		BigDecimal storesAmountBigDecimal = BigDecimal.ZERO;
		String storesAmountString = "";
		// 各商户参与优惠总价和
		BigDecimal storesAmountBigDecimalForFavorable = BigDecimal.ZERO;
		// 商户价格列表
		boolean showMarketActivity = false;
		String marketActivity = "";
		String marketActivityContent = "";
		String priceForIntegralCoupon = "";
		// 用于生成订单
		BigDecimal storeActivityPrice = BigDecimal.ZERO; // 商户优惠
		// 积分扣除
		BigDecimal integralSub = BigDecimal.ZERO;
		boolean integralPay = false;
		// 市场优惠
		BigDecimal marketActivityPrice = BigDecimal.ZERO;

		// vip优惠
		BigDecimal vipFavourRiderOrder = BigDecimal.ZERO;

		// 会员折扣
		Double memberDiscount = 0.95;
		// 会员运费折扣
		Double memberDeliverfeeDiscount = 0.5;
		// 会员单笔limit
		Double limitOne = 200d;
		Double limitDayRelief = 10d;
		// 代码配置获取
		String memberDeliverfee = propUtil.getMemberDeliverfee();
		String memberSelfMentionDeliverfee = propUtil.getSelfmentionDeliverfee();
		String originalfee = propUtil.getOriginalfee();

		String deliverfee = originalfee;
		BigDecimal deliverfeeBigDecimal = new BigDecimal(originalfee);
		Integer orderId = null;
		int createTime = (int) (System.currentTimeMillis() / 1000);
		// 小订单拼接
		StringBuilder orderJoint = new StringBuilder();
		Map<String, Object> storeOrders = null;// 总集合
		List<String> storeIds = null;
		if (toCreateOrder) {
			storeOrders = new HashMap<String, Object>();
			storeOrders.put("rider_status", 0);
			storeIds = new ArrayList<String>();
		}

		// 计算商品价格（已算商户活动）-----start
		List<Map<String, Object>> storeList = new ArrayList<Map<String, Object>>();
		for (int i = 0; i < sids.length; i++) {
			Map<String, Object> storeOrderInfo = null;
			List<Map<String, Object>> hmCartList = null;

			if (toCreateOrder) {
				hmCartList = new ArrayList<Map<String, Object>>();
				storeOrderInfo = new HashMap<String, Object>();

			}
			Map<String, Object> store = new HashMap<String, Object>();
			// 查出价格商户名(原价)
			BigDecimal storeAmountBigDecimal = BigDecimal.ZERO;
			// 商户活动后价格
			BigDecimal actualStoreAmountBigDecimal = BigDecimal.ZERO;
			List<Map<String, Object>> singleStoreInfoList = orderDao.getBasketByUidAndSid(sids[i], uid);
			int unFavorable = 0;// 默认参与市场优惠
			String sname = "";
			String sid = "";
			if (singleStoreInfoList.size() == 0) {
				continue;
			}
			for (Iterator<Map<String, Object>> iterator = singleStoreInfoList.iterator(); iterator.hasNext();) {
				Map<String, Object> hmCart = null;// 用于生成订单hm_cart
				boolean flag = true;
				Map<String, Object> map = (Map<String, Object>) iterator.next();
				if (flag == true) {
					marketId = (Integer) map.get("marketid");
					sname = (String) map.get("sname");// 商户名
					sid = map.get("pid").toString();
					unFavorable = (int) map.get("unFavorable");
					// 显示不参与满减
					if (unFavorable != 0) {
						sname = sname + "（不参与市场满减）";
					}
					flag = false;
				}
				// 商品录入价格
				BigDecimal singleAmount = null;
				// 购物车物品总价
				BigDecimal basketPrice = (BigDecimal) map.get("basketprice");
				// 数量
				BigDecimal amoBigDecimal = (BigDecimal) map.get("amount");
				// 购物车单价
				BigDecimal unitPrice = (BigDecimal) map.get("unitPrice");
				// 商品单价
				BigDecimal goodUnitPrice = (BigDecimal) map.get("price");
				if (goodUnitPrice == null || unitPrice.compareTo(goodUnitPrice) == 0) {
					singleAmount = basketPrice;
				} else {
					singleAmount = amoBigDecimal.multiply(goodUnitPrice).setScale(2, BigDecimal.ROUND_HALF_UP);
				}
				if (toCreateOrder) {
					hmCart = new HashMap<String, Object>();
					Integer gid = (Integer) map.get("gid");
					String gname = (String) map.get("gname");
					String unit = (String) map.get("unit");
					String remark = (String) map.get("remark");
					hmCart.put("gid", gid);
					hmCart.put("gname", gname == null ? "其他" : gname);
					hmCart.put("unit", unit == null ? "个" : unit);
					hmCart.put("uid", uid);
					hmCart.put("price", singleAmount);
					hmCart.put("amount", amoBigDecimal);
					hmCart.put("sid", sid);
					hmCart.put("status", 10005);
					hmCart.put("ctime", createTime);
					hmCart.put("remark", remark);
					hmCart.put("oid", 0);
					hmCartList.add(hmCart);
				}
				storeAmountBigDecimal = storeAmountBigDecimal.add(singleAmount);
			}
			if (unFavorable == 0) {
				// 享受市场优惠
				storesAmountBigDecimalForFavorable = storesAmountBigDecimalForFavorable.add(storeAmountBigDecimal);
			}
			CartStoreActivityResultDTO storeActivtiy = orderDao.getStoreActivtiy(sids[i], storeAmountBigDecimal);
			if (storeActivtiy == null) {
				actualStoreAmountBigDecimal = storeAmountBigDecimal;
			} else {
				if (storeActivtiy.getType() == 0) {
					actualStoreAmountBigDecimal = storeAmountBigDecimal
							.subtract(new BigDecimal(storeActivtiy.getReduce())).setScale(2, BigDecimal.ROUND_HALF_UP);
				} else {
					actualStoreAmountBigDecimal = storeAmountBigDecimal
							.multiply(new BigDecimal(storeActivtiy.getDiscount()))
							.setScale(2, BigDecimal.ROUND_HALF_UP);
				}
			}
			store.put("sid", sid);
			store.put("sname", sname);
			store.put("originPrice", "￥" + storeAmountBigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP).toString());
			store.put("totalPrice", "￥" + actualStoreAmountBigDecimal.toString());
			// 计算商户优惠的价格
			if (toCreateOrder) {
				storeActivityPrice = storeActivityPrice
						.add(storeAmountBigDecimal.subtract(actualStoreAmountBigDecimal));
				// 为生成hm_order做准备
				String smallOrderSn = "HM" + idWorkers.getOrderIdWork().nextId();
				if (orderJoint.length() == 0) {
					orderJoint.append(smallOrderSn);
				} else {
					orderJoint.append("|").append(smallOrderSn);
				}

				Integer pid = Integer.valueOf(sid);
				BigDecimal mRatio = orderDao.getMratio(pid);
				BigDecimal finalRatio = BigDecimal.ZERO;
				if (mRatio != null) {
					BigDecimal ratio = orderDao.getRatio();
					BigDecimal sratio = null;
					if (orderDao.isFree(pid)) {
						sratio = orderDao.getFSratio(pid);
					} else {
						sratio = orderDao.getSratio(pid);
					}
					finalRatio = ratio.multiply(sratio).multiply(mRatio).setScale(4, BigDecimal.ROUND_HALF_UP);
				}
				BigDecimal computeRatio = new BigDecimal("1").subtract(finalRatio).setScale(4,
						BigDecimal.ROUND_HALF_UP);
				BigDecimal actualStoreAmountBigDecimalTake = computeRatio.multiply(actualStoreAmountBigDecimal).setScale(2, BigDecimal.ROUND_HALF_UP);
				storeOrderInfo.put("pid", Integer.valueOf(sid));
				storeOrderInfo.put("order_sn", smallOrderSn);
				storeOrderInfo.put("uid", uid);
				storeOrderInfo.put("marketid", marketId);
				storeOrderInfo.put("total", storeAmountBigDecimal);
				storeOrderInfo.put("payment", actualStoreAmountBigDecimal);
				storeOrderInfo.put("actual_achieve",actualStoreAmountBigDecimalTake);
				storeOrderInfo.put("ctime", createTime);
				storeOrderInfo.put("is_appointment", isAppointment);
				storeOrderInfo.put("roid", 0);
				storeOrderInfo.put("cartList", hmCartList);
				storeOrders.put(sid, storeOrderInfo);
				storeIds.add(sid);
			}
			storeList.add(store);
			storesAmountBigDecimal = storesAmountBigDecimal.add(actualStoreAmountBigDecimal);
		}
		storesAmountString = storesAmountBigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
		needPay = storesAmountBigDecimal;
		// 计算商品价格（已算商户活动）-----end

		// 市场开始后结束时间
		BigDecimal priceForCoupon = needPay;
		// 检测市场活动--start
		Map<String, Object> marketActivtiy = marketDao.getMarketActivtiy(marketId);
		if (marketActivtiy != null) {
			showMarketActivity = true;
			String rule = (String) marketActivtiy.get("rule");
			int upLimit = (int) marketActivtiy.get("up_limit");
			int marketActivtiyType = (int) marketActivtiy.get("type");
			BigDecimal maxRight = BigDecimal.ZERO;
			BigDecimal maxRightSub = BigDecimal.ZERO;
			if (marketActivtiyType == 0) {
				String[] activitys = rule.split(",");
				for (int i = 0; i < activitys.length; i++) {
					String fullString = activitys[i].split("-")[0];
					String subString = activitys[i].split("-")[1];
					BigDecimal currentFull = new BigDecimal(fullString);
					BigDecimal currentSub = new BigDecimal(subString);
					if (maxRight.compareTo(BigDecimal.ZERO) == 0
							&& storesAmountBigDecimalForFavorable.compareTo(currentFull) >= 0) {
						maxRight = currentFull;
						maxRightSub = currentSub;
					}
					if (maxRight.compareTo(BigDecimal.ZERO) != 0 && currentFull.compareTo(maxRight) > 0
							&& storesAmountBigDecimalForFavorable.compareTo(currentFull) >= 0) {
						maxRight = currentFull;
						maxRightSub = currentSub;

					}
				}
				marketActivity = "首单买满" + maxRight + "减" + maxRightSub;
			} else {
				marketActivity = "首单买满" + upLimit + "打" + (int) (Float.valueOf(rule) * 10) + "折";
			}
			BigDecimal upBigDecimal = new BigDecimal(upLimit);
			// 检查首单
			if (!orderDao.checkFirstOrderByUid(uid)) {
				marketActivity = "仅限当日首单";
				if (((marketActivtiyType == 1 && storesAmountBigDecimalForFavorable.compareTo(upBigDecimal) >= 0)
						|| marketActivtiyType == 0 && maxRight.compareTo(BigDecimal.ZERO) == 1) && "0".equals(type)) {
					if (!orderDao.checkFirstPayOrderByUid(uid)) {
						if (orderDao.checkToPayNum(uid) == 1) {
							// 没有已经支付的订单
							orderId = orderDao.checkFirstToPayOrderByUid(uid);
						}
					}
				}
			} else {
				// 没有选择优惠券或积分
				if ("0".equals(type)) {
					if ((marketActivtiyType == 1 && storesAmountBigDecimalForFavorable.compareTo(upBigDecimal) >= 0)
							|| marketActivtiyType == 0 && maxRight.compareTo(BigDecimal.ZERO) == 1) {
						BigDecimal needSuBigDecimal = BigDecimal.ZERO;
						if (maxRight.compareTo(BigDecimal.ZERO) == 1) {
							needSuBigDecimal = maxRightSub;
							needPay = needPay.subtract(needSuBigDecimal);
						} else {
							needSuBigDecimal = upBigDecimal.subtract(
									new BigDecimal(rule).multiply(upBigDecimal).setScale(2, BigDecimal.ROUND_HALF_UP));
							needPay = needPay.subtract(needSuBigDecimal);
						}
						marketActivityPrice = needSuBigDecimal;

						marketActivityContent = "-￥"
								+ needSuBigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
					} else {
						marketActivity = "未达到满减额度";
					}
				} else {
					marketActivity = "现金券不与菜场活动共享";
				}
			}

		}
		// 检测市场活动--end

		// 积分优惠券初始化显示----start
		Map<String, Object> uMap = integralVipDao.getIntegralAndVipInfo(uid);
		Integer integral = (Integer) (uMap.get("integral"));
		integralContent = "共" + integral + "积分";
		if (integral > 0) {
			integralCanUse = 1;
		}
		Integer vipStatus = (Integer) (uMap.get("vip_status"));
		// 动态获取VIP参数 --start
		Map<String, Object> config = getConfigByUidAndStatus(uid, vipStatus);
		limitDayRelief = config.get("limitDayRelief") == null ? limitDayRelief : (Double) config.get("limitDayRelief");
		limitOne = config.get("limitOne") == null ? limitOne : (Double) config.get("limitOne");
		memberDiscount = config.get("discount") == null ? memberDiscount : (Double) config.get("discount");
		memberDeliverfeeDiscount = config.get("riderDiscount") == null ? memberDeliverfeeDiscount
				: (Double) config.get("riderDiscount");

		// 动态获取VIP参数 --end
		memberDeliverfee = new BigDecimal(deliverfee).multiply(new BigDecimal(memberDeliverfeeDiscount))
				.stripTrailingZeros().toString();
		BigDecimal subtract = new BigDecimal(1).subtract(new BigDecimal(memberDiscount));
		Double canFavourOne = subtract.multiply(new BigDecimal(limitOne)).setScale(2, BigDecimal.ROUND_HALF_UP)
				.doubleValue();
		Double vipRelief = orderDao.getVipRelief(uid, (int) DateUtil.getTodayZeroTime());
		if (vipRelief == null) {
			vipRelief = 0.00;
		}
		Double canFavourDay = limitDayRelief - vipRelief;
		if (canFavourDay < 0.00) {
			canFavourDay = 0.00;
		}
		BigDecimal vipFavourMoney = BigDecimal.ZERO;
		if (canFavourDay < canFavourOne) {
			vipFavourMoney = new BigDecimal(canFavourDay);
		} else {
			vipFavourMoney = new BigDecimal(canFavourOne);
		}

		BigDecimal vipFavourable = subtract.multiply(needPay).setScale(2, BigDecimal.ROUND_HALF_UP);
		if (vipFavourable.compareTo(vipFavourMoney) == 1) {
			vipFavourable = vipFavourMoney;
		}

		vipFavourRiderOrder = vipFavourable;

		// 判断是否是会员
		if (vipStatus == 1) {
			isVIp = 1;
			needPay = needPay.subtract(vipFavourable);
			deliverfee = "￥" + memberDeliverfee;
			deliverfeeBigDecimal = new BigDecimal(memberDeliverfee);

			if ("1".equals(isSelfMention)) {
				deliverfee = "￥" + memberSelfMentionDeliverfee;
				deliverfeeBigDecimal = new BigDecimal(memberSelfMentionDeliverfee);
				if (toCreateOrder) {
					storeOrders.put("rider_status", 3);
				}
			}
		}

		boolean todayCouponUse = orderDao.checkCouponOrderByUid(uid);
		BigDecimal priceForIntegralor = needPay.add(deliverfeeBigDecimal);

		if (orderDao.getCouponsNumCanUse(uid) > 0) {
			if (todayCouponUse) {
				couponContent = "有可用优惠券";
				couponCanUse = 1;
			} else {
				couponContent = "每日一张限用一张";
			}
		} else {
			couponContent = "";
		}
		// 积分优惠券初始化显示----end

		// 算积分或优惠券----start
		boolean needPayNeedHandleFlag = true;

		priceForIntegralCoupon = priceForCoupon.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
		if ("1".equals(type) && integral > 0) {
			BigDecimal hundredBigDecimal = new BigDecimal(100);
			// 使用积分
			BigDecimal integralBigDecimal = new BigDecimal(integral);
			BigDecimal integralPrice = integralBigDecimal.divide(hundredBigDecimal);

			if (priceForIntegralor.compareTo(integralPrice) > 0) {
				integralContent = "共" + integral + "积分,可抵扣" + integral + "积分";
				integralPriceString = "-￥" + integralPrice.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
				needPay = priceForIntegralor.subtract(integralPrice);
				if (toCreateOrder) {
					integralSub = integralBigDecimal;
				}
				needPayNeedHandleFlag = false;
			} else {
				// 全积分支付
				integralContent = "共" + integral + "积分，可抵扣"
						+ priceForIntegralor.multiply(hundredBigDecimal).setScale(0, BigDecimal.ROUND_HALF_UP) + "积分";
				integralPriceString = "-￥" + priceForIntegralor.setScale(2, BigDecimal.ROUND_HALF_UP);
				needPay = BigDecimal.ZERO;
				if (toCreateOrder) {
					integralPay = true;
					integralSub = priceForIntegralor.multiply(hundredBigDecimal).setScale(0, BigDecimal.ROUND_HALF_UP);
				}
				needPayNeedHandleFlag = false;
			}
			if (toCreateOrder) {
				// 卡积分
				try {
					integralVipDao.updateUserIntegral(uid, "-", integralSub.intValue());
				} catch (RuntimeException e) {
					throw new NDCException.IntegralException();
				}

			}
		}
		if ("2".equals(type) && todayCouponUse) {
			Map<String, Object> couponInfo = orderDao.getCouponInfo(uid, extra);
			if (couponInfo != null) {
				BigDecimal payFullBigDecimal = (BigDecimal) couponInfo.get("pay_full");
				Integer couponType = (Integer) couponInfo.get("type");
				Integer ext = (Integer) couponInfo.get("ext");
				if (couponType == 0) {
					if (priceForCoupon.compareTo(payFullBigDecimal) >= 0) {
						BigDecimal couponPrice = (BigDecimal) couponInfo.get("price");
						needPay = needPay.subtract(couponPrice);
						if (priceForCoupon.compareTo(couponPrice) < 0) {
							couponPrice = priceForCoupon;
							// vip减免改为0
							vipFavourable = BigDecimal.ZERO;
							// 落地保持一致
							vipFavourRiderOrder = vipFavourable;
							needPay = BigDecimal.ZERO;
						}
						if (ext.equals(1)) {
							// 配送券
							deliverfeeBigDecimal = BigDecimal.ZERO;
							deliverfee = "￥0";
						}

						couponContent = "-￥" + couponPrice.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
						if (toCreateOrder) {
							storeOrders.put("couponid", extra);
							storeOrders.put("coupon_price", couponPrice);
						}
					}
				} else {
					// 配送券
					deliverfeeBigDecimal = BigDecimal.ZERO;
					deliverfee = "￥0";
					if (toCreateOrder) {
						storeOrders.put("couponid", extra);
					}
					needPayNeedHandleFlag = false;
				}
				if (toCreateOrder) {
					// 卡券
					if (!orderDao.changeCouponToOne(extra)) {
						throw new NDCException.CouponException();
					}
				}
			}
		}
		// 算积分或优惠券----end

		// 最终价格补算
		if (needPayNeedHandleFlag) {
			needPay = needPay.add(deliverfeeBigDecimal);
		}
		// needpay负值处理
		if (needPay.compareTo(BigDecimal.ZERO) == -1) {
			needPay = BigDecimal.ZERO;
		}
		needPayString = needPay.setScale(2, BigDecimal.ROUND_HALF_UP).toString();

		// 生成订单
		Map<String, Object> resMap = new HashMap<String, Object>();
		if (toCreateOrder) {
			storeOrders.put("rider_sn", "RI" + idWorkers.getRiderOrderIdWork().nextId());
			storeOrders.put("order_sn", orderJoint.toString());
			storeOrders.put("uid", uid);
			storeOrders.put("marketid", marketId);
			storeOrders.put("rider_pay", deliverfeeBigDecimal);
			storeOrders.put("address_id", addressId);
			storeOrders.put("integral", integralSub.intValue());
			storeOrders.put("totalPrice", needPay);
			storeOrders.put("ctime", createTime);
			storeOrders.put("service_time", unixTime);
			storeOrders.put("is_appointment", isAppointment);
			storeOrders.put("original_price", storesAmountBigDecimal);
			String outTradeNo = String.valueOf(idWorkers.getOutTradeIdWork().nextId());
			storeOrders.put("out_trade_no", outTradeNo);// 生成订单的时候三方号同时生成
			storeOrders.put("market_activity_price",
					marketActivityPrice == BigDecimal.ZERO ? null : marketActivityPrice);
			storeOrders.put("store_activity_price", storeActivityPrice);
			storeOrders.put("vip_relief", isVIp == 1 ? vipFavourRiderOrder : BigDecimal.ZERO);
			storeOrders.put("pay_status", 0);
			// 回调
			// 1.支付状态变化
			// 2.订单骑手生成
			// 3.订单地址生成
			// 4.积分增加(totalPrice数值)
			// 6.发消息
			// 7.默认市场
			// 8.默认地址
			// 插入骑手

			Integer roid = orderDao.addHmRiderOrder(storeOrders);
			for (String storeId : storeIds) {
				Object sobj = storeOrders.get(storeId);
				if (sobj != null) {
					Map<String, Object> sInfo = (Map<String, Object>) sobj;
					sInfo.put("roid", roid);
					sInfo.put("pay_status", storeOrders.get("pay_status"));
					sInfo.put("order_status", storeOrders.get("rider_status"));
					Integer oid = orderDao.addHmOrder(sInfo);
					List<Map<String, Object>> cartList = (List<Map<String, Object>>) sInfo.get("cartList");
					for (Map<String, Object> cart : cartList) {
						cart.put("oid", oid);
						orderDao.addHmCart(cart);
						String smallOrderSn = (String) sInfo.get("order_sn");
						orderDao.addOrderDetail(cart, smallOrderSn);
					}
				}
			}

			// 删除购物车
			if (!orderDao.deleteBasketBySid(sids, uid)) {
				throw new NDCException.DeleteBasketExcpetion();
			}
			if (integralPay || needPay.compareTo(BigDecimal.ZERO) == 0) {
				handleROrder(RandomUtil.getRandom620(6) + outTradeNo, needPayString, "integral");
				resMap.put("roid", roid);
				resMap.put("status", 1);
			} else {
				resMap.put("roid", roid);
				resMap.put("status", 0);
			}

		} else {
			// 数据封装
			resMap.put("isMember", isVIp);
			resMap.put("marketActivity", marketActivity);
			resMap.put("marketActivityContent", marketActivityContent);
			resMap.put("showMarketActivity", showMarketActivity);
			resMap.put("fee", deliverfee);
			resMap.put("originalfee", originalfee);
			resMap.put("totalPrice", "￥" + storesAmountString);
			resMap.put("payTotal", needPayString);
			resMap.put("integralCanUse", integralCanUse);
			resMap.put("couponCanUse", couponCanUse);
			resMap.put("integralContent", integralContent);
			resMap.put("couponContent", couponContent);
			resMap.put("orderList", storeList);
			resMap.put("integralPrice", integralPriceString);
			resMap.put("marketid", marketId);
			Map<String, Object> startAndEnd = marketDao.getStartAndEnd(marketId);
			Object starttime = null;
			Object endtime = null;
			if (startAndEnd != null) {
				starttime = startAndEnd.get("starttime");
				endtime = startAndEnd.get("endtime");
			}
			if (starttime == null || endtime == null) {
				resMap.put("marketTime", "07:00-18:30");
			} else {
				resMap.put("marketTime", starttime + "-" + endtime);
			}
			if (orderId != null) {
				resMap.put("needCancelId", orderId);
				resMap.put("cancelReason", "检测到您有待支付订单使用首单优惠，是否取消让该单享受优惠，直接支付视为放弃首单优惠");
			} else {
				resMap.put("cancelReason", "");
			}
			resMap.put("priceForCoupon", priceForIntegralCoupon);
			if (isVIp == 0) {
				resMap.put("memberContent", "会员用户专享");
				resMap.put("riderPayContent", "会员用户专享");
				BigDecimal maxDeliverFeeFavor = new BigDecimal(originalfee)
						.subtract(new BigDecimal(memberSelfMentionDeliverfee));
				vipFavour = vipFavourable.add(maxDeliverFeeFavor).setScale(2, BigDecimal.ROUND_HALF_UP).toString();
				resMap.put("delMemberPrice", "开通会员，预计最高可为您节省" + vipFavour + "元");
			} else {
				vipFavour = vipFavourable.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
				if (vipFavourable.compareTo(BigDecimal.ZERO) == 0) {
					resMap.put("memberContent", "当日vip优惠额度达到上限");
					resMap.put("delMemberPrice", "");
				} else {
					resMap.put("delMemberPrice", "-￥" + vipFavour);
					String disCountStr = new BigDecimal(memberDiscount).multiply(new BigDecimal(100))
							.setScale(0, BigDecimal.ROUND_HALF_UP).toString();
					resMap.put("memberContent", "会员享" + getHan(disCountStr));
				}

				resMap.put("riderPayContent", "");
			}
		}
		return resMap;

	}

	public Integer getRidFormMarket(Integer marketId, String strategy) {
		if ("least".equals(strategy)) {
			List<Map<String, Object>> ridOrginList = orderDao.getRidsByMarketId(marketId);
			if (ridOrginList.size() == 0) {
				return -1;
			}
			// 便利店骑手对应单数统计
			for (Map<String, Object> map : ridOrginList) {
				Integer rid = (Integer) map.get("rid");
				map.put("sum", (Integer) map.get("sum") + cvOrderDao.getCVOrderNumOfRider(rid));
			}
			List<Map<String, Object>> ridList = orderDao.getRidOrderNumByMarketId(marketId,
					(int) DateUtil.getTodayZeroTime());
			if (ridList.size() == 0) {
				// 随机从ridOrginList取一个
				return (Integer) (ridOrginList.get(0).get("rid"));
			} else {
				if (ridList.size() == ridOrginList.size()) {
					// 取出ridList中最少的
					return (Integer) (ridList.get(0).get("rid"));
				} else {
					List<Integer> list1 = new ArrayList<Integer>();
					List<Integer> list2 = new ArrayList<Integer>();
					for (Map<String, Object> map : ridList) {
						list1.add((Integer) map.get("rid"));
					}
					for (Map<String, Object> map : ridOrginList) {
						list2.add((Integer) map.get("rid"));
					}
					list2.removeAll(list1);
					return list2.get(0);
				}

			}

		} else {
			return -1;
		}

	}

	@Override
	// 付款时检测商户状态服务 ext增加检测是否同一市场
	public ResultDTO<String> judgeHmShopownStatus(OrderStatusQueryDTO orderStatusQueryDTO) {
		ResultDTO<String> resultDTO = new ResultDTO<String>();
		List<OrderShopownBean> orderShopownBeans = this.dao.executeListMethod(orderStatusQueryDTO,
				"selectHmShopownStatusByPids", OrderShopownBean.class);
		// 默认返回状态匹配
		resultDTO.setFlag(true);
		resultDTO.setData("0");
		Integer marketId = 0;
		boolean flag = false;
		int isAppointMentSize = 0;
		int openSize = 0;
		int listSize = orderShopownBeans.size();
		for (OrderShopownBean orderShopownBean : orderShopownBeans) {
			if (flag == false) {
				marketId = orderShopownBean.getMarketid();
				flag = true;
			} else {
				if (!marketId.equals(orderShopownBean.getMarketid())) {
					resultDTO.setData("1");
					return resultDTO;
				}
			}
			if (1 == orderShopownBean.getStatus()) {
				resultDTO.setData("1");
				return resultDTO;
			}
			if (orderStatusQueryDTO.getStatus() == 0 && orderShopownBean.getStatus() != 0) {
				isAppointMentSize++;
			}
			if (orderStatusQueryDTO.getStatus() == 2 && orderShopownBean.getStatus() != 2) {
				openSize++;
			}
		}
		if (isAppointMentSize == 0 && openSize == 0) {
			resultDTO.setData(resultDTO.getData() + "_" + marketId);
			return resultDTO;// 状态监测通过
		}
		if (isAppointMentSize == listSize) {
			resultDTO.setData("2");// 店铺全为预约
			return resultDTO;
		}

		if (openSize == listSize) {
			resultDTO.setData("3");// 店铺全为开张
			return resultDTO;
		}
		resultDTO.setData("1");
		return resultDTO;

	}

	@Override
	public String previewOrderTime(Integer isAppointment) {
		// 传值如果传0则为没有预约就将当前时间加上48分返回. 如果传1则为预约就为当前时间加1天后的第二天的8点48精确到分.
		// 如果当前时间为这个月的最后一天,则显示为下个月的1号8点48,如果为今年的最后一天.则显示为明年的1月1号的8点48分.
		if (FinalDatas.ONE != isAppointment) {
			Calendar nowTime = Calendar.getInstance();
			nowTime.add(Calendar.MINUTE, 48);
			String format = DateUtil.HourMinute.format(nowTime.getTime());
			return format;
		} else {
			try {
				// 首先先判断今天是否为这个月的最后一天.如果不是最后一天则为第二天的08:48分
				// 获取今天的天数
				Date nowDate = new Date();
				SimpleDateFormat sdf1 = new SimpleDateFormat("HH");
				String hourString = sdf1.format(nowDate);
				Integer hour = Integer.valueOf(hourString);
				if (hour.compareTo(0) >= 0 && hour.compareTo(7) <= 0) {
					Date date = DateUtil.lastDayTime.parse(DateUtil.formateDate(nowDate) + " 00:00");
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(date);
					calendar.add(Calendar.HOUR_OF_DAY, 8);
					calendar.add(Calendar.MINUTE, 48);
					// 获取第二天的08点48分.
					String format = DateUtil.lastDayTime.format(calendar.getTime());
					return format;
				}

				String today = DateUtil.day.format(nowDate);
				// 获取这个月的月份
				String month = DateUtil.month.format(nowDate);
				// 获取这个月最后一天的天数.
				Date lastDayOfMonth = DateUtil.getLastDayOfMonth(nowDate);
				String lastDay = DateUtil.day.format(lastDayOfMonth);
				// 如果等于0那么.今天则为最后一天.那么就是月数加1后的08点48分.
				if (today.compareTo(lastDay) == 0) {
					// 如果当前是12月.则直接将月份设置为1月.如果不是则月份加1;
					Date date;
					if (FinalDatas.twelve.compareTo(month) == 0) {
						date = DateUtil.getFirstDayOfYear(DateUtil.addYearFroDate(nowDate, 1));
					} else {
						// 将月数加1
						date = DateUtil.addMonthFroDate(nowDate, 1); // 2019/05/30 20:00
					}
					// 获取今天的最小时间
					Date parse = DateUtil.lastDayTime.parse(DateUtil.formateDate(date) + " 00:00");
					Calendar nowTime = Calendar.getInstance();
					nowTime.setTime(parse);
					// 天数设置为第一天
					nowTime.set(Calendar.DATE, 1);
					nowTime.add(Calendar.HOUR_OF_DAY, 8);
					nowTime.add(Calendar.MINUTE, 48);
					String format = DateUtil.lastDayTime.format(nowTime.getTime());
					return format;
				} else {
					Date parse = DateUtil.lastDayTime.parse(DateUtil.formateDate(nowDate) + " 00:00");
					Date date = DateUtil.addDayFroDate(parse, 1);
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(date);
					calendar.add(Calendar.HOUR_OF_DAY, 8);
					calendar.add(Calendar.MINUTE, 48);
					// 获取第二天的08点48分.
					String format = DateUtil.lastDayTime.format(calendar.getTime());
					return format;
				}
			} catch (ParseException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	@Override
	public Map<String, Object> getOutTradeNoAndAmount(Integer uid, Integer orderId, String business) {
		if (business.equals("RIO")) {
			Map<String, Object> orderInfo = orderDao.getDetailByOrderId(uid, orderId);
			if (orderInfo != null) {
				orderInfo.put("out_trade_no", RandomUtil.getRandom620(6) + (String) orderInfo.get("out_trade_no"));
				orderInfo.put("body", "倪的菜商品订单支付");
				orderInfo.put("subject", "订单总价");
			}
			return orderInfo;
		} else if (business.equals("CV")) {
			Map<String, Object> orderInfo = cvOrderDao.getDetailByOrderId(uid, orderId);
			if (orderInfo != null) {
				orderInfo.put("out_trade_no",
						"CV" + RandomUtil.getRandom620(6) + (String) orderInfo.get("out_trade_no"));
				orderInfo.put("body", "倪的菜商品订单支付");
				orderInfo.put("subject", "订单总价");
			}
			return orderInfo;
		} else {
			return null;
		}
	}

	@Override
	@Transactional
	public boolean handleROrder(String out_trade_no, String total_amount, String payType) {
		int currentTime = (int) (System.currentTimeMillis() / 1000);
		if (out_trade_no.startsWith("CV")) {
			String dataNo = out_trade_no.substring(8);
			if (cvOrderDao.updateUCVOrderToS(dataNo, currentTime, payType, out_trade_no)) {
				Map<String, Object> cvUserorderDetail = cvOrderDao.getUidByOutTradeNo(out_trade_no);
				Integer uoid = (Integer) cvUserorderDetail.get("id");
				Integer uid = (Integer) cvUserorderDetail.get("uid");
				BigDecimal integralPrice = (BigDecimal) cvUserorderDetail.get("integralPrice");
				cvOrderDao.updateCVOrderToS(uoid);
				// 把订单推送至rabbitmq处理，confirm即可
				// 降级至本地处理

				// 1.把订单待接单持久化以免宕机未推给平台处理
				if (cvOrderDao.getOrderStatusByUoid(uoid) != 3) {
					SimpleDateFormat df = new SimpleDateFormat("HH:mm");// 设置日期格式
					Date nowTime = null;
					Date beginTime = null;
					Date endTime = null;
					try {
						nowTime = df.parse(df.format(new Date()));
						beginTime = df.parse("7:30");
						endTime = df.parse("18:45");
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (DateUtil.belongCalendar(nowTime, beginTime, endTime)) {
						Map<String, Object> waitDeliver = new HashMap<String, Object>();
						waitDeliver.put("id", UUID.randomUUID().toString().replaceAll("\\-", ""));
						waitDeliver.put("servercenter", propUtil.getOrderServerCenter());
						waitDeliver.put("orderId", uoid);
						cvOrderDao.addWaitDeliverOrder(waitDeliver);
						orderShedule.delayHandleCVOrder(uoid, 60);
						// 定时交由平台处理
						// 1.订单延时60s去改变deliverModel为2
						// 2.派单删除持久化记录，并推送消息给骑手
						// (宕机重启后查询持久化记录，遍历为2的重复1，2步骤，否则直接派单)
					} else {
						// 直接让商户接单
						cvOrderDao.updateCVOrderOrderStatus(uoid, currentTime);
					}
				}

				Map<String, Object> addressAndOrderSn = cvOrderDao.getAddressAndOrderSn(uoid);
				String orderSn = (String) addressAndOrderSn.get("order_sn");
				Integer addressId = (Integer) addressAndOrderSn.get("addressid");
				// 3.记录积分使用
				if (integralPrice.compareTo(BigDecimal.ZERO) > 0) {
					Integer integral = integralPrice.multiply(new BigDecimal(100)).intValue();
					integralVipDao.addIntegralLog(uid, integral, 0, currentTime);
				}
				// 4.生成地址
				OrderAddressBean addressBean = addressDao.getAddressById(addressId);
				OrderAddressOrderBean addressOrderBean = new OrderAddressOrderBean();
				BeanUtils.copyProperties(addressBean, addressOrderBean);
				addressOrderBean.setCtime(currentTime);
				addressOrderBean.setRiderSn(orderSn);
				addressDao.addOrderAddress(addressOrderBean);
				// 5.异步设置用户默认地址
				addressTask.setAddressTask(addressId, uid);
				// 2.异步推送消息给商户
				orderTask.handleCVOrderPushShop(uoid);
			}
			return true;
		} else {
			String dataNo = out_trade_no.substring(6);
			if (orderDao.updateROStatusToSuccess(dataNo, currentTime, payType, out_trade_no)) {
				Map<String, Object> rorderDetail = orderDao.getROrderIdByOutTradeNo(out_trade_no);
				Integer rorderId = (Integer) rorderDetail.get("id");
				Integer marketId = (Integer) rorderDetail.get("marketid");
				Integer addressId = (Integer) rorderDetail.get("address_id");
				String riderSn = (String) rorderDetail.get("rider_sn");
				Integer uid = (Integer) rorderDetail.get("uid");
				List<Integer> orderIds = orderDao.getOrderIdsByRoid(rorderId);
				orderDao.updateOStatus(orderIds, 1, currentTime);
				Integer rid = getRidFormMarket(marketId, "least");
				if (rid != -1) {
					// 生成骑手
					orderDao.updateroRider(rid, rorderId);
				}
				// 生成地址
				OrderAddressBean addressBean = addressDao.getAddressById(addressId);
				OrderAddressOrderBean addressOrderBean = new OrderAddressOrderBean();
				BeanUtils.copyProperties(addressBean, addressOrderBean);
				addressOrderBean.setCtime(currentTime);
				addressOrderBean.setRiderSn(riderSn);
				addressDao.addOrderAddress(addressOrderBean);
				// 增加积分
				Integer integralAdd = Double.valueOf(total_amount).intValue();
				if (!integralAdd.equals(0)) {
					integralVipDao.updateUserIntegral(uid, "+", integralAdd);
					integralVipDao.addIntegralLog(uid, integralAdd, 4, currentTime);
				}
				// 记录
				Map<String, Object> orderInfo = orderDao.getDetailByOrderId(rorderId);
				Object integralObj = orderInfo.get("integral");
				if (integralObj != null) {
					Integer integral = (Integer) integralObj;
					if (!integral.equals(0)) {
						integralVipDao.addIntegralLog(uid, integral, 0, currentTime);
					}
				}
				// 异步处理
				addressTask.setAddressTask(addressId, uid);
				addressTask.setLateMarket(marketId, uid);
				orderTask.handleOrderPush(rorderId);
			}
			return true; // 流程走完告诉支付宝不需再回调

		}
	}

	@Override
	@Transactional
	public boolean cancelOrder(Integer orderId) {
		if (orderDao.updateROStatusToTimeout(orderId)) {
			List<Integer> orderIds = orderDao.getOrderIdsByRoid(orderId);
			if (orderIds.size() != 0) {
				orderDao.updateOStatus(orderIds, 2, 0);
			}
			Map<String, Object> orderInfo = orderDao.getDetailByOrderId(orderId);
			Object integralObj = orderInfo.get("integral");
			Object couponIdObj = orderInfo.get("couponid");
			Integer uid = (Integer) orderInfo.get("uid");
			if (integralObj != null) {
				Integer integral = (Integer) integralObj;
				integralVipDao.updateUserIntegral(uid, "+", integral);
			}
			if (couponIdObj != null) {
				Integer couponId = (Integer) couponIdObj;
				orderDao.changeCouponToZero(couponId);
			}
			return true;
		}
		return false;
	}

	private Map<String, Object> getConfigByUidAndStatus(Integer uid, Integer vipStatus) {
		if (vipStatus == 0) {
			return integralVipDao.getDefualtVipConfig();
		} else {
			// 如果是会员
			return integralVipDao.getVipConfigByUid(uid);
		}
	}

	public static String getHan(String numberString) {
		StringBuilder result = new StringBuilder();
		if (numberString.length() == 2 && numberString.contains("0")) {
			numberString = numberString.substring(0, 1);
		} else if (numberString.length() == 1) {
			result.append("零点");
		}
		for (int i = 0; i < numberString.length(); i++) {
			char current = numberString.charAt(i);
			if (current == '9') {
				result.append("九");
			} else if (current == '8') {
				result.append("八");
			} else if (current == '7') {
				result.append("七");
			} else if (current == '6') {
				result.append("六");
			} else if (current == '5') {
				result.append("五");
			} else if (current == '4') {
				result.append("四");
			} else if (current == '3') {
				result.append("三");
			} else if (current == '2') {
				result.append("二");
			} else if (current == '1') {
				result.append("一");
			}
		}
		result.append("折");
		return result.toString();
	}

	@Override
	public boolean orderCheck(Integer orderId, String business) {
		if ("CV".equals(business)) {
			// 便利店地址
			Map<String, Object> storeAdress = cvOrderDao.getStoreAdressByOrderId(orderId);
			// 订单地址（如果为空不通过）
			Map<String, Object> cvOrderAddress = cvOrderDao.getCVOrderAddress(orderId);
			double longitude = Double.parseDouble((String) storeAdress.get("longitude"));
			double latitude = Double.parseDouble((String) storeAdress.get("latitude"));
			double longitude1 = Double.parseDouble((String) cvOrderAddress.get("longitude"));
			double latitude1 = Double.parseDouble((String) cvOrderAddress.get("latitude"));
			double distance = DistanceUtils.getDistance(longitude, latitude, longitude1, latitude1);
			if (distance <= 3.6) {
				return true;
			} else {
				return false;
			}
		} else if ("RIO".equals(business)) {
			// 菜场地址
			Map<String, Object> marketAdress = orderDao.getMarketAdressByOrderId(orderId);
			// 订单地址（如果为空不通过）
			Map<String, Object> orderAddress = orderDao.getOrderAddress(orderId);

			double longitude = Double.parseDouble((String) marketAdress.get("longitude"));
			double latitude = Double.parseDouble((String) marketAdress.get("latitude"));
			double longitude1 = Double.parseDouble((String) orderAddress.get("longitude"));
			double latitude1 = Double.parseDouble((String) orderAddress.get("latitude"));
			double distance = DistanceUtils.getDistance(longitude, latitude, longitude1, latitude1);
			if (distance <= 0.6) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

}
