-- 用户表
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `nick_name` VARCHAR(50) COMMENT '昵称',
    `icon` VARCHAR(200) COMMENT '头像',
    `role` VARCHAR(20) COMMENT '角色',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 秒杀商品表
CREATE TABLE `seckill_product` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `img` VARCHAR(200) COMMENT '商品图片',
    `desc` VARCHAR(500) COMMENT '商品描述',
    `price` INT NOT NULL COMMENT '商品价格（分）',
    `stock` INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    `start_time` DATETIME NOT NULL COMMENT '秒杀开始时间',
    `end_time` DATETIME NOT NULL COMMENT '秒杀结束时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀商品表';

-- 秒杀订单表
CREATE TABLE `seckill_product_order` (
    `id` BIGINT NOT NULL COMMENT '订单ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `seckill_product_id` BIGINT NOT NULL COMMENT '秒杀商品ID',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0-未支付 1-已支付 2-已取消',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `pay_time` DATETIME COMMENT '支付时间',
    `cancel_time` DATETIME COMMENT '取消时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_product_id` (`seckill_product_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单表';