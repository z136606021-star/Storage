package com.storage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequestDTO {

    @NotBlank(message = "请输入账号")
    @Size(min = 3, max = 32, message = "账号长度为 3-32 个字符")
    private String username;

    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 64, message = "密码长度为 6-64 个字符")
    private String password;

    private Boolean rememberMe;
}
