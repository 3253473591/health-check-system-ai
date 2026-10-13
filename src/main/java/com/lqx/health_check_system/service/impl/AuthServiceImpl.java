package com.lqx.health_check_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lqx.health_check_system.common.Result;
import com.lqx.health_check_system.entity.SysUser;
import com.lqx.health_check_system.mapper.SysUserMapper;
import com.lqx.health_check_system.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Override
    public Result<Map<String, Object>> login(Map<String, String> loginForm) {
        String username = loginForm.get("username");
        String password = loginForm.get("password");

        if (username == null || password == null) {
            return Result.error("用户名和密码不能为空");
        }

        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));

        if (user == null)              return Result.error("用户不存在");
        if (user.getIsActive() != 1)   return Result.error("用户已被禁用");
        if (!PASSWORD_ENCODER.matches(password, user.getPassword())) {
            return Result.error("密码错误");
        }

        String token = "mock-token-" + user.getUserId()
                + "-" + System.currentTimeMillis()
                + "-" + user.getRole();

        Map<String, Object> data = new HashMap<>();
        data.put("token",    token);
        data.put("role",     user.getRole());
        data.put("realName", user.getRealName());
        data.put("userId",   user.getUserId());

        return Result.success(data);
    }
}