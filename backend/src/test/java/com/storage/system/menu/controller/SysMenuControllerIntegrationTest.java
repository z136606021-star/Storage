package com.storage.system.menu.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storage.system.auth.dto.LoginRequestDTO;
import com.storage.system.menu.dto.SysMenuSaveDTO;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SysMenuControllerIntegrationTest {

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
    void createVisibleRouteMenu_withoutComponentKey_returns400() throws Exception {
        String token = loginAsAdmin("menuadmin1");
        SysMenuSaveDTO dto = new SysMenuSaveDTO();
        dto.setMenuType("SUB");
        dto.setParentId(110L);
        dto.setName("动态菜单");
        dto.setPermission("dynamic:menu:read");
        dto.setPath("/dynamic/menu");
        dto.setVisible(1);
        dto.setSortOrder(10);

        mockMvc.perform(post("/api/system/menus")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("可见路由菜单必须填写组件路径"));
    }

    @Test
    void createHiddenActionMenu_withoutComponentKey_returnsCreatedMenu() throws Exception {
        String token = loginAsAdmin("menuadmin2");
        SysMenuSaveDTO dto = new SysMenuSaveDTO();
        dto.setMenuType("BUTTON");
        dto.setParentId(111L);
        dto.setName("隐藏动作权限");
        dto.setPermission("dynamic:menu:write");
        dto.setVisible(0);
        dto.setSortOrder(20);

        mockMvc.perform(post("/api/system/menus")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permission").value("dynamic:menu:write"))
                .andExpect(jsonPath("$.componentKey").doesNotExist());
    }

    @Test
    void navTree_returnsComponentKeyForAuthorizedVisibleMenu() throws Exception {
        String token = loginAsAdmin("menuadmin3");

        mockMvc.perform(get("/api/menus/nav-tree")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].key").value("110"))
                .andExpect(jsonPath("$[1].icon").value("InboxOutlined"))
                .andExpect(jsonPath("$[1].children[0].componentKey").value("views/warehouse/MaterialLedgerView.vue"))
                .andExpect(jsonPath("$[1].children[0].permission").value("warehouse:material-ledger:read"));
    }

    @Test
    void navTree_returnsOnlyWarehouseAndSystemTopLevelModules() throws Exception {
        String token = loginAsAdmin("menuadmin6");

        String response = mockMvc.perform(get("/api/menus/nav-tree")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var root = objectMapper.readTree(response);
        var rootKeys = new java.util.ArrayList<String>();
        root.forEach(node -> rootKeys.add(node.get("key").asText()));
        assertThat(rootKeys).containsExactly("10", "110", "200");
        assertThat(response).doesNotContain("\"label\":\"经验库\"");
        assertThat(response).doesNotContain("\"label\":\"设计指引\"");
    }

    @Test
    void navTree_excludesHiddenRootRoutesButKeepsHiddenChildren() throws Exception {
        String token = loginAsAdmin("menuadmin4");

        String response = mockMvc.perform(get("/api/menus/nav-tree")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).doesNotContain("\"key\":\"5\"");
        assertThat(response).contains("\"key\":\"6\"");
    }

    @Test
    void navTree_returnsFlatSystemManagementMenus() throws Exception {
        String token = loginAsAdmin("menuadmin5");

        String response = mockMvc.perform(get("/api/menus/nav-tree")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var root = objectMapper.readTree(response);
        var catalogNode = root.findParents("key").stream()
                .filter(node -> "200".equals(node.get("key").asText()))
                .findFirst()
                .orElseThrow();
        var children = catalogNode.get("children");
        assertThat(children).isNotNull();
        assertThat(children).hasSize(4);
        assertThat(children.findValuesAsText("key")).containsExactlyInAnyOrder("201", "202", "203", "204");
        for (var child : children) {
            assertThat(child.hasNonNull("children")).isFalse();
            assertThat(child.get("path").asText()).startsWith("/system/");
        }
    }

    @Test
    void navTree_returnsConfigManagementGroupWithChildren() throws Exception {
        String token = loginAsAdmin("menuadmin7");

        String response = mockMvc.perform(get("/api/menus/nav-tree")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).contains("\"label\":\"配置管理\"");
        assertThat(response).contains("views/warehouse/config/BinManageView.vue");
        assertThat(response).contains("views/warehouse/config/BomManageView.vue");
    }

    @Test
    void navTree_hidesGroupSubWhenNoChildPermission() throws Exception {
        SysUser user = new SysUser();
        user.setUsername("ledgeronly");
        user.setDisplayName("仅台账用户");
        user.setEmail("ledgeronly@example.com");
        user.setPasswordHash(passwordEncoder.encode("oldpass"));
        user.setStatus(1);
        sysUserMapper.insert(user);
        sysMenuMapper.insertUserRole(user.getId(), 2L);

        LoginRequestDTO login = new LoginRequestDTO();
        login.setUsername("ledgeronly");
        login.setPassword("oldpass");
        String token = objectMapper.readTree(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()).get("accessToken").asText();

        String response = mockMvc.perform(get("/api/menus/nav-tree")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).doesNotContain("\"label\":\"配置管理\"");
        assertThat(response).contains("warehouse:material-ledger:read");
    }

    @Test
    void createGroupSub_withoutPermissionAndRoute_returnsCreated() throws Exception {
        String token = loginAsAdmin("menuadmin8");
        SysMenuSaveDTO dto = new SysMenuSaveDTO();
        dto.setMenuType("SUB");
        dto.setParentId(110L);
        dto.setName("测试分组");
        dto.setVisible(1);
        dto.setSortOrder(99);

        mockMvc.perform(post("/api/system/menus")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("测试分组"))
                .andExpect(jsonPath("$.permission").doesNotExist());
    }

    @Test
    void createGroupSub_withPathButNoPermission_returns400() throws Exception {
        String token = loginAsAdmin("menuadmin9");
        SysMenuSaveDTO dto = new SysMenuSaveDTO();
        dto.setMenuType("SUB");
        dto.setParentId(110L);
        dto.setName("非法分组");
        dto.setPath("/warehouse/config/test");
        dto.setVisible(1);
        dto.setSortOrder(99);

        mockMvc.perform(post("/api/system/menus")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("分组子菜单不能填写路由或组件路径"));
    }

    private String loginAsAdmin(String username) throws Exception {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setDisplayName("菜单管理员-" + username);
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
