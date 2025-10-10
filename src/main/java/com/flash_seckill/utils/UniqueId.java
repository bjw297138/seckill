package com.flash_seckill.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class UniqueId {

    // 开始时间戳
    private static final long BEGIN_TIMESTAMP = 1640995200L;
    // 序列号位数
    private static final int COUNT_BITS = 32;

    private final StringRedisTemplate stringRedisTemplate;

    public UniqueId(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // 生成id
    public long nextId(String keyprefix) {
        // 时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowsecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowsecond - BEGIN_TIMESTAMP;
        // 序列号
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 指定用于生成什么的id
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyprefix + ":" + date);

        return timestamp << COUNT_BITS | count;
    }
}
