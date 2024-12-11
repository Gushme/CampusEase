package com.CampusEase;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.CampusEase.mapper")
@SpringBootApplication
@EnableRabbit
public class CampusEaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusEaseApplication.class, args);
    }

}
