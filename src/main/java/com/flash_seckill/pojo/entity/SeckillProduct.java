package com.flash_seckill.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@TableName("seckill_product")
public class SeckillProduct {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;
    private String img;
    @TableField("`desc`")
    private String desc;
    private Integer price;
    private Integer stock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // 数据库自动填充
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
