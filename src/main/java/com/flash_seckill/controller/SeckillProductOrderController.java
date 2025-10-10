package com.flash_seckill.controller;

import com.flash_seckill.result.Result;
import com.flash_seckill.service.ISeckillProductOrderService;
import com.flash_seckill.pojo.vo.OrderDetailVO;
import com.flash_seckill.pojo.vo.OrderListVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order")
public class SeckillProductOrderController {

    @Autowired
    private ISeckillProductOrderService seckillProductOrderService;

    // 创建订单
    @PostMapping("/seckill/{id}")
    public Result<Long> createOrder(@PathVariable Long id) {
        Long order = seckillProductOrderService.createOrder(id);
        return Result.success(order);
    }

    // 支付订单
    @PostMapping("/pay/{id}")
    public Result payOrder(@PathVariable Long id) {
        seckillProductOrderService.payOrder(id);
        return Result.success();
    }

    // 游标查询订单
    @GetMapping("/page")
    public Result<List<OrderListVO>> listOrders(Long lastId) {
        List<OrderListVO> list = seckillProductOrderService.getOrdersByCursor(lastId, 10);
        return Result.success(list);
    }

    // 根据订单id查询订单详情
    @GetMapping("/detail/{id}")
    public Result<OrderDetailVO> getById(@PathVariable Long id) {
        OrderDetailVO orderDetailVO = seckillProductOrderService.queryById(id);
        return Result.success(orderDetailVO);
    }
}
