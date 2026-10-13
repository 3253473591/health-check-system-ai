package com.lqx.health_check_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lqx.health_check_system.entity.Examination;
import com.lqx.health_check_system.entity.SysUser;
import com.lqx.health_check_system.mapper.ExaminationMapper;
import com.lqx.health_check_system.mapper.SysUserMapper;
import com.lqx.health_check_system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper     sysUserMapper;
    private final ExaminationMapper examinationMapper;

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Override
    public Page<SysUser> listUsers(int page, int size,
                                   String username, String realName,
                                   String role, Integer isActive) {
        Page<SysUser> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .like(StringUtils.hasText(username), SysUser::getUsername, username)
                .like(StringUtils.hasText(realName), SysUser::getRealName, realName)
                .eq(StringUtils.hasText(role),       SysUser::getRole,     role)
                .eq(isActive != null,                SysUser::getIsActive, isActive)
                .orderByAsc(SysUser::getUserId);
        Page<SysUser> result = sysUserMapper.selectPage(pageParam, wrapper);
        result.getRecords().forEach(u -> u.setPassword(null));
        return result;
    }

    @Override
    public void addUser(SysUser user) {
        if (!StringUtils.hasText(user.getPassword())) {
            throw new RuntimeException("新增用户必须填写密码");
        }
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, user.getUsername()));
        if (count > 0) throw new RuntimeException("用户名已存在");

        user.setPassword(PASSWORD_ENCODER.encode(user.getPassword()));
        user.setIsActive(1);
        sysUserMapper.insert(user);
    }

    @Override
    public void updateUser(SysUser user) {
        SysUser existing = sysUserMapper.selectById(user.getUserId());
        if (existing == null) throw new RuntimeException("用户不存在");
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(PASSWORD_ENCODER.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        sysUserMapper.updateById(user);
    }

    @Override
    public void toggleStatus(Long userId, Integer isActive) {
        if (isActive != null && isActive == 0) {
            Long count = examinationMapper.selectCount(
                    new LambdaQueryWrapper<Examination>()
                            .eq(Examination::getRegisterDoctorId, userId)
                            .in(Examination::getStatus,
                                    Arrays.asList("PENDING", "IN_PROGRESS", "LOCKED")));
            if (count > 0) {
                throw new RuntimeException(
                        "该医生名下存在未完成的体检记录（待体检/进行中/已锁定），无法禁用");
            }
        }
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setIsActive(isActive);
        sysUserMapper.updateById(user);
    }
}