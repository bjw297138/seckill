package com.flash_seckill.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequestDTO {
    
    @NotBlank(message = "刷新令牌不能为空")
    private String refreshToken;
}