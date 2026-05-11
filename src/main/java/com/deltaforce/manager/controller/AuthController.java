package com.deltaforce.manager.controller;

import com.deltaforce.manager.dto.LoginRequest;
import com.deltaforce.manager.dto.LoginResponse;
import com.deltaforce.manager.dto.Result;
import com.deltaforce.manager.entity.SysUser;
import com.deltaforce.manager.service.ISysUserService;
import com.deltaforce.manager.util.JwtUtil;
import com.deltaforce.manager.util.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    @Resource
    private ISysUserService sysUserService;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            return Result.error("用户名和密码不能为空");
        }

        SysUser user = sysUserService.getByUsername(request.getUsername());
        if (user == null) {
            return Result.error("用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            return Result.error("账号已被禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return Result.error("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserInfo(user);
        return Result.OK("登录成功", response);
    }

    @GetMapping("/current")
    public Result<SysUser> currentUser() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.OK(user);
    }
}
