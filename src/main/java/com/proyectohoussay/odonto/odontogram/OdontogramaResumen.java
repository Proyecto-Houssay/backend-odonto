package com.proyectohoussay.odonto.odontogram;

import java.time.Instant;

public record OdontogramaResumen(Long id, Long pacienteId, Instant creadoEn, Long creadoPorId, String moneda,
        String versionCatalogo, int totalPresupuesto, String versionConsentimiento) {}
