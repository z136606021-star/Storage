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
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
public interface SysRoleImportService {
    ImportResultVO importExcel(MultipartFile file) throws IOException;
}
