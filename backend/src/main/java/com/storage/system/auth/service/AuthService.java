package com.storage.system.auth.service;

import com.storage.system.user.entity.SysUser;
import com.storage.system.auth.dto.AuthSessionVO;
import com.storage.system.auth.dto.ForgotPasswordDTO;
import com.storage.system.auth.dto.ForgotPasswordResetDTO;
import com.storage.system.auth.dto.LoginRequestDTO;
import com.storage.system.auth.dto.RegisterRequestDTO;

public interface AuthService {

    AuthSessionVO login(LoginRequestDTO request);

    AuthSessionVO register(RegisterRequestDTO request);

    void logout();

    void forgotPassword(ForgotPasswordDTO request);

    void resetPassword(ForgotPasswordResetDTO request);

    AuthSessionVO currentSession();

    SysUser currentUser();
}
