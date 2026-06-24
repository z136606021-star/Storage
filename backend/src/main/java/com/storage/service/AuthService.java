package com.storage.service;

import com.storage.dto.AuthSessionVO;
import com.storage.dto.AuthUserVO;
import com.storage.dto.LoginRequestDTO;
import com.storage.dto.RegisterRequestDTO;
import com.storage.entity.SysRole;
import com.storage.entity.SysUser;
import com.storage.exception.BusinessException;
import com.storage.mapper.SysMenuMapper;
import com.storage.mapper.SysRoleMapper;
import com.storage.mapper.SysUserMapper;
import com.storage.shiro.UserRealm;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserRealm userRealm;

    public AuthSessionVO login(LoginRequestDTO request) {
        SysUser user = sysUserMapper.selectByUsername(request.getUsername());
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号或密码错误");
        }
        if (!userRealm.matchesPassword(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("账号或密码错误");
        }

        UsernamePasswordToken token = new UsernamePasswordToken(
                request.getUsername(),
                request.getPassword(),
                false
        );
        Subject subject = SecurityUtils.getSubject();
        try {
            subject.login(token);
        } catch (AuthenticationException ex) {
            throw new BusinessException("账号或密码错误");
        }
        return buildSession(user);
    }

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
        user.setStatus(1);
        sysUserMapper.insert(user);
        sysMenuMapper.insertUserRole(user.getId(), userRole.getId());

        UsernamePasswordToken token = new UsernamePasswordToken(
                request.getUsername(),
                request.getPassword(),
                false
        );
        Subject subject = SecurityUtils.getSubject();
        try {
            subject.login(token);
        } catch (AuthenticationException ex) {
            throw new BusinessException("注册成功但自动登录失败，请手动登录");
        }
        return buildSession(user);
    }

    public void logout() {
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated()) {
            subject.logout();
        }
    }

    public AuthSessionVO currentSession() {
        SysUser user = currentUser();
        if (user == null) {
            throw new BusinessException("未登录");
        }
        return buildSession(user);
    }

    public SysUser currentUser() {
        Subject subject = SecurityUtils.getSubject();
        Object principal = subject.getPrincipal();
        if (principal instanceof SysUser user) {
            return user;
        }
        return null;
    }

    private AuthSessionVO buildSession(SysUser user) {
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
                .build();
    }
}
