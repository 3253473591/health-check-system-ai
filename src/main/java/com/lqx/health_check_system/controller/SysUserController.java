package com.lqx.health_check_system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lqx.health_check_system.common.Result;
import com.lqx.health_check_system.entity.SysUser;
import com.lqx.health_check_system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    @GetMapping
    public Result<Page<SysUser>> listUsers(
            @RequestParam(defaultValue = "1")   int page,
            @RequestParam(defaultValue = "10")  int size,
            @RequestParam(required = false)     String username,
            @RequestParam(required = false)     String realName,
            @RequestParam(required = false)     String role,
            @RequestParam(required = false)     Integer isActive) {
        return Result.success(sysUserService.listUsers(page, size, username, realName, role, isActive));
    }

    @PostMapping
    public Result<Void> addUser(@RequestBody SysUser user) {
        sysUserService.addUser(user);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody SysUser user) {
        user.setUserId(id);
        sysUserService.updateUser(user);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer isActive) {
        sysUserService.toggleStatus(id, isActive);
        return Result.success();
    }
}