package com.flash_seckill.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flash_seckill.utils.CacheUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.flash_seckill.config.RabbitMqConfig.BINLOG_QUEUE;
import static com.flash_seckill.constants.RedisConstants.CACHE_PRODUCT_KEY;

@Slf4j
@Component
public class BinlogConsumer {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private CacheUtil cacheUtil;

    // ===== 监听product表的binlog =====
    @RabbitListener(queues = BINLOG_QUEUE)
    public void handleProductBinlog(byte[] body) {
        try {
            // 将body转为字符串（canal发送的消息未设置contentType:application/json），所以需要手动转换为字符串
            String json = new String(body, StandardCharsets.UTF_8);
            // 解析 json 至 Map（使用ObjectMapper手动反序列化）
            Map<String, Object> binlogData = new ObjectMapper().readValue(json, Map.class);
            // 获取操作类型
            String type = (String) binlogData.get("type");
            Long productId = null;
            // 提取商品信息
            List<Map<String, Object>> data = (List<Map<String, Object>>) binlogData.get("data");
            // 获取商品id
            Object id = data.get(0).get("id");
            productId = id == null ? null : Long.valueOf(id.toString());

            // ===== 新增数据 添加到布隆过滤器 =====
            if ("INSERT".equals(type)) {
                if (productId != null) {
                    // 将数据添加到布隆过滤器
                    cacheUtil.addToBloomFilter(productId);
                    log.info("INSERT 添加商品至布隆过滤器: {}", productId);
                }
                return;
            }
            // ===== 删除数据 直接删缓存 =====
            if ("DELETE".equals(type)) {
                // 直接删缓存
                if (productId != null) {
                    stringRedisTemplate.delete(CACHE_PRODUCT_KEY + productId);
                    log.info("DELETE 删除商品缓存: {}", CACHE_PRODUCT_KEY + productId);
                }
                return;
            }
            // ===== 更新数据 判断是否修改敏感字段 =====
            if ("UPDATE".equals(type)) {
                List<Map<String, Object>> oldList = (List<Map<String, Object>>) binlogData.get("old");
                Map<String, Object> old = oldList == null || oldList.isEmpty() ? null : oldList.get(0);
                // 设置敏感字段字段（只有更新这些字段才会删除缓存，排除stock和update_time）
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
                return;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
