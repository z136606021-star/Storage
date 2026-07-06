package com.storage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storage.dto.ForgotPasswordDTO;
import com.storage.dto.ForgotPasswordResetDTO;
import com.storage.dto.LoginRequestDTO;
import com.storage.entity.PasswordResetToken;
import com.storage.entity.SysUser;
import com.storage.mapper.SysMenuMapper;
import com.storage.mapper.PasswordResetTokenMapper;
import com.storage.mapper.SysUserMapper;
import com.storage.shiro.UserRealm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([A-Za-z0-9_-]+)");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysMenuMapper sysMenuMapper;

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
    }

    @Test
    void login_withValidCredentials_returnsAccessTokenAndSession() throws Exception {
        SysUser user = insertActiveUser("loginok", "loginok@example.com", "oldpass");
        sysMenuMapper.insertUserRole(user.getId(), 1L);

        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setUsername("loginok");
        dto.setPassword("oldpass");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.user.username").value("loginok"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.permissions").isArray())
                .andExpect(jsonPath("$.permissions[?(@ == 'warehouse:material-ledger:read')]").exists());
    }

    @Test
    void login_withInvalidBody_returns400() throws Exception {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setUsername("a");
        dto.setPassword("123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void login_afterTooManyFailures_returnsThrottleMessage() throws Exception {
        insertActiveUser("loginlimited", "loginlimited@example.com", "oldpass");

        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setUsername("loginlimited");
        dto.setPassword("wrongpass");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("账号或密码错误"));
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("登录失败次数过多，请 15 分钟后再试"));
    }

    @Test
    void forgotPassword_withInvalidBody_returns400() throws Exception {
        ForgotPasswordDTO dto = new ForgotPasswordDTO();
        dto.setUsername("");
        dto.setEmail("not-an-email");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void forgotPassword_withWrongEmail_returnsUnifiedMessage() throws Exception {
        insertActiveUser("forgotwrong", "forgotwrong@example.com", "oldpass");

        ForgotPasswordDTO dto = new ForgotPasswordDTO();
        dto.setUsername("forgotwrong");
        dto.setEmail("wrong@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("账号或邮箱不正确，或账号未绑定邮箱"));
    }

    @Test
    void forgotPassword_afterTooManyFailures_returnsThrottleMessage() throws Exception {
        insertActiveUser("forgotlimited", "forgotlimited@example.com", "oldpass");

        ForgotPasswordDTO dto = new ForgotPasswordDTO();
        dto.setUsername("forgotlimited");
        dto.setEmail("wrong@example.com");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("账号或邮箱不正确，或账号未绑定邮箱"));
        }

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("密码找回尝试次数过多，请 15 分钟后再试"));
    }

    @Test
    void forgotPassword_withValidIdentity_returns204AndSendsResetMailWithoutUpdatingPassword() throws Exception {
        insertActiveUser("forgotok", "forgotok@example.com", "oldpass");

        ForgotPasswordDTO dto = new ForgotPasswordDTO();
        dto.setUsername("forgotok");
        dto.setEmail("forgotok@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNoContent());

        SysUser updated = sysUserMapper.selectByUsername("forgotok");
        assertThat(userRealm.matchesPassword("oldpass", updated.getPasswordHash())).isTrue();
        assertThat(passwordResetTokenMapper.selectCount(null)).isEqualTo(1);
        assertThat(sentMail().getText()).contains("/login?tab=reset&token=");
    }

    @Test
    void resetPassword_withInvalidBody_returns400() throws Exception {
        ForgotPasswordResetDTO dto = new ForgotPasswordResetDTO();
        dto.setToken("");
        dto.setNewPassword("123");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void resetPassword_withValidToken_returns204AndUpdatesPassword() throws Exception {
        insertActiveUser("resetok", "resetok@example.com", "oldpass");
        ForgotPasswordDTO forgot = new ForgotPasswordDTO();
        forgot.setUsername("resetok");
        forgot.setEmail("resetok@example.com");
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgot)))
                .andExpect(status().isNoContent());

        ForgotPasswordResetDTO resetPassword = new ForgotPasswordResetDTO();
        resetPassword.setToken(extractToken(sentMail()));
        resetPassword.setNewPassword("newpass123");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetPassword)))
                .andExpect(status().isNoContent());

        SysUser updated = sysUserMapper.selectByUsername("resetok");
        assertThat(userRealm.matchesPassword("newpass123", updated.getPasswordHash())).isTrue();
        PasswordResetToken stored = passwordResetTokenMapper.selectList(null).get(0);
        assertThat(stored.getUsedAt()).isNotNull();
    }

    @Test
    void resetPassword_withFakeToken_returnsUnifiedMessage() throws Exception {
        ForgotPasswordResetDTO dto = new ForgotPasswordResetDTO();
        dto.setToken("fake-token");
        dto.setNewPassword("newpass123");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("重置链接无效或已过期"));
    }

    @Test
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void me_withBearerToken_returnsCurrentSession() throws Exception {
        String token = loginAndExtractToken("meok", "meok@example.com", "oldpass", 1L);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.user.username").value("meok"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
    }

    @Test
    void me_withFakeBearerToken_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void register_returnsAccessTokenThatCanAccessMe() throws Exception {
        var request = new com.storage.dto.RegisterRequestDTO();
        request.setUsername("registerok");
        request.setDisplayName("注册用户");
        request.setPassword("newpass123");
        request.setEmail("registerok@example.com");

        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(response).get("accessToken").asText();
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("registerok"));
    }

    private SysUser insertActiveUser(String username, String email, String password) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setDisplayName("接口用户-" + username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(1);
        sysUserMapper.insert(user);
        return user;
    }

    private String loginAndExtractToken(String username, String email, String password, Long roleId) throws Exception {
        SysUser user = insertActiveUser(username, email, password);
        sysMenuMapper.insertUserRole(user.getId(), roleId);

        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setUsername(username);
        dto.setPassword(password);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
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
