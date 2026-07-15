package com.storage.system.auth.service;

import com.storage.common.exception.BusinessException;
import com.storage.common.mapper.StringMapping;
import com.storage.common.util.IdentityTextValidation;
import com.storage.system.auth.config.PasswordVerificationProperties;
import com.storage.system.auth.dto.AuthSessionVO;
import com.storage.system.auth.dto.AuthUserVO;
import com.storage.system.auth.dto.ChangePasswordByCurrentPasswordDTO;
import com.storage.system.auth.dto.ChangePasswordByVerificationCodeDTO;
import com.storage.system.auth.dto.ForgotPasswordDTO;
import com.storage.system.auth.dto.ForgotPasswordResetDTO;
import com.storage.system.auth.dto.JwtClaims;
import com.storage.system.auth.dto.LoginRequestDTO;
import com.storage.system.auth.dto.RegisterRequestDTO;
import com.storage.system.auth.dto.SendRegistrationVerificationCodeDTO;
import com.storage.system.auth.dto.UpdateCurrentUserPhoneDTO;
import com.storage.system.auth.entity.EmailVerificationCode;
import com.storage.system.auth.entity.PasswordResetToken;
import com.storage.system.auth.entity.RegistrationVerificationCode;
import com.storage.system.auth.mapper.EmailVerificationCodeMapper;
import com.storage.system.auth.mapper.PasswordResetTokenMapper;
import com.storage.system.auth.mapper.RegistrationVerificationCodeMapper;
import com.storage.system.auth.shiro.UserRealm;
import com.storage.system.menu.mapper.SysMenuMapper;
import com.storage.system.role.entity.SysRole;
import com.storage.system.role.mapper.SysRoleMapper;
import com.storage.system.user.entity.SysUser;
import com.storage.system.user.mapper.SysUserMapper;
import com.storage.system.auth.config.PasswordResetProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String FORGOT_PASSWORD_IDENTITY_ERROR = "邮箱不正确或未绑定账号";
    private static final String RESET_PASSWORD_TOKEN_ERROR = "重置链接无效或已过期";
    private static final String LOGIN_IDENTITY_ERROR = "账号或密码错误";
    private static final String VERIFICATION_CODE_ERROR = "验证码无效或已过期";
    private static final String CHANGE_PASSWORD_PURPOSE = "CHANGE_PASSWORD";
    private static final String REGISTER_VERIFICATION_CODE_ERROR = "验证码无效或已过期";

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;
    private final PasswordResetTokenMapper passwordResetTokenMapper;
    private final EmailVerificationCodeMapper emailVerificationCodeMapper;
    private final RegistrationVerificationCodeMapper registrationVerificationCodeMapper;
    private final PasswordResetMailService passwordResetMailService;
    private final PasswordVerificationMailService passwordVerificationMailService;
    private final RegistrationVerificationMailService registrationVerificationMailService;
    private final PasswordResetProperties passwordResetProperties;
    private final PasswordVerificationProperties passwordVerificationProperties;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserRealm userRealm;
    private final JwtService jwtService;
    private final JwtRevocationService jwtRevocationService;
    private final LoginFailureLimiter loginFailureLimiter;
    private final ForgotPasswordFailureLimiter forgotPasswordFailureLimiter;
    private final PasswordVerificationFailureLimiter passwordVerificationFailureLimiter;
    private final RegistrationVerificationFailureLimiter registrationVerificationFailureLimiter;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public AuthSessionVO login(LoginRequestDTO request) {
        String identity = normalizeLoginIdentity(request.getUsername());
        loginFailureLimiter.assertAllowed(identity);

        SysUser user = resolveLoginUser(identity);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            loginFailureLimiter.recordFailure(identity);
            throw new BusinessException(LOGIN_IDENTITY_ERROR);
        }
        if (!userRealm.matchesPassword(request.getPassword(), user.getPasswordHash())) {
            loginFailureLimiter.recordFailure(identity);
            throw new BusinessException(LOGIN_IDENTITY_ERROR);
        }

        loginFailureLimiter.reset(identity);
        return buildSession(user, jwtService.issueToken(user));
    }

    @Override
    @Transactional
    public void sendRegistrationVerificationCode(SendRegistrationVerificationCodeDTO request) {
        String email = requireNormalizedEmail(request.getEmail());
        if (sysUserMapper.selectByEmail(email) != null) {
            throw new BusinessException("该邮箱已被注册");
        }

        LocalDateTime now = LocalDateTime.now();
        RegistrationVerificationCode latest = registrationVerificationCodeMapper.selectLatestByEmail(email);
        if (latest != null) {
            LocalDateTime cooldownStartedAt = resolveVerificationCooldownStartedAt(
                    latest.getCreatedAt(),
                    latest.getExpiresAt()
            );
            if (cooldownStartedAt != null
                    && cooldownStartedAt.plusSeconds(passwordVerificationProperties.getSendCooldownSeconds()).isAfter(now)) {
                throw new BusinessException("发送过于频繁，请稍后再试");
            }
        }

        String rawCode = generateVerificationCode();
        RegistrationVerificationCode code = new RegistrationVerificationCode();
        code.setEmail(email);
        code.setCodeHash(hashToken(rawCode));
        code.setExpiresAt(now.plusMinutes(passwordVerificationProperties.getTtlMinutes()));
        code.setCreatedAt(now);
        registrationVerificationCodeMapper.insert(code);

        registrationVerificationMailService.sendVerificationCode(email, rawCode);
    }

    @Override
    @Transactional
    public AuthSessionVO register(RegisterRequestDTO request) {
        IdentityTextValidation.requireNoWhitespace(request.getUsername(), "账号");
        IdentityTextValidation.requireNoWhitespace(request.getDisplayName(), "显示名称");

        String username = request.getUsername().trim();
        String displayName = request.getDisplayName().trim();
        String email = requireNormalizedEmail(request.getEmail());

        if (sysUserMapper.selectByUsername(username) != null) {
            throw new BusinessException("账号已存在");
        }
        assertEmailAvailable(email, null);

        registrationVerificationFailureLimiter.assertAllowedVerify(email);
        LocalDateTime now = LocalDateTime.now();
        RegistrationVerificationCode code = registrationVerificationCodeMapper.selectUnusedForUpdate(
                email,
                hashToken(request.getVerificationCode().trim())
        );
        if (code == null || code.getExpiresAt() == null || !code.getExpiresAt().isAfter(now)) {
            registrationVerificationFailureLimiter.recordVerifyFailure(email);
            throw new BusinessException(REGISTER_VERIFICATION_CODE_ERROR);
        }

        SysRole userRole = sysRoleMapper.selectByCode("USER");
        if (userRole == null) {
            throw new BusinessException("系统未配置 USER 角色");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setTokenVersion(0);
        user.setEmail(email);
        user.setStatus(1);
        sysUserMapper.insert(user);
        sysMenuMapper.insertUserRole(user.getId(), userRole.getId());

        code.setUsedAt(now);
        registrationVerificationCodeMapper.updateById(code);
        registrationVerificationFailureLimiter.resetVerifyFailures(email);

        return buildSession(user, jwtService.issueToken(user));
    }

    @Override
    public void logout(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return;
        }
        String token = authorization.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            return;
        }
        try {
            JwtClaims claims = jwtService.parseClaims(token);
            jwtRevocationService.revoke(claims);
        } catch (RuntimeException ignored) {
            // Invalid tokens are already rejected at authentication time.
        }
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordDTO request) {
        String email = StringMapping.trimToNullLowercase(request.getEmail());
        forgotPasswordFailureLimiter.assertAllowedForgotPassword(email);

        SysUser user = sysUserMapper.selectByEmail(email);
        if (user == null || user.getStatus() == null || user.getStatus() != 1 || !StringUtils.hasText(user.getEmail())) {
            forgotPasswordFailureLimiter.recordForgotPasswordFailure(email);
            throw new BusinessException(FORGOT_PASSWORD_IDENTITY_ERROR);
        }

        String rawToken = generateResetToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(user.getId());
        token.setTokenHash(hashToken(rawToken));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(passwordResetProperties.getTokenTtlMinutes()));
        passwordResetTokenMapper.insert(token);

        passwordResetMailService.sendResetLink(user, rawToken);
        forgotPasswordFailureLimiter.resetForgotPassword(email);
    }

    @Override
    @Transactional
    public void resetPassword(ForgotPasswordResetDTO request) {
        PasswordResetToken token = passwordResetTokenMapper.selectByTokenHashForUpdate(hashToken(request.getToken().trim()));
        if (token == null || token.getUsedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(RESET_PASSWORD_TOKEN_ERROR);
        }
        SysUser user = sysUserMapper.selectById(token.getUserId());
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(RESET_PASSWORD_TOKEN_ERROR);
        }
        updatePasswordAndInvalidateSessions(user, request.getNewPassword());

        token.setUsedAt(LocalDateTime.now());
        passwordResetTokenMapper.updateById(token);
        if (StringUtils.hasText(user.getEmail())) {
            forgotPasswordFailureLimiter.resetForgotPassword(StringMapping.trimToNullLowercase(user.getEmail()));
        }
    }

    @Override
    @Transactional
    public void changePasswordByCurrentPassword(ChangePasswordByCurrentPasswordDTO request) {
        SysUser user = requireCurrentUser();
        validatePasswordChange(request.getNewPassword(), request.getConfirmPassword());
        if (!userRealm.matchesPassword(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException("原密码不正确");
        }
        if (userRealm.matchesPassword(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        updatePasswordAndInvalidateSessions(user, request.getNewPassword());
    }

    @Override
    @Transactional
    public void sendPasswordVerificationCode() {
        SysUser user = requireCurrentUser();
        if (!StringUtils.hasText(user.getEmail())) {
            throw new BusinessException("当前账号未绑定邮箱，无法发送验证码");
        }

        LocalDateTime now = LocalDateTime.now();
        EmailVerificationCode latest = emailVerificationCodeMapper.selectLatestByUserAndPurpose(user.getId(), CHANGE_PASSWORD_PURPOSE);
        if (latest != null) {
            LocalDateTime cooldownStartedAt = resolveVerificationCooldownStartedAt(
                    latest.getCreatedAt(),
                    latest.getExpiresAt()
            );
            if (cooldownStartedAt != null
                    && cooldownStartedAt.plusSeconds(passwordVerificationProperties.getSendCooldownSeconds()).isAfter(now)) {
                throw new BusinessException("发送过于频繁，请稍后再试");
            }
        }

        String rawCode = generateVerificationCode();
        EmailVerificationCode code = new EmailVerificationCode();
        code.setUserId(user.getId());
        code.setPurpose(CHANGE_PASSWORD_PURPOSE);
        code.setCodeHash(hashToken(rawCode));
        code.setExpiresAt(now.plusMinutes(passwordVerificationProperties.getTtlMinutes()));
        code.setCreatedAt(now);
        emailVerificationCodeMapper.insert(code);

        passwordVerificationMailService.sendVerificationCode(user, rawCode);
    }

    @Override
    @Transactional
    public void changePasswordByVerificationCode(ChangePasswordByVerificationCodeDTO request) {
        SysUser user = requireCurrentUser();
        if (!StringUtils.hasText(user.getEmail())) {
            throw new BusinessException("当前账号未绑定邮箱，无法使用验证码修改密码");
        }
        validatePasswordChange(request.getNewPassword(), request.getConfirmPassword());
        passwordVerificationFailureLimiter.assertAllowedVerify(user.getId());

        LocalDateTime now = LocalDateTime.now();
        EmailVerificationCode code = emailVerificationCodeMapper.selectUnusedForUpdate(
                user.getId(),
                CHANGE_PASSWORD_PURPOSE,
                hashToken(request.getVerificationCode().trim())
        );
        if (code == null || code.getExpiresAt() == null || !code.getExpiresAt().isAfter(now)) {
            passwordVerificationFailureLimiter.recordVerifyFailure(user.getId());
            throw new BusinessException(VERIFICATION_CODE_ERROR);
        }
        if (userRealm.matchesPassword(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException("新密码不能与原密码相同");
        }

        updatePasswordAndInvalidateSessions(user, request.getNewPassword());
        code.setUsedAt(now);
        emailVerificationCodeMapper.updateById(code);
        passwordVerificationFailureLimiter.resetVerifyFailures(user.getId());
    }

    @Override
    public AuthSessionVO currentSession() {
        SysUser user = currentUser();
        if (user == null) {
            throw new BusinessException("未登录");
        }
        return buildSession(user, null);
    }

    @Override
    public AuthUserVO updateCurrentUserPhone(UpdateCurrentUserPhoneDTO request) {
        SysUser user = requireCurrentUser();
        user.setPhone(StringMapping.trimToNull(request.getPhone()));
        sysUserMapper.updateById(user);
        SysUser updated = sysUserMapper.selectById(user.getId());
        return AuthUserVO.builder()
                .id(updated.getId())
                .username(updated.getUsername())
                .displayName(updated.getDisplayName())
                .email(updated.getEmail())
                .phone(updated.getPhone())
                .build();
    }

    @Override
    public SysUser currentUser() {
        Subject subject = SecurityUtils.getSubject();
        Object principal = subject.getPrincipal();
        if (principal instanceof SysUser user) {
            return user;
        }
        return null;
    }

    private SysUser requireCurrentUser() {
        SysUser user = currentUser();
        if (user == null) {
            throw new BusinessException("未登录");
        }
        return user;
    }

    private void validatePasswordChange(String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException("两次输入的新密码不一致");
        }
    }

    private void updatePasswordAndInvalidateSessions(SysUser user, String rawPassword) {
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        int nextVersion = user.getTokenVersion() == null ? 1 : user.getTokenVersion() + 1;
        user.setTokenVersion(nextVersion);
        sysUserMapper.updateById(user);
    }

    private AuthSessionVO buildSession(SysUser user, String accessToken) {
        List<String> roles = sysUserMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissions = sysUserMapper.selectPermissionsByUserId(user.getId());
        return AuthSessionVO.builder()
                .user(AuthUserVO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .displayName(user.getDisplayName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .build())
                .roles(roles)
                .permissions(permissions)
                .accessToken(accessToken)
                .build();
    }

    private String generateResetToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateVerificationCode() {
        int bound = (int) Math.pow(10, passwordVerificationProperties.getCodeLength());
        int floor = bound / 10;
        int code = secureRandom.nextInt(bound - floor) + floor;
        return String.valueOf(code);
    }

    private LocalDateTime resolveVerificationCooldownStartedAt(
            LocalDateTime databaseCreatedAt,
            LocalDateTime applicationExpiresAt
    ) {
        LocalDateTime inferredCreatedAt = applicationExpiresAt == null
                ? null
                : applicationExpiresAt.minusMinutes(passwordVerificationProperties.getTtlMinutes());
        if (databaseCreatedAt == null) {
            return inferredCreatedAt;
        }
        if (inferredCreatedAt == null) {
            return databaseCreatedAt;
        }
        return databaseCreatedAt.isBefore(inferredCreatedAt) ? databaseCreatedAt : inferredCreatedAt;
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", ex);
        }
    }

    private String normalizeLoginIdentity(String rawIdentity) {
        String identity = rawIdentity.trim();
        if (identity.contains("@")) {
            return StringMapping.trimToNullLowercase(identity);
        }
        return identity;
    }

    private SysUser resolveLoginUser(String identity) {
        if (identity.contains("@")) {
            return sysUserMapper.selectByEmail(identity);
        }
        return sysUserMapper.selectByUsername(identity);
    }

    private String requireNormalizedEmail(String rawEmail) {
        String email = StringMapping.trimToNullLowercase(rawEmail);
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new BusinessException("邮箱格式不正确");
        }
        return email;
    }

    private void assertEmailAvailable(String email, Long excludeUserId) {
        if (email == null) {
            return;
        }
        SysUser existing = sysUserMapper.selectByEmail(email);
        if (existing != null && (excludeUserId == null || !existing.getId().equals(excludeUserId))) {
            throw new BusinessException("邮箱已被其他用户使用");
        }
    }
}
