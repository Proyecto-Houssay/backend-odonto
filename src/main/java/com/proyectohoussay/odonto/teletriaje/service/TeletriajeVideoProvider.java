package com.proyectohoussay.odonto.teletriaje.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class TeletriajeVideoProvider {

    private final boolean enabled;
    private final String baseUrl;
    private final Duration linkDuration;

    public TeletriajeVideoProvider(
            @Value("${teletriaje.video-provider.enabled:false}") boolean enabled,
            @Value("${teletriaje.video-provider.base-url:https://telemedicina.novadent.local/sala}") String baseUrl,
            @Value("${teletriaje.video-provider.duracion-minutos:60}") long duracionMinutos) {
        this.enabled = enabled;
        this.baseUrl = baseUrl;
        this.linkDuration = Duration.ofMinutes(duracionMinutos);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public record GeneracionEnlace(String url, LocalDateTime expiraEn) {}

    public Optional<GeneracionEnlace> generarEnlaceTemporal(Long citaId, Long pacienteId) {
        if (!enabled) {
            // Sin proveedor configurado, mantener explícitamente fuera de P0 sin llamada funcional
            return Optional.empty();
        }

        String roomToken = UUID.randomUUID().toString();
        LocalDateTime expiraEn = LocalDateTime.now().plus(linkDuration);
        String url = String.format("%s/%s?cita=%d&token=%s&exp=%s",
                baseUrl, roomToken, citaId, roomToken, expiraEn);

        return Optional.of(new GeneracionEnlace(url, expiraEn));
    }
}
