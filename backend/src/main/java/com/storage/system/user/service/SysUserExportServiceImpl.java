package com.storage.system.user.service;

import com.storage.common.excel.ExcelExportWriter;
import com.storage.system.user.dto.SysUserVO;
import com.storage.system.user.excel.SysUserExportRow;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SysUserExportServiceImpl implements SysUserExportService {

    public byte[] export(List<SysUserVO> records) throws IOException {
        List<SysUserExportRow> rows = new ArrayList<>();
        for (SysUserVO record : records) {
            SysUserExportRow row = new SysUserExportRow();
            row.setUsername(record.getUsername());
            row.setDisplayName(record.getDisplayName());
            row.setEmail(record.getEmail());
            row.setPhone(record.getPhone());
            String roleCodes = record.getRoleCodes() == null ? "" : String.join(",", record.getRoleCodes());
            row.setRoleCodes(roleCodes);
            row.setStatus(formatStatus(record.getStatus()));
            rows.add(row);
        }
        return ExcelExportWriter.writeBytes("用户", SysUserExportRow.class, rows);
    }

    public byte[] exportTemplate() throws IOException {
        return export(List.of());
    }

    private String formatStatus(Integer status) {
        return status != null && status == 1 ? "启用" : "停用";
    }
}
