package com.deltaforce.manager;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.deltaforce.manager.mapper")
@EnableAsync
public class DeltaForceManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeltaForceManagerApplication.class, args);
    }
}
