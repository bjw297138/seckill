package com.flash_seckill.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    // 创建redisson客户端
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()//说明当前用的是单节点的redis
                .setAddress("redis://localhost:6379")
                .setPassword("123456");
        return Redisson.create(config);
    }

}