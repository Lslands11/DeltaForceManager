package com.deltaforce.manager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.deltaforce.manager.dto.Result;
import com.deltaforce.manager.entity.SysUser;
import com.deltaforce.manager.service.ISysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/api/user")
@Slf4j
public class SysUserController {

    @Resource
    private ISysUserService sysUserService;
    @Resource
    private PasswordEncoder passwordEncoder;

    @GetMapping("/list")
    public Result<IPage<SysUser>> queryPageList(
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "username", required = false) String username) {

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isEmpty()) {
            wrapper.like(SysUser::getUsername, username);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);

        Page<SysUser> page = new Page<>(pageNo, pageSize);
        IPage<SysUser> pageList = sysUserService.page(page, wrapper);
        return Result.OK(pageList);
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody SysUser user) {
        SysUser existing = sysUserService.getByUsername(user.getUsername());
        if (existing != null) {
            return Result.error("用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("USER");
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        sysUserService.save(user);
        return Result.OK("添加成功！");
    }

    @PutMapping("/edit")
    public Result<String> edit(@RequestBody SysUser user) {
        SysUser existing = sysUserService.getById(user.getId());
        if (existing == null) {
            return Result.error("用户不存在");
        }
        // 如果传了新密码则更新，否则保留原密码
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        sysUserService.updateById(user);
        return Result.OK("编辑成功!");
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam("id") Long id) {
        sysUserService.removeById(id);
        return Result.OK("删除成功!");
    }

    @PutMapping("/resetPassword")
    public Result<String> resetPassword(@RequestParam("id") Long id,
                                        @RequestParam("newPassword") String newPassword) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserService.updateById(user);
        return Result.OK("密码重置成功!");
    }
}
