package com.storage.system.role.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.system.menu.mapper.SysMenuMapper;
import com.storage.system.role.entity.SysRole;
import com.storage.system.role.excel.SysRoleExcelColumn;
import com.storage.system.role.mapper.SysRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.Map;

import static com.storage.common.excel.ExcelTemplateTestSupport.fillTemplate;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SysRoleImportServiceIntegrationTest {

    private static final String ROLE_CODE = "EXCEL_IMPORT_ROLE";
    private static final String PERMISSION = "warehouse:material-ledger:read";

    @Autowired
    private SysRoleImportService sysRoleImportService;

    @Autowired
    private SysRoleService sysRoleService;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysMenuMapper sysMenuMapper;

    @BeforeEach
    void setUp() {
        deleteImportedRole();
    }

    @AfterEach
    void tearDown() {
        deleteImportedRole();
    }

    private void deleteImportedRole() {
        SysRole existing = sysRoleMapper.selectByCode(ROLE_CODE);
        if (existing != null) {
            sysMenuMapper.deleteRoleMenusByRoleId(existing.getId());
            sysRoleMapper.deleteById(existing.getId());
        }
    }

    @Test
    void downloadedTemplate_canBeFilledAndImported() throws IOException {
        MockMultipartFile file = fillTemplate(
                sysRoleService.exportTemplate(),
                "roles.xlsx",
                Map.of(
                        SysRoleExcelColumn.CODE.getIndex(), ROLE_CODE,
                        SysRoleExcelColumn.NAME.getIndex(), "Excel 导入角色",
                        SysRoleExcelColumn.STATUS.getIndex(), "启用",
                        SysRoleExcelColumn.PERMISSIONS.getIndex(), PERMISSION
                )
        );

        ImportResultVO result = sysRoleImportService.importExcel(file);

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailCount()).isZero();
        SysRole created = sysRoleMapper.selectByCode(ROLE_CODE);
        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("Excel 导入角色");
        assertThat(sysRoleMapper.selectMenuIdsByRoleId(created.getId()))
                .contains(sysMenuMapper.selectIdByPermission(PERMISSION));
    }
}
