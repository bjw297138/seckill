package com.flash_seckill.utils;


import com.flash_seckill.mapper.SeckillProductMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static com.flash_seckill.constants.RedisConstants.*;

@Slf4j
@Component
public class TimedTask {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private SeckillProductMapper seckillProductMapper;

    // 每5分钟更新一次秒杀商品热度排行榜（前15名）
    @Scheduled(cron = "0 * * * * ?")
    public void updatePopularity() {
        // 获取锁
        RLock lock = redissonClient.getLock(LOCK_HEAT_KEY);
        try {
            if (lock.tryLock(0, 10, TimeUnit.SECONDS)) {
                Collection<Long> ids = seckillProductMapper.idList();
                // 遍历所有seckillProduct
                for (Long id : ids) {
                    String heatKey = PRODUCT_HEAT_KEY + id + ":";
                    // 获取点击数
                    Long clickCount = stringRedisTemplate.opsForValue().get(heatKey + "clickCount") == null
                            ? 0 : Long.parseLong(stringRedisTemplate.opsForValue().get(heatKey + "clickCount").toString());
                    // 获取收藏数
                    Long collectCount = stringRedisTemplate.opsForValue().get(heatKey + "collectCount") == null
                            ? 0 : Long.parseLong(stringRedisTemplate.opsForValue().get(heatKey + "collectCount").toString());
                    // 计算热度
                    long heatValue = (long) (clickCount + collectCount * 3);
                    // 更新Redis有序集合
                    stringRedisTemplate.opsForZSet().add(RANK_ALL_KEY, id.toString(), heatValue);
                    // 保留前15名
                    stringRedisTemplate.opsForZSet().removeRange(RANK_ALL_KEY, 0, -16);

                }
            }
            log.info("更新排行榜成功");
        } catch (Exception e) {
            log.error("更新失败");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

}
