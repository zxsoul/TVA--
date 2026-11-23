package com.tva.startrail;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.tva.startrail.mapper")
public class TvaApplication {

    public static void main(String[] args) {
        SpringApplication.run(TvaApplication.class, args);
    }
}
