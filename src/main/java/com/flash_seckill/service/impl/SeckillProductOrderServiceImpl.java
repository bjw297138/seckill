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
import com.rabbitmq.client.MessageProperties;
import org.springframework.amqp.core.Correlation;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
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
import java.util.UUID;

import static com.flash_seckill.config.RabbitMqConfig.ORDER_CREATE_EXCHANGE;
import static com.flash_seckill.config.RabbitMqConfig.ORDER_CREATE_ROUTING_KEY;

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
        // 1. 查询商品信息
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
        // 执行脚本（传入商品id和用户id）
        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT,
                Collections.emptyList(),
                productId.toString(),
                userId.toString());

        // 2. 获取结果
        int r = result.intValue();
        if (r != 0) {
            if (r == 1) {
                // 2.1 库存不足
                throw new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_ENOUGH);
            }
            if (r == 2) {
                // 2.2 重复秒杀
                throw new BusinessException(ErrorCode.ORDER_REPEAT_SECKILL);
            }
        }

        // 3. 有购买资格 生成订单ID
        Long orderId = uniqueId.nextId("order");
        // 创建订单
        SeckillProductOrder order = new SeckillProductOrder();
        order.setId(orderId);
        order.setStatus(0);
        order.setUserId(userId);
        order.setSeckillProductId(productId);
        order.setCreateTime(LocalDateTime.now());

        // 4. 异步发送消息（消费者可靠性）
        // 4.1 创建CorrelationData对象，设置消息类型和订单ID，用于发送失败后人工干预
        CorrelationData correlationData = new CorrelationData("CREATE_" + orderId);
        // 4.2 发送消息
        rabbitTemplate.convertAndSend(
                ORDER_CREATE_EXCHANGE,      // 交换机
                ORDER_CREATE_ROUTING_KEY,   // 路由键
                order,
                msg -> {
                    // 设置消息持久化
                    msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return msg;},
                correlationData              // 订单id
        );
        // 5. 返回订单ID
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
