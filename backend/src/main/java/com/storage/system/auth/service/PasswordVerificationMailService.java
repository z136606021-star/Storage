package com.storage.system.auth.service;

import com.storage.system.user.entity.SysUser;

public interface PasswordVerificationMailService {

    void sendVerificationCode(SysUser user, String rawCode);
}
