package com.storage.system.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordByVerificationCodeDTO {

    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码格式不正确")
    private String verificationCode;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度为 6-64 个字符")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度为 6-64 个字符")
    private String confirmPassword;
}
