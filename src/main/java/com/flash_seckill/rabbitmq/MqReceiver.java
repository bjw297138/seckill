package com.flash_seckill.rabbitmq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flash_seckill.pojo.entity.SeckillProductOrder;
import com.flash_seckill.service.ISeckillProductOrderService;
import com.flash_seckill.service.ISeckillProductService;
import com.flash_seckill.utils.CacheUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.flash_seckill.constants.RedisConstants.CACHE_PRODUCT_KEY;


@Slf4j
@Component
@Transactional
public class MqReceiver {

    @Autowired
    private ISeckillProductService seckillProductService;
    @Autowired
    private ISeckillProductOrderService seckillProductOrderService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private CacheUtil cacheUtil;

    // 监听订单创建队列
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "order.create.queue", durable = "true"),
            exchange = @Exchange(name = "order.direct"),
            key = "order.create"
    ))
    public void handleOrderCreate(SeckillProductOrder order) {
        // 扣减库存, 保存订单
        seckillProductService.update()
                .setSql("stock = stock - 1")
                .gt("stock", 0)
                .eq("id", order.getSeckillProductId()).update();
        seckillProductOrderService.save(order);

        // 发送延迟消息，取消超时未支付订单（5分钟超时）
        // 计算剩余延迟时间 = 总超时时间 - (当前时间 - 订单创建时间)
        long createTime = order.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - createTime;
        int remainingDelay = (int) Math.max(0, 5 * 60 * 1000 - elapsedTime); // 确保不为负数

        rabbitTemplate.convertAndSend("order.delayed.direct", "order.timeout", order.getId(), message -> {
            message.getMessageProperties().setDelay(remainingDelay);
            return message;
        });
        log.info("订单同步成功，订单ID：{}", order.getId());
    }


    // 监听订单超时取消队列
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "order.timeout.queue", durable = "true"),
            exchange = @Exchange(name = "order.delayed.direct", delayed = "true"),
            key = "order.timeout"
    ))
    public void handleOrderTimeoutCancel(Long orderId) {
        // 如果未支付，支付超时，取消订单
        boolean isSuccess = seckillProductOrderService.update()
                .setSql("status = 2")
                .set("cancel_time", LocalDateTime.now())
                .eq("id", orderId)
                .eq("status", 0)
                .update();
        if (isSuccess) {
            // 取消成功，查询订单，获取秒杀商品ID
            SeckillProductOrder seckillProductOrder = seckillProductOrderService.getById(orderId);
            // 恢复库存
            seckillProductService.update()
                    .setSql("stock = stock + 1")
                    .eq("id", seckillProductOrder.getSeckillProductId())
                    .update();
            // 将用户移出set，允许用户再次下单
            stringRedisTemplate.opsForSet().remove("seckill:order:" + seckillProductOrder.getSeckillProductId(), seckillProductOrder.getUserId().toString());
            log.info("订单超时取消成功，订单ID：{}", orderId);
        } else {
            log.info("订单超时取消失败，订单ID：{}", orderId);
        }
    }

    // 监听product表的binlog 队列
    @RabbitListener(queues = "binlog.queue")
    public void handleProductBinlog(String json) {
        try {
            // 解析 json 至 Map
            Map<String, Object> binlogData = new ObjectMapper().readValue(json, Map.class);
            // 获取操作类型
            String type = (String) binlogData.get("type");
            Long productId = null;
            // 提取商品信息
            List<Map<String, Object>> data = (List<Map<String, Object>>) binlogData.get("data");
            if (data != null && !data.isEmpty()) {
                Object id = data.get(0).get("id");
                // 获取商品ID
                productId = id == null ? null : Long.valueOf(id.toString());
            }

            // 新增数据 将数据添加到布隆过滤器
            if ("INSERT".equals(type)) {
                if (productId != null) {
                    // 将数据添加到布隆过滤器
                    cacheUtil.addToBloomFilter(productId);
                    log.info("INSERT 添加商品至布隆过滤器: {}", productId);
                }
                return;
            }

            // 删除数据 直接删缓存
            if ("DELETE".equals(type)) {
                // 直接删缓存
                if (productId != null) {
                    stringRedisTemplate.delete(CACHE_PRODUCT_KEY + productId);
                    log.info("DELETE 事件，已删除商品缓存: {}", CACHE_PRODUCT_KEY + productId);
                }
                return;
            }

            // 更新数据 判断是否修改敏感字段
            if ("UPDATE".equals(type)) {
                List<Map<String, Object>> oldList = (List<Map<String, Object>>) binlogData.get("old");
                Map<String, Object> old = oldList == null || oldList.isEmpty() ? null : oldList.get(0);

                // 缓存关心的字段
                Set<String> SENSITIVE_FIELDS = Set.of("name", "img", "desc", "price", "start_time", "end_time");

                // old 不为空 且 有任一敏感字段被修改
                if (old != null && !Collections.disjoint(old.keySet(), SENSITIVE_FIELDS)) {
                    if (productId != null) {
                        stringRedisTemplate.delete(CACHE_PRODUCT_KEY + productId);
                        log.info("UPDATE 修改了缓存敏感字段，已删除商品缓存: {}", CACHE_PRODUCT_KEY + productId);
                    }
                } else {
                    log.debug("UPDATE 仅修改了非敏感字段，跳过缓存删除");
                }
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}