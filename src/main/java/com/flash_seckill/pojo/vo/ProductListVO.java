package com.flash_seckill.pojo.vo;

import lombok.Data;

@Data
public class ProductListVO {
    private Long id;
    private String name;
    private String img;
    private Integer price;
    private Integer stock;
}
