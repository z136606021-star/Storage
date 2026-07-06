package com.storage.system.role.service;

import com.storage.common.excel.ExcelCellUtils;
import com.storage.system.menu.mapper.SysMenuMapper;
import com.storage.system.role.dto.SysRoleVO;
import com.storage.system.role.excel.SysRoleExcelColumn;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SysRoleExportService {

    private final SysMenuMapper sysMenuMapper;

    public SysRoleExportService(SysMenuMapper sysMenuMapper) {
        this.sysMenuMapper = sysMenuMapper;
    }

    public byte[] export(List<SysRoleVO> records) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("角色");
            CellStyle headerStyle = ExcelCellUtils.createHeaderStyle(workbook);
            CellStyle dataStyle = ExcelCellUtils.createDataStyle(workbook);

            String[] headers = SysRoleExcelColumn.headers();
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                ExcelCellUtils.setCell(headerRow, i, headers[i], headerStyle);
            }

            int rowIndex = 1;
            for (SysRoleVO record : records) {
                Row row = sheet.createRow(rowIndex++);
                ExcelCellUtils.setCell(row, SysRoleExcelColumn.CODE.getIndex(), record.getCode(), dataStyle);
                ExcelCellUtils.setCell(row, SysRoleExcelColumn.NAME.getIndex(), record.getName(), dataStyle);
                ExcelCellUtils.setCell(row, SysRoleExcelColumn.STATUS.getIndex(), formatStatus(record.getStatus()), dataStyle);
                List<String> permissions = resolvePermissions(record);
                ExcelCellUtils.setCell(row, SysRoleExcelColumn.PERMISSIONS.getIndex(), String.join(",", permissions), dataStyle);
            }

            autoSizeColumns(sheet, headers.length);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportTemplate() throws IOException {
        return export(List.of());
    }

    private List<String> resolvePermissions(SysRoleVO record) {
        if (record.getPermissions() != null && !record.getPermissions().isEmpty()) {
            return record.getPermissions();
        }
        if (record.getMenuIds() == null) {
            return List.of();
        }
        List<String> permissions = new ArrayList<>();
        for (Long menuId : record.getMenuIds()) {
            var menu = sysMenuMapper.selectById(menuId);
            if (menu != null && menu.getPermission() != null && !menu.getPermission().isBlank()) {
                permissions.add(menu.getPermission());
            }
        }
        return permissions;
    }

    private String formatStatus(Integer status) {
        return status != null && status == 1 ? "启用" : "停用";
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(width + 512, 256 * 40));
        }
    }
}
