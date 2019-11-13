package com.zhongjian.service.order;

import java.net.URISyntaxException;
import java.util.Map;

import com.zhongjian.dto.common.ResultDTO;
import com.zhongjian.dto.order.order.query.OrderStatusQueryDTO;
import com.zhongjian.exception.NDCException;

public interface OrderService {

    //type 1选用积分 extra=null 2选用优惠券 extra = couponId
    Map<String, Object> previewOrCreateOrder(Integer uid, Integer sids[], String type,
                                             Integer extra, String isSelfMention, boolean toCreateOrder,
                                             Integer addressId, Integer unixTime, Integer isAppointment,String deviceId) throws NDCException;

    /**
     * 判断所有商铺是不是指定状态
     *
     * @param orderStatusQueryDTO
     * @return
     */
    ResultDTO<String> judgeHmShopownStatus(OrderStatusQueryDTO orderStatusQueryDTO);

    /**
     * 获取预约时间
     *
     * @param isAppointment
     * @return
     */
    String previewOrderTime(Integer isAppointment);

    Map<String, Object> getOutTradeNoAndAmount(Integer uid, Integer orderId, String business);
    
    /**
	 *订单回调处理核心
	 *
	 * @param out_trade_no
	 * @param total_amount
	 * @return orderId
	 */
    boolean handleROrder(String out_trade_no,String total_amount,String payType);
    
    boolean cancelOrder(Integer orderId);
    
    public Integer getRidFormMarket(Integer marketId, String strategy);
    
    boolean orderCheck(Integer orderId, String business);
    
    /**
             * 菜场收佣判断
     */
    boolean isCommission(Integer sid);
    
    /**
    *获取菜场id
   */
    Integer getMarketIdByPid(Integer sid);
    
    void createFalseRorder(Integer marketid, Integer uid, Integer addressid);
    
    void createRorderShedule(Integer marketid, Integer uid, Integer addressid);
    
    void withdraw() throws URISyntaxException;
}
