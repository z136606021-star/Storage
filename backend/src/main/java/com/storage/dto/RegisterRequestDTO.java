package com.storage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequestDTO {

    @NotBlank(message = "请输入账号")
    @Size(min = 3, max = 32, message = "账号长度为 3-32 个字符")
    private String username;

    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 64, message = "密码长度为 6-64 个字符")
    private String password;

    @NotBlank(message = "请输入显示名称")
    @Size(max = 64, message = "显示名称不能超过 64 个字符")
    private String displayName;
}
