package com.ecommerce.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = "com.ecommerce")
@EnableAsync
public class AplicacionEcommerce {

    public static void main(String[] args) {
        SpringApplication.run(AplicacionEcommerce.class, args);
    }
}
