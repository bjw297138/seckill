package com.flash_seckill.rabbitmq;

import com.flash_seckill.pojo.entity.SeckillProductOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.flash_seckill.config.RabbitMqConfig.DLQ_ORDER_CREATE;
import static com.flash_seckill.config.RabbitMqConfig.DLQ_ORDER_TIMEOUT;

@Component
@Slf4j
public class DeadLetterConsumer {

    // 订单创建死信消息
    @RabbitListener(queues = DLQ_ORDER_CREATE)
    public void handleOrderCreateDL(SeckillProductOrder order) {
        log.info("❌ 订单创建死信消息：订单id={}，用户id={}，商品id={}", order.getId(),order.getUserId(),order.getSeckillProductId());
    }

    // 订单超时死信消息
    @RabbitListener(queues = DLQ_ORDER_TIMEOUT)
    public void handleOrderTimeoutDL(Long orderId) {
        log.info("❌ 订单超时死信消息：订单id={}", orderId);
    }
}
