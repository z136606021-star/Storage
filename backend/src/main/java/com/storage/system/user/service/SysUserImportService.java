package com.storage.system.user.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.common.excel.ExcelCellUtils;
import com.storage.common.excel.ExcelImportTemplate;
import com.storage.common.exception.ImportFormatException;
import com.storage.system.role.mapper.SysRoleMapper;
import com.storage.system.user.dto.SysUserSaveDTO;
import com.storage.system.user.excel.SysUserExcelColumn;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
public interface SysUserImportService {
    ImportResultVO importExcel(MultipartFile file) throws IOException;
}
