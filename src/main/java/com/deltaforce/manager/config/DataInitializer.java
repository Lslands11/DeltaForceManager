package com.deltaforce.manager.config;

import com.deltaforce.manager.entity.SysUser;
import com.deltaforce.manager.service.ISysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final ISysUserService sysUserService;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(ISysUserService sysUserService, PasswordEncoder passwordEncoder) {
        this.sysUserService = sysUserService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 如果没有管理员账号，自动创建默认管理员
        if (sysUserService.getByUsername("admin") == null) {
            SysUser admin = new SysUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setNickname("管理员");
            admin.setRole("ADMIN");
            admin.setStatus(1);
            sysUserService.save(admin);
            log.info("已创建默认管理员账号: admin / admin123");
        }
    }
}
