package com.storage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SysUserSaveDTO {

    @NotBlank(message = "请输入 NTID")
    @Size(min = 3, max = 64, message = "NTID 长度为 3-64 个字符")
    private String username;

    @NotBlank(message = "请输入用户姓名")
    @Size(max = 64, message = "用户姓名不能超过 64 个字符")
    private String displayName;

    @Size(max = 128, message = "邮箱不能超过 128 个字符")
    private String email;

    @Size(max = 32, message = "手机号不能超过 32 个字符")
    private String phone;

    @Size(max = 64, message = "密码长度不能超过 64 个字符")
    private String password;

    @NotNull(message = "请选择状态")
    private Integer status;

    @NotEmpty(message = "请至少分配一个角色")
    private List<Long> roleIds;
}
