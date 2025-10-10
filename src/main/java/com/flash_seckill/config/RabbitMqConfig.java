package com.flash_seckill.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Slf4j
@Configuration
public class RabbitMqConfig {
    // 交换机
    public static final String ORDER_CREATE_EXCHANGE = "order.exchange";
    public static final String ORDER_DELAYED_EXCHANGE = "order.delayed.exchange";
    public static final String DLX_EXCHANGE = "dlx.exchange";

    // 队列
    public static final String ORDER_CREATE_QUEUE = "order.create.queue";
    public static final String ORDER_TIMEOUT_QUEUE = "order.timeout.queue";
    public static final String BINLOG_QUEUE = "binlog.queue";
    public static final String DLQ_ORDER_CREATE = "order.create.dlq";
    public static final String DLQ_ORDER_TIMEOUT = "order.timeout.dlq";


    // 路由键
    public static final String ORDER_CREATE_ROUTING_KEY = "order.create";
    public static final String ORDER_TIMEOUT_ROUTING_KEY = "order.timeout";
    public static final String DLQ_ORDER_CREATE_KEY = "order.create.dlq";
    public static final String DLQ_ORDER_TIMEOUT_KEY = "order.timeout.dlq";

    // ===== 交换机 =====
    @Bean
    public DirectExchange orderExchange(){
        // 订单交换机 持久化 不自动删除
        return new DirectExchange(ORDER_CREATE_EXCHANGE, true, false);
    }
    @Bean
    public DirectExchange ORDER_DELAYED_EXCHANGE(){
        // 订单延迟消息交换机 持久化 不自动删除
        return new DirectExchange(ORDER_DELAYED_EXCHANGE, true, false);
    }
    @Bean
    public DirectExchange dlxExchange(){
        // 死信交换机 持久化 不自动删除
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    // ===== 订单创建队列 & dlq =====
    @Bean
    public Queue orderCreateQueue(){
        // 订单创建队列 持久化 不自动删除，设置死信交换机和路由键
        return QueueBuilder.durable(ORDER_CREATE_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ORDER_CREATE_KEY)
                .build();
    }
    @Bean
    public Queue orderCreateDlq(){
        // 订单创建死信队列 持久化 不自动删除
        return QueueBuilder.durable(DLQ_ORDER_CREATE).build();
    }
    @Bean
    public Binding orderCreateBinding(){
        // 绑定订单创建队列到订单交换机
        return BindingBuilder.bind(orderCreateQueue())
                .to(orderExchange())
                .with(ORDER_CREATE_ROUTING_KEY);
    }
    @Bean
    public Binding orderCreateDlqBinding(){
        // 绑定死信队列到死信交换机
        return BindingBuilder.bind(orderCreateDlq())
                .to(dlxExchange())
                .with(DLQ_ORDER_CREATE_KEY);
    }

    // ===== 订单超时队列 & dlq =====
    @Bean
    public Queue orderTimeoutQueue(){
        // 订单超时队列 持久化 非排他 不自动删除
        return QueueBuilder.durable(ORDER_TIMEOUT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ORDER_TIMEOUT_KEY)
                .build();
    }
    @Bean
    public Queue orderTimeoutDlq(){
        // 订单超时死信队列 持久化 不自动删除
        return QueueBuilder.durable(DLQ_ORDER_TIMEOUT).build();
    }
    @Bean
    public Binding orderTimeoutBinding(){
        // 绑定订单超时队列到订单延迟交换机
        return BindingBuilder.bind(orderTimeoutQueue())
                .to(ORDER_DELAYED_EXCHANGE())
                .with(ORDER_TIMEOUT_ROUTING_KEY);
    }
    @Bean
    public Binding orderTimeOutDlqBinding(){
        // 绑定死信队列到死信交换机
        return BindingBuilder.bind(orderTimeoutDlq())
                .to(dlxExchange())
                .with(DLQ_ORDER_TIMEOUT_KEY);
    }

    // ===== RabbitTemplate（发送端） =====
    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        // 设置json消息转换器
        template.setMessageConverter(jsonMessageConverter());
        // 设置消息发布确认回调处理器
        template.setConfirmCallback(((correlationData, ack, cause) -> {
            String[] str = correlationData.getId().split("_");
            String type = str[0];
            String orderId = str[1];
            if (ack) {
                log.info(type + "消息发送成功，orderId: {}", orderId);
            } else {
                // 人工干预
                log.error(type + "消息发送失败, orderId: {}, cause: {}", orderId, cause);
            }
        }));
        return template;
    }

    // ===== 创建消息转换器 支持LocalDateTime =====
    @Bean
    public MessageConverter jsonMessageConverter(){
        // 处理时间戳
        ObjectMapper objectMapper = new ObjectMapper();
        // 注册JavaTimeModule来处理LocalDateTime等时间类型
        objectMapper.registerModule(new JavaTimeModule());
        // 禁用时间戳序列化
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
