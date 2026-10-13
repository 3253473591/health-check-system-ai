package com.lqx.health_check_system.controller;

import com.lqx.health_check_system.common.Result;
import com.lqx.health_check_system.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(
            @RequestBody Map<String, String> loginForm) {
        return authService.login(loginForm);
    }
}