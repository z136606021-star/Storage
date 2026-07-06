package com.storage.system.role.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.common.excel.ExcelCellUtils;
import com.storage.common.exception.ImportFormatException;
import com.storage.system.menu.mapper.SysMenuMapper;
import com.storage.system.role.dto.SysRoleSaveDTO;
import com.storage.system.role.excel.SysRoleExcelColumn;
import com.storage.system.role.mapper.SysRoleMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
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
        if (file == null || file.isEmpty()) {
            throw new ImportFormatException("请上传 Excel 文件");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            throw new ImportFormatException("仅支持 .xlsx 或 .xls 格式");
        }

        ImportResultVO result = new ImportResultVO();
        List<ImportResultVO.ImportErrorVO> errors = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new ImportFormatException("Excel 文件中没有工作表");
            }

            int lastRow = sheet.getLastRowNum();
            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                int excelRow = i + 1;
                try {
                    SysRoleSaveDTO dto = parseRow(row);
                    var existing = sysRoleMapper.selectByCode(dto.getCode());
                    if (existing != null) {
                        sysRoleService.update(existing.getId(), dto);
                    } else {
                        sysRoleService.create(dto);
                    }
                    result.setSuccessCount(result.getSuccessCount() + 1);
                } catch (Exception ex) {
                    result.setFailCount(result.getFailCount() + 1);
                    errors.add(new ImportResultVO.ImportErrorVO(excelRow, ex.getMessage()));
                }
            }
        }

        result.setErrors(errors);
        return result;
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
