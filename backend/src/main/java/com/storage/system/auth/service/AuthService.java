package com.storage.system.auth.service;

import com.storage.system.user.entity.SysUser;
import com.storage.system.auth.dto.AuthSessionVO;
import com.storage.system.auth.dto.ChangePasswordByCurrentPasswordDTO;
import com.storage.system.auth.dto.ChangePasswordByVerificationCodeDTO;
import com.storage.system.auth.dto.ForgotPasswordDTO;
import com.storage.system.auth.dto.ForgotPasswordResetDTO;
import com.storage.system.auth.dto.LoginRequestDTO;
import com.storage.system.auth.dto.RegisterRequestDTO;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    AuthSessionVO login(LoginRequestDTO request);

    AuthSessionVO register(RegisterRequestDTO request);

    void logout(HttpServletRequest request);

    void forgotPassword(ForgotPasswordDTO request);

    void resetPassword(ForgotPasswordResetDTO request);

    void changePasswordByCurrentPassword(ChangePasswordByCurrentPasswordDTO request);

    void sendPasswordVerificationCode();

    void changePasswordByVerificationCode(ChangePasswordByVerificationCodeDTO request);

    AuthSessionVO currentSession();

    SysUser currentUser();
}
