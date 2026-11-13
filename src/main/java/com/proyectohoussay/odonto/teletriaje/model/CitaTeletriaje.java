package com.proyectohoussay.odonto.teletriaje.model;

import com.proyectohoussay.odonto.model.Odontologo;
import com.proyectohoussay.odonto.model.Turno;
import com.proyectohoussay.odonto.patient.Paciente;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "citas_teletriaje")
public class CitaTeletriaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id")
    private SolicitudTeletriaje solicitud;

    @NotNull(message = "El turno asociado es obligatorio.")
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "turno_id", nullable = false, unique = true)
    private Turno turno;

    @NotNull(message = "El paciente es obligatorio.")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @NotNull(message = "El odontólogo es obligatorio.")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "odontologo_id", nullable = false)
    private Odontologo odontologo;

    @Column(nullable = false)
    private boolean consentimientoInformado = false;

    private LocalDateTime fechaConsentimiento;

    @Column(nullable = false)
    private boolean grabacionHabilitada = false; // "no guardar video por defecto"

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoSalaTeletriaje estadoSala = EstadoSalaTeletriaje.PENDIENTE_PROVEEDOR;

    @Column(length = 500)
    private String enlaceVideo; // null si no hay proveedor activo

    private LocalDateTime enlaceExpiraEn;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    public CitaTeletriaje() {
    }

    public CitaTeletriaje(SolicitudTeletriaje solicitud, Turno turno, Paciente paciente,
                          Odontologo odontologo, boolean consentimientoInformado) {
        this.solicitud = solicitud;
        this.turno = turno;
        this.paciente = paciente;
        this.odontologo = odontologo;
        this.consentimientoInformado = consentimientoInformado;
        this.fechaConsentimiento = consentimientoInformado ? LocalDateTime.now() : null;
        this.grabacionHabilitada = false; // No guardar video por defecto
        this.fechaCreacion = LocalDateTime.now();
    }

    public boolean estaExpirada() {
        if (enlaceExpiraEn == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(enlaceExpiraEn);
    }

    public boolean isEnlaceFuncional() {
        return enlaceVideo != null && !enlaceVideo.isBlank() &&
                estadoSala == EstadoSalaTeletriaje.DISPONIBLE && !estaExpirada();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SolicitudTeletriaje getSolicitud() {
        return solicitud;
    }

    public void setSolicitud(SolicitudTeletriaje solicitud) {
        this.solicitud = solicitud;
    }

    public Turno getTurno() {
        return turno;
    }

    public void setTurno(Turno turno) {
        this.turno = turno;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
    }

    public Odontologo getOdontologo() {
        return odontologo;
    }

    public void setOdontologo(Odontologo odontologo) {
        this.odontologo = odontologo;
    }

    public boolean isConsentimientoInformado() {
        return consentimientoInformado;
    }

    public void setConsentimientoInformado(boolean consentimientoInformado) {
        this.consentimientoInformado = consentimientoInformado;
    }

    public LocalDateTime getFechaConsentimiento() {
        return fechaConsentimiento;
    }

    public void setFechaConsentimiento(LocalDateTime fechaConsentimiento) {
        this.fechaConsentimiento = fechaConsentimiento;
    }

    public boolean isGrabacionHabilitada() {
        return grabacionHabilitada;
    }

    public void setGrabacionHabilitada(boolean grabacionHabilitada) {
        this.grabacionHabilitada = grabacionHabilitada;
    }

    public EstadoSalaTeletriaje getEstadoSala() {
        return estadoSala;
    }

    public void setEstadoSala(EstadoSalaTeletriaje estadoSala) {
        this.estadoSala = estadoSala;
    }

    public String getEnlaceVideo() {
        return enlaceVideo;
    }

    public void setEnlaceVideo(String enlaceVideo) {
        this.enlaceVideo = enlaceVideo;
    }

    public LocalDateTime getEnlaceExpiraEn() {
        return enlaceExpiraEn;
    }

    public void setEnlaceExpiraEn(LocalDateTime enlaceExpiraEn) {
        this.enlaceExpiraEn = enlaceExpiraEn;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
