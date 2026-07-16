package com.storage.system.user.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.system.menu.mapper.SysMenuMapper;
import com.storage.system.user.entity.SysUser;
import com.storage.system.user.excel.SysUserExcelColumn;
import com.storage.system.user.mapper.SysUserMapper;
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
class SysUserImportServiceIntegrationTest {

    private static final String USERNAME = "excelimportuser";

    @Autowired
    private SysUserImportService sysUserImportService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysMenuMapper sysMenuMapper;

    @BeforeEach
    void setUp() {
        deleteImportedUser();
    }

    @AfterEach
    void tearDown() {
        deleteImportedUser();
    }

    private void deleteImportedUser() {
        SysUser existing = sysUserMapper.selectByUsername(USERNAME);
        if (existing != null) {
            sysMenuMapper.deleteUserRolesByUserId(existing.getId());
            sysUserMapper.deleteById(existing.getId());
        }
    }

    @Test
    void downloadedTemplate_canBeFilledAndImported() throws IOException {
        MockMultipartFile file = fillTemplate(
                sysUserService.exportTemplate(),
                "users.xlsx",
                Map.of(
                        SysUserExcelColumn.NTID.getIndex(), USERNAME,
                        SysUserExcelColumn.DISPLAY_NAME.getIndex(), "Excel 导入用户",
                        SysUserExcelColumn.EMAIL.getIndex(), "EXCEL.USER@EXAMPLE.COM",
                        SysUserExcelColumn.PHONE.getIndex(), "13800000000",
                        SysUserExcelColumn.ROLE_CODES.getIndex(), "USER",
                        SysUserExcelColumn.STATUS.getIndex(), "启用"
                )
        );

        ImportResultVO result = sysUserImportService.importExcel(file);

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailCount()).isZero();
        SysUser created = sysUserMapper.selectByUsername(USERNAME);
        assertThat(created).isNotNull();
        assertThat(created.getEmail()).isEqualTo("excel.user@example.com");
        assertThat(sysUserMapper.selectRoleCodesByUserId(created.getId())).containsExactly("USER");
    }
}
