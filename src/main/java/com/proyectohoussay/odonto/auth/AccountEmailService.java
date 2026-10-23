package com.proyectohoussay.odonto.auth;

public interface AccountEmailService {
    boolean isConfigured();

    boolean sendRegistrationConfirmation(String recipient, String displayName);

    boolean sendPasswordReset(String recipient, String displayName, String resetUrl);
}
