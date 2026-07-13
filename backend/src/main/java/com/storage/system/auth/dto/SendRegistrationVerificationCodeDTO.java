package com.storage.system.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendRegistrationVerificationCodeDTO {

    @NotBlank(message = "请输入邮箱")
    @Size(max = 128, message = "邮箱不能超过 128 个字符")
    @Email(message = "邮箱格式不正确")
    private String email;
}
