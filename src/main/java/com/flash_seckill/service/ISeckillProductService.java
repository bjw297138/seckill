package com.flash_seckill.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.flash_seckill.pojo.dto.SeckillProductDTO;
import com.flash_seckill.pojo.entity.SeckillProduct;
import com.flash_seckill.pojo.vo.ProductDetailVO;
import com.flash_seckill.pojo.vo.ProductListVO;
import com.flash_seckill.pojo.vo.RankVO;

import java.util.List;

public interface ISeckillProductService extends IService<SeckillProduct> {
    // 添加秒杀商品
    void addProduct(SeckillProductDTO seckillProductDTO);
    // 收藏商品
    void collect(Long id);
    // 根据商品id查询商品详情
    ProductDetailVO queryById(Long id);
    // 游标查询秒杀商品
    List<ProductListVO> getProductsByCursor(Long lastId, int size);
    // 查询秒杀商品排行榜
    List<RankVO> getProductRank();
    // 修改秒杀商品
    void updateProduct(SeckillProductDTO seckillProductDTO);
    // 删除秒杀商品
    void deleteProduct(Long id);


}
