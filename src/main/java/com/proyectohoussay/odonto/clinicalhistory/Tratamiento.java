package com.proyectohoussay.odonto.clinicalhistory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Treatment record associated with a diagnosis.
 * Tracks treatment lifecycle through {@link EstadoTratamiento}.
 */
@Entity
@Table(name = "tratamientos")
public class Tratamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "La descripción del tratamiento es obligatoria")
    @Column(nullable = false, length = 500)
    private String descripcion;

    @NotNull(message = "La fecha del tratamiento es obligatoria")
    @Column(nullable = false)
    private LocalDate fecha;

    @NotNull(message = "El estado del tratamiento es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTratamiento estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diagnostico_id", nullable = false)
    private Diagnostico diagnostico;

    protected Tratamiento() {
        // Required by JPA
    }

    public Tratamiento(String descripcion, LocalDate fecha, EstadoTratamiento estado) {
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.estado = estado;
    }

    public Long getId() {
        return id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public EstadoTratamiento getEstado() {
        return estado;
    }

    public void setEstado(EstadoTratamiento estado) {
        this.estado = estado;
    }

    public Diagnostico getDiagnostico() {
        return diagnostico;
    }

    void setDiagnostico(Diagnostico diagnostico) {
        this.diagnostico = diagnostico;
    }
}
