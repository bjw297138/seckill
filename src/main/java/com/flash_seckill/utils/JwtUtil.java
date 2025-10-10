package com.flash_seckill.utils;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;


@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret; // 从 application.yml 读取，至少 32 字节

    @Value("${jwt.expiration}")
    private Long expiration; // 单位：秒，例如 3600

    // 缓存 SecretKey（线程安全）
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // 生成 Token

    public String generateToken(Long userId) {
        return generateToken(userId, new HashMap<>());
    }

    public String generateToken(Long userId, Map<String, Object> claims) {
        return Jwts.builder()
                .subject(userId.toString())
                .claims(claims)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(getSigningKey())
                .compact();
    }

    // 解析 Claims
    public Long getUserId(String token) {
        return Long.parseLong(Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject());
    }

    public String getUsername(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("username", String.class);
    }


    public Date getExpiration(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
    }

    public Collection<? extends GrantedAuthority> getRole(String token) {
        Object roles = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role");
        
        if (roles instanceof List) {
            return ((List<?>) roles).stream()
                    .map(role -> new SimpleGrantedAuthority(role.toString()))
                    .collect(Collectors.toList());
        } else if (roles != null) {
            // 如果存入的是单个角色，转换为权限集合
            return Collections.singletonList(new SimpleGrantedAuthority(roles.toString()));
        } else {
            return Collections.emptyList();
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 验证 Token
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public boolean isTokenExpired(String token) {
        // 过期返回 true
        return getExpiration(token).before(new Date());
    }

    // ========== 刷新令牌相关方法 ==========
    
    // 生成刷新令牌
    public String generateRefreshToken(Long userId, Map<String, Object> claims) {
        return Jwts.builder()
                .subject(userId.toString())
                .claims(claims)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 7 * 24 * 3600 * 1000)) // 7天
                .signWith(getSigningKey())
                .compact();
    }
    
    // 验证刷新令牌是否过期
    public boolean validateRefreshToken(String refreshToken) {
        try {
            return !isTokenExpired(refreshToken);
        } catch (Exception e) {
            return false;
        }
    }
    
    // 从刷新令牌中提取用户ID
    public Long getUserIdFromRefreshToken(String refreshToken) {
        return getUserId(refreshToken);
    }
    
    // 从刷新令牌中提取声明信息 
    public Map<String, Object> getClaimsFromRefreshToken(String refreshToken) {
        Claims claims = getClaims(refreshToken);
        Map<String, Object> result = new HashMap<>();
        
        // 复制除标准声明外的所有自定义声明
        claims.forEach((key, value) -> {
            // 排除标准声明：subject, issuedAt, expiration
            if (!key.equals("sub") && !key.equals("iat") && !key.equals("exp")) {
                result.put(key, value);
            }
        });
        
        return result;
    }
}
