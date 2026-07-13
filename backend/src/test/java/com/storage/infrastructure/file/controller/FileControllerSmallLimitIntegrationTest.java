package com.storage.infrastructure.file.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storage.system.auth.dto.LoginRequestDTO;
import com.storage.system.menu.mapper.SysMenuMapper;
import com.storage.system.user.entity.SysUser;
import com.storage.system.user.mapper.SysUserMapper;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.servlet.multipart.max-file-size=1048576",
        "spring.servlet.multipart.max-request-size=2097152",
        "storage.file-upload.max-size-bytes=1048576"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileControllerSmallLimitIntegrationTest {

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

    @MockBean
    private MinioClient minioClient;

    @BeforeEach
    void setUp() throws Exception {
        sysUserMapper.delete(null);
        when(minioClient.putObject(any())).thenReturn(mock(ObjectWriteResponse.class));
    }

    @Test
    void upload_withOversizedFile_returns413() throws Exception {
        String token = loginAsAdmin("fileuploadadmin3");
        byte[] content = new byte[2 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "too-large.bin",
                "application/octet-stream",
                content
        );

        mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isIn(400, 413);
                })
                .andExpect(jsonPath("$.message").value("文件大小超过限制"));
    }

    private String loginAsAdmin(String username) throws Exception {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setDisplayName("上传管理员-" + username);
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
