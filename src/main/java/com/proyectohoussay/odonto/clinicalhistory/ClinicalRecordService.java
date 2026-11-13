package com.proyectohoussay.odonto.clinicalhistory;

import com.proyectohoussay.odonto.patient.PacienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ClinicalRecordService {
    private final PacienteRepository pacienteRepository;
    private final HistoriaClinicaRepository historiaRepository;
    private final DiagnosticoRepository diagnosticoRepository;
    private final TratamientoRepository tratamientoRepository;

    public ClinicalRecordService(PacienteRepository pacienteRepository, HistoriaClinicaRepository historiaRepository,
                                 DiagnosticoRepository diagnosticoRepository, TratamientoRepository tratamientoRepository) {
        this.pacienteRepository = pacienteRepository;
        this.historiaRepository = historiaRepository;
        this.diagnosticoRepository = diagnosticoRepository;
        this.tratamientoRepository = tratamientoRepository;
    }

    @Transactional
    public HistoriaClinica createHistory(Long pacienteId, LocalDate fechaApertura, String observaciones) {
        if (pacienteRepository.findLockedById(pacienteId).isEmpty()) {
            throw new ClinicalRecordNotFoundException("Paciente", pacienteId);
        }
        if (historiaRepository.existsByPacienteId(pacienteId)) {
            throw new ClinicalRecordConflictException("El paciente ya tiene una historia clínica");
        }
        try {
            return historiaRepository.saveAndFlush(new HistoriaClinica(fechaApertura, pacienteId, observaciones));
        } catch (DataIntegrityViolationException exception) {
            throw new ClinicalRecordConflictException("El paciente ya tiene una historia clínica");
        }
    }

    public List<HistoriaClinica> listHistories(Long pacienteId) {
        return pacienteId == null ? historiaRepository.findAll() : historiaRepository.findAllByPacienteId(pacienteId);
    }

    public HistoriaClinica getHistory(Long id) {
        return historiaRepository.findById(id)
                .orElseThrow(() -> new ClinicalRecordNotFoundException("Historia clínica", id));
    }

    @Transactional
    public HistoriaClinica updateHistory(Long id, LocalDate fechaApertura, String observaciones) {
        HistoriaClinica history = getHistory(id);
        history.setFechaApertura(fechaApertura);
        history.setObservaciones(observaciones);
        return historiaRepository.save(history);
    }

    @Transactional
    public Diagnostico createDiagnosis(Long historiaId, LocalDate fecha, String descripcion) {
        HistoriaClinica history = getHistory(historiaId);
        Diagnostico diagnosis = new Diagnostico(fecha, descripcion);
        history.agregarDiagnostico(diagnosis);
        return diagnosticoRepository.save(diagnosis);
    }

    public List<Diagnostico> listDiagnoses(Long historiaId) {
        return historiaId == null ? diagnosticoRepository.findAll()
                : diagnosticoRepository.findAllByHistoriaClinicaId(historiaId);
    }

    public Diagnostico getDiagnosis(Long id) {
        return diagnosticoRepository.findById(id)
                .orElseThrow(() -> new ClinicalRecordNotFoundException("Diagnóstico", id));
    }

    @Transactional
    public Diagnostico updateDiagnosis(Long id, LocalDate fecha, String descripcion) {
        Diagnostico diagnosis = getDiagnosis(id);
        diagnosis.setFecha(fecha);
        diagnosis.setDescripcion(descripcion);
        return diagnosticoRepository.save(diagnosis);
    }

    @Transactional
    public Tratamiento createTreatment(Long diagnosticoId, String descripcion, LocalDate fecha, EstadoTratamiento estado) {
        Diagnostico diagnosis = getDiagnosis(diagnosticoId);
        Tratamiento treatment = new Tratamiento(descripcion, fecha, estado);
        diagnosis.agregarTratamiento(treatment);
        return tratamientoRepository.save(treatment);
    }

    public List<Tratamiento> listTreatments(Long diagnosticoId) {
        return diagnosticoId == null ? tratamientoRepository.findAll()
                : tratamientoRepository.findAllByDiagnosticoId(diagnosticoId);
    }

    public Tratamiento getTreatment(Long id) {
        return tratamientoRepository.findById(id)
                .orElseThrow(() -> new ClinicalRecordNotFoundException("Tratamiento", id));
    }

    @Transactional
    public Tratamiento updateTreatment(Long id, String descripcion, LocalDate fecha, EstadoTratamiento estado) {
        Tratamiento treatment = getTreatment(id);
        treatment.setDescripcion(descripcion);
        treatment.setFecha(fecha);
        treatment.setEstado(estado);
        return tratamientoRepository.save(treatment);
    }
}
