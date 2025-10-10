package com.flash_seckill.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    // 创建消息转换器，JSON格式
    @Bean
    public MessageConverter messageConverter(){
        ObjectMapper objectMapper = new ObjectMapper();
        // 注册JavaTimeModule来处理LocalDateTime等时间类型
        objectMapper.registerModule(new JavaTimeModule());
        // 禁用时间戳序列化
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
