package com.secondhand.coreservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class CoreServiceApplication {

    public static void main(String[] args) {
        System.out.println("VẬy là xong rồi sao?");
        SpringApplication.run(CoreServiceApplication.class, args);
        System.out.println("hahahhahaahah");
        System.out.println("xin chao");

    }

}
