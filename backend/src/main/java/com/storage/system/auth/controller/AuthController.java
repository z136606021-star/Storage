package com.storage.system.auth.controller;

import com.storage.common.annotation.IgnoreAuth;
import com.storage.system.auth.dto.AuthSessionVO;
import com.storage.system.auth.dto.AuthUserVO;
import com.storage.system.auth.dto.ChangePasswordByCurrentPasswordDTO;
import com.storage.system.auth.dto.ChangePasswordByVerificationCodeDTO;
import com.storage.system.auth.dto.ForgotPasswordDTO;
import com.storage.system.auth.dto.ForgotPasswordResetDTO;
import com.storage.system.auth.dto.LoginRequestDTO;
import com.storage.system.auth.dto.RegisterRequestDTO;
import com.storage.system.auth.dto.SendRegistrationVerificationCodeDTO;
import com.storage.system.auth.dto.UpdateCurrentUserPhoneDTO;
import com.storage.system.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthSessionVO login(@Valid @RequestBody LoginRequestDTO request) {
        return authService.login(request);
    }

    @IgnoreAuth
    @PostMapping("/register/verification-code")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendRegistrationVerificationCode(@Valid @RequestBody SendRegistrationVerificationCodeDTO request) {
        authService.sendRegistrationVerificationCode(request);
    }

    @PostMapping("/register")
    public AuthSessionVO register(@Valid @RequestBody RegisterRequestDTO request) {
        return authService.register(request);
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordDTO request) {
        authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ForgotPasswordResetDTO request) {
        authService.resetPassword(request);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePasswordByCurrentPassword(@Valid @RequestBody ChangePasswordByCurrentPasswordDTO request) {
        authService.changePasswordByCurrentPassword(request);
    }

    @PostMapping("/password/verification-code")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendPasswordVerificationCode() {
        authService.sendPasswordVerificationCode();
    }

    @PutMapping("/password/by-verification-code")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePasswordByVerificationCode(@Valid @RequestBody ChangePasswordByVerificationCodeDTO request) {
        authService.changePasswordByVerificationCode(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        authService.logout(request);
    }

    @GetMapping("/me")
    public AuthSessionVO me() {
        return authService.currentSession();
    }

    @PutMapping("/me/phone")
    public AuthUserVO updateMyPhone(@Valid @RequestBody UpdateCurrentUserPhoneDTO request) {
        return authService.updateCurrentUserPhone(request);
    }
}
