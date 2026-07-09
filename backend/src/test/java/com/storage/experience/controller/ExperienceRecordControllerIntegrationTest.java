package com.storage.experience.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storage.experience.dto.ExperienceRecordSaveDTO;
import com.storage.experience.dto.ExperienceTypeSaveDTO;
import com.storage.experience.entity.ExperienceType;
import com.storage.experience.mapper.ExperienceAttachmentMapper;
import com.storage.experience.mapper.ExperienceProjectLinkMapper;
import com.storage.experience.mapper.ExperienceRecordMapper;
import com.storage.experience.mapper.ExperienceTypeMapper;
import com.storage.experience.service.ExperienceTypeService;
import com.storage.system.auth.dto.LoginRequestDTO;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExperienceRecordControllerIntegrationTest {

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

    @Autowired
    private ExperienceTypeService experienceTypeService;

    @Autowired
    private ExperienceRecordMapper experienceRecordMapper;

    @Autowired
    private ExperienceTypeMapper experienceTypeMapper;

    @Autowired
    private ExperienceProjectLinkMapper experienceProjectLinkMapper;

    @Autowired
    private ExperienceAttachmentMapper experienceAttachmentMapper;

    private ExperienceType type;

    @BeforeEach
    void setUp() {
        experienceAttachmentMapper.delete(null);
        experienceProjectLinkMapper.delete(null);
        experienceRecordMapper.delete(null);
        experienceTypeMapper.delete(null);
        sysUserMapper.delete(null);

        ExperienceTypeSaveDTO dto = new ExperienceTypeSaveDTO();
        dto.setName("设计经验");
        dto.setStatus(1);
        dto.setSortOrder(10);
        type = experienceTypeService.create(dto);
    }

    @Test
    void createExperience_withInvalidBody_returns400() throws Exception {
        String token = loginAsAdmin("experienceadmin1");
        ExperienceRecordSaveDTO dto = new ExperienceRecordSaveDTO();
        dto.setTypeId(type.getId());
        dto.setDescription("");

        mockMvc.perform(post("/api/experience/records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    private String loginAsAdmin(String username) throws Exception {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setDisplayName("经验管理员-" + username);
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
