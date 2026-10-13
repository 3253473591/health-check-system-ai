package com.lqx.health_check_system.service;

import com.lqx.health_check_system.common.Result;
import java.util.Map;

public interface AuthService {

    Result<Map<String, Object>> login(Map<String, String> loginForm);
}