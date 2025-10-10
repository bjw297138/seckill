package com.flash_seckill.filter;

import com.flash_seckill.service.impl.UserDetailsServiceImpl;
import com.flash_seckill.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsServiceImpl userDetailService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final String authorization = request.getHeader("Authorization");
        String username = null;
        String token = null;

        try {
            // 提取用户名
            if (authorization != null && authorization.startsWith("Bearer")) {
                token = authorization.substring(7);
                username = jwtUtil.getUsername(token);
            }

            // 将用户信息存入 SecurityContextHolder
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (!jwtUtil.isTokenExpired(token)) {
                    // 创建 已认证令牌 存入 用户ID 和 角色信息
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    jwtUtil.getUserId(token),
                                    null,
                                    jwtUtil.getRole(token));
                    // 将已认证令牌存入 SecurityContextHolder
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            }
            // 继续执行过滤器链
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            // 处理JWT异常
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"Token无效\"}");
        }
    }
}
