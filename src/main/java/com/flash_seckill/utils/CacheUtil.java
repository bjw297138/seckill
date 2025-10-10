package com.flash_seckill.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.flash_seckill.mapper.SeckillProductMapper;
import com.flash_seckill.pojo.entity.SeckillProduct;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.flash_seckill.constants.RedisConstants.CACHE_NULL_TTL;
import static com.flash_seckill.constants.RedisConstants.CACHE_PRODUCT_KEY;

@Slf4j
@Component
public class CacheUtil {


    @Autowired
    private SeckillProductMapper seckillProductMapper;

    private final StringRedisTemplate stringRedisTemplate;

    public CacheUtil(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // 设置缓存
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    // 从缓存中查询数据
    public <T> T getProduct(String keyPrefix, Long id, Class<T> type,
                          Function<Long, T> dbFallback) {
        // 布隆过滤 判断查询是否为已有商品
        if (!bloomFilter.mightContain(id)) {
            return null;
        }
        // 从Redis中查询商品数据
        String key = keyPrefix + id;
        String jsonStr = stringRedisTemplate.opsForValue().get(key);

        T t = null;
        // 判断缓存是否命中
        if (StrUtil.isNotBlank(jsonStr)) {
            // 缓存命中，直接返回店铺数据
            t = JSONUtil.toBean(jsonStr, type);
            return t;
        }
        // jsonStr 为 null 或者 ""
        if (Objects.nonNull(jsonStr)) {
            // 当前数据是""（说明该数据是之前缓存的空对象），直接返回失败信息
            return null;
        }
        // 当前数据是null，则从数据库中查询店铺数据
        t = dbFallback.apply(id);

        // 判断数据库是否存在商品
        if (Objects.isNull(t)) {
            // 数据库中不存在，缓存空对象（解决缓存穿透），返回失败信息
            this.set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 数据库中存在，重建缓存，并返回秒杀商品数据
        // 缓存永不过期
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(t));
        return t;
    }

    // 插入数据个数
    private static int size = 10000;
    // 期望错误率
    private static double fpp = 0.01;
    // 初始化布隆过滤器
    private static BloomFilter<Long> bloomFilter = BloomFilter.create(Funnels.longFunnel(), size, fpp);

    // 初始化布隆过滤器
    @PostConstruct
    public void initBloomFilter() {
        // 获取所有seckillProduct的id到集合
        Collection<Long> ids = seckillProductMapper.idList();
        // 将所有有效ID添加到布隆过滤器
        for (Long id : ids) {
            log.info("初始化布隆过滤器，添加id：{}", id);
            bloomFilter.put(id);
        }
    }

    // 添加ID至布隆过滤器
    public void addToBloomFilter(Long id) {
        bloomFilter.put(id);
    }

    // 缓存预热
    public void preheat() {
        Collection<Long> ids = seckillProductMapper.idList();
        for (Long id : ids) {
            SeckillProduct seckillProduct = seckillProductMapper.selectById(id);
            stringRedisTemplate.opsForValue().set(CACHE_PRODUCT_KEY + id, JSONUtil.toJsonStr(seckillProduct));
        }
    }
}
