package com.storage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SysRoleSaveDTO {

    @NotBlank(message = "请输入角色编码")
    @Size(max = 64, message = "角色编码不能超过 64 个字符")
    private String code;

    @NotBlank(message = "请输入角色名称")
    @Size(max = 64, message = "角色名称不能超过 64 个字符")
    private String name;

    @NotNull(message = "请选择状态")
    private Integer status;

    @NotEmpty(message = "请至少选择一个菜单权限")
    private List<Long> menuIds;
}
