package com.proyectohoussay.odonto.dto;

import com.proyectohoussay.odonto.payment.PagoEstado;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PagoRequest(
        @NotNull(message = "El monto es obligatorio.")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero.")
        @Digits(integer = 10, fraction = 2, message = "El monto admite hasta 10 dígitos enteros y 2 decimales.")
        BigDecimal monto,
        @NotNull(message = "La fecha es obligatoria.")
        LocalDate fecha,
        @NotNull(message = "El estado del pago es obligatorio.")
        PagoEstado estado,
        @NotBlank(message = "El método de pago es obligatorio.")
        @Size(max = 40, message = "El método de pago no puede superar los 40 caracteres.")
        String metodo) {
}
