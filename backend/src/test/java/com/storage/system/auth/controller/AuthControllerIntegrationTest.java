package com.storage.system.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storage.system.menu.mapper.SysMenuMapper;
import com.storage.system.user.entity.SysUser;
import com.storage.system.user.mapper.SysUserMapper;
import com.storage.system.auth.dto.ForgotPasswordDTO;
import com.storage.system.auth.dto.ForgotPasswordResetDTO;
import com.storage.system.auth.dto.LoginRequestDTO;
import com.storage.system.auth.dto.RegisterRequestDTO;
import com.storage.system.auth.dto.SendRegistrationVerificationCodeDTO;
import com.storage.system.auth.dto.UpdateCurrentUserPhoneDTO;
import com.storage.system.auth.entity.PasswordResetToken;
import com.storage.system.auth.entity.RegistrationVerificationCode;
import com.storage.system.auth.mapper.JwtRevokedTokenMapper;
import com.storage.system.auth.mapper.PasswordResetTokenMapper;
import com.storage.system.auth.mapper.RegistrationVerificationCodeMapper;
import com.storage.system.auth.shiro.UserRealm;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    private JwtRevokedTokenMapper jwtRevokedTokenMapper;

    @Autowired
    private RegistrationVerificationCodeMapper registrationVerificationCodeMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private UserRealm userRealm;

    @MockBean
    private JavaMailSender mailSender;

    @BeforeEach
    void setUp() {
        passwordResetTokenMapper.delete(null);
        jwtRevokedTokenMapper.delete(null);
        registrationVerificationCodeMapper.delete(null);
        sysUserMapper.delete(null);
        reset(mailSender);
    }

    @Test
    void login_withEmail_returnsAccessToken() throws Exception {
        SysUser user = insertActiveUser("loginemail", "loginemail@example.com", "oldpass");
        sysMenuMapper.insertUserRole(user.getId(), 1L);

        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setUsername("loginemail@example.com");
        dto.setPassword("oldpass");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("loginemail@example.com"));
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
        dto.setEmail("wrong@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("邮箱不正确或未绑定账号"));
    }

    @Test
    void forgotPassword_afterTooManyFailures_returnsThrottleMessage() throws Exception {
        insertActiveUser("forgotlimited", "forgotlimited@example.com", "oldpass");

        ForgotPasswordDTO dto = new ForgotPasswordDTO();
        dto.setEmail("throttlefail@example.com");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("邮箱不正确或未绑定账号"));
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
    void logout_revokesTokenAndMeReturns401() throws Exception {
        String token = loginAndExtractToken("logoutok", "logoutok@example.com", "oldpass", 1L);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());

        assertThat(jwtRevokedTokenMapper.selectCount(null)).isEqualTo(1);
    }

    @Test
    void me_withBearerToken_returnsCurrentUserPhone() throws Exception {
        SysUser user = insertActiveUser("phoneuser", "phoneuser@example.com", "oldpass");
        user.setPhone("13800138000");
        sysUserMapper.updateById(user);
        sysMenuMapper.insertUserRole(user.getId(), 1L);

        String token = loginExistingUser("phoneuser", "oldpass");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.phone").value("13800138000"));
    }

    @Test
    void updateMePhone_withBearerToken_updatesOnlyCurrentUser() throws Exception {
        SysUser caller = insertActiveUser("phonecaller", "phonecaller@example.com", "oldpass");
        SysUser other = insertActiveUser("phoneother", "phoneother@example.com", "oldpass");
        other.setPhone("13900139000");
        sysUserMapper.updateById(other);
        sysMenuMapper.insertUserRole(caller.getId(), 1L);

        String token = loginExistingUser("phonecaller", "oldpass");

        UpdateCurrentUserPhoneDTO request = new UpdateCurrentUserPhoneDTO();
        request.setPhone(" 13800138001 ");

        mockMvc.perform(put("/api/auth/me/phone")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("13800138001"));

        assertThat(sysUserMapper.selectById(caller.getId()).getPhone()).isEqualTo("13800138001");
        assertThat(sysUserMapper.selectById(other.getId()).getPhone()).isEqualTo("13900139000");
    }

    @Test
    void updateMePhone_withBlankPhone_clearsPhone() throws Exception {
        SysUser user = insertActiveUser("phoneclear", "phoneclear@example.com", "oldpass");
        user.setPhone("13800138000");
        sysUserMapper.updateById(user);
        sysMenuMapper.insertUserRole(user.getId(), 1L);

        String token = loginExistingUser("phoneclear", "oldpass");

        UpdateCurrentUserPhoneDTO request = new UpdateCurrentUserPhoneDTO();
        request.setPhone("   ");

        mockMvc.perform(put("/api/auth/me/phone")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").isEmpty());

        assertThat(sysUserMapper.selectById(user.getId()).getPhone()).isNull();
    }

    @Test
    void updateMePhone_withTooLongPhone_returns400() throws Exception {
        String token = loginAndExtractToken("phonelimit", "phonelimit@example.com", "oldpass", 1L);

        UpdateCurrentUserPhoneDTO request = new UpdateCurrentUserPhoneDTO();
        request.setPhone("1".repeat(33));

        mockMvc.perform(put("/api/auth/me/phone")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("手机号不能超过 32 个字符"));
    }

    @Test
    void updateMePhone_withoutToken_returns401() throws Exception {
        UpdateCurrentUserPhoneDTO request = new UpdateCurrentUserPhoneDTO();
        request.setPhone("13800138000");

        mockMvc.perform(put("/api/auth/me/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void sendRegistrationVerificationCode_withoutAuth_returnsNoContent() throws Exception {
        SendRegistrationVerificationCodeDTO request = new SendRegistrationVerificationCodeDTO();
        request.setEmail("newregister@example.com");

        mockMvc.perform(post("/api/auth/register/verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("newregister@example.com");
    }

    @Test
    void sendRegistrationVerificationCode_withLegacyDatabaseClockSkew_allowsResendThenAppliesCooldown() throws Exception {
        String email = "clockskew@example.com";
        LocalDateTime now = LocalDateTime.now();
        RegistrationVerificationCode legacyCode = new RegistrationVerificationCode();
        legacyCode.setEmail(email);
        legacyCode.setCodeHash(hashCode("123456"));
        legacyCode.setExpiresAt(now.minusMinutes(1));
        legacyCode.setCreatedAt(now.plusHours(8));
        registrationVerificationCodeMapper.insert(legacyCode);

        SendRegistrationVerificationCodeDTO request = new SendRegistrationVerificationCodeDTO();
        request.setEmail(email);

        mockMvc.perform(post("/api/auth/register/verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/register/verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("发送过于频繁，请稍后再试"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly(email);
    }

    @Test
    void sendPasswordVerificationCode_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/password/verification-code"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("未登录或登录已过期"));
    }

    @Test
    void register_withMissingVerificationCode_returns400() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("registernocode");
        request.setDisplayName("注册用户");
        request.setPassword("newpass123");
        request.setEmail("registernocode@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请输入验证码"));
    }

    @Test
    void register_withWhitespaceUsername_returns400() throws Exception {
        RegisterRequestDTO request = buildRegisterRequest(
                "123 asd",
                "注册用户",
                "newpass123",
                "whitespace@example.com",
                "123456"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("账号不能包含空格或空白字符"));
    }

    @Test
    void register_withEnglishDisplayName_preservesInternalSpaceAndTrimsOuterWhitespace() throws Exception {
        insertRegistrationCode("englishname@example.com", "123456");
        RegisterRequestDTO request = buildRegisterRequest(
                "englishname",
                "  Zixuan Zhu  ",
                "newpass123",
                "englishname@example.com",
                "123456"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.displayName").value("Zixuan Zhu"));

        SysUser user = sysUserMapper.selectByUsername("englishname");
        assertThat(user).isNotNull();
        assertThat(user.getDisplayName()).isEqualTo("Zixuan Zhu");
    }

    @Test
    void register_withDuplicateEmail_rejects() throws Exception {
        insertActiveUser("existingmail", "dupmail@example.com", "oldpass");
        insertRegistrationCode("dupmail@example.com", "123456");

        RegisterRequestDTO request = buildRegisterRequest(
                "newuser1",
                "新用户",
                "newpass123",
                "dupmail@example.com",
                "123456"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("邮箱已被其他用户使用"));
    }

    @Test
    void register_withMissingEmail_returns400() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("registernoemail");
        request.setDisplayName("注册用户");
        request.setPassword("newpass123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请输入验证码；请输入邮箱"));
    }

    @Test
    void register_withBlankEmail_returns400() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("registerblankemail");
        request.setDisplayName("注册用户");
        request.setPassword("newpass123");
        request.setEmail("");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请输入验证码；请输入邮箱"));
    }

    @Test
    void register_returnsAccessTokenThatCanAccessMe() throws Exception {
        insertRegistrationCode("registerok@example.com", "123456");
        RegisterRequestDTO request = buildRegisterRequest(
                "registerok",
                "注册用户",
                "newpass123",
                "registerok@example.com",
                "123456"
        );

        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.user.email").value("registerok@example.com"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(response).get("accessToken").asText();
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("registerok"))
                .andExpect(jsonPath("$.user.email").value("registerok@example.com"));
    }

    @Test
    void register_normalizesMixedCaseEmailToLowercase() throws Exception {
        insertRegistrationCode("regemailmix@example.com", "654321");
        RegisterRequestDTO request = buildRegisterRequest(
                "regemailmix",
                "混合大小写邮箱",
                "newpass123",
                "RegEmailMix@Example.COM",
                "654321"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        SysUser user = sysUserMapper.selectByUsername("regemailmix");
        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo("regemailmix@example.com");
    }

    private RegisterRequestDTO buildRegisterRequest(
            String username,
            String displayName,
            String password,
            String email,
            String verificationCode
    ) {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername(username);
        request.setDisplayName(displayName);
        request.setPassword(password);
        request.setEmail(email);
        request.setVerificationCode(verificationCode);
        return request;
    }

    private void insertRegistrationCode(String email, String rawCode) {
        RegistrationVerificationCode code = new RegistrationVerificationCode();
        code.setEmail(email.trim().toLowerCase());
        code.setCodeHash(hashCode(rawCode));
        code.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        registrationVerificationCodeMapper.insert(code);
    }

    private String hashCode(String rawCode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawCode.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
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

    private String loginExistingUser(String username, String password) throws Exception {
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

    private String loginAndExtractToken(String username, String email, String password, Long roleId) throws Exception {
        SysUser user = insertActiveUser(username, email, password);
        sysMenuMapper.insertUserRole(user.getId(), roleId);

        return loginExistingUser(username, password);
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
