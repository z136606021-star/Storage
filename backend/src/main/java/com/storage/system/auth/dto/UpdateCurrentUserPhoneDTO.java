package com.storage.system.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCurrentUserPhoneDTO {

    @Size(max = 32, message = "手机号不能超过 32 个字符")
    private String phone;
}
