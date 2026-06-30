package com.storage.service;

import com.storage.dto.ForgotPasswordDTO;
import com.storage.dto.ForgotPasswordResetDTO;
import com.storage.entity.PasswordResetToken;
import com.storage.entity.SysUser;
import com.storage.exception.BusinessException;
import com.storage.mapper.PasswordResetTokenMapper;
import com.storage.mapper.SysUserMapper;
import com.storage.shiro.UserRealm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceForgotPasswordIntegrationTest {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([A-Za-z0-9_-]+)");

    @Autowired
    private AuthService authService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PasswordResetTokenMapper passwordResetTokenMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private UserRealm userRealm;

    @MockBean
    private JavaMailSender mailSender;

    @BeforeEach
    void setUp() {
        passwordResetTokenMapper.delete(null);
        sysUserMapper.delete(null);
        reset(mailSender);

        SysUser user = new SysUser();
        user.setUsername("resetme");
        user.setDisplayName("重置用户");
        user.setEmail("reset@example.com");
        user.setPasswordHash(passwordEncoder.encode("oldpass"));
        user.setStatus(1);
        sysUserMapper.insert(user);
    }

    @Test
    void forgotPassword_withMatchingEmail_createsTokenAndSendsMailWithoutUpdatingPassword() {
        authService.forgotPassword(forgotDto("resetme", "reset@example.com"));

        SysUser updated = sysUserMapper.selectByUsername("resetme");
        assertThat(userRealm.matchesPassword("oldpass", updated.getPasswordHash())).isTrue();
        assertThat(passwordResetTokenMapper.selectCount(null)).isEqualTo(1);

        SimpleMailMessage message = sentMail();
        assertThat(message.getTo()).containsExactly("reset@example.com");
        assertThat(message.getText()).contains("/login?tab=reset&token=");
    }

    @Test
    void resetPassword_withValidToken_updatesPasswordAndMarksTokenUsed() {
        authService.forgotPassword(forgotDto("resetme", "reset@example.com"));
        String rawToken = extractToken(sentMail());

        ForgotPasswordResetDTO dto = new ForgotPasswordResetDTO();
        dto.setToken(rawToken);
        dto.setNewPassword("newpass123");

        authService.resetPassword(dto);

        SysUser updated = sysUserMapper.selectByUsername("resetme");
        assertThat(userRealm.matchesPassword("newpass123", updated.getPasswordHash())).isTrue();
        PasswordResetToken stored = passwordResetTokenMapper.selectList(null).get(0);
        assertThat(stored.getUsedAt()).isNotNull();
    }

    @Test
    void resetPassword_reusingToken_rejects() {
        authService.forgotPassword(forgotDto("resetme", "reset@example.com"));
        String rawToken = extractToken(sentMail());

        ForgotPasswordResetDTO dto = new ForgotPasswordResetDTO();
        dto.setToken(rawToken);
        dto.setNewPassword("newpass123");
        authService.resetPassword(dto);

        assertThatThrownBy(() -> authService.resetPassword(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("重置链接无效");
    }

    @Test
    void resetPassword_withExpiredToken_rejects() {
        authService.forgotPassword(forgotDto("resetme", "reset@example.com"));
        String rawToken = extractToken(sentMail());
        PasswordResetToken stored = passwordResetTokenMapper.selectList(null).get(0);
        stored.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        passwordResetTokenMapper.updateById(stored);

        ForgotPasswordResetDTO dto = new ForgotPasswordResetDTO();
        dto.setToken(rawToken);
        dto.setNewPassword("newpass123");

        assertThatThrownBy(() -> authService.resetPassword(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("重置链接无效");
    }

    @Test
    void forgotPassword_withWrongEmail_rejects() {
        assertThatThrownBy(() -> authService.forgotPassword(forgotDto("resetme", "wrong@example.com")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号或邮箱不正确");
    }

    @Test
    void forgotPassword_withoutEmail_rejectsWithHint() {
        SysUser user = sysUserMapper.selectByUsername("resetme");
        user.setEmail("");
        sysUserMapper.updateById(user);

        assertThatThrownBy(() -> authService.forgotPassword(forgotDto("resetme", "any@example.com")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号或邮箱不正确");
    }

    @Test
    void forgotPassword_afterTooManyFailures_rejectsTemporarily() {
        SysUser user = new SysUser();
        user.setUsername("limited");
        user.setDisplayName("限流用户");
        user.setEmail("limited@example.com");
        user.setPasswordHash(passwordEncoder.encode("oldpass"));
        user.setStatus(1);
        sysUserMapper.insert(user);

        ForgotPasswordDTO dto = forgotDto("limited", "wrong@example.com");

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> authService.forgotPassword(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("账号或邮箱不正确");
        }

        assertThatThrownBy(() -> authService.forgotPassword(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("尝试次数过多");
    }

    private ForgotPasswordDTO forgotDto(String username, String email) {
        ForgotPasswordDTO dto = new ForgotPasswordDTO();
        dto.setUsername(username);
        dto.setEmail(email);
        return dto;
    }

    private SimpleMailMessage sentMail() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    private String extractToken(SimpleMailMessage message) {
        Matcher matcher = TOKEN_PATTERN.matcher(message.getText());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
