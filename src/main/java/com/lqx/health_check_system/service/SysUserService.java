package com.lqx.health_check_system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lqx.health_check_system.entity.SysUser;

public interface SysUserService {

    Page<SysUser> listUsers(int page, int size,
                            String username, String realName,
                            String role, Integer isActive);

    void addUser(SysUser user);

    void updateUser(SysUser user);

    void toggleStatus(Long userId, Integer isActive);
}