package com.flash_seckill;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@MapperScan("com.flash_seckill.mapper")
@EnableScheduling
public class FlashSeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashSeckillApplication.class, args);
    }

}
