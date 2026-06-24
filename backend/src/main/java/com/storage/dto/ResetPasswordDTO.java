package com.storage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordDTO {

    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 64, message = "密码长度为 6-64 个字符")
    private String password;
}
