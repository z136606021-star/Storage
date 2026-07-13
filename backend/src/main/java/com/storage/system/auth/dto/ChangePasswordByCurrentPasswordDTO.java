package com.storage.system.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordByCurrentPasswordDTO {

    @NotBlank(message = "原密码不能为空")
    @Size(max = 64, message = "原密码长度不能超过 64 个字符")
    private String currentPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度为 6-64 个字符")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度为 6-64 个字符")
    private String confirmPassword;
}
