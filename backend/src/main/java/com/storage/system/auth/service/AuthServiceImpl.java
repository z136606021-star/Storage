package com.storage.system.auth.service;

import com.storage.common.exception.BusinessException;
import com.storage.system.menu.mapper.SysMenuMapper;
import com.storage.system.role.entity.SysRole;
import com.storage.system.role.mapper.SysRoleMapper;
import com.storage.system.user.entity.SysUser;
import com.storage.system.user.mapper.SysUserMapper;
import com.storage.system.auth.config.PasswordResetProperties;
import com.storage.system.auth.dto.AuthSessionVO;
import com.storage.system.auth.dto.AuthUserVO;
import com.storage.system.auth.dto.ForgotPasswordDTO;
import com.storage.system.auth.dto.ForgotPasswordResetDTO;
import com.storage.system.auth.dto.LoginRequestDTO;
import com.storage.system.auth.dto.RegisterRequestDTO;
import com.storage.system.auth.entity.PasswordResetToken;
import com.storage.system.auth.mapper.PasswordResetTokenMapper;
import com.storage.system.auth.shiro.UserRealm;
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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String FORGOT_PASSWORD_IDENTITY_ERROR = "账号或邮箱不正确，或账号未绑定邮箱";
    private static final String RESET_PASSWORD_TOKEN_ERROR = "重置链接无效或已过期";
    private static final int FORGOT_PASSWORD_MAX_FAILURES = 5;
    private static final Duration FORGOT_PASSWORD_FAILURE_WINDOW = Duration.ofMinutes(15);
    private static final String LOGIN_IDENTITY_ERROR = "账号或密码错误";
    private static final int LOGIN_MAX_FAILURES = 5;
    private static final Duration LOGIN_FAILURE_WINDOW = Duration.ofMinutes(15);

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;
    private final PasswordResetTokenMapper passwordResetTokenMapper;
    private final PasswordResetMailService passwordResetMailService;
    private final PasswordResetProperties passwordResetProperties;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserRealm userRealm;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentMap<String, ForgotPasswordFailures> forgotPasswordFailures = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LoginFailures> loginFailures = new ConcurrentHashMap<>();

    @Override
    public AuthSessionVO login(LoginRequestDTO request) {
        String username = request.getUsername().trim();
        String failureKey = normalizeLoginKey(username);
        assertLoginAllowed(failureKey);

        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            recordLoginFailure(failureKey);
            throw new BusinessException(LOGIN_IDENTITY_ERROR);
        }
        if (!userRealm.matchesPassword(request.getPassword(), user.getPasswordHash())) {
            recordLoginFailure(failureKey);
            throw new BusinessException(LOGIN_IDENTITY_ERROR);
        }

        loginFailures.remove(failureKey);
        return buildSession(user, jwtService.issueToken(user));
    }

    @Override
    @Transactional
    public AuthSessionVO register(RegisterRequestDTO request) {
        if (sysUserMapper.selectByUsername(request.getUsername()) != null) {
            throw new BusinessException("账号已存在");
        }
        SysRole userRole = sysRoleMapper.selectByCode("USER");
        if (userRole == null) {
            throw new BusinessException("系统未配置 USER 角色");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setDisplayName(request.getDisplayName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        if (StringUtils.hasText(request.getEmail())) {
            String email = request.getEmail().trim();
            if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new BusinessException("邮箱格式不正确");
            }
            user.setEmail(email);
        }
        user.setStatus(1);
        sysUserMapper.insert(user);
        sysMenuMapper.insertUserRole(user.getId(), userRole.getId());

        return buildSession(user, jwtService.issueToken(user));
    }

    @Override
    public void logout() {
        // JWT is stateless; the frontend clears its token.
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordDTO request) {
        String username = request.getUsername().trim();
        String failureKey = normalizeForgotPasswordKey(username);
        assertForgotPasswordAllowed(failureKey);

        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            recordForgotPasswordFailure(failureKey);
            throw new BusinessException(FORGOT_PASSWORD_IDENTITY_ERROR);
        }
        if (!StringUtils.hasText(user.getEmail())) {
            recordForgotPasswordFailure(failureKey);
            throw new BusinessException(FORGOT_PASSWORD_IDENTITY_ERROR);
        }
        if (!user.getEmail().trim().equalsIgnoreCase(request.getEmail().trim())) {
            recordForgotPasswordFailure(failureKey);
            throw new BusinessException(FORGOT_PASSWORD_IDENTITY_ERROR);
        }

        String rawToken = generateResetToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(user.getId());
        token.setTokenHash(hashToken(rawToken));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(passwordResetProperties.getTokenTtlMinutes()));
        passwordResetTokenMapper.insert(token);

        passwordResetMailService.sendResetLink(user, rawToken);
        forgotPasswordFailures.remove(failureKey);
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
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        sysUserMapper.updateById(user);

        token.setUsedAt(LocalDateTime.now());
        passwordResetTokenMapper.updateById(token);
        forgotPasswordFailures.remove(normalizeForgotPasswordKey(user.getUsername()));
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
    public SysUser currentUser() {
        Subject subject = SecurityUtils.getSubject();
        Object principal = subject.getPrincipal();
        if (principal instanceof SysUser user) {
            return user;
        }
        return null;
    }

    private AuthSessionVO buildSession(SysUser user, String accessToken) {
        List<String> roles = sysUserMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissions = sysUserMapper.selectPermissionsByUserId(user.getId());
        return AuthSessionVO.builder()
                .user(AuthUserVO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .displayName(user.getDisplayName())
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

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", ex);
        }
    }

    private void assertForgotPasswordAllowed(String failureKey) {
        ForgotPasswordFailures failures = forgotPasswordFailures.get(failureKey);
        if (failures == null || failures.isExpired()) {
            return;
        }
        if (failures.count() >= FORGOT_PASSWORD_MAX_FAILURES) {
            throw new BusinessException("密码找回尝试次数过多，请 15 分钟后再试");
        }
    }

    private void recordForgotPasswordFailure(String failureKey) {
        forgotPasswordFailures.compute(failureKey, (key, failures) -> {
            if (failures == null || failures.isExpired()) {
                return new ForgotPasswordFailures(1, Instant.now());
            }
            return new ForgotPasswordFailures(failures.count() + 1, failures.firstFailedAt());
        });
    }

    private void assertLoginAllowed(String failureKey) {
        LoginFailures failures = loginFailures.get(failureKey);
        if (failures == null || failures.isExpired()) {
            return;
        }
        if (failures.count() >= LOGIN_MAX_FAILURES) {
            throw new BusinessException("登录失败次数过多，请 15 分钟后再试");
        }
    }

    private void recordLoginFailure(String failureKey) {
        loginFailures.compute(failureKey, (key, failures) -> {
            if (failures == null || failures.isExpired()) {
                return new LoginFailures(1, Instant.now());
            }
            return new LoginFailures(failures.count() + 1, failures.firstFailedAt());
        });
    }

    private String normalizeForgotPasswordKey(String username) {
        return username.toLowerCase(Locale.ROOT);
    }

    private String normalizeLoginKey(String username) {
        return username.toLowerCase(Locale.ROOT);
    }

    private record ForgotPasswordFailures(int count, Instant firstFailedAt) {
        boolean isExpired() {
            return firstFailedAt.plus(FORGOT_PASSWORD_FAILURE_WINDOW).isBefore(Instant.now());
        }
    }

    private record LoginFailures(int count, Instant firstFailedAt) {
        boolean isExpired() {
            return firstFailedAt.plus(LOGIN_FAILURE_WINDOW).isBefore(Instant.now());
        }
    }
}
