package com.flash_seckill.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flash_seckill.pojo.entity.SeckillProduct;
import com.flash_seckill.pojo.entity.SeckillProductOrder;
import com.flash_seckill.exception.BusinessException;
import com.flash_seckill.exception.ErrorCode;
import com.flash_seckill.mapper.SeckillProductOrderMapper;
import com.flash_seckill.service.ISeckillProductOrderService;
import com.flash_seckill.service.ISeckillProductService;
import com.flash_seckill.utils.UniqueId;
import com.flash_seckill.pojo.vo.OrderDetailVO;
import com.flash_seckill.pojo.vo.OrderListVO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class SeckillProductOrderServiceImpl extends ServiceImpl<SeckillProductOrderMapper, SeckillProductOrder> implements ISeckillProductOrderService {

    @Autowired
    private UniqueId uniqueId;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ISeckillProductService seckillProductService;

    // 秒杀脚本
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    // 初始化
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    // 创建订单
    @Override
    public Long createOrder(Long productId) {
        SeckillProduct product = seckillProductService.getById(productId);
        // 检查商品是否存在
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        // 检查是否在秒杀时间内
        if (product.getStartTime().isAfter(LocalDateTime.now()) || product.getEndTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_IN_SECKILL_TIME);
        }
        // 获取用户ID
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // 执行脚本
        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT,
                Collections.emptyList(),
                productId.toString(),
                userId.toString());
        // 获取结果
        int r = result.intValue();
        if (r != 0) {
            // 秒杀失败
            if (r == 1) {
                // 库存不足
                throw new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_ENOUGH);
            }
            if (r == 2) {
                // 重复秒杀
                throw new BusinessException(ErrorCode.ORDER_REPEAT_SECKILL);
            }
        }

        // 有购买资格 创建订单ID 
        Long orderId = uniqueId.nextId("order");
        // 创建订单
        SeckillProductOrder order = new SeckillProductOrder();
        order.setId(orderId);
        order.setStatus(0);
        order.setUserId(userId);
        order.setSeckillProductId(productId);
        order.setCreateTime(LocalDateTime.now());

        rabbitTemplate.convertAndSend("order.direct", "order.create", order);

        // 返回订单ID
        return orderId;
    }

    // 支付订单
    @Override
    public void payOrder(Long orderId) {
        // 验证操作权限
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        SeckillProductOrder order = getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            // 订单不存在或用户权限不足
            throw new BusinessException(ErrorCode.ORDER_PERMISSION_DENIED);
        }
        // 更新订单状态
        boolean isSuccess = update()
                .setSql("status = 1")
                .set("pay_time", LocalDateTime.now())
                .eq("id", orderId)
                .eq("status", 0)
                .update();
        if (!isSuccess) {
            // 订单状态异常
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }
    }

    // 游标查询订单
    @Override
    public List<OrderListVO> getOrdersByCursor(Long lastId, int size) {
        // 获取当前用户ID
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        // 构建查询条件
        List<SeckillProductOrder> orders;
        if (lastId == null || lastId == 0) {
            // 第一次查询，获取最新的size条记录
            orders = query().eq("user_id", userId)
                    .orderByDesc("id")
                    .last("LIMIT " + size)
                    .list();
        } else {
            // 游标查询，获取id小于lastId的size条记录
            orders = query().eq("user_id", userId)
                    .lt("id", lastId)
                    .orderByDesc("id")
                    .last("LIMIT " + size)
                    .list();
        }
        
        // 转换为VO列表
        return orders.stream().map(order -> {
            OrderListVO vo = new OrderListVO();
            vo.setId(String.valueOf(order.getId()));
            vo.setStatus(order.getStatus());
            vo.setCreateTime(order.getCreateTime());
            
            // 查询关联的商品信息
            SeckillProduct product = seckillProductService.getById(order.getSeckillProductId());
            if (product != null) {
                vo.setProductName(product.getName());
                vo.setImg(product.getImg());
                vo.setPrice(product.getPrice());
            }
            
            return vo;
        }).toList();
    }

    // 根据订单id查询订单详情
    @Override
    public OrderDetailVO queryById(Long orderId) {
        // 从数据库中查询订单数据
        SeckillProductOrder order = getById(orderId);
        if (order == null) {
            // 订单不存在
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        OrderDetailVO orderDetailVO = BeanUtil.copyProperties(order, OrderDetailVO.class);
        SeckillProduct product = seckillProductService.getById(order.getSeckillProductId());
        orderDetailVO.setProductName(product.getName());
        orderDetailVO.setImg(product.getImg());
        orderDetailVO.setPrice(product.getPrice());
        
        return orderDetailVO;
    }
}
