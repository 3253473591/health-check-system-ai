package com.lqx.health_check_system.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lqx.health_check_system.common.Result;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class RoleInterceptor implements HandlerInterceptor {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final ObjectMapper   MAPPER  = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest req,
                             HttpServletResponse res,
                             Object handler) throws Exception {

        String path = req.getRequestURI();

        if (MATCHER.match("/api/auth/**", path) || !path.startsWith("/api/")) {
            return true;
        }

        String token = req.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            write403(res, "未登录，请先登录");
            return false;
        }

        String role = parseRole(token);
        if (role == null) {
            write403(res, "无效的 Token");
            return false;
        }

        req.setAttribute("currentRole",  role);
        req.setAttribute("currentToken", token);

        if (MATCHER.match("/api/admin/**", path) && !"ADMIN".equals(role)) {
            write403(res, "无权限访问管理员接口");
            return false;
        }

        if (MATCHER.match("/api/doctor/**", path) && !"DOCTOR".equals(role)) {
            write403(res, "无权限访问医生接口");
            return false;
        }

        if (MATCHER.match("/api/hr/**", path) && !"HR".equals(role)) {
            write403(res, "无权限访问HR接口");
            return false;
        }

        return true;
    }

    private String parseRole(String token) {
        try {
            String[] parts = token.split("-");
            return parts[parts.length - 1];
        } catch (Exception e) {
            return null;
        }
    }

    private void write403(HttpServletResponse res, String msg) throws IOException {
        res.setStatus(403);
        res.setContentType("application/json;charset=UTF-8");
        Result<?> r = Result.error(403, msg);
        res.getWriter().write(MAPPER.writeValueAsString(r));
    }
}