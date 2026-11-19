package com.yujing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class YujingApplication {

    public static void main(String[] args) {
        // The server stores all timestamps in UTC; display conversion belongs to clients.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(YujingApplication.class, args);
    }
}
