package com.storage.system.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storage.system.auth.dto.ChangePasswordByCurrentPasswordDTO;
import com.storage.system.auth.dto.ChangePasswordByVerificationCodeDTO;
import com.storage.system.auth.dto.LoginRequestDTO;
import com.storage.system.auth.entity.EmailVerificationCode;
import com.storage.system.auth.mapper.EmailVerificationCodeMapper;
import com.storage.system.menu.mapper.SysMenuMapper;
import com.storage.system.user.entity.SysUser;
import com.storage.system.user.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthPasswordChangeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysMenuMapper sysMenuMapper;

    @Autowired
    private EmailVerificationCodeMapper emailVerificationCodeMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @MockBean
    private JavaMailSender javaMailSender;

    @BeforeEach
    void setUp() {
        sysUserMapper.delete(null);
        emailVerificationCodeMapper.delete(null);
        reset(javaMailSender);
    }

    @Test
    void changePasswordByCurrentPassword_invalidatesOldToken() throws Exception {
        String token = loginAs("pwduser", "oldpass123");
        ChangePasswordByCurrentPasswordDTO dto = new ChangePasswordByCurrentPasswordDTO();
        dto.setCurrentPassword("oldpass123");
        dto.setNewPassword("newpass123");
        dto.setConfirmPassword("newpass123");

        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto("pwduser", "newpass123"))))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordByCurrentPassword_withWrongCurrentPassword_returns400() throws Exception {
        String token = loginAs("pwduser2", "oldpass123");
        ChangePasswordByCurrentPasswordDTO dto = new ChangePasswordByCurrentPasswordDTO();
        dto.setCurrentPassword("wrongpass");
        dto.setNewPassword("newpass123");
        dto.setConfirmPassword("newpass123");

        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("原密码不正确"));
    }

    @Test
    void changePasswordByVerificationCode_withValidCode_succeeds() throws Exception {
        SysUser user = insertUser("codeuser", "oldpass123", "codeuser@example.com");
        insertVerificationCode(user.getId(), "123456");
        String token = loginExisting("codeuser", "oldpass123");

        ChangePasswordByVerificationCodeDTO dto = new ChangePasswordByVerificationCodeDTO();
        dto.setVerificationCode("123456");
        dto.setNewPassword("newpass123");
        dto.setConfirmPassword("newpass123");

        mockMvc.perform(put("/api/auth/password/by-verification-code")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNoContent());

        SysUser updated = sysUserMapper.selectByUsername("codeuser");
        assertThat(passwordEncoder.matches("newpass123", updated.getPasswordHash())).isTrue();
        assertThat(updated.getTokenVersion()).isEqualTo(1);
    }

    @Test
    void sendPasswordVerificationCode_withoutEmail_returns400() throws Exception {
        insertUser("noemail", "oldpass123", null);
        String token = loginExisting("noemail", "oldpass123");

        mockMvc.perform(post("/api/auth/password/verification-code")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("当前账号未绑定邮箱，无法发送验证码"));
    }

    private String loginAs(String username, String password) throws Exception {
        insertUser(username, password, username + "@example.com");
        return loginExisting(username, password);
    }

    private String loginExisting(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto(username, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private SysUser insertUser(String username, String password, String email) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setDisplayName(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setTokenVersion(0);
        user.setStatus(1);
        sysUserMapper.insert(user);
        sysMenuMapper.insertUserRole(user.getId(), 1L);
        return user;
    }

    private void insertVerificationCode(Long userId, String rawCode) throws Exception {
        EmailVerificationCode code = new EmailVerificationCode();
        code.setUserId(userId);
        code.setPurpose("CHANGE_PASSWORD");
        code.setCodeHash(hash(rawCode));
        code.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        emailVerificationCodeMapper.insert(code);
    }

    private LoginRequestDTO loginDto(String username, String password) {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        return dto;
    }

    private String hash(String raw) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
    }
}
