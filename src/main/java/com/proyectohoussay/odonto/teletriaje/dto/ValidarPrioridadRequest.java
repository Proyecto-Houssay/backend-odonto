package com.proyectohoussay.odonto.teletriaje.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidarPrioridadRequest(
        @NotBlank(message = "La nueva prioridad es obligatoria (ROJO, AMARILLO, VERDE)")
        String nuevaPrioridad,

        String observacionesClinicas
) {}
