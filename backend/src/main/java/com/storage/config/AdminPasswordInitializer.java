package com.storage.config;

import com.storage.entity.SysUser;
import com.storage.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminPasswordInitializer implements ApplicationRunner {

    private static final String ADMIN_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin123";

    private final SysUserMapper sysUserMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        try {
            SysUser admin = sysUserMapper.selectByUsername(ADMIN_USERNAME);
            if (admin == null) {
                return;
            }
            if (!passwordEncoder.matches(DEFAULT_PASSWORD, admin.getPasswordHash())) {
                admin.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
                sysUserMapper.updateById(admin);
            }
        } catch (DataAccessException ex) {
            log.warn("跳过管理员密码初始化（sys_user 表可能尚未创建，请执行 docker compose down -v && docker compose up -d 重建数据库）");
        }
    }
}
