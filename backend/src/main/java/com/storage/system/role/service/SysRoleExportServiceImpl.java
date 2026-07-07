package com.storage.system.role.service;

import com.storage.common.excel.ExcelExportWriter;
import com.storage.system.menu.mapper.SysMenuMapper;
import com.storage.system.role.dto.SysRoleVO;
import com.storage.system.role.excel.SysRoleExportRow;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SysRoleExportServiceImpl implements SysRoleExportService {

    private final SysMenuMapper sysMenuMapper;

    public SysRoleExportServiceImpl(SysMenuMapper sysMenuMapper) {
        this.sysMenuMapper = sysMenuMapper;
    }

    public byte[] export(List<SysRoleVO> records) throws IOException {
        List<SysRoleExportRow> rows = new ArrayList<>();
        for (SysRoleVO record : records) {
            SysRoleExportRow row = new SysRoleExportRow();
            row.setCode(record.getCode());
            row.setName(record.getName());
            row.setStatus(formatStatus(record.getStatus()));
            List<String> permissions = resolvePermissions(record);
            row.setPermissions(String.join(",", permissions));
            rows.add(row);
        }
        return ExcelExportWriter.writeBytes("角色", SysRoleExportRow.class, rows);
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
}
