package com.flash_seckill.controller;

import cn.hutool.core.bean.BeanUtil;
import com.flash_seckill.pojo.entity.User;
import com.flash_seckill.mapper.UserMapper;
import com.flash_seckill.result.Result;
import com.flash_seckill.pojo.vo.UserVO;
import com.flash_seckill.exception.BusinessException;
import com.flash_seckill.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    // 获取当前登录用户信息
    @GetMapping("/me")
    public Result<UserVO> me(@AuthenticationPrincipal Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        return Result.success(userVO);
    }

}
