package com.zhongjian.service.pay;

import java.util.Map;

public interface GenerateSignatureService {

    String getAliSignature(String out_trade_no, String totalAmount, String subject);

    Map<String, String> getWxAppSignature(String outTradeNo, String totalPrice, String openId, String spbillCreateIp, Integer type,String body);

    String getYinHangWxApp(String outTrandeNo, String totalPrice,  String spbillCreateIp,String body);

    Map<String, String> getFlPayData(String outTrandeNo,String totalPrice,String subject);

}
