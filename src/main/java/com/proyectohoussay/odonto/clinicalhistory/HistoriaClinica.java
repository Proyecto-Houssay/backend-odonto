package com.proyectohoussay.odonto.clinicalhistory;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Clinical history record for a patient.
 * References the patient by ID to respect module boundaries.
 */
@Entity
@Table(name = "historias_clinicas")
public class HistoriaClinica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La fecha de apertura es obligatoria")
    @Column(name = "fecha_apertura", nullable = false)
    private LocalDate fechaApertura;

    @NotNull(message = "El paciente es obligatorio")
    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(length = 1000)
    private String observaciones;

    @OneToMany(mappedBy = "historiaClinica", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Diagnostico> diagnosticos = new ArrayList<>();

    protected HistoriaClinica() {
        // Required by JPA
    }

    public HistoriaClinica(LocalDate fechaApertura, Long pacienteId, String observaciones) {
        this.fechaApertura = fechaApertura;
        this.pacienteId = pacienteId;
        this.observaciones = observaciones;
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

    public Long getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(Long pacienteId) {
        this.pacienteId = pacienteId;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public List<Diagnostico> getDiagnosticos() {
        return Collections.unmodifiableList(diagnosticos);
    }

    public void agregarDiagnostico(Diagnostico diagnostico) {
        diagnosticos.add(diagnostico);
        diagnostico.setHistoriaClinica(this);
    }

    public void removerDiagnostico(Diagnostico diagnostico) {
        diagnosticos.remove(diagnostico);
        diagnostico.setHistoriaClinica(null);
    }
}
