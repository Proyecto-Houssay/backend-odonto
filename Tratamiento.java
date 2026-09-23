package com.houssay.odonto.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "tratamientos")
public class Tratamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTratamiento estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diagnostico_id", nullable = false)
    private Diagnostico diagnostico;

    protected Tratamiento() {
        // requerido por JPA
    }

    public Tratamiento(String descripcion, LocalDate fecha,
                       EstadoTratamiento estado, Diagnostico diagnostico) {
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.estado = estado;
        this.diagnostico = diagnostico;
    }

    public Long getId() { return id; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public EstadoTratamiento getEstado() { return estado; }
    public void setEstado(EstadoTratamiento estado) { this.estado = estado; }

    public Diagnostico getDiagnostico() { return diagnostico; }
    public void setDiagnostico(Diagnostico diagnostico) { this.diagnostico = diagnostico; }
}
