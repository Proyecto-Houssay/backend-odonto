package ar.houssay.odonto.historiaclinica;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "diagnostico")
public class Diagnostico {

    public static final int DESCRIPCION_MAX = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "historia_clinica_id", nullable = false)
    private HistoriaClinica historiaClinica;

    @Column(name = "descripcion", nullable = false, length = DESCRIPCION_MAX)
    private String descripcion;

    @Column(name = "fecha_diagnostico", nullable = false)
    private LocalDate fechaDiagnostico;

    protected Diagnostico() {
        // requerido por JPA
    }

    public Diagnostico(String descripcion, LocalDate fechaDiagnostico) {
        this.descripcion = descripcion;
        this.fechaDiagnostico = fechaDiagnostico;
    }

    public Long getId() {
        return id;
    }

    public HistoriaClinica getHistoriaClinica() {
        return historiaClinica;
    }

    void setHistoriaClinica(HistoriaClinica historiaClinica) {
        this.historiaClinica = historiaClinica;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDate getFechaDiagnostico() {
        return fechaDiagnostico;
    }

    public void setFechaDiagnostico(LocalDate fechaDiagnostico) {
        this.fechaDiagnostico = fechaDiagnostico;
    }
}
