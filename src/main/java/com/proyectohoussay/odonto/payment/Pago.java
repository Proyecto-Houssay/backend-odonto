package com.proyectohoussay.odonto.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "pagos", indexes = @Index(name = "idx_pagos_estado_fecha", columnList = "estado, fecha"))
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El monto es obligatorio.")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero.")
    @Digits(integer = 10, fraction = 2, message = "El monto admite hasta 10 dígitos enteros y 2 decimales.")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @NotNull(message = "La fecha es obligatoria.")
    @Column(nullable = false)
    private LocalDate fecha;

    @NotNull(message = "El estado del pago es obligatorio.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PagoEstado estado;

    @NotBlank(message = "El método de pago es obligatorio.")
    @Size(max = 40, message = "El método de pago no puede superar los 40 caracteres.")
    @Column(nullable = false, length = 40)
    private String metodo;

    protected Pago() {
    }

    public Pago(BigDecimal monto, LocalDate fecha, PagoEstado estado, String metodo) {
        this.monto = monto;
        this.fecha = fecha;
        this.estado = estado;
        this.metodo = metodo;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public PagoEstado getEstado() {
        return estado;
    }

    public String getMetodo() {
        return metodo;
    }
}
