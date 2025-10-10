package com.flash_seckill.exception;

/**
 * 统一业务异常
 * 使用错误码枚举来区分不同类型的业务异常
 */
public class BusinessException extends RuntimeException {
    
    private final ErrorCode errorCode;
    
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public int getCode() {
        return errorCode.getCode();
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}