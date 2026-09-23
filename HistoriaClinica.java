package ar.houssay.odonto.historiaclinica;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "historia_clinica")
public class HistoriaClinica {

    public static final int OBSERVACIONES_MAX = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Límite explícito con el módulo de pacientes: solo se guarda el id,
     * sin relación JPA hacia la entidad Paciente.
     */
    @Column(name = "paciente_id", nullable = false, updatable = false)
    private Long pacienteId;

    @Column(name = "fecha_apertura", nullable = false, updatable = false)
    private LocalDate fechaApertura;

    @Column(name = "observaciones", length = OBSERVACIONES_MAX)
    private String observaciones;

    @OneToMany(mappedBy = "historiaClinica", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Diagnostico> diagnosticos = new ArrayList<>();

    protected HistoriaClinica() {
        // requerido por JPA
    }

    public HistoriaClinica(Long pacienteId, LocalDate fechaApertura, String observaciones) {
        this.pacienteId = pacienteId;
        this.fechaApertura = fechaApertura;
        this.observaciones = observaciones;
    }

    public void agregarDiagnostico(Diagnostico diagnostico) {
        diagnosticos.add(diagnostico);
        diagnostico.setHistoriaClinica(this);
    }

    public void quitarDiagnostico(Diagnostico diagnostico) {
        diagnosticos.remove(diagnostico);
        diagnostico.setHistoriaClinica(null);
    }

    public Long getId() {
        return id;
    }

    public Long getPacienteId() {
        return pacienteId;
    }

    public LocalDate getFechaApertura() {
        return fechaApertura;
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
}
