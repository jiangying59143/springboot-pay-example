package com.example.pay;

import com.example.pay.configuration.AlipayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
public class SpringbootPayExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootPayExampleApplication.class, args);
    }
}
