package com.storage.system.user.service;

import com.storage.common.excel.ExcelExportWriter;
import com.storage.system.user.dto.SysUserVO;
import com.storage.system.user.excel.SysUserExportRow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
public interface SysUserExportService {
    byte[] export(List<SysUserVO> records) throws IOException;
    byte[] exportTemplate() throws IOException;
}
