package com.flash_seckill.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AuthVO {

    private String token;
    private String refreshToken;
    private String username;
    
}
