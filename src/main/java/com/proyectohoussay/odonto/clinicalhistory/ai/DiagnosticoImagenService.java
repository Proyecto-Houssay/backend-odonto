package com.proyectohoussay.odonto.clinicalhistory.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyectohoussay.odonto.clinicalhistory.Diagnostico;
import com.proyectohoussay.odonto.clinicalhistory.DiagnosticoRepository;
import com.proyectohoussay.odonto.clinicalhistory.HistoriaClinicaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service boundary for Dr. Annie. Analysis is disabled until a real clinical provider is configured.
 */
@Service
public class DiagnosticoImagenService {

    public static final String MODELO_ACTUAL = "Dr. Annie (en preparación)";

    private final DiagnosticoImagenRepository diagnosticoImagenRepository;
    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final DiagnosticoRepository diagnosticoRepository;
    private final ObjectMapper objectMapper;

    public DiagnosticoImagenService(
            DiagnosticoImagenRepository diagnosticoImagenRepository,
            HistoriaClinicaRepository historiaClinicaRepository,
            DiagnosticoRepository diagnosticoRepository,
            ObjectMapper objectMapper
    ) {
        this.diagnosticoImagenRepository = diagnosticoImagenRepository;
        this.historiaClinicaRepository = historiaClinicaRepository;
        this.diagnosticoRepository = diagnosticoRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AnalisisImagenResponse analizarImagen(AnalisisImagenRequest request) {
        throw analisisNoDisponible();
    }

    @Transactional
    public AnalisisImagenResponse analizarArchivo(
            MultipartFile archivo,
            TipoImagenDental tipoImagen,
            Long pacienteId,
            Long historiaClinicaId,
            String observaciones,
            boolean guardarEnHistoria
    ) {
        throw analisisNoDisponible();
    }

    private ResponseStatusException analisisNoDisponible() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Dr. Annie todavía no está conectado. No se generaron ni guardaron diagnósticos.");
    }

    @Transactional(readOnly = true)
    public List<DiagnosticoImagen> obtenerHistorialPorPaciente(Long pacienteId) {
        if (pacienteId == null) {
            throw new IllegalArgumentException("El ID del paciente es obligatorio.");
        }
        return diagnosticoImagenRepository.findAllByPacienteIdOrderByFechaAnalisisDesc(pacienteId);
    }

    @Transactional(readOnly = true)
    public DiagnosticoImagen obtenerPorId(Long id) {
        return diagnosticoImagenRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Diagnóstico por imagen con ID " + id + " no encontrado."));
    }

    public CatalogoIaDto obtenerCatalogo() {
        return new CatalogoIaDto(List.of(), List.of(), List.of(), MODELO_ACTUAL, "EN_PREPARACION");
    }

    public List<HallazgoImagenDto> deserializarHallazgos(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<HallazgoImagenDto>>() {});
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return List.of();
        }
    }

}
