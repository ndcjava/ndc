package com.zhongjian;

import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;
import com.zhongjian.common.util.LogUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.swing.*;

@EnableTransactionManagement
@ServletComponentScan
@SpringBootApplication(scanBasePackages = "com.zhongjian", exclude = {DataSourceAutoConfiguration.class})
@EnableDubbo
public class NDCServiceApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(NDCServiceApplication.class, args);
        if (run.isActive()) {
            LogUtil.info("ndc-application", "启动完成");
        }
    }

}
