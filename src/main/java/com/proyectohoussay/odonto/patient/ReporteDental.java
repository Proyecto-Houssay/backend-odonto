package com.proyectohoussay.odonto.patient;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "reporte_dental", indexes = @Index(name = "idx_reporte_dental_paciente_pieza", columnList = "paciente_id,pieza"))
public class ReporteDental {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false, updatable = false)
    private Paciente paciente;
    @Column(nullable = false, updatable = false)
    private int pieza;
    @Column(nullable = false, updatable = false)
    private int dolor;
    @Column(nullable = false, updatable = false, length = 500)
    private String sintomas;
    @Lob @Column(columnDefinition = "LONGTEXT", updatable = false)
    private String fotoBase64;
    @Column(nullable = false, updatable = false)
    private boolean consentimientoFoto;
    @Column(nullable = false, updatable = false)
    private Instant creadoEn;
    @Column(length = 2000)
    private String revisionProfesional;
    private Long revisadoPorId;
    private Instant revisadoEn;

    protected ReporteDental() { }

    public ReporteDental(Paciente paciente, int pieza, int dolor, String sintomas, String fotoBase64, boolean consentimientoFoto) {
        this.paciente = paciente;
        this.pieza = pieza;
        this.dolor = dolor;
        this.sintomas = sintomas;
        this.fotoBase64 = fotoBase64;
        this.consentimientoFoto = consentimientoFoto;
        this.creadoEn = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public void revisar(String revision, Long profesionalId) {
        if (revisadoEn != null) throw new IllegalStateException("Este reporte ya fue revisado");
        this.revisionProfesional = revision;
        this.revisadoPorId = profesionalId;
        this.revisadoEn = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public Long getId() { return id; }
    public Paciente getPaciente() { return paciente; }
    public int getPieza() { return pieza; }
    public int getDolor() { return dolor; }
    public String getSintomas() { return sintomas; }
    public String getFotoBase64() { return fotoBase64; }
    public boolean isConsentimientoFoto() { return consentimientoFoto; }
    public Instant getCreadoEn() { return creadoEn; }
    public String getRevisionProfesional() { return revisionProfesional; }
    public Long getRevisadoPorId() { return revisadoPorId; }
    public Instant getRevisadoEn() { return revisadoEn; }
}
