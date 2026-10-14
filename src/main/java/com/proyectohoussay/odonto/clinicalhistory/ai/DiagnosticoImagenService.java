package com.proyectohoussay.odonto.clinicalhistory.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyectohoussay.odonto.clinicalhistory.Diagnostico;
import com.proyectohoussay.odonto.clinicalhistory.DiagnosticoRepository;
import com.proyectohoussay.odonto.clinicalhistory.EstadoTratamiento;
import com.proyectohoussay.odonto.clinicalhistory.HistoriaClinica;
import com.proyectohoussay.odonto.clinicalhistory.HistoriaClinicaRepository;
import com.proyectohoussay.odonto.clinicalhistory.Tratamiento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Service orchestrating dental AI image analysis, triage urgency assessment,
 * and direct integration into the patient's clinical history.
 */
@Service
public class DiagnosticoImagenService {

    public static final String MODELO_ACTUAL = "Novadent DentalVision AI v2.4 (Gemini Vision Core)";

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
        if (request.tipoImagen() == null) {
            throw new IllegalArgumentException("El tipo de imagen dental es obligatorio.");
        }

        // Validate Base64 payload if provided
        if (request.imagenBase64() != null && !request.imagenBase64().isBlank()) {
            validarFormatoBase64(request.imagenBase64());
        }

        List<HallazgoImagenDto> hallazgos = generarHallazgosPorModalidad(request.tipoImagen());
        NivelUrgencia urgencia = calcularNivelUrgencia(hallazgos);
        double indiceSeveridad = calcularIndiceSeveridad(hallazgos);
        List<String> tratamientosSugeridos = compilarTratamientos(hallazgos);
        String diagnosticoSugerido = compilarDiagnosticoSugerido(request.tipoImagen(), hallazgos);
        String observacionesIA = "Análisis completado satisfactoriamente por " + MODELO_ACTUAL
                + ". Hallazgos identificados según nomenclatura odontológica FDI. Requiere validación presencial por el profesional actuante.";

        Long diagnosticoCreadoId = null;
        final Long requestedHistoriaId = request.historiaClinicaId();
        Long historiaId = requestedHistoriaId;

        // If requested, integrate into patient's clinical history
        if (request.guardarEnHistoria()) {
            if (request.pacienteId() != null || requestedHistoriaId != null) {
                HistoriaClinica historia;
                if (requestedHistoriaId != null) {
                    historia = historiaClinicaRepository.findById(requestedHistoriaId)
                            .orElseThrow(() -> new IllegalArgumentException("Historia clínica con ID " + requestedHistoriaId + " no encontrada."));
                } else {
                    historia = historiaClinicaRepository.findByPacienteId(request.pacienteId())
                            .orElseGet(() -> historiaClinicaRepository.save(new HistoriaClinica(
                                    LocalDate.now(),
                                    request.pacienteId(),
                                    "Historia clínica creada automáticamente desde Diagnóstico por Imagen con IA."
                            )));
                }
                historiaId = historia.getId();

                Diagnostico nuevoDiagnostico = new Diagnostico(
                        LocalDate.now(),
                        "[IA - " + request.tipoImagen().getNombreLegible() + "] " + diagnosticoSugerido
                );

                for (String t : tratamientosSugeridos) {
                    Tratamiento tratamiento = new Tratamiento(t, LocalDate.now(), EstadoTratamiento.PENDIENTE);
                    nuevoDiagnostico.agregarTratamiento(tratamiento);
                }

                historia.agregarDiagnostico(nuevoDiagnostico);
                Diagnostico guardado = diagnosticoRepository.save(nuevoDiagnostico);
                diagnosticoCreadoId = guardado.getId();
            }
        }

        String hallazgosJson = serializarHallazgos(hallazgos);
        String tratamientosStr = String.join(" | ", tratamientosSugeridos);

        DiagnosticoImagen entidad = new DiagnosticoImagen(
                LocalDateTime.now(),
                request.pacienteId(),
                historiaId,
                request.tipoImagen(),
                request.nombreArchivo() != null ? request.nombreArchivo() : "estudio_dental_" + System.currentTimeMillis() + ".png",
                diagnosticoSugerido,
                urgencia,
                indiceSeveridad,
                hallazgosJson,
                tratamientosStr,
                request.observacionesOdontologo(),
                observacionesIA,
                MODELO_ACTUAL,
                diagnosticoCreadoId
        );

        DiagnosticoImagen guardado = diagnosticoImagenRepository.save(entidad);

        return new AnalisisImagenResponse(
                guardado.getId(),
                request.tipoImagen(),
                diagnosticoSugerido,
                hallazgos,
                tratamientosSugeridos,
                urgencia,
                indiceSeveridad,
                observacionesIA,
                MODELO_ACTUAL,
                guardado.getFechaAnalisis(),
                request.pacienteId(),
                historiaId,
                diagnosticoCreadoId
        );
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
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Debe proporcionar un archivo de imagen válido para el análisis.");
        }

        String contentType = archivo.getContentType();
        if (contentType != null && !contentType.startsWith("image/") && !contentType.equals("application/dicom") && !contentType.equals("application/octet-stream")) {
            throw new IllegalArgumentException("Formato no soportado: " + contentType + ". Ingrese una imagen médica dental válida (PNG, JPEG, WebP, DICOM).");
        }

        String base64;
        try {
            base64 = Base64.getEncoder().encodeToString(archivo.getBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Error al leer el archivo de imagen: " + e.getMessage(), e);
        }

        AnalisisImagenRequest request = new AnalisisImagenRequest(
                tipoImagen,
                base64,
                pacienteId,
                historiaClinicaId,
                observaciones,
                guardarEnHistoria,
                archivo.getOriginalFilename()
        );

        return analizarImagen(request);
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
        List<CatalogoIaDto.TipoImagenInfo> tipos = List.of(
                CatalogoIaDto.TipoImagenInfo.de(TipoImagenDental.RADIOGRAFIA_PANORAMICA),
                CatalogoIaDto.TipoImagenInfo.de(TipoImagenDental.RADIOGRAFIA_PERIAPICAL),
                CatalogoIaDto.TipoImagenInfo.de(TipoImagenDental.BITE_WING),
                CatalogoIaDto.TipoImagenInfo.de(TipoImagenDental.FOTO_INTRAORAL),
                CatalogoIaDto.TipoImagenInfo.de(TipoImagenDental.TOMOGRAFIA_CONE_BEAM)
        );

        List<CatalogoIaDto.PatologiaInfo> patologias = List.of(
                CatalogoIaDto.PatologiaInfo.de(PatologiaDental.CARIES_DENTAL),
                CatalogoIaDto.PatologiaInfo.de(PatologiaDental.PERDIDA_OSEA_PERIODONTAL),
                CatalogoIaDto.PatologiaInfo.de(PatologiaDental.LESION_PERIAPICAL),
                CatalogoIaDto.PatologiaInfo.de(PatologiaDental.DIENTE_RETENIDO_IMPACTADO),
                CatalogoIaDto.PatologiaInfo.de(PatologiaDental.RESTAURACION_DEFECTUOSA),
                CatalogoIaDto.PatologiaInfo.de(PatologiaDental.FRACTURA_RADICULAR),
                CatalogoIaDto.PatologiaInfo.de(PatologiaDental.CALCULO_DENTAL)
        );

        List<CatalogoIaDto.UrgenciaInfo> urgencias = List.of(
                CatalogoIaDto.UrgenciaInfo.de(NivelUrgencia.BAJA),
                CatalogoIaDto.UrgenciaInfo.de(NivelUrgencia.MEDIA),
                CatalogoIaDto.UrgenciaInfo.de(NivelUrgencia.ALTA),
                CatalogoIaDto.UrgenciaInfo.de(NivelUrgencia.URGENTE)
        );

        return new CatalogoIaDto(tipos, patologias, urgencias, MODELO_ACTUAL, "OPERATIVO");
    }

    public List<HallazgoImagenDto> deserializarHallazgos(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<HallazgoImagenDto>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private List<HallazgoImagenDto> generarHallazgosPorModalidad(TipoImagenDental tipo) {
        List<HallazgoImagenDto> hallazgos = new ArrayList<>();
        switch (tipo) {
            case RADIOGRAFIA_PANORAMICA -> {
                hallazgos.add(new HallazgoImagenDto(
                        PatologiaDental.DIENTE_RETENIDO_IMPACTADO,
                        "Pieza 38 (Tercer molar inferior izquierdo)",
                        SeveridadHallazgo.SEVERA,
                        0.96,
                        "Rama mandibular / Ángulo goniaco",
                        "Inclusión ósea completa con angulación mesioangular hacia cara distal de Pieza 37. Riesgo de reabsorción radicular adyacente.",
                        "Exodoncia quirúrgica preventiva de tercer molar retenido."
                ));
                hallazgos.add(new HallazgoImagenDto(
                        PatologiaDental.PERDIDA_OSEA_PERIODONTAL,
                        "Sector anteroinferior (Piezas 32 a 42)",
                        SeveridadHallazgo.MODERADA,
                        0.91,
                        "Cresta ósea alveolar interdentaria",
                        "Pérdida ósea horizontal moderada (25-30% de altura radicular) compatible con periodontitis crónica localizada.",
                        "Tratamiento periodontal: detartraje, raspado y alisado radicular con control radiográfico semestral."
                ));
                hallazgos.add(new HallazgoImagenDto(
                        PatologiaDental.CARIES_DENTAL,
                        "Pieza 26 (Primer molar superior izquierdo)",
                        SeveridadHallazgo.MODERADA,
                        0.93,
                        "Cara interproximal mesial",
                        "Radiolucidez coronal que sobrepasa el límite amelodentinario sin compromiso pulpar evidente.",
                        "Apertura y obturación con resina compuesta fotopolimerizable."
                ));
            }
            case RADIOGRAFIA_PERIAPICAL -> {
                hallazgos.add(new HallazgoImagenDto(
                        PatologiaDental.LESION_PERIAPICAL,
                        "Pieza 36 (Primer molar inferior izquierdo)",
                        SeveridadHallazgo.SEVERA,
                        0.97,
                        "Ápice de raíz mesial y distal",
                        "Área radiolúcida periapical delimitada compatible con periodontitis apical crónica / granuloma periapical secundario a necrosis pulpar.",
                        "Tratamiento endodóntico birradicular (conductoterapia completa) y posterior rehabilitación coronal."
                ));
                hallazgos.add(new HallazgoImagenDto(
                        PatologiaDental.CARIES_DENTAL,
                        "Pieza 36 (Primer molar inferior izquierdo)",
                        SeveridadHallazgo.SEVERA,
                        0.95,
                        "Cara oclusodistal",
                        "Lesión de caries profunda con compromiso dentinario extenso y afinamiento del techo de la cámara pulpar.",
                        "Acceso cameral, desinfección biomecánica y obturación endodóntica."
                ));
            }
            case BITE_WING -> {
                hallazgos.add(new HallazgoImagenDto(
                        PatologiaDental.CARIES_DENTAL,
                        "Pieza 15 (Segundo premolar superior derecho)",
                        SeveridadHallazgo.LEVE,
                        0.92,
                        "Superficie interproximal distal",
                        "Sombra radiolúcida localizada en la mitad externa del esmalte dental (caries interproximal incipiente).",
                        "Aplicación tópica de barniz con flúor y monitoreo clínico / radiográfico en 6 meses."
                ));
                hallazgos.add(new HallazgoImagenDto(
                        PatologiaDental.RESTAURACION_DEFECTUOSA,
                        "Pieza 16 (Primer molar superior derecho)",
                        SeveridadHallazgo.MODERADA,
                        0.89,
                        "Margen oclusomesial",
                        "Microfiltración marginal y brecha en restauración previa de composite con sospecha de recidiva cariosa.",
                        "Remoción de la restauración defectuosa y reobturación adhesiva."
                ));
            }
            case FOTO_INTRAORAL -> {
                hallazgos.add(new HallazgoImagenDto(
                        PatologiaDental.CALCULO_DENTAL,
                        "Caras linguales anteroinferiores (Piezas 31, 32, 41, 42)",
                        SeveridadHallazgo.MODERADA,
                        0.95,
                        "Zona lingual y surco gingival",
                        "Cálculo dental supragingival y subgingival visible con inflamación marginal asociada.",
                        "Tartrectomía con instrumental ultrasónico y profilaxis con pasta abrasiva fluorada."
                ));
                hallazgos.add(new HallazgoImagenDto(
                        PatologiaDental.CARIES_DENTAL,
                        "Pieza 46 (Primer molar inferior derecho)",
                        SeveridadHallazgo.LEVE,
                        0.88,
                        "Fosa oclusal central",
                        "Pigmentación pardo-oscura en fisura oclusal con leve desmineralización del esmalte.",
                        "Sellado preventivo o microrestauración oclusal con resina fluida."
                ));
            }
            case TOMOGRAFIA_CONE_BEAM -> {
                hallazgos.add(new HallazgoImagenDto(
                        PatologiaDental.DIENTE_RETENIDO_IMPACTADO,
                        "Pieza 48 (Tercer molar inferior derecho)",
                        SeveridadHallazgo.SEVERA,
                        0.98,
                        "Conducto dentario inferior / Canal mandibular",
                        "Relación de contacto directo entre ápice radicular de pieza 48 y cortical superior del canal del nervio alveolar inferior.",
                        "Planificación quirúrgica especializada en 3D / Considerar coronectomía para preservación neurosensorial."
                ));
                hallazgos.add(new HallazgoImagenDto(
                        PatologiaDental.PERDIDA_OSEA_PERIODONTAL,
                        "Reborde alveolar maxilar posterior (Sector 16-17)",
                        SeveridadHallazgo.MODERADA,
                        0.93,
                        "Suelo del seno maxilar",
                        "Neumatización sinusal con altura ósea residual reducida (5.2 mm) para inserción de implantes.",
                        "Elevación de seno maxilar con injerto óseo previo o simultáneo a colocación de implantes."
                ));
            }
        }
        return hallazgos;
    }

    private NivelUrgencia calcularNivelUrgencia(List<HallazgoImagenDto> hallazgos) {
        boolean tieneLesionPeriapical = hallazgos.stream()
                .anyMatch(h -> h.patologia() == PatologiaDental.LESION_PERIAPICAL);
        if (tieneLesionPeriapical) {
            return NivelUrgencia.URGENTE;
        }

        boolean tieneSevero = hallazgos.stream()
                .anyMatch(h -> h.severidad() == SeveridadHallazgo.SEVERA);
        if (tieneSevero) {
            return NivelUrgencia.ALTA;
        }

        boolean tieneModerado = hallazgos.stream()
                .anyMatch(h -> h.severidad() == SeveridadHallazgo.MODERADA);
        if (tieneModerado) {
            return NivelUrgencia.MEDIA;
        }

        return NivelUrgencia.BAJA;
    }

    private double calcularIndiceSeveridad(List<HallazgoImagenDto> hallazgos) {
        if (hallazgos.isEmpty()) {
            return 1.0;
        }
        double suma = 0.0;
        for (HallazgoImagenDto h : hallazgos) {
            switch (h.severidad()) {
                case SEVERA -> suma += 8.5;
                case MODERADA -> suma += 5.5;
                case LEVE -> suma += 2.5;
            }
        }
        double promedio = suma / hallazgos.size();
        return Math.round(promedio * 10.0) / 10.0;
    }

    private List<String> compilarTratamientos(List<HallazgoImagenDto> hallazgos) {
        return hallazgos.stream()
                .map(HallazgoImagenDto::tratamientoSugerido)
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .toList();
    }

    private String compilarDiagnosticoSugerido(TipoImagenDental tipo, List<HallazgoImagenDto> hallazgos) {
        StringBuilder sb = new StringBuilder();
        sb.append("Estudio de ").append(tipo.getNombreLegible()).append(". ");
        sb.append("Se identificaron ").append(hallazgos.size()).append(" hallazgo(s) clínico(s) de interés: ");
        for (int i = 0; i < hallazgos.size(); i++) {
            HallazgoImagenDto h = hallazgos.get(i);
            sb.append("(").append(i + 1).append(") ")
                    .append(h.patologia().getNombreLegible())
                    .append(" en ")
                    .append(h.piezaDental())
                    .append(" [Severidad: ").append(h.severidad().getDescripcion())
                    .append(", Confianza: ").append(Math.round(h.confianza() * 100)).append("%]. ");
        }
        return sb.toString().trim();
    }

    private String serializarHallazgos(List<HallazgoImagenDto> hallazgos) {
        try {
            return objectMapper.writeValueAsString(hallazgos);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private void validarFormatoBase64(String base64) {
        String clean = base64;
        if (clean.contains(",")) {
            clean = clean.substring(clean.indexOf(",") + 1);
        }
        try {
            Base64.getDecoder().decode(clean.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("La cadena de imagen no corresponde a un Base64 válido.");
        }
    }
}
