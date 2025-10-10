package com.flash_seckill.rabbitmq;

import com.flash_seckill.pojo.entity.SeckillProductOrder;
import com.flash_seckill.service.ISeckillProductOrderService;
import com.flash_seckill.service.ISeckillProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static com.flash_seckill.config.RabbitMqConfig.*;
import static com.flash_seckill.constants.RedisConstants.PRODUCT_USER_KEY;

@Component
@Slf4j
public class OrderConsumer {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ISeckillProductService seckillProductService;
    @Autowired
    private ISeckillProductOrderService seckillProductOrderService;

    // ===== 订单同步至数据库 =====
    @RabbitListener(queues = ORDER_CREATE_QUEUE)
    public void handleOrderCreate(SeckillProductOrder order) {
        log.info("📥 收到订单消息，orderId：{}",order.getId());
        if (seckillProductOrderService.getById(order.getId()) != null){
            log.info("订单已存在，无需重复处理：{}", order.getId());
            return;
        }
        // 扣减库存，保存订单
        seckillProductService.update()
                .setSql("stock = stock - 1")
                .gt("stock", 0)
                .eq("id", order.getSeckillProductId()).update();
        seckillProductOrderService.save(order);
        log.info("📥 订单保存成功，orderId：{}",order.getId());

        // 剩余延迟时间 = 总超时时间 - (当前时间 - 订单创建时间)
        long createTime = order.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - createTime;
        int remainingDelay = (int) Math.max(0, 5 * 60 * 1000 - elapsedTime);

        // 发送延迟消息
        // 创建CorrelationData对象，设置消息类型和订单ID，用于发送失败后人工干预
        CorrelationData correlationData = new CorrelationData("TIMEOUT_" + order.getId());
        rabbitTemplate.convertAndSend(ORDER_DELAYED_EXCHANGE, ORDER_TIMEOUT_ROUTING_KEY,
                order.getId(),
                msg -> {
                    // 设置延迟时间 与 消息持久化
                    msg.getMessageProperties().setDelay(remainingDelay);
                    msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return msg;},
                correlationData
        );
        return;
    }

    // ===== 订单超时处理 =====
    @RabbitListener(queues = ORDER_TIMEOUT_QUEUE)
    public void handleOrderTimeout(Long orderId) {
        log.info("⏰ 收到订单超时消息，orderId：{}", orderId);
        // 查询订单
        SeckillProductOrder order = seckillProductOrderService.getById(orderId);
        if (order.getStatus() == 2){
            log.info("订单已取消，无需重复处理：{}", orderId);
            return;
        }
        // 取消订单
        boolean isSuccess = seckillProductOrderService.update()
                .setSql("status = 2")
                .set("cancel_time", LocalDateTime.now())
                .eq("id", orderId)
                .eq("status", 0)
                .update();
        if (isSuccess) {
            // 取消成功，恢复库存
            seckillProductService.update()
                    .setSql("stock = stock + 1")
                    .eq("id", order.getSeckillProductId())
                    .update();
            // 将用户移出set，允许用户再次下单
            stringRedisTemplate.opsForSet().remove(PRODUCT_USER_KEY + order.getSeckillProductId(), order.getUserId().toString());
            log.info("⏰ 订单超时取消成功，订单ID：{}", orderId);
        } else {
            log.info("⏰ 订单超时取消失败，订单ID：{}", orderId);
        }
    }
}
