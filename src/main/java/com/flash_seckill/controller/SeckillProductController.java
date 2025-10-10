package com.flash_seckill.controller;

import com.flash_seckill.pojo.dto.SeckillProductDTO;
import com.flash_seckill.result.Result;
import com.flash_seckill.service.ISeckillProductService;
import com.flash_seckill.pojo.vo.ProductDetailVO;
import com.flash_seckill.pojo.vo.ProductListVO;
import com.flash_seckill.pojo.vo.RankVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
public class SeckillProductController {

    @Autowired
    private ISeckillProductService seckillProductService;

    // 添加秒杀商品
    @PostMapping("/add")
    public Result addProduct(@RequestBody SeckillProductDTO seckillProductDTO) {
        seckillProductService.addProduct(seckillProductDTO);
        return Result.success();
    }

    // 修改秒杀商品
    @PutMapping("/update")
    public Result updateProduct(@RequestBody SeckillProductDTO seckillProductDTO) {
        seckillProductService.updateProduct(seckillProductDTO);
        return Result.success();
    }

    // 删除秒杀商品
    @DeleteMapping("/delete/{id}")
    public Result deleteProduct(@PathVariable Long id) {
        seckillProductService.deleteProduct(id);
        return Result.success();
    }

    // 游标查询秒杀商品
    @GetMapping("/page")
    public Result<List<ProductListVO>> listProducts(@RequestParam(required = false) Long lastId) {
        List<ProductListVO> list = seckillProductService.getProductsByCursor(lastId, 10);
        return Result.success(list);
    }

    // 根据id查询秒杀商品详情
    @GetMapping("/{id}")
    public Result<ProductDetailVO> getById(@PathVariable Long id) {

        ProductDetailVO productDetailVO = seckillProductService.queryById(id);
        return Result.success(productDetailVO);
    }

    // 收藏商品
    @PostMapping("/collect/{id}")
    public Result collect(@PathVariable Long id) {
        seckillProductService.collect(id);
        return Result.success();
    }

    // 获取商品排行榜
    @GetMapping("/rank")
    public Result<List<RankVO>> getRank() {
        List<RankVO> rankList = seckillProductService.getProductRank();
        return Result.success(rankList);
    }
}
