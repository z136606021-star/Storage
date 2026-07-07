package com.storage.system.user.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class SysUserExportRow {

    @ExcelProperty(value = "NTID", index = 0)
    private String username;

    @ExcelProperty(value = "用户姓名", index = 1)
    private String displayName;

    @ExcelProperty(value = "邮箱", index = 2)
    private String email;

    @ExcelProperty(value = "手机号", index = 3)
    private String phone;

    @ExcelProperty(value = "角色编码", index = 4)
    private String roleCodes;

    @ExcelProperty(value = "状态", index = 5)
    private String status;
}
