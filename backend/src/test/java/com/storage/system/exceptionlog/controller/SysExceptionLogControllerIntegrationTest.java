package com.storage.system.exceptionlog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storage.system.auth.dto.LoginRequestDTO;
import com.storage.system.exceptionlog.dto.ExceptionLogCleanupDTO;
import com.storage.system.exceptionlog.dto.FrontendExceptionReportDTO;
import com.storage.system.exceptionlog.mapper.SysExceptionLogMapper;
import com.storage.system.menu.mapper.SysMenuMapper;
import com.storage.system.user.entity.SysUser;
import com.storage.system.user.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SysExceptionLogControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysMenuMapper sysMenuMapper;

    @Autowired
    private SysExceptionLogMapper sysExceptionLogMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        sysExceptionLogMapper.delete(null);
        sysUserMapper.delete(null);
    }

    @Test
    void unexpectedException_returns500WithRequestIdAndPersistsLog() throws Exception {
        String token = loginAsAdmin("exlog-admin-1");

        mockMvc.perform(get("/api/test/boom")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.message").value("服务器内部错误，请联系管理员"))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());

        assertThat(sysExceptionLogMapper.selectCount(null)).isEqualTo(1);
    }

    @Test
    void reportFrontendException_requiresAuthentication() throws Exception {
        FrontendExceptionReportDTO dto = buildFrontendReport();

        mockMvc.perform(post("/api/system/exception-logs/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reportFrontendException_persistsAuthenticatedReport() throws Exception {
        String token = loginAsAdmin("exlog-admin-2");
        FrontendExceptionReportDTO dto = buildFrontendReport();

        mockMvc.perform(post("/api/system/exception-logs/report")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNoContent());

        assertThat(sysExceptionLogMapper.selectCount(null)).isEqualTo(1);
    }

    @Test
    void pageAndDetail_requireReadPermission() throws Exception {
        String token = loginAsAdmin("exlog-admin-3");
        FrontendExceptionReportDTO dto = buildFrontendReport();
        mockMvc.perform(post("/api/system/exception-logs/report")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNoContent());

        String pageResponse = mockMvc.perform(get("/api/system/exception-logs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].summary").value("test frontend failure"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = objectMapper.readTree(pageResponse).get("records").get(0).get("id").asLong();

        mockMvc.perform(get("/api/system/exception-logs/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("VUE_RUNTIME_ERROR"))
                .andExpect(jsonPath("$.source").value("FRONTEND"));
    }

    @Test
    void cleanup_deletesLogsBeforeCutoff() throws Exception {
        String token = loginAsAdmin("exlog-admin-4");
        FrontendExceptionReportDTO dto = buildFrontendReport();
        mockMvc.perform(post("/api/system/exception-logs/report")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNoContent());

        ExceptionLogCleanupDTO cleanup = new ExceptionLogCleanupDTO();
        cleanup.setBefore(LocalDateTime.now().plusMinutes(1));

        mockMvc.perform(delete("/api/system/exception-logs/cleanup")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cleanup)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(1));

        assertThat(sysExceptionLogMapper.selectCount(null)).isZero();
    }

    private FrontendExceptionReportDTO buildFrontendReport() {
        FrontendExceptionReportDTO dto = new FrontendExceptionReportDTO();
        dto.setErrorCode("VUE_RUNTIME_ERROR");
        dto.setSummary("test frontend failure");
        dto.setStackTrace("Error: test\n    at App.vue:1:1");
        dto.setFrontendRoute("/system/exception-logs");
        dto.setBrowserInfo("vitest");
        dto.setReportType("FRONTEND");
        return dto;
    }

    private String loginAsAdmin(String username) throws Exception {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setDisplayName("异常日志管理员-" + username);
        user.setEmail(username + "@example.com");
        user.setPasswordHash(passwordEncoder.encode("oldpass"));
        user.setStatus(1);
        sysUserMapper.insert(user);
        sysMenuMapper.insertUserRole(user.getId(), 1L);

        LoginRequestDTO login = new LoginRequestDTO();
        login.setUsername(username);
        login.setPassword("oldpass");

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
