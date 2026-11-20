package com.proyectohoussay.odonto.clinicalhistory.ai;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Diagnostic analysis report produced by the AI engine.
 */
public record AnalisisImagenResponse(
        Long id,
        TipoImagenDental tipoImagen,
        String diagnosticoSugerido,
        List<HallazgoImagenDto> hallazgos,
        List<String> tratamientosSugeridos,
        NivelUrgencia nivelUrgencia,
        double indiceSeveridad,
        String observacionesIA,
        String modeloUtilizado,
        LocalDateTime fechaAnalisis,
        Long pacienteId,
        Long historiaClinicaId,
        Long diagnosticoCreadoId,
        JsonNode resultadoProveedor
) {
    public AnalisisImagenResponse(Long id, TipoImagenDental tipoImagen, String diagnosticoSugerido,
                                  List<HallazgoImagenDto> hallazgos, List<String> tratamientosSugeridos,
                                  NivelUrgencia nivelUrgencia, double indiceSeveridad, String observacionesIA,
                                  String modeloUtilizado, LocalDateTime fechaAnalisis, Long pacienteId,
                                  Long historiaClinicaId, Long diagnosticoCreadoId) {
        this(id, tipoImagen, diagnosticoSugerido, hallazgos, tratamientosSugeridos, nivelUrgencia,
                indiceSeveridad, observacionesIA, modeloUtilizado, fechaAnalisis, pacienteId,
                historiaClinicaId, diagnosticoCreadoId, null);
    }
}
