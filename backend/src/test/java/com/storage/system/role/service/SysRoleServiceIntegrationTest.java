package com.storage.system.role.service;

import com.storage.system.menu.mapper.SysMenuMapper;
import com.storage.system.role.dto.SysRoleSaveDTO;
import com.storage.system.role.mapper.SysRoleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SysRoleServiceIntegrationTest {

    @Autowired
    private SysRoleService sysRoleService;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysMenuMapper sysMenuMapper;

    @Test
    @Transactional
    void replaceRoleMenus_expandsAncestorsWhenSavingButtonOnly() {
        SysRoleSaveDTO dto = new SysRoleSaveDTO();
        dto.setCode("TEST_ROLE_BTN");
        dto.setName("按钮继承测试");
        dto.setStatus(1);
        dto.setMenuIds(List.of(2L));

        var created = sysRoleService.create(dto);
        var detail = sysRoleService.getById(created.getId());

        assertThat(detail.getMenuIds()).containsExactlyInAnyOrder(2L, 111L, 110L);
    }

    @Test
    @Transactional
    void replaceRoleMenus_doesNotExpandChildrenWhenSavingTopOnly() {
        SysRoleSaveDTO dto = new SysRoleSaveDTO();
        dto.setCode("TEST_ROLE_TOP");
        dto.setName("一级不向下扩权");
        dto.setStatus(1);
        dto.setMenuIds(List.of(110L));

        var created = sysRoleService.create(dto);
        var detail = sysRoleService.getById(created.getId());

        assertThat(detail.getMenuIds()).containsExactly(110L);
    }
}
