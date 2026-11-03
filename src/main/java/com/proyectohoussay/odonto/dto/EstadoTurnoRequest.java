package com.proyectohoussay.odonto.dto;

import com.proyectohoussay.odonto.model.EstadoTurno;
import jakarta.validation.constraints.NotNull;

public record EstadoTurnoRequest(@NotNull EstadoTurno estado) { }
