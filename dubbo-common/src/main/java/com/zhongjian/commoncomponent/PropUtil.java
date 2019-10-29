package com.zhongjian.commoncomponent;

import javax.annotation.PostConstruct;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@Data
public class PropUtil {

    @Autowired
    private PropComponent propComponent;

    @PostConstruct
    private void setProp() {
        setYxUrl((String) propComponent.getMap().get("user.yx.msgurl"));
        setYxAccid((String) propComponent.getMap().get("user.yx.accid"));
        setYxAppKey((String) propComponent.getMap().get("user.yx.appkey"));
        setYxAppSecret((String) propComponent.getMap().get("user.yx.appsecret"));

        setAliAppId((String) propComponent.getMap().get("pay.ali.appid"));
        setAliBusinessId((String) propComponent.getMap().get("pay.ali.businessid"));
        setAliRSAPrivateKey((String) propComponent.getMap().get("pay.ali.rsaprivatekey"));
        setAliPayPublicKey((String) propComponent.getMap().get("pay.ali.paypublickey"));
        setAliNotifyUrl((String) propComponent.getMap().get("pay.ali.notifyurl"));
        setAliUrl((String) propComponent.getMap().get("pay.ali.url"));
        setAliCharset((String) propComponent.getMap().get("pay.ali.charset"));
        setAliFormat((String) propComponent.getMap().get("pay.ali.format"));
        setAliSignType((String) propComponent.getMap().get("pay.ali.signtype"));

        setWxAppUrl((String) propComponent.getMap().get("pay.wxApp.url"));
        setWxAppAppId((String) propComponent.getMap().get("pay.wxApp.appId"));
        setWxAppMchId((String) propComponent.getMap().get("pay.wxApp.mchId"));
        setWxAppNotifyUrl((String) propComponent.getMap().get("pay.wxApp.notifyUrl"));
        setWxAppKey((String) propComponent.getMap().get("pay.wxApp.key"));

        setWxAppLetsId((String) propComponent.getMap().get("pay.wxApp.appletsId"));
        setWxAppLetsKey((String) propComponent.getMap().get("pay.wxApp.appletsKey"));
        setWxAppletsNotifyUrl((String) propComponent.getMap().get("pay.wxApp.appletsNotifyUrl"));


        setWxYinHangMchId((String) propComponent.getMap().get("pay.wxYinHang.mchId"));
        setWxYinHangKey((String) propComponent.getMap().get("pay.wxYinHang.key"));
        setWxYinHangAppid((String) propComponent.getMap().get("pay.wxYinHang.appid"));
        setWxYinHangNotifyUrl((String) propComponent.getMap().get("pay.wxYinHang.notifyUrl"));
        setWxYinHangPrivateRsaKey((String) propComponent.getMap().get("pay.wxYinHang.privateRsaKey"));
        setWxYinHangPublicRsaKey((String) propComponent.getMap().get("pay.wxYinHang.publicRsaKey"));
        setWxYinHangSignType((String) propComponent.getMap().get("pay.wxYinHang.signType"));
        setWxYinHangUrl((String) propComponent.getMap().get("pay.wxYinHang.url"));
        
        setMongoIp((String) propComponent.getMap().get("mogodb.datasource.ip"));
        setMongoPort(Integer.valueOf((String) propComponent.getMap().get("mogodb.datasource.port")));
        setMongoUserName((String) propComponent.getMap().get("mogodb.datasource.username"));
        setMongoPassword((String) propComponent.getMap().get("mogodb.datasource.password"));
        setMongodbName((String) propComponent.getMap().get("mogodb.datasource.dbname"));
        
        setOriginalfee((String) propComponent.getMap().get("order.fee.originalfee"));
        setMemberDeliverfee((String) propComponent.getMap().get("order.fee.memberdeliverfee"));
        setSelfmentionDeliverfee((String) propComponent.getMap().get("order.fee.selfmentiondeliverfee"));
        setNovipselfmentionDeliverfee((String) propComponent.getMap().get("order.fee.novipselfmentiondeliverfee"));
        setOrderServerCenter((String) propComponent.getMap().get("order.distribute.servercenter"));
        setOrderFinishUrl((String) propComponent.getMap().get("order.finish.url"));
        setOrderBankListUrl((String) propComponent.getMap().get("order.banklist.url"));
        setOrderAddBankUrl((String) propComponent.getMap().get("order.addbank.url"));
        setOrderApplyUrl((String) propComponent.getMap().get("order.apply.url"));
        
        
        setWorkerId((String) propComponent.getMap().get("server.workid"));
        setDatacenterId((String) propComponent.getMap().get("server.datacenterid"));
        
        setZkAddress((String) propComponent.getMap().get("spring.dubbo.registry.address"));
    }

    private String yxUrl;
    private String yxAppKey;
    private String yxAppSecret;
    private String yxAccid;

    private String aliAppId;
    private String aliBusinessId;
    private String aliRSAPrivateKey;
    private String aliPayPublicKey;
    private String aliNotifyUrl;
    private String aliUrl;
    private String aliCharset;
    private String aliFormat;
    private String aliSignType;

    private String wxAppUrl;
    private String wxAppNotifyUrl;
    private String wxAppAppId;
    private String wxAppMchId;
    private String wxAppKey;

    private String wxAppLetsId;
    private String wxAppLetsKey;
    private String WxAppletsNotifyUrl;

    private String wxYinHangUrl;
    private String wxYinHangAppid;
    private String wxYinHangMchId;
    private String wxYinHangKey;
    private String wxYinHangSignType;
    private String wxYinHangNotifyUrl;
    private String wxYinHangPublicRsaKey;
    private String wxYinHangPrivateRsaKey;
    
    private String mongoIp;
    private Integer mongoPort;
    private String mongoUserName;
    private String mongodbName;
    private String mongoPassword;
    
    private String originalfee;
    private String memberDeliverfee;
    private String selfmentionDeliverfee;
    private String novipselfmentionDeliverfee;
    private String orderServerCenter;
    private String orderFinishUrl;
    private String orderBankListUrl;
    private String orderApplyUrl;
    private String orderAddBankUrl;
    
    private String workerId;
    private String datacenterId;

    private String zkAddress;
}
