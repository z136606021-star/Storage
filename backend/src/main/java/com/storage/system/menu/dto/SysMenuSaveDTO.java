package com.storage.system.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SysMenuSaveDTO {

    private Long parentId;

    @NotBlank(message = "请选择菜单类型")
    private String menuType;

    @NotBlank(message = "请输入菜单名称")
    @Size(max = 64, message = "菜单名称不能超过 64 个字符")
    private String name;

    @Size(max = 128, message = "权限标识不能超过 128 个字符")
    private String permission;

    @Size(max = 128, message = "路由路径不能超过 128 个字符")
    private String path;

    @Size(max = 128, message = "组件 Key 不能超过 128 个字符")
    private String componentKey;

    @Size(max = 64, message = "图标名不能超过 64 个字符")
    private String icon;

    @NotNull(message = "请指定是否显示")
    private Integer visible;

    @NotNull(message = "请指定排序")
    private Integer sortOrder;
}
