package com.proyectohoussay.odonto.teletriaje.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record ReservarTeletriajeRequest(
        @NotNull(message = "El ID del odontólogo es obligatorio")
        Long odontologoId,

        @NotNull(message = "La fecha del turno es obligatoria")
        LocalDate fecha,

        @NotNull(message = "La hora del turno es obligatoria")
        LocalTime hora,

        String motivo,

        @NotNull(message = "El consentimiento informado es obligatorio para la reserva")
        Boolean consentimientoInformado,

        Boolean requiereVideollamada
) {}
