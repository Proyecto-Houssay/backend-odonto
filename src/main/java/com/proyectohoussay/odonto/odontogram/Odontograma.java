package com.proyectohoussay.odonto.odontogram;

import com.proyectohoussay.odonto.patient.Paciente;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Entity
@Table(name = "odontograma")
public class Odontograma {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "paciente_id", nullable = false, updatable = false) private Paciente paciente;
    @Column(nullable = false, updatable = false) private Instant creadoEn;
    @Column(nullable = false, updatable = false) private Long creadoPorId;
    @Column(nullable = false, updatable = false) private String moneda;
    @Column(nullable = false, updatable = false) private String versionCatalogo;
    @ElementCollection
    @CollectionTable(name = "odontograma_diente", joinColumns = @JoinColumn(name = "odontograma_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"odontograma_id", "numero"}))
    @OrderColumn(name = "orden") private List<OdontogramaDiente> dientes;
    @Column(nullable = false, updatable = false) private int totalPresupuesto;
    @Column(nullable = false, updatable = false) private boolean consentimientoAceptado;
    @Column(nullable = false, updatable = false) private String versionConsentimiento;
    @Column(nullable = false, updatable = false, length = 2000) private String textoConsentimiento;
    @Lob @Column(nullable = false, updatable = false, columnDefinition = "LONGTEXT") private String firmaBase64;

    protected Odontograma() {}
    Odontograma(Paciente paciente, Long creator, List<OdontogramaDiente> dientes, String firma) {
        this.paciente = paciente; creadoPorId = creator; creadoEn = Instant.now().truncatedTo(ChronoUnit.MICROS); moneda = "ARS";
        versionCatalogo = OdontogramaCatalogo.VERSION; this.dientes = List.copyOf(dientes);
        totalPresupuesto = dientes.stream().mapToInt(OdontogramaDiente::costo).sum();
        consentimientoAceptado = true; versionConsentimiento = OdontogramaCatalogo.VERSION;
        textoConsentimiento = OdontogramaCatalogo.CONSENTIMIENTO; firmaBase64 = firma;
    }
    OdontogramaResponse response() {
        return new OdontogramaResponse(id, paciente.getId(), creadoEn, creadoPorId, moneda, versionCatalogo,
                dientes.stream().map(OdontogramaDiente::response).toList(), totalPresupuesto,
                consentimientoAceptado, versionConsentimiento, textoConsentimiento, firmaBase64);
    }
}
