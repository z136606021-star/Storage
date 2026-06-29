package com.storage.service;

import com.storage.dto.ForgotPasswordDTO;
import com.storage.entity.SysUser;
import com.storage.exception.BusinessException;
import com.storage.mapper.SysUserMapper;
import com.storage.shiro.UserRealm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceForgotPasswordIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private UserRealm userRealm;

    @BeforeEach
    void setUp() {
        sysUserMapper.delete(null);

        SysUser user = new SysUser();
        user.setUsername("resetme");
        user.setDisplayName("重置用户");
        user.setEmail("reset@example.com");
        user.setPasswordHash(passwordEncoder.encode("oldpass"));
        user.setStatus(1);
        sysUserMapper.insert(user);
    }

    @Test
    void forgotPassword_withMatchingEmail_updatesPassword() {
        ForgotPasswordDTO dto = new ForgotPasswordDTO();
        dto.setUsername("resetme");
        dto.setEmail("reset@example.com");
        dto.setNewPassword("newpass123");

        authService.forgotPassword(dto);

        SysUser updated = sysUserMapper.selectByUsername("resetme");
        assertThat(userRealm.matchesPassword("newpass123", updated.getPasswordHash())).isTrue();
    }

    @Test
    void forgotPassword_withWrongEmail_rejects() {
        ForgotPasswordDTO dto = new ForgotPasswordDTO();
        dto.setUsername("resetme");
        dto.setEmail("wrong@example.com");
        dto.setNewPassword("newpass123");

        assertThatThrownBy(() -> authService.forgotPassword(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号或邮箱不正确");
    }

    @Test
    void forgotPassword_withoutEmail_rejectsWithHint() {
        SysUser user = sysUserMapper.selectByUsername("resetme");
        user.setEmail("");
        sysUserMapper.updateById(user);

        ForgotPasswordDTO dto = new ForgotPasswordDTO();
        dto.setUsername("resetme");
        dto.setEmail("any@example.com");
        dto.setNewPassword("newpass123");

        assertThatThrownBy(() -> authService.forgotPassword(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未绑定邮箱");
    }
}
