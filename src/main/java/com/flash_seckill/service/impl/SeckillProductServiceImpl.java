package com.flash_seckill.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flash_seckill.pojo.dto.SeckillProductDTO;
import com.flash_seckill.pojo.entity.SeckillProduct;
import com.flash_seckill.exception.BusinessException;
import com.flash_seckill.exception.ErrorCode;
import com.flash_seckill.mapper.SeckillProductMapper;

import com.flash_seckill.service.ISeckillProductService;
import com.flash_seckill.utils.CacheUtil;
import com.flash_seckill.pojo.vo.ProductDetailVO;
import com.flash_seckill.pojo.vo.ProductListVO;
import com.flash_seckill.pojo.vo.RankVO;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static com.flash_seckill.constants.RedisConstants.*;

@Service
public class SeckillProductServiceImpl extends ServiceImpl<SeckillProductMapper, SeckillProduct> implements ISeckillProductService {

    @Autowired
    private CacheUtil cacheUtil;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 添加秒杀商品
    @Override
    @Transactional
    public void addProduct(SeckillProductDTO seckillProductDTO) {
        SeckillProduct seckillProduct = BeanUtil.copyProperties(seckillProductDTO, SeckillProduct.class);
        save(seckillProduct);
        // 添加商品库存至redis
        stringRedisTemplate.opsForValue().set(PRODUCT_STOCK_KEY + seckillProduct.getId(),String.valueOf(seckillProduct.getStock()));
    }

    // 收藏商品
    @Override
    public void collect(Long id) {
        // 检查商品是否存在
        SeckillProduct seckillProduct = getById(id);
        if (seckillProduct == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // 检查用户是否已收藏
        if (stringRedisTemplate.opsForSet().isMember(PRODUCT_COLLECT_KEY + id, String.valueOf(userId))) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "已收藏该商品");
        }
        // 增加收藏数
        stringRedisTemplate.opsForValue().increment(PRODUCT_HEAT_KEY + id + ":collectCount", 1);
        // 添加收藏者id到收藏集合
        stringRedisTemplate.opsForSet().add(PRODUCT_COLLECT_KEY + id, String.valueOf(userId));
    }

    // 查询秒杀商品排行榜（前15名）
    @Override
    public List<RankVO> getProductRank() {
        // 从Redis有序集合中获取排行榜前15名（按热度值降序排列）
        Set<String> rankSet = stringRedisTemplate.opsForZSet().reverseRange(RANK_ALL_KEY, 0, 14);
        
        if (rankSet == null || rankSet.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 转换为RankVO列表
        List<RankVO> rankList = new ArrayList<>();
        int rank = 1;
        
        for (String productIdStr : rankSet) {
            Long productId = Long.parseLong(productIdStr);
            
            // 从缓存或数据库中获取商品详情
            SeckillProduct product = cacheUtil.getProduct(CACHE_PRODUCT_KEY, productId, SeckillProduct.class, this::getById);
            
            if (product != null) {
                RankVO rankVO = new RankVO();
                rankVO.setName(product.getName());
                rankVO.setImg(product.getImg());
                rankVO.setPrice(product.getPrice());
                rankVO.setStock(product.getStock());
                
                // 从Redis有序集合中获取热度值
                Double heatValue = stringRedisTemplate.opsForZSet().score(RANK_ALL_KEY, productIdStr);
                rankVO.setHeatValue(heatValue != null ? heatValue.intValue() : 0);
                rankVO.setRank(rank++);
                
                rankList.add(rankVO);
            }
        }
        
        return rankList;
    }

    // 修改秒杀商品
    @Override
    public void updateProduct(SeckillProductDTO seckillProductDTO) {
        SeckillProduct seckillProduct = BeanUtil.copyProperties(seckillProductDTO, SeckillProduct.class);
        boolean isSuccess = updateById(seckillProduct);
        if (!isSuccess) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "更新商品失败");
        }
    }

    // 删除秒杀商品
    @Override
    public void deleteProduct(Long id) {
        // 删除商品
        boolean isSuccess = removeById(id);
        if (!isSuccess) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "删除商品失败");
        }
    }

    // 游标查询秒杀商品
    @Override
    public List<ProductListVO> getProductsByCursor(Long lastId, int size) {
        // 构建查询条件
        List<SeckillProduct> products;
        if (lastId == null || lastId == 0) {
            // 第一次查询，获取最新的size条记录
            products = query().orderByDesc("id")
                    .last("LIMIT " + size)
                    .list();
        } else {
            // 游标查询，获取id小于lastId的size条记录
            products = query().lt("id", lastId)
                    .orderByDesc("id")
                    .last("LIMIT " + size)
                    .list();
        }
        
        // 转换为VO列表
        return products.stream().map(product -> {
            ProductListVO vo = new ProductListVO();
            vo.setId(product.getId());
            vo.setName(product.getName());
            vo.setImg(product.getImg());
            vo.setPrice(product.getPrice());
            vo.setStock(product.getStock());
            return vo;
        }).toList();
    }

    // 根据商品id查询商品详情
    @Override
    public ProductDetailVO queryById(Long id) {
        // 从缓存中查询商品数据
        SeckillProduct seckillProduct = cacheUtil.getProduct(CACHE_PRODUCT_KEY, id, SeckillProduct.class,this::getById);
        if (seckillProduct == null){
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        // 添加点击数
        stringRedisTemplate.opsForValue().increment(PRODUCT_HEAT_KEY + id + ":clickCount", 1);

        return BeanUtil.copyProperties(seckillProduct, ProductDetailVO.class);
    }
}
