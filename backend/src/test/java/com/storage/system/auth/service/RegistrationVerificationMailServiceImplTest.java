package com.storage.system.auth.service;

import com.storage.common.exception.BusinessException;
import com.storage.system.auth.config.PasswordVerificationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RegistrationVerificationMailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    private RegistrationVerificationMailService service;

    @BeforeEach
    void setUp() {
        PasswordVerificationProperties properties = new PasswordVerificationProperties();
        properties.setTtlMinutes(10);
        service = new RegistrationVerificationMailServiceImpl(mailSender, properties);
        ReflectionTestUtils.setField(service, "mailFrom", "noreply@example.com");
    }

    @Test
    void sendVerificationCode_withBlankSpringMailUsername_stillSendsMail() {
        service.sendVerificationCode("newuser@example.com", "123456");

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(message.capture());
        assertThat(message.getValue().getFrom()).isEqualTo("noreply@example.com");
        assertThat(message.getValue().getTo()).containsExactly("newuser@example.com");
        assertThat(message.getValue().getSubject()).isEqualTo("仓库管理系统注册验证码");
        assertThat(message.getValue().getText()).contains("123456");
    }

    @Test
    void sendVerificationCode_whenMailSenderFails_throwsClearBusinessException() {
        doThrow(new MailSendException("relay unavailable")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> service.sendVerificationCode("newuser@example.com", "123456"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("验证码邮件发送失败，请检查邮件配置或稍后再试");
    }
}
