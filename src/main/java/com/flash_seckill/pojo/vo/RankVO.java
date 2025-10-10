package com.flash_seckill.pojo.vo;

import lombok.Data;

@Data
public class RankVO {
    private String name;
    private String img;
    private Integer price;
    private Integer stock;
    private Integer heatValue;
    private Integer rank;
}