package com.storage.system.user.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.common.excel.ExcelCellUtils;
import com.storage.common.exception.ImportFormatException;
import com.storage.system.role.mapper.SysRoleMapper;
import com.storage.system.user.dto.SysUserSaveDTO;
import com.storage.system.user.excel.SysUserExcelColumn;
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
public class SysUserImportService {

    private final SysUserService sysUserService;
    private final SysRoleMapper sysRoleMapper;

    public SysUserImportService(@Lazy SysUserService sysUserService, SysRoleMapper sysRoleMapper) {
        this.sysUserService = sysUserService;
        this.sysRoleMapper = sysRoleMapper;
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
                    SysUserSaveDTO dto = parseRow(row);
                    sysUserService.create(dto);
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

    private SysUserSaveDTO parseRow(Row row) {
        String username = ExcelCellUtils.getCellString(row, SysUserExcelColumn.NTID.getIndex());
        String displayName = ExcelCellUtils.getCellString(row, SysUserExcelColumn.DISPLAY_NAME.getIndex());
        if (!StringUtils.hasText(username)) {
            throw new ImportFormatException("NTID 不能为空");
        }
        if (!StringUtils.hasText(displayName)) {
            throw new ImportFormatException("用户姓名不能为空");
        }

        SysUserSaveDTO dto = new SysUserSaveDTO();
        dto.setUsername(username.trim());
        dto.setDisplayName(displayName.trim());
        dto.setEmail(trimToNull(ExcelCellUtils.getCellString(row, SysUserExcelColumn.EMAIL.getIndex())));
        dto.setPhone(trimToNull(ExcelCellUtils.getCellString(row, SysUserExcelColumn.PHONE.getIndex())));
        dto.setStatus(parseStatus(ExcelCellUtils.getCellString(row, SysUserExcelColumn.STATUS.getIndex())));
        dto.setRoleIds(parseRoleIds(ExcelCellUtils.getCellString(row, SysUserExcelColumn.ROLE_CODES.getIndex())));
        return dto;
    }

    private List<Long> parseRoleIds(String roleCodesRaw) {
        if (!StringUtils.hasText(roleCodesRaw)) {
            throw new ImportFormatException("角色编码不能为空");
        }
        List<Long> roleIds = Arrays.stream(roleCodesRaw.split("[,，]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(code -> {
                    var role = sysRoleMapper.selectByCode(code);
                    if (role == null) {
                        throw new ImportFormatException("角色编码不存在: " + code);
                    }
                    if (role.getStatus() != null && role.getStatus() != 1) {
                        throw new ImportFormatException("角色已停用: " + code);
                    }
                    return role.getId();
                })
                .collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            throw new ImportFormatException("角色编码不能为空");
        }
        return roleIds;
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean isEmptyRow(Row row) {
        for (int i = 0; i <= SysUserExcelColumn.STATUS.getIndex(); i++) {
            if (StringUtils.hasText(ExcelCellUtils.getCellString(row, i))) {
                return false;
            }
        }
        return true;
    }
}
