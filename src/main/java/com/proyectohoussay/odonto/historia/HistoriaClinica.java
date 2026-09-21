package com.proyectohoussay.odonto.historia;

import com.proyectohoussay.odonto.diagnostico.Diagnostico;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "historias_clinicas")
public class HistoriaClinica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "fecha_apertura", nullable = false)
    private LocalDate fechaApertura;

    @NotNull
    @Size(max = 2000)
    @Column(nullable = false, length = 2000)
    private String observaciones;

    @NotNull
    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @OneToMany(
            mappedBy = "historiaClinica",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Diagnostico> diagnosticos = new ArrayList<>();

    public HistoriaClinica() {
    }

    public HistoriaClinica(
            LocalDate fechaApertura,
            String observaciones,
            Long pacienteId
    ) {
        this.fechaApertura = fechaApertura;
        this.observaciones = observaciones;
        this.pacienteId = pacienteId;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getFechaApertura() {
        return fechaApertura;
    }

    public void setFechaApertura(LocalDate fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public Long getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(Long pacienteId) {
        this.pacienteId = pacienteId;
    }

    public List<Diagnostico> getDiagnosticos() {
        return diagnosticos;
    }

    public void setDiagnosticos(List<Diagnostico> diagnosticos) {
        this.diagnosticos = diagnosticos;
    }

    public void agregarDiagnostico(Diagnostico diagnostico) {
        diagnosticos.add(diagnostico);
        diagnostico.setHistoriaClinica(this);
    }

    public void eliminarDiagnostico(Diagnostico diagnostico) {
        diagnosticos.remove(diagnostico);
        diagnostico.setHistoriaClinica(null);
    }
}
