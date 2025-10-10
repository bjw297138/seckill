package com.flash_seckill.exception;

/**
 * 错误码枚举
 * 定义系统中所有可能的错误类型
 */
public enum ErrorCode {
    
    // 通用错误 (1000-1999)
    PARAM_ERROR(1001, "参数错误"),
    SYSTEM_ERROR(1002, "系统内部错误"),
    TOKEN_EXPIRED(1003, "Token已过期"),
    TOKEN_INVALID(1004, "Token无效"),
    TOKEN_FORMAT_ERROR(1005, "Token格式错误"),
    TOKEN_SIGNATURE_ERROR(1006, "Token签名验证失败"),
    
    // 用户相关错误 (2000-2999)
    USER_NOT_FOUND(2001, "用户不存在"),
    PHONE_FORMAT_ERROR(2002, "手机号格式错误"),
    VERIFICATION_CODE_ERROR(2003, "验证码错误"),
    
    // 商品相关错误 (3000-3999)
    PRODUCT_NOT_FOUND(3001, "商品不存在"),
    PRODUCT_STOCK_NOT_ENOUGH(3002, "库存不足"),
    PRODUCT_NOT_IN_SECKILL_TIME(3003, "不在秒杀时间内"),
    
    // 订单相关错误 (4000-4999)
    ORDER_NOT_FOUND(4001, "订单不存在"),
    ORDER_STATUS_ERROR(4002, "订单状态错误"),
    ORDER_REPEAT_SECKILL(4003, "重复秒杀"),
    ORDER_PERMISSION_DENIED(4004, "订单权限不足"),
    
    // 业务逻辑错误 (5000-5999)
    BUSINESS_ERROR(5001, "业务逻辑错误");
    
    private final int code;
    private final String message;
    
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
}