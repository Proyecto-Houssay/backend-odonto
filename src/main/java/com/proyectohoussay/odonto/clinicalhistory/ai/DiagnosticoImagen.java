package com.proyectohoussay.odonto.clinicalhistory.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Historical record of an AI dental image diagnostic session.
 */
@Entity
@Table(name = "diagnosticos_imagen")
public class DiagnosticoImagen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La fecha de análisis es obligatoria.")
    @Column(name = "fecha_analisis", nullable = false)
    private LocalDateTime fechaAnalisis;

    @Column(name = "paciente_id")
    private Long pacienteId;

    @Column(name = "historia_clinica_id")
    private Long historiaClinicaId;

    @NotNull(message = "El tipo de imagen dental es obligatorio.")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_imagen", nullable = false, length = 40)
    private TipoImagenDental tipoImagen;

    @Column(name = "nombre_archivo", length = 255)
    private String nombreArchivo;

    @Column(name = "diagnostico_sugerido", nullable = false, length = 2000)
    private String diagnosticoSugerido;

    @NotNull(message = "El nivel de urgencia es obligatorio.")
    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_urgencia", nullable = false, length = 20)
    private NivelUrgencia nivelUrgencia;

    @Column(name = "indice_severidad")
    private Double indiceSeveridad;

    @Column(name = "hallazgos_json", length = 4000)
    private String hallazgosJson;

    @Column(name = "tratamientos_sugeridos", length = 2000)
    private String tratamientosSugeridos;

    @Column(name = "observaciones_odontologo", length = 1000)
    private String observacionesOdontologo;

    @Column(name = "observaciones_ia", length = 2000)
    private String observacionesIA;

    @Column(name = "modelo_utilizado", length = 100)
    private String modeloUtilizado;

    @Column(name = "diagnostico_id")
    private Long diagnosticoId;

    protected DiagnosticoImagen() {
        // Required by JPA
    }

    public DiagnosticoImagen(
            LocalDateTime fechaAnalisis,
            Long pacienteId,
            Long historiaClinicaId,
            TipoImagenDental tipoImagen,
            String nombreArchivo,
            String diagnosticoSugerido,
            NivelUrgencia nivelUrgencia,
            Double indiceSeveridad,
            String hallazgosJson,
            String tratamientosSugeridos,
            String observacionesOdontologo,
            String observacionesIA,
            String modeloUtilizado,
            Long diagnosticoId
    ) {
        this.fechaAnalisis = fechaAnalisis != null ? fechaAnalisis : LocalDateTime.now();
        this.pacienteId = pacienteId;
        this.historiaClinicaId = historiaClinicaId;
        this.tipoImagen = tipoImagen;
        this.nombreArchivo = nombreArchivo;
        this.diagnosticoSugerido = diagnosticoSugerido;
        this.nivelUrgencia = nivelUrgencia;
        this.indiceSeveridad = indiceSeveridad;
        this.hallazgosJson = hallazgosJson;
        this.tratamientosSugeridos = tratamientosSugeridos;
        this.observacionesOdontologo = observacionesOdontologo;
        this.observacionesIA = observacionesIA;
        this.modeloUtilizado = modeloUtilizado;
        this.diagnosticoId = diagnosticoId;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getFechaAnalisis() {
        return fechaAnalisis;
    }

    public void setFechaAnalisis(LocalDateTime fechaAnalisis) {
        this.fechaAnalisis = fechaAnalisis;
    }

    public Long getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(Long pacienteId) {
        this.pacienteId = pacienteId;
    }

    public Long getHistoriaClinicaId() {
        return historiaClinicaId;
    }

    public void setHistoriaClinicaId(Long historiaClinicaId) {
        this.historiaClinicaId = historiaClinicaId;
    }

    public TipoImagenDental getTipoImagen() {
        return tipoImagen;
    }

    public void setTipoImagen(TipoImagenDental tipoImagen) {
        this.tipoImagen = tipoImagen;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getDiagnosticoSugerido() {
        return diagnosticoSugerido;
    }

    public void setDiagnosticoSugerido(String diagnosticoSugerido) {
        this.diagnosticoSugerido = diagnosticoSugerido;
    }

    public NivelUrgencia getNivelUrgencia() {
        return nivelUrgencia;
    }

    public void setNivelUrgencia(NivelUrgencia nivelUrgencia) {
        this.nivelUrgencia = nivelUrgencia;
    }

    public Double getIndiceSeveridad() {
        return indiceSeveridad;
    }

    public void setIndiceSeveridad(Double indiceSeveridad) {
        this.indiceSeveridad = indiceSeveridad;
    }

    public String getHallazgosJson() {
        return hallazgosJson;
    }

    public void setHallazgosJson(String hallazgosJson) {
        this.hallazgosJson = hallazgosJson;
    }

    public String getTratamientosSugeridos() {
        return tratamientosSugeridos;
    }

    public void setTratamientosSugeridos(String tratamientosSugeridos) {
        this.tratamientosSugeridos = tratamientosSugeridos;
    }

    public String getObservacionesOdontologo() {
        return observacionesOdontologo;
    }

    public void setObservacionesOdontologo(String observacionesOdontologo) {
        this.observacionesOdontologo = observacionesOdontologo;
    }

    public String getObservacionesIA() {
        return observacionesIA;
    }

    public void setObservacionesIA(String observacionesIA) {
        this.observacionesIA = observacionesIA;
    }

    public String getModeloUtilizado() {
        return modeloUtilizado;
    }

    public void setModeloUtilizado(String modeloUtilizado) {
        this.modeloUtilizado = modeloUtilizado;
    }

    public Long getDiagnosticoId() {
        return diagnosticoId;
    }

    public void setDiagnosticoId(Long diagnosticoId) {
        this.diagnosticoId = diagnosticoId;
    }
}
