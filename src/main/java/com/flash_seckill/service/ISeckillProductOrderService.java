package com.flash_seckill.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.flash_seckill.pojo.entity.SeckillProductOrder;
import com.flash_seckill.pojo.vo.OrderDetailVO;
import com.flash_seckill.pojo.vo.OrderListVO;

import java.util.List;

public interface ISeckillProductOrderService extends IService<SeckillProductOrder> {
    // 根据订单id查询订单详情
    OrderDetailVO queryById(Long orderId);
    // 创建订单
    Long createOrder(Long productId);
    // 支付订单
    void payOrder(Long orderId);
    // 游标查询订单
    List<OrderListVO> getOrdersByCursor (Long lastId, int size);
}
