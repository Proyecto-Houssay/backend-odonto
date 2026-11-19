package com.proyectohoussay.odonto.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SmtpAccountEmailServiceTest {
    @Test
    void gmailIsNotConfiguredUntilSenderAndAppCredentialsArePresent() {
        JavaMailSender sender = mock(JavaMailSender.class);

        assertThat(new SmtpAccountEmailService(sender, true, "smtp.gmail.com",
                "sender@gmail.com", "", "").isConfigured()).isFalse();
        assertThat(new SmtpAccountEmailService(sender, true, "smtp.gmail.com",
                "sender@gmail.com", "sender@gmail.com", "app-password").isConfigured()).isTrue();
    }
}
