package com.storage.system.auth.service;

import com.storage.common.exception.BusinessException;
import com.storage.system.user.entity.SysUser;
import com.storage.system.auth.config.PasswordResetProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class PasswordResetMailServiceImpl implements PasswordResetMailService {

    private final JavaMailSender mailSender;
    private final PasswordResetProperties properties;

    @Value("${storage.mail.from:}")
    private String mailFrom;

    public void sendResetLink(SysUser user, String rawToken) {
        if (!StringUtils.hasText(user.getEmail())) {
            throw new BusinessException("邮箱不正确或未绑定账号");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(mailFrom)) {
            message.setFrom(mailFrom.trim());
        }
        message.setTo(user.getEmail().trim());
        message.setSubject("仓库管理系统密码重置");
        message.setText(buildMessage(user, rawToken));
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new BusinessException("重置邮件发送失败，请稍后再试");
        }
    }

    private String buildMessage(SysUser user, String rawToken) {
        String displayName = StringUtils.hasText(user.getDisplayName()) ? user.getDisplayName() : user.getUsername();
        return """
                %s，您好：

                您正在申请重置仓库管理系统登录密码。请在 %d 分钟内打开以下链接完成重置：

                %s

                如果不是您本人操作，请忽略本邮件。
                """.formatted(displayName, properties.getTokenTtlMinutes(), buildResetUrl(rawToken));
    }

    private String buildResetUrl(String rawToken) {
        String baseUrl = properties.getPublicBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            baseUrl = "http://localhost:5173";
        }
        baseUrl = baseUrl.trim().replaceAll("/+$", "");
        String token = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        return baseUrl + "/login?tab=reset&token=" + token;
    }
}
