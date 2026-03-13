package com.secondhand.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        System.out.println("Rebuild auth service successfull");
        SpringApplication.run(AuthServiceApplication.class, args);
        System.out.println("WTf re build ok ko vay troi");
    }

}
