package com.proyectohoussay.odonto.teletriaje.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ReservarTeletriajeCompletoRequest(
        @Valid @NotNull SolicitudTeletriajeRequest solicitud,
        @Valid @NotNull ReservarTeletriajeRequest reserva
) {}
