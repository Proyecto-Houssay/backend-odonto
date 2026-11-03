package com.proyectohoussay.odonto.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpAccountEmailService implements AccountEmailService {
    private static final Logger logger = LoggerFactory.getLogger(SmtpAccountEmailService.class);

    private final JavaMailSender mailSender;
    private final boolean mailEnabled;
    private final String smtpHost;
    private final String senderAddress;

    public SmtpAccountEmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.enabled:false}") boolean mailEnabled,
            @Value("${spring.mail.host:}") String smtpHost,
            @Value("${app.mail.from:}") String senderAddress) {
        this.mailSender = mailSender;
        this.mailEnabled = mailEnabled;
        this.smtpHost = smtpHost;
        this.senderAddress = senderAddress;
    }

    @Override
    public boolean isConfigured() {
        return mailEnabled && !smtpHost.isBlank() && !senderAddress.isBlank();
    }

    @Override
    public boolean sendRegistrationConfirmation(String recipient, String displayName) {
        return send(recipient, "Tu cuenta de Novadent fue creada",
                "Hola " + displayName + ",\n\nTu cuenta del sistema académico Novadent fue creada correctamente. Ya podés ingresar con tu usuario y contraseña.\n\nSi no solicitaste esta cuenta, avisá al administrador del proyecto.");
    }

    @Override
    public boolean sendPasswordReset(String recipient, String displayName, String resetUrl) {
        return send(recipient, "Restablecer contraseña de Novadent",
                "Hola " + displayName + ",\n\nRecibimos una solicitud para restablecer la contraseña de tu cuenta. Abrí este enlace dentro de los próximos 30 minutos y elegí una nueva contraseña:\n\n"
                        + resetUrl + "\n\nSi no solicitaste este cambio, ignorá este mensaje. Tu contraseña no cambiará.");
    }

    private boolean send(String recipient, String subject, String body) {
        if (!isConfigured()) {
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderAddress);
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            return true;
        } catch (MailException exception) {
            logger.warn("Account email delivery failed; SMTP configuration or availability should be checked.");
            return false;
        }
    }
}
