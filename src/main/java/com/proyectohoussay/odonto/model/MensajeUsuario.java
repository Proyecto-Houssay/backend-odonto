package com.proyectohoussay.odonto.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "mensajes_usuario",
        indexes = @Index(name = "idx_mensajes_destinatario", columnList = "destinatario_id")
)
public class MensajeUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "destinatario_id", nullable = false)
    private Long destinatarioId;

    @Column(name = "remitente_id")
    private Long remitenteId;

    @Column(name = "remitente_nombre", nullable = false, length = 120)
    private String remitenteNombre;

    @Column(nullable = false, length = 160)
    private String asunto;

    @Column(nullable = false, length = 1000)
    private String contenido;

    @Column(nullable = false)
    private boolean leido = false;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "leido_en")
    private LocalDateTime leidoEn;

    public MensajeUsuario() {
    }

    public MensajeUsuario(
            Long destinatarioId,
            Long remitenteId,
            String remitenteNombre,
            String asunto,
            String contenido) {
        this.destinatarioId = destinatarioId;
        this.remitenteId = remitenteId;
        this.remitenteNombre = remitenteNombre;
        this.asunto = asunto;
        this.contenido = contenido;
        this.leido = false;
        this.creadoEn = LocalDateTime.now();
    }

    @PrePersist
    void onCreate() {
        if (this.creadoEn == null) {
            this.creadoEn = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDestinatarioId() {
        return destinatarioId;
    }

    public void setDestinatarioId(Long destinatarioId) {
        this.destinatarioId = destinatarioId;
    }

    public Long getRemitenteId() {
        return remitenteId;
    }

    public void setRemitenteId(Long remitenteId) {
        this.remitenteId = remitenteId;
    }

    public String getRemitenteNombre() {
        return remitenteNombre;
    }

    public void setRemitenteNombre(String remitenteNombre) {
        this.remitenteNombre = remitenteNombre;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public boolean isLeido() {
        return leido;
    }

    public void setLeido(boolean leido) {
        this.leido = leido;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }

    public LocalDateTime getLeidoEn() {
        return leidoEn;
    }

    public void setLeidoEn(LocalDateTime leidoEn) {
        this.leidoEn = leidoEn;
    }
}
