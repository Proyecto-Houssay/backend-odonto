package com.proyectohoussay.odonto.teletriaje.model;

import com.proyectohoussay.odonto.model.Turno;
import com.proyectohoussay.odonto.patient.Paciente;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes_teletriaje")
public class SolicitudTeletriaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El paciente es obligatorio.")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @NotBlank(message = "El motivo de consulta es obligatorio.")
    @Column(nullable = false, length = 255)
    private String motivoConsulta;

    @Column(length = 2000)
    private String descripcionSintomas;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NivelPrioridad prioridadSugerida = NivelPrioridad.VERDE;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NivelPrioridad prioridadValidada;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoValidacion estadoValidacion = EstadoValidacion.SUGERIDA;

    @Column(nullable = false)
    private boolean signosAlarma = false;

    @Column(nullable = false)
    private boolean derivacionHumana = false;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigenSolicitud origen = OrigenSolicitud.WEB;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    private Long profesionalValidadorId;

    private String profesionalValidadorNombre;

    @Column(length = 2000)
    private String observacionesClinicas;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    private LocalDateTime fechaValidacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turno_id")
    private Turno turno;

    public SolicitudTeletriaje() {
    }

    public SolicitudTeletriaje(Paciente paciente, String motivoConsulta, String descripcionSintomas,
                              NivelPrioridad prioridadSugerida, boolean signosAlarma, OrigenSolicitud origen) {
        this.paciente = paciente;
        this.motivoConsulta = motivoConsulta;
        this.descripcionSintomas = descripcionSintomas;
        this.prioridadSugerida = prioridadSugerida != null ? prioridadSugerida : NivelPrioridad.VERDE;
        this.signosAlarma = signosAlarma;
        this.derivacionHumana = signosAlarma; // Signos de alarma activan derivacion inmediata a atencion humana
        this.origen = origen != null ? origen : OrigenSolicitud.WEB;
        this.estadoValidacion = EstadoValidacion.SUGERIDA;
        this.estado = signosAlarma ? EstadoSolicitud.DERIVADA_URGENTE : EstadoSolicitud.PENDIENTE;
        this.fechaCreacion = LocalDateTime.now();
    }

    public NivelPrioridad getPrioridadEfectiva() {
        if (prioridadValidada != null) {
            return prioridadValidada;
        }
        return prioridadSugerida != null ? prioridadSugerida : NivelPrioridad.VERDE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
    }

    public String getMotivoConsulta() {
        return motivoConsulta;
    }

    public void setMotivoConsulta(String motivoConsulta) {
        this.motivoConsulta = motivoConsulta;
    }

    public String getDescripcionSintomas() {
        return descripcionSintomas;
    }

    public void setDescripcionSintomas(String descripcionSintomas) {
        this.descripcionSintomas = descripcionSintomas;
    }

    public NivelPrioridad getPrioridadSugerida() {
        return prioridadSugerida;
    }

    public void setPrioridadSugerida(NivelPrioridad prioridadSugerida) {
        this.prioridadSugerida = prioridadSugerida;
    }

    public NivelPrioridad getPrioridadValidada() {
        return prioridadValidada;
    }

    public void setPrioridadValidada(NivelPrioridad prioridadValidada) {
        this.prioridadValidada = prioridadValidada;
    }

    public EstadoValidacion getEstadoValidacion() {
        return estadoValidacion;
    }

    public void setEstadoValidacion(EstadoValidacion estadoValidacion) {
        this.estadoValidacion = estadoValidacion;
    }

    public boolean isSignosAlarma() {
        return signosAlarma;
    }

    public void setSignosAlarma(boolean signosAlarma) {
        this.signosAlarma = signosAlarma;
    }

    public boolean isDerivacionHumana() {
        return derivacionHumana;
    }

    public void setDerivacionHumana(boolean derivacionHumana) {
        this.derivacionHumana = derivacionHumana;
    }

    public OrigenSolicitud getOrigen() {
        return origen;
    }

    public void setOrigen(OrigenSolicitud origen) {
        this.origen = origen;
    }

    public EstadoSolicitud getEstado() {
        return estado;
    }

    public void setEstado(EstadoSolicitud estado) {
        this.estado = estado;
    }

    public Long getProfesionalValidadorId() {
        return profesionalValidadorId;
    }

    public void setProfesionalValidadorId(Long profesionalValidadorId) {
        this.profesionalValidadorId = profesionalValidadorId;
    }

    public String getProfesionalValidadorNombre() {
        return profesionalValidadorNombre;
    }

    public void setProfesionalValidadorNombre(String profesionalValidadorNombre) {
        this.profesionalValidadorNombre = profesionalValidadorNombre;
    }

    public String getObservacionesClinicas() {
        return observacionesClinicas;
    }

    public void setObservacionesClinicas(String observacionesClinicas) {
        this.observacionesClinicas = observacionesClinicas;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaValidacion() {
        return fechaValidacion;
    }

    public void setFechaValidacion(LocalDateTime fechaValidacion) {
        this.fechaValidacion = fechaValidacion;
    }

    public Turno getTurno() {
        return turno;
    }

    public void setTurno(Turno turno) {
        this.turno = turno;
    }
}
