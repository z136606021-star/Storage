package com.storage.system.auth.service;

import com.storage.common.exception.BusinessException;
import com.storage.system.user.entity.SysUser;
import com.storage.system.auth.config.PasswordResetProperties;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
public interface PasswordResetMailService {
    void sendResetLink(SysUser user, String rawToken);
}
