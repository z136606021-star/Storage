package com.storage.system.auth.service;

import com.storage.common.exception.BusinessException;
import com.storage.system.auth.config.PasswordVerificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RegistrationVerificationMailServiceImpl implements RegistrationVerificationMailService {

    private final JavaMailSender mailSender;
    private final PasswordVerificationProperties properties;

    @Value("${storage.mail.from:}")
    private String mailFrom;

    @Override
    public void sendVerificationCode(String email, String rawCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(mailFrom)) {
            message.setFrom(mailFrom.trim());
        }
        message.setTo(email);
        message.setSubject("仓库管理系统注册验证码");
        message.setText("""
                您好：

                您正在注册仓库管理系统账号。验证码为：%s

                验证码 %d 分钟内有效，请勿泄露给他人。如非本人操作，请忽略本邮件。
                """.formatted(rawCode, properties.getTtlMinutes()));
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new BusinessException("验证码邮件发送失败，请稍后再试");
        }
    }
}
