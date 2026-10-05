package com.proyectohoussay.odonto.diagnostico;

import com.proyectohoussay.odonto.historia.HistoriaClinica;
import com.proyectohoussay.odonto.tratamiento.Tratamiento;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "diagnosticos")
public class Diagnostico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private LocalDate fecha;

    @NotNull
    @Size(max = 1000)
    @Column(nullable = false, length = 1000)
    private String descripcion;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "historia_clinica_id", nullable = false)
    private HistoriaClinica historiaClinica;

    @OneToMany(
            mappedBy = "diagnostico",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Tratamiento> tratamientos = new ArrayList<>();

    public Diagnostico() {
    }

    public Diagnostico(
            LocalDate fecha,
            String descripcion,
            HistoriaClinica historiaClinica
    ) {
        this.fecha = fecha;
        this.descripcion = descripcion;
        this.historiaClinica = historiaClinica;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public HistoriaClinica getHistoriaClinica() {
        return historiaClinica;
    }

    public void setHistoriaClinica(HistoriaClinica historiaClinica) {
        this.historiaClinica = historiaClinica;
    }

    public List<Tratamiento> getTratamientos() {
        return tratamientos;
    }

    public void setTratamientos(List<Tratamiento> tratamientos) {
        this.tratamientos = tratamientos;
    }

    public void agregarTratamiento(Tratamiento tratamiento) {
        tratamientos.add(tratamiento);
        tratamiento.setDiagnostico(this);
    }

    public void eliminarTratamiento(Tratamiento tratamiento) {
        tratamientos.remove(tratamiento);
        tratamiento.setDiagnostico(null);
    }
}
