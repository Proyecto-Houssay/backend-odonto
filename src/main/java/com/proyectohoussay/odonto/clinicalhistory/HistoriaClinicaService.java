package com.proyectohoussay.odonto.clinicalhistory;

import com.proyectohoussay.odonto.clinicalhistory.dto.DiagnosticoDto;
import com.proyectohoussay.odonto.clinicalhistory.dto.HistoriaClinicaRequest;
import com.proyectohoussay.odonto.clinicalhistory.dto.TratamientoDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service managing clinical histories, diagnoses, and treatments.
 */
@Service
public class HistoriaClinicaService {

    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final DiagnosticoRepository diagnosticoRepository;
    private final TratamientoRepository tratamientoRepository;

    public HistoriaClinicaService(
            HistoriaClinicaRepository historiaClinicaRepository,
            DiagnosticoRepository diagnosticoRepository,
            TratamientoRepository tratamientoRepository
    ) {
        this.historiaClinicaRepository = historiaClinicaRepository;
        this.diagnosticoRepository = diagnosticoRepository;
        this.tratamientoRepository = tratamientoRepository;
    }

    @Transactional
    public HistoriaClinica crearHistoriaClinica(HistoriaClinicaRequest request) {
        if (request.pacienteId() == null) {
            throw new IllegalArgumentException("El paciente es obligatorio.");
        }
        if (historiaClinicaRepository.existsByPacienteId(request.pacienteId())) {
            throw new IllegalStateException("El paciente ya cuenta con una historia clínica registrada.");
        }

        LocalDate fecha = request.fechaApertura() != null ? request.fechaApertura() : LocalDate.now();
        HistoriaClinica historia = new HistoriaClinica(fecha, request.pacienteId(), request.observaciones());
        return historiaClinicaRepository.save(historia);
    }

    @Transactional(readOnly = true)
    public HistoriaClinica obtenerPorId(Long id) {
        return historiaClinicaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Historia clínica con ID " + id + " no encontrada."));
    }

    @Transactional(readOnly = true)
    public HistoriaClinica obtenerPorPacienteId(Long pacienteId) {
        return historiaClinicaRepository.findByPacienteId(pacienteId)
                .orElseThrow(() -> new IllegalArgumentException("Historia clínica para el paciente ID " + pacienteId + " no encontrada."));
    }

    @Transactional
    public Diagnostico agregarDiagnostico(Long historiaClinicaId, LocalDate fecha, String descripcion, List<TratamientoDto> tratamientos) {
        HistoriaClinica historia = obtenerPorId(historiaClinicaId);

        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripción del diagnóstico es obligatoria.");
        }

        LocalDate fechaDiag = fecha != null ? fecha : LocalDate.now();
        Diagnostico diagnostico = new Diagnostico(fechaDiag, descripcion);

        if (tratamientos != null) {
            for (TratamientoDto tDto : tratamientos) {
                LocalDate fechaTrat = tDto.fecha() != null ? tDto.fecha() : LocalDate.now();
                EstadoTratamiento estado = tDto.estado() != null ? tDto.estado() : EstadoTratamiento.PENDIENTE;
                Tratamiento tratamiento = new Tratamiento(tDto.descripcion(), fechaTrat, estado);
                diagnostico.agregarTratamiento(tratamiento);
            }
        }

        historia.agregarDiagnostico(diagnostico);
        return diagnosticoRepository.save(diagnostico);
    }

    @Transactional(readOnly = true)
    public List<Diagnostico> listarDiagnosticosPorHistoria(Long historiaClinicaId) {
        return diagnosticoRepository.findAllByHistoriaClinicaId(historiaClinicaId);
    }

    @Transactional(readOnly = true)
    public Diagnostico obtenerDiagnosticoPorId(Long id) {
        return diagnosticoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Diagnóstico con ID " + id + " no encontrado."));
    }

    @Transactional
    public Tratamiento agregarTratamiento(Long diagnosticoId, String descripcion, LocalDate fecha, EstadoTratamiento estado) {
        Diagnostico diagnostico = obtenerDiagnosticoPorId(diagnosticoId);

        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripción del tratamiento es obligatoria.");
        }

        LocalDate fechaTrat = fecha != null ? fecha : LocalDate.now();
        EstadoTratamiento est = estado != null ? estado : EstadoTratamiento.PENDIENTE;

        Tratamiento tratamiento = new Tratamiento(descripcion, fechaTrat, est);
        diagnostico.agregarTratamiento(tratamiento);
        return tratamientoRepository.save(tratamiento);
    }

    @Transactional
    public Tratamiento actualizarEstadoTratamiento(Long tratamientoId, EstadoTratamiento nuevoEstado) {
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("El nuevo estado del tratamiento es obligatorio.");
        }
        Tratamiento tratamiento = tratamientoRepository.findById(tratamientoId)
                .orElseThrow(() -> new IllegalArgumentException("Tratamiento con ID " + tratamientoId + " no encontrado."));
        tratamiento.setEstado(nuevoEstado);
        return tratamientoRepository.save(tratamiento);
    }
}
