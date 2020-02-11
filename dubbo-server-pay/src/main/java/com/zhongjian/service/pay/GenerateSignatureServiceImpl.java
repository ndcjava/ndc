package com.zhongjian.service.pay;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.csii.upay.api.CSIIUPayAPIException;
import com.csii.upay.api.factory.CSIIBeanFactory;
import com.csii.upay.api.net.Client;
import com.csii.upay.api.request.IPEMRequest;
import com.csii.upay.api.request.IQSRRequest;
import com.csii.upay.api.request.MerTransDetail;
import com.zhongjian.commoncomponent.PropUtil;
import com.zhongjian.dao.jdbctemplate.OrderDao;
import com.zhongjian.util.*;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.*;

@Service("generateSignatureService")
public class GenerateSignatureServiceImpl implements GenerateSignatureService {

    @Resource
    private PropUtil propUtil;
    
    @Resource
    private OrderDao orderDao;

    @Override
    public String getAliSignature(String out_trade_no, String totalAmount,String subject) {
        //生成签名
        Map<String, String> orderMap = new LinkedHashMap<String, String>(); // 订单实体
        /****** 2.商品参数封装开始 *****/ // 手机端用
        // 商户订单号，商户网站订单系统中唯一订单号，必填
        orderMap.put("out_trade_no", out_trade_no);
        orderMap.put("subject", subject);
        // 付款金额，必填
        orderMap.put("total_amount", totalAmount);
        // 商品描述，可空
        orderMap.put("body", "本次订单花费" + totalAmount + "元");
        // 超时时间 可空
        orderMap.put("timeout_express", "15m");
        // 销售产品码 必填
        orderMap.put("product_code", "QUICK_MSECURITY_PAY");
        // 实例化客户端
        AlipayClient client = new DefaultAlipayClient(propUtil.getAliUrl(), propUtil.getAliAppId(),
                propUtil.getAliRSAPrivateKey(), propUtil.getAliFormat(), propUtil.getAliCharset(), propUtil.getAliPayPublicKey(),
                propUtil.getAliSignType());
        // 实例化具体API对应的request类,类名称和接口名称对应,当前调用接口名称：alipay.trade.app.pay
        AlipayTradeAppPayRequest ali_request = new AlipayTradeAppPayRequest();
        // SDK已经封装掉了公共参数，这里只需要传入业务参数。以下方法为sdk的model入参方式(model和biz_content同时存在的情况下取biz_content)。
        AlipayTradeAppPayModel model = new AlipayTradeAppPayModel();
        model.setPassbackParams(URLEncoder.encode((String) orderMap.get("body").toString()));
        // 描述信息 添加附加数据
        model.setBody(orderMap.get("body")); // 商品信息
        model.setSubject(orderMap.get("subject")); // 商品名称
        model.setOutTradeNo(orderMap.get("out_trade_no")); // 商户订单号(自动生成)
        model.setTimeoutExpress(orderMap.get("timeout_express")); // 交易超时时间
        model.setTotalAmount(orderMap.get("total_amount")); // 支付金额
        model.setProductCode(orderMap.get("product_code")); // 销售产品码
        ali_request.setBizModel(model);
        ali_request.setNotifyUrl(propUtil.getAliNotifyUrl()); // 回调地址
        AlipayTradeAppPayResponse response = null;
        String orderString = null;
        try {
            response = client.sdkExecute(ali_request);
            orderString = response.getBody();
        } catch (AlipayApiException e) {
            return null;
        }
        return orderString;
    }

    @Override
    public Map<String, String> getWxAppSignature(String outTradeNo, String totalPrice, String openId, String spbillCreateIp, Integer type,String body) {
        SortedMap<String, String> stringObjectSortedMap = null;
        try {
            stringObjectSortedMap = PayCommonUtil.wxAppOrAppletsPay(outTradeNo, totalPrice, spbillCreateIp, propUtil.getWxAppLetsId(), propUtil.getWxAppAppId(), propUtil.getWxAppKey(), propUtil.getWxAppLetsKey(), propUtil.getWxAppMchId(), propUtil.getWxAppNotifyUrl(), propUtil.getWxAppletsNotifyUrl(), propUtil.getWxAppUrl(), body, openId, type);
        } catch (Exception e) {
            LogUtil.info("获取签名异常", "e:" + e.getMessage());
            e.getMessage();
        }
        return stringObjectSortedMap;
    }

    @Override
    public String getYinHangWxApp(String outTrandeNo, String totalPrice,  String spbillCreateIp,String body) {
        SortedMap<String, String> finalpackage = new TreeMap<>();
        finalpackage.put("mch_id", propUtil.getWxYinHangMchId());
        finalpackage.put("notify_url", propUtil.getWxYinHangNotifyUrl());
        finalpackage.put("sign_type", "RSA_1_256");
        finalpackage.put("charset", "UTF-8");
        finalpackage.put("nonce_str", String.valueOf(new Date().getTime()));
        finalpackage.put("sub_appid", propUtil.getWxYinHangAppid());
        finalpackage.put("service", "pay.weixin.raw.app");
        finalpackage.put("appid", propUtil.getWxYinHangAppid());
        finalpackage.put("out_trade_no", outTrandeNo);
        finalpackage.put("body", body);
        BigDecimal total = new BigDecimal(totalPrice).multiply(new BigDecimal(100));
        DecimalFormat df = new DecimalFormat("0");
        finalpackage.put("total_fee", df.format(total));
        finalpackage.put("mch_create_ip", spbillCreateIp);
        finalpackage.put("version", "2.0");
        Map<String, String> params = PayCommonUtil.paraFilter(finalpackage);
        StringBuilder buf = new StringBuilder((params.size() + 1) * 10);
        PayCommonUtil.buildPayParams(buf, params, false);
        String preStr = buf.toString();
        finalpackage.put("sign", PayCommonUtil.getSign("RSA_1_256", preStr, propUtil.getWxYinHangPrivateRsaKey()));
        String requestXml = PayCommonUtil.getRequestXml(finalpackage);

        String responseResultForBase = null;
        try {
            responseResultForBase = HttpClientUtil.getResponseResultForBase(propUtil.getWxYinHangUrl(), requestXml, "UTF-8", RequestTypeEnum.JSON);
            Map<String, String> result = PayCommonUtil.xmlToMap(responseResultForBase);
            return result.get("pay_info");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return responseResultForBase;
    }

	@Override
	public Map<String, String> getFlPayData(String outTrandeNo, String totalPrice, String subject) {
		orderDao.addOrder(new BigDecimal(totalPrice), outTrandeNo);
		IPEMRequest ipemReq = new IPEMRequest(); 
		ipemReq.setMerchantId(propUtil.getMerchantId());
		ipemReq.setMerSeqNo(outTrandeNo);
		ipemReq.setOrderId(outTrandeNo);
		ipemReq.setTransAmt(new BigDecimal(totalPrice));
		ipemReq.setMerDateTime(new Date());
		ipemReq.setCurrency("01");
		ipemReq.setIsFoisonPay("1");
		ipemReq.setMerURL(propUtil.getFlNotifyUrl());
		ipemReq.setMerURL1("https://java-api.nidcai.com/#/pages/paySuccess/paySuccess");
		List<MerTransDetail> list = new ArrayList<>();
		MerTransDetail merTransDetail = new MerTransDetail();
		merTransDetail.setSubMerchantId(propUtil.getSubMerchantId());
		merTransDetail.setSubMerSeqNo(outTrandeNo + "1");
		merTransDetail.setSubMerDateTime(new Date());
		merTransDetail.setSubTransAmt(new BigDecimal(totalPrice));
		list.add(merTransDetail);
		ipemReq.setMerTransList(list);
		String plain = "";
		String signature = "";
		try {
			 plain = ipemReq.getPlain();
			 signature = ipemReq.getSignature();
		} catch (CSIIUPayAPIException e) {
		}
		HashMap<String,String> map = new HashMap<>();
		map.put("Plain", plain);
		map.put("Signature", signature);
		return map;
		
	}

	@Override
	public void queryFlPayData() {
		IQSRRequest iqsrRequest = new IQSRRequest();
		iqsrRequest.setMerchantId(propUtil.getMerchantId());
		iqsrRequest.setSubMerchantId(propUtil.getSubMerchantId());
		iqsrRequest.setMerSeqNo("");
		iqsrRequest.setMerTransDate(new Date());
		iqsrRequest.setMerTransAmt(new BigDecimal("6"));
		
		try {
			Client client = CSIIBeanFactory.getInstance().getDefaultClient();
			client.post(iqsrRequest);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
