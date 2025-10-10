package com.flash_seckill.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@TableName("seckill_product_order")
public class SeckillProductOrder {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    private Long userId;
    private Long seckillProductId;
    // 0:未支付 1:已支付 2:已取消
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime cancelTime;
}
