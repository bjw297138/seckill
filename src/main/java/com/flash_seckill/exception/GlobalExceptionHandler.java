package com.flash_seckill.exception;

import com.flash_seckill.result.Result;
import lombok.extern.slf4j.Slf4j;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 全局异常处理器
 * 用于统一处理业务异常并返回友好的错误信息
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public Result<String> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public Result<String> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数校验异常: {}", e.getMessage());
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), "参数错误: " + e.getMessage());
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseBody
    public Result<String> handleNullPointerException(NullPointerException e) {
        log.error("空指针异常", e);
        return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), "系统内部错误");
    }

    /**
     * 处理认证异常 - 密码错误
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseBody
    public Result<String> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("认证失败: 用户名或密码错误");
        return Result.error(ErrorCode.BUSINESS_ERROR.getCode(), "用户名或密码错误");
    }

    /**
     * 处理用户不存在异常
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    @ResponseBody
    public Result<String> handleUsernameNotFoundException(UsernameNotFoundException e) {
        log.warn("用户不存在: {}", e.getMessage());
        return Result.error(ErrorCode.USER_NOT_FOUND.getCode(), "用户不存在");
    }


    /**
     * 处理其他未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result<String> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), "系统繁忙，请稍后再试");
    }
}