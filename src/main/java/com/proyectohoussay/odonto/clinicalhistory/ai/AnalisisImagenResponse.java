package com.proyectohoussay.odonto.clinicalhistory.ai;

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
        Long diagnosticoCreadoId
) {
}
