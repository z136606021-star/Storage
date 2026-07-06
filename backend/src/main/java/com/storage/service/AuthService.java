package com.storage.service;

import com.storage.dto.AuthSessionVO;
import com.storage.dto.ForgotPasswordResetDTO;
import com.storage.dto.ForgotPasswordDTO;
import com.storage.dto.LoginRequestDTO;
import com.storage.dto.RegisterRequestDTO;
import com.storage.entity.SysUser;

public interface AuthService {

    AuthSessionVO login(LoginRequestDTO request);

    AuthSessionVO register(RegisterRequestDTO request);

    void logout();

    void forgotPassword(ForgotPasswordDTO request);

    void resetPassword(ForgotPasswordResetDTO request);

    AuthSessionVO currentSession();

    SysUser currentUser();
}
