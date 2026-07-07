package com.storage.system.role.service;

import com.storage.common.excel.ExcelCellUtils;
import com.storage.system.menu.entity.SysMenu;
import com.storage.system.menu.mapper.SysMenuMapper;
import com.storage.system.role.dto.SysRoleVO;
import com.storage.system.role.excel.SysRoleExcelColumn;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysRoleExportServiceTest {

    @Mock
    private SysMenuMapper sysMenuMapper;

    private SysRoleExportService exportService;

    @BeforeEach
    void setUp() {
        exportService = new SysRoleExportServiceImpl(sysMenuMapper);
    }

    @Test
    void export_withPermissionsOnVo_writesExpectedValues() throws IOException {
        SysRoleVO role = SysRoleVO.builder()
                .code("ADMIN")
                .name("管理员")
                .status(1)
                .permissions(List.of("warehouse:ledger:read", "system:user:write"))
                .build();

        byte[] bytes = exportService.export(List.of(role));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("角色");

            Row headerRow = sheet.getRow(0);
            String[] headers = SysRoleExcelColumn.headers();
            for (int i = 0; i < headers.length; i++) {
                assertThat(ExcelCellUtils.getCellString(headerRow, i)).isEqualTo(headers[i]);
            }

            Row dataRow = sheet.getRow(1);
            assertThat(ExcelCellUtils.getCellString(dataRow, SysRoleExcelColumn.CODE.getIndex()))
                    .isEqualTo("ADMIN");
            assertThat(ExcelCellUtils.getCellString(dataRow, SysRoleExcelColumn.NAME.getIndex()))
                    .isEqualTo("管理员");
            assertThat(ExcelCellUtils.getCellString(dataRow, SysRoleExcelColumn.STATUS.getIndex()))
                    .isEqualTo("启用");
            assertThat(ExcelCellUtils.getCellString(dataRow, SysRoleExcelColumn.PERMISSIONS.getIndex()))
                    .isEqualTo("warehouse:ledger:read,system:user:write");
        }
    }

    @Test
    void export_withMenuIds_resolvesPermissionsFromMapper() throws IOException {
        SysMenu menu = new SysMenu();
        menu.setId(10L);
        menu.setPermission("warehouse:bin:write");
        when(sysMenuMapper.selectById(10L)).thenReturn(menu);

        SysRoleVO role = SysRoleVO.builder()
                .code("USER")
                .name("普通用户")
                .status(0)
                .menuIds(List.of(10L))
                .build();

        byte[] bytes = exportService.export(List.of(role));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Row dataRow = workbook.getSheetAt(0).getRow(1);
            assertThat(ExcelCellUtils.getCellString(dataRow, SysRoleExcelColumn.STATUS.getIndex()))
                    .isEqualTo("停用");
            assertThat(ExcelCellUtils.getCellString(dataRow, SysRoleExcelColumn.PERMISSIONS.getIndex()))
                    .isEqualTo("warehouse:bin:write");
        }
    }

    @Test
    void exportTemplate_writesHeaderOnly() throws IOException {
        byte[] bytes = exportService.exportTemplate();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("角色");
            assertThat(sheet.getRow(0)).isNotNull();
            assertThat(sheet.getRow(1)).isNull();
        }
    }
}
