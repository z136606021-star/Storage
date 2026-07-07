package com.storage.system.role.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.common.excel.ExcelCellUtils;
import com.storage.common.excel.ExcelImportTemplate;
import com.storage.common.exception.ImportFormatException;
import com.storage.system.menu.mapper.SysMenuMapper;
import com.storage.system.role.dto.SysRoleSaveDTO;
import com.storage.system.role.excel.SysRoleExcelColumn;
import com.storage.system.role.mapper.SysRoleMapper;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysRoleImportService {

    private final SysRoleService sysRoleService;
    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;

    public SysRoleImportService(
            @Lazy SysRoleService sysRoleService,
            SysRoleMapper sysRoleMapper,
            SysMenuMapper sysMenuMapper) {
        this.sysRoleService = sysRoleService;
        this.sysRoleMapper = sysRoleMapper;
        this.sysMenuMapper = sysMenuMapper;
    }

    public ImportResultVO importExcel(MultipartFile file) throws IOException {
        return ExcelImportTemplate.importRows(file, this::isEmptyRow, (excelRow, row) -> {
            SysRoleSaveDTO dto = parseRow(row);
            var existing = sysRoleMapper.selectByCode(dto.getCode());
            if (existing != null) {
                sysRoleService.update(existing.getId(), dto);
            } else {
                sysRoleService.create(dto);
            }
        });
    }

    private SysRoleSaveDTO parseRow(Row row) {
        String code = ExcelCellUtils.getCellString(row, SysRoleExcelColumn.CODE.getIndex());
        String name = ExcelCellUtils.getCellString(row, SysRoleExcelColumn.NAME.getIndex());
        if (!StringUtils.hasText(code)) {
            throw new ImportFormatException("角色编码不能为空");
        }
        if (!StringUtils.hasText(name)) {
            throw new ImportFormatException("角色名称不能为空");
        }

        SysRoleSaveDTO dto = new SysRoleSaveDTO();
        dto.setCode(code.trim());
        dto.setName(name.trim());
        dto.setStatus(parseStatus(ExcelCellUtils.getCellString(row, SysRoleExcelColumn.STATUS.getIndex())));
        dto.setMenuIds(parseMenuIds(ExcelCellUtils.getCellString(row, SysRoleExcelColumn.PERMISSIONS.getIndex())));
        return dto;
    }

    private List<Long> parseMenuIds(String permissionsRaw) {
        if (!StringUtils.hasText(permissionsRaw)) {
            throw new ImportFormatException("菜单权限标识不能为空");
        }
        return Arrays.stream(permissionsRaw.split("[,，]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(permission -> {
                    Long menuId = sysMenuMapper.selectIdByPermission(permission);
                    if (menuId == null) {
                        throw new ImportFormatException("权限标识不存在: " + permission);
                    }
                    return menuId;
                })
                .distinct()
                .collect(Collectors.toList());
    }

    private Integer parseStatus(String raw) {
        if (!StringUtils.hasText(raw)) {
            return 1;
        }
        String value = raw.trim();
        if ("1".equals(value) || "启用".equals(value)) {
            return 1;
        }
        if ("0".equals(value) || "停用".equals(value) || "禁用".equals(value)) {
            return 0;
        }
        throw new ImportFormatException("状态无效，请填写「启用」或「停用」");
    }

    private boolean isEmptyRow(Row row) {
        for (int i = 0; i <= SysRoleExcelColumn.PERMISSIONS.getIndex(); i++) {
            if (StringUtils.hasText(ExcelCellUtils.getCellString(row, i))) {
                return false;
            }
        }
        return true;
    }
}
