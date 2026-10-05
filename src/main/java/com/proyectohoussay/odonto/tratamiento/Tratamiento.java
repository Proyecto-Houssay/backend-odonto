package com.proyectohoussay.odonto.tratamiento;

import com.proyectohoussay.odonto.diagnostico.Diagnostico;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Entity
@Table(name = "tratamientos")
public class Tratamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max = 1000)
    @Column(nullable = false, length = 1000)
    private String descripcion;

    @NotNull
    @Column(nullable = false)
    private LocalDate fecha;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTratamiento estado;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diagnostico_id", nullable = false)
    private Diagnostico diagnostico;

    public Tratamiento() {
    }

    public Tratamiento(
            String descripcion,
            LocalDate fecha,
            EstadoTratamiento estado,
            Diagnostico diagnostico
    ) {
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.estado = estado;
        this.diagnostico = diagnostico;
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

    public void setDiagnostico(Diagnostico diagnostico) {
        this.diagnostico = diagnostico;
    }
}
