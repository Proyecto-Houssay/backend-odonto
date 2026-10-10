package com.proyectohoussay.odonto.dto;

import com.proyectohoussay.odonto.payment.Pago;
import com.proyectohoussay.odonto.payment.PagoEstado;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PagoResponse(Long id, BigDecimal monto, LocalDate fecha, PagoEstado estado, String metodo) {

    public static PagoResponse de(Pago pago) {
        return new PagoResponse(pago.getId(), pago.getMonto(), pago.getFecha(), pago.getEstado(), pago.getMetodo());
    }
}
