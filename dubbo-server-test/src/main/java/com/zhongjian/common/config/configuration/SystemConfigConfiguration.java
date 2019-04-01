package com.zhongjian.common.config.configuration;

import com.zhongjian.common.config.properties.MessagePushProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author xyl
 */

@Configuration
public class SystemConfigConfiguration {

    @Bean
    public MessagePushProperties getMessagePushProperties() {
        return new MessagePushProperties();
    }
}
