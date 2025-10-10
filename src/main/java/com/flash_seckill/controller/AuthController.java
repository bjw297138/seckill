package com.flash_seckill.controller;

import cn.hutool.core.util.RandomUtil;
import com.flash_seckill.pojo.dto.LoginRequestDTO;
import com.flash_seckill.pojo.dto.RefreshTokenRequestDTO;
import com.flash_seckill.pojo.entity.User;
import com.flash_seckill.pojo.entity.UserDetailsImpl;
import com.flash_seckill.mapper.UserMapper;
import com.flash_seckill.result.Result;
import com.flash_seckill.utils.JwtUtil;
import com.flash_seckill.pojo.vo.AuthVO;
import com.flash_seckill.exception.BusinessException;
import com.flash_seckill.exception.ErrorCode;
import org.springframework.security.core.GrantedAuthority;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public Result<AuthVO> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        // 1.认证用户
        Authentication authentication = authenticationManager.authenticate(
                // 传入 未认证令牌
                new UsernamePasswordAuthenticationToken(
                        loginRequestDTO.getUsername(),
                        loginRequestDTO.getPassword()
                )
        );
        // 2.获取已认证用户信息
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", userDetails.getUsername());
        // 将角色信息转换为字符串列表
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority) // 提取字符串
                .collect(Collectors.toList());
        claims.put("role", roles);

        // 3.传入 用户ID和角色信息 生成访问令牌和刷新令牌
        String accessToken = jwtUtil.generateToken(userDetails.getUser().getId(), claims);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails.getUser().getId(), claims);
        
        // 4.构建认证响应
        AuthVO authVO = AuthVO.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .username(userDetails.getUsername())
                .build();
        return Result.success(authVO);
    }

    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        if (userMapper.selectByUsername(loginRequestDTO.getUsername()) != null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户名已存在");
        }
        User user = User.builder()
                .username(loginRequestDTO.getUsername())
                .password(passwordEncoder.encode(loginRequestDTO.getPassword()))
                .nickName("user_" + RandomUtil.randomString(10))
                .icon("")
                .role("USER")
                .build();
        userMapper.insert(user);
        return Result.success();
    }

    @PostMapping("/logout")
    public Result<String> logout() {
        return Result.success("退出登录成功");
    }

    @PostMapping("/refresh")
    public Result<AuthVO> refresh(@Valid @RequestBody RefreshTokenRequestDTO refreshTokenRequestDTO) {
        String refreshToken = refreshTokenRequestDTO.getRefreshToken();
        
        // 1.验证刷新令牌是否有效
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "刷新令牌已过期或无效");
        }
        
        // 2.从刷新令牌中提取用户ID和声明信息
        Long userId = jwtUtil.getUserIdFromRefreshToken(refreshToken);
        Map<String, Object> claims = jwtUtil.getClaimsFromRefreshToken(refreshToken);
        
        // 3.根据用户ID查询用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        // 4.生成新的访问令牌和刷新令牌
        String newAccessToken = jwtUtil.generateToken(userId, claims);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId, claims);
        
        // 5.构建认证响应
        AuthVO authVO = AuthVO.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .username(user.getUsername())
                .build();
        
        return Result.success(authVO);
    }
}
