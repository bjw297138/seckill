package com.flash_seckill.pojo.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SeckillProductDTO {

    private String name;
    private String img;
    private String desc;
    private Integer price;
    private Integer stock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

}
