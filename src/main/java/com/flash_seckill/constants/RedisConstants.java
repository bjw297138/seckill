package com.flash_seckill.constants;

public class RedisConstants {

    // 缓存空值过期时间
    public static final Long CACHE_NULL_TTL = 2L;
    // 缓存商品信息key
    public static final String CACHE_PRODUCT_KEY = "cache:product:";
    // 热度排行榜 分布式锁 key
    public static final String LOCK_HEAT_KEY = "lock:heat";
    // 热度 key
    public static final String PRODUCT_HEAT_KEY = "product:heat:";
    // 热度排行榜
    public static final String RANK_ALL_KEY = "rank:all";
    // 商品收藏set
    public static final String PRODUCT_COLLECT_KEY = "product:collect:";
    // 商品库存
    public static final String PRODUCT_STOCK_KEY = "product:stock:";
    // 商品下单用户set
    public static final String PRODUCT_USER_KEY = "product:user:";
}
