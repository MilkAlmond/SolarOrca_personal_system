package com.yujing.config;

import com.yujing.security.OwnerIdGuardInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisConfig {

    @Bean
    public OwnerIdGuardInterceptor ownerIdGuardInterceptor() {
        return new OwnerIdGuardInterceptor();
    }
}
