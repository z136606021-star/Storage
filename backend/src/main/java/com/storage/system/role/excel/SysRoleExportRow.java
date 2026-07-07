package com.storage.system.role.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class SysRoleExportRow {

    @ExcelProperty(value = "角色编码", index = 0)
    private String code;

    @ExcelProperty(value = "角色名称", index = 1)
    private String name;

    @ExcelProperty(value = "状态", index = 2)
    private String status;

    @ExcelProperty(value = "菜单权限标识", index = 3)
    private String permissions;
}
