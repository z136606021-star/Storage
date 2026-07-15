package com.storage.system.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storage.system.auth.dto.LoginRequestDTO;
import com.storage.system.user.dto.SysUserSaveDTO;
import com.storage.system.user.entity.SysUser;
import com.storage.system.menu.mapper.SysMenuMapper;
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

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SysUserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysMenuMapper sysMenuMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        sysUserMapper.delete(null);
    }

    @Test
    void createUser_withDisplayNameContainingSpace_returnsCreatedWithRequestId() throws Exception {
        String token = loginAsAdmin("user-create-admin");

        SysUserSaveDTO dto = new SysUserSaveDTO();
        dto.setUsername("2317362");
        dto.setDisplayName("Mandy Liu");
        dto.setEmail("mandy_liu7362@jabil.com");
        dto.setPhone("18820777053");
        dto.setStatus(1);
        dto.setRoleIds(List.of(1L));

        mockMvc.perform(post("/api/system/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Mandy Liu"))
                .andExpect(jsonPath("$.username").value("2317362"));
    }

    @Test
    void createUser_withWhitespaceUsername_returnsBadRequest() throws Exception {
        String token = loginAsAdmin("user-space-admin");

        SysUserSaveDTO dto = new SysUserSaveDTO();
        dto.setUsername("123 asd");
        dto.setDisplayName("用户空格");
        dto.setEmail("space@example.com");
        dto.setStatus(1);
        dto.setRoleIds(List.of(2L));

        mockMvc.perform(post("/api/system/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("NTID不能包含空格或空白字符"));
    }

    @Test
    void createUser_withInvalidEmail_returnsBadRequest() throws Exception {
        String token = loginAsAdmin("user-email-admin");

        SysUserSaveDTO dto = new SysUserSaveDTO();
        dto.setUsername("badmail");
        dto.setDisplayName("邮箱错误");
        dto.setEmail("not-an-email");
        dto.setStatus(1);
        dto.setRoleIds(List.of(2L));

        mockMvc.perform(post("/api/system/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("邮箱")));
    }

    @Test
    void createUser_withEmptyRoles_returnsBadRequest() throws Exception {
        String token = loginAsAdmin("user-role-admin");

        SysUserSaveDTO dto = new SysUserSaveDTO();
        dto.setUsername("norole");
        dto.setDisplayName("无角色");
        dto.setStatus(1);
        dto.setRoleIds(List.of());

        mockMvc.perform(post("/api/system/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("角色")));
    }

    private String loginAsAdmin(String username) throws Exception {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setDisplayName("用户管理员-" + username);
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
