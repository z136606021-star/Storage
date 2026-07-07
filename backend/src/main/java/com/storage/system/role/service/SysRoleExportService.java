package com.storage.system.role.service;

import com.storage.common.excel.ExcelExportWriter;
import com.storage.system.menu.mapper.SysMenuMapper;
import com.storage.system.role.dto.SysRoleVO;
import com.storage.system.role.excel.SysRoleExportRow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
public interface SysRoleExportService {
    byte[] export(List<SysRoleVO> records) throws IOException;
    byte[] exportTemplate() throws IOException;
}
