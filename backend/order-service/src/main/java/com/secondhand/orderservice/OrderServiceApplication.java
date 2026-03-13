package com.secondhand.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        System.out.println("Hello ");
        SpringApplication.run(OrderServiceApplication.class, args);
        System.out.println("Hú hú in ra mới đi");
    }

}
