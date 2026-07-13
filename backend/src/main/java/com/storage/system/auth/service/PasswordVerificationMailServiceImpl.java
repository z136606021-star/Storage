package com.storage.system.auth.service;

import com.storage.common.exception.BusinessException;
import com.storage.system.auth.config.PasswordVerificationProperties;
import com.storage.system.user.entity.SysUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PasswordVerificationMailServiceImpl implements PasswordVerificationMailService {

    private final JavaMailSender mailSender;
    private final PasswordVerificationProperties properties;

    @Value("${storage.mail.from:}")
    private String mailFrom;

    @Override
    public void sendVerificationCode(SysUser user, String rawCode) {
        if (!StringUtils.hasText(user.getEmail())) {
            throw new BusinessException("当前账号未绑定邮箱，无法发送验证码");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(mailFrom)) {
            message.setFrom(mailFrom.trim());
        }
        message.setTo(user.getEmail().trim());
        message.setSubject("仓库管理系统修改密码验证码");
        message.setText(buildMessage(user, rawCode));
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new BusinessException("验证码邮件发送失败，请稍后再试");
        }
    }

    private String buildMessage(SysUser user, String rawCode) {
        String displayName = StringUtils.hasText(user.getDisplayName()) ? user.getDisplayName() : user.getUsername();
        return """
                %s，您好：

                您正在修改仓库管理系统登录密码。验证码为：%s

                验证码 %d 分钟内有效，请勿泄露给他人。如非本人操作，请忽略本邮件。
                """.formatted(displayName, rawCode, properties.getTtlMinutes());
    }
}
