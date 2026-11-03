package com.proyectohoussay.odonto.dto;

import com.proyectohoussay.odonto.model.TipoMovimientoFinanciero;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoFinancieroRequest(
        @NotNull TipoMovimientoFinanciero tipo,
        @NotNull @PastOrPresent(message = "La fecha del movimiento no puede ser futura") LocalDate fecha,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal importe,
        @NotBlank @Size(max = 160) String concepto,
        @Size(max = 40) String medioPago,
        @Positive Long turnoId) { }
