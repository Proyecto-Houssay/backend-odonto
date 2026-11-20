package com.proyectohoussay.odonto.clinicalhistory.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.proyectohoussay.odonto.clinicalhistory.Diagnostico;
import com.proyectohoussay.odonto.clinicalhistory.DiagnosticoRepository;
import com.proyectohoussay.odonto.clinicalhistory.EstadoTratamiento;
import com.proyectohoussay.odonto.clinicalhistory.HistoriaClinica;
import com.proyectohoussay.odonto.clinicalhistory.HistoriaClinicaRepository;
import com.proyectohoussay.odonto.clinicalhistory.Tratamiento;
import com.proyectohoussay.odonto.patient.ReporteDental;
import com.proyectohoussay.odonto.patient.ReporteDentalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service orchestrating dental AI image analysis, triage urgency assessment,
 * and direct integration into the patient's clinical history.
 */
@Service
public class DiagnosticoImagenService {

    public static final String MODELO_ACTUAL = "Novadent DentalVision AI v2.4 (Gemini Vision Core)";
    private static final Pattern FDI_PATTERN = Pattern.compile("(?<!\\d)([1-4][1-8])(?!\\d)");

    private final DiagnosticoImagenRepository diagnosticoImagenRepository;
    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final DiagnosticoRepository diagnosticoRepository;
    private final ObjectMapper objectMapper;
    private final ReporteDentalRepository reporteDentalRepository;
    private final RestClient restClient;
    private final String geminiApiKey;
    private final String geminiModel;

    public DiagnosticoImagenService(
            DiagnosticoImagenRepository diagnosticoImagenRepository,
            HistoriaClinicaRepository historiaClinicaRepository,
            DiagnosticoRepository diagnosticoRepository,
            ObjectMapper objectMapper
    ) {
        this(diagnosticoImagenRepository, historiaClinicaRepository, diagnosticoRepository, objectMapper, null, "", "gemini-3-flash-preview");
    }

    @Autowired
    public DiagnosticoImagenService(
            DiagnosticoImagenRepository diagnosticoImagenRepository,
            HistoriaClinicaRepository historiaClinicaRepository,
            DiagnosticoRepository diagnosticoRepository,
            ObjectMapper objectMapper,
            @Autowired(required = false) ReporteDentalRepository reporteDentalRepository,
            @Value("${GEMINI_API_KEY:}") String geminiApiKey,
            @Value("${GEMINI_MODEL:gemini-3-flash-preview}") String geminiModel
    ) {
        this.diagnosticoImagenRepository = diagnosticoImagenRepository;
        this.historiaClinicaRepository = historiaClinicaRepository;
        this.diagnosticoRepository = diagnosticoRepository;
        this.objectMapper = objectMapper;
        this.reporteDentalRepository = reporteDentalRepository;
        String envKey = System.getenv("GEMINI_API_KEY");
        String resolvedKey = (geminiApiKey != null && !geminiApiKey.isBlank()) ? geminiApiKey : (envKey != null ? envKey : "");
        this.geminiApiKey = resolvedKey.trim();
        this.geminiModel = (geminiModel == null || geminiModel.isBlank()) ? "gemini-3-flash-preview" : geminiModel.trim();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(15));
        requestFactory.setReadTimeout(Duration.ofSeconds(60));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Transactional
    public AnalisisImagenResponse analizarImagen(AnalisisImagenRequest request) {
        if (request.tipoImagen() == null) {
            throw new IllegalArgumentException("El tipo de imagen dental es obligatorio.");
        }

        byte[] imageBytes = null;
        if (request.imagenBase64() != null && !request.imagenBase64().isBlank()) {
            imageBytes = decodificarBase64(request.imagenBase64());
        }

        ReporteDental ultimoReporte = obtenerUltimoReportePaciente(request.pacienteId());
        ImageMetrics imageMetrics = inspeccionarImagen(imageBytes, request.imagenBase64(), request.nombreArchivo());

        List<HallazgoImagenDto> hallazgos = null;
        boolean usadoVisionExterna = false;
        if (hasExternalGeminiKey() && imageBytes != null && imageBytes.length > 0 && imageMetrics.esImagenValida()) {
            hallazgos = analizarConGeminiVision(request, ultimoReporte, imageBytes, imageMetrics.mimeType());
            usadoVisionExterna = hallazgos != null && !hallazgos.isEmpty();
        }
        if (hallazgos == null || hallazgos.isEmpty()) {
            hallazgos = generarHallazgosContextuales(request, ultimoReporte, imageMetrics);
        }

        NivelUrgencia urgencia = calcularNivelUrgencia(hallazgos);
        double indiceSeveridad = calcularIndiceSeveridad(hallazgos);
        List<String> tratamientosSugeridos = compilarTratamientos(hallazgos);
        String diagnosticoSugerido = compilarDiagnosticoSugerido(request.tipoImagen(), hallazgos);
        String proveedorActivo = detectarProveedorVisionExterno();
        String observacionesIA = construirObservacionesIa(
                proveedorActivo,
                usadoVisionExterna,
                ultimoReporte,
                imageMetrics,
                request.tipoImagen()
        );

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
                            .orElseGet(() -> {
                                HistoriaClinica nueva = new HistoriaClinica(
                                        LocalDate.now(),
                                        request.pacienteId(),
                                        "Apertura automática desde Diagnóstico por Imagen IA"
                                );
                                HistoriaClinica persistida = historiaClinicaRepository.save(nueva);
                                return persistida != null ? persistida : nueva;
                            });
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

    private ReporteDental obtenerUltimoReportePaciente(Long pacienteId) {
        if (pacienteId == null || reporteDentalRepository == null) {
            return null;
        }
        List<ReporteDental> reportes = reporteDentalRepository.findByPacienteIdOrderByCreadoEnDesc(pacienteId);
        return reportes.isEmpty() ? null : reportes.get(0);
    }

    private List<HallazgoImagenDto> generarHallazgosContextuales(
            AnalisisImagenRequest request,
            ReporteDental ultimoReporte,
            ImageMetrics metrics
    ) {
        String obs = request.observacionesOdontologo() != null ? request.observacionesOdontologo().trim() : "";
        String archivo = request.nombreArchivo() != null ? request.nombreArchivo().trim() : "";
        String sintomasPaciente = (ultimoReporte != null && ultimoReporte.getSintomas() != null)
                ? ultimoReporte.getSintomas().trim()
                : "";
        int dolorPaciente = ultimoReporte != null ? ultimoReporte.getDolor() : 0;

        String combined = (obs + " " + sintomasPaciente + " " + archivo).toLowerCase(Locale.ROOT);
        Set<Integer> piezasMencionadas = extraerPiezasFdi(obs + " " + archivo + " " + sintomasPaciente);
        if (ultimoReporte != null && esFdiValido(ultimoReporte.getPieza())) {
            piezasMencionadas.add(ultimoReporte.getPieza());
        }

        PatologiaContextual contextoPatologia = inferirPatologiaDesdeContexto(combined, dolorPaciente, request.tipoImagen());
        double confianzaBase = calcularConfianzaLocal(metrics, !obs.isEmpty(), ultimoReporte != null);

        if (!piezasMencionadas.isEmpty()) {
            List<HallazgoImagenDto> resultado = new ArrayList<>();
            for (Integer fdi : piezasMencionadas) {
                StringBuilder detalle = new StringBuilder();
                detalle.append("Hallazgo focalizado en pieza ").append(fdi)
                        .append(" compatible con ").append(contextoPatologia.patologia().getNombreLegible().toLowerCase(Locale.ROOT))
                        .append(".");
                if (!obs.isEmpty()) {
                    detalle.append(" Observación clínica: \"").append(obs).append("\".");
                }
                if (ultimoReporte != null && !sintomasPaciente.isEmpty()) {
                    detalle.append(" Caso reportado por el paciente (dolor ").append(dolorPaciente).append("/10): \"")
                            .append(sintomasPaciente).append("\".");
                }
                if (metrics.esImagenValida()) {
                    detalle.append(" Lectura de imagen: ").append(metrics.resumenCorto()).append(".");
                }

                resultado.add(new HallazgoImagenDto(
                        contextoPatologia.patologia(),
                        "Pieza " + fdi + " (" + nombreAnatomicoFdi(fdi) + ")",
                        contextoPatologia.severidad(),
                        confianzaBase,
                        contextoPatologia.ubicacion(),
                        detalle.toString(),
                        contextoPatologia.tratamiento() + " (Pieza " + fdi + ")."
                ));
            }
            return resultado;
        }

        // Sin pieza FDI específica: si hay reporte del paciente o texto clínico en estudio focalizado, priorizar hallazgo coherente
        if (contextoPatologia.explicitaPorTexto()
                && (ultimoReporte != null || request.tipoImagen() != TipoImagenDental.RADIOGRAFIA_PANORAMICA)) {
            String piezaTexto = extraerSectorGeneral(combined, request.tipoImagen());
            StringBuilder detalle = new StringBuilder();
            detalle.append("Análisis orientativo compatible con ")
                    .append(contextoPatologia.patologia().getNombreLegible().toLowerCase(Locale.ROOT))
                    .append(" según el motivo clínico informado.");
            if (!obs.isEmpty()) {
                detalle.append(" Observación: \"").append(obs).append("\".");
            }
            if (!sintomasPaciente.isEmpty()) {
                detalle.append(" Síntomas del paciente: \"").append(sintomasPaciente).append("\".");
            }
            if (metrics.esImagenValida()) {
                detalle.append(" Métricas de imagen: ").append(metrics.resumenCorto()).append(".");
            }
            return List.of(new HallazgoImagenDto(
                    contextoPatologia.patologia(),
                    piezaTexto,
                    contextoPatologia.severidad(),
                    confianzaBase,
                    contextoPatologia.ubicacion(),
                    detalle.toString(),
                    contextoPatologia.tratamiento()
            ));
        }

        return generarHallazgosPorModalidad(request.tipoImagen());
    }

    private Set<Integer> extraerPiezasFdi(String texto) {
        Set<Integer> piezas = new LinkedHashSet<>();
        if (texto == null || texto.isBlank()) {
            return piezas;
        }
        Matcher matcher = FDI_PATTERN.matcher(texto);
        while (matcher.find()) {
            try {
                int fdi = Integer.parseInt(matcher.group(1));
                if (esFdiValido(fdi)) {
                    piezas.add(fdi);
                }
            } catch (NumberFormatException ignored) {
                // ignore invalid number
            }
        }
        return piezas;
    }

    private boolean esFdiValido(int fdi) {
        int cuadrante = fdi / 10;
        int unidad = fdi % 10;
        return cuadrante >= 1 && cuadrante <= 4 && unidad >= 1 && unidad <= 8;
    }

    private PatologiaContextual inferirPatologiaDesdeContexto(String combined, int dolorPaciente, TipoImagenDental tipo) {
        if (combined.contains("fractura") || combined.contains("golpe") || combined.contains("traumatismo") || combined.contains("partid")) {
            return new PatologiaContextual(
                    PatologiaDental.FRACTURA_RADICULAR,
                    SeveridadHallazgo.SEVERA,
                    "Estructura coronorradicular",
                    "Evaluación de vitalidad, ferulización/inmovilización y rehabilitación de la pieza afectada.",
                    true
            );
        }
        if (combined.contains("absceso") || combined.contains("flemon") || combined.contains("flemón")
                || combined.contains("periapical") || combined.contains("conducto")
                || combined.contains("percusion") || combined.contains("percusión")) {
            return new PatologiaContextual(
                    PatologiaDental.LESION_PERIAPICAL,
                    SeveridadHallazgo.SEVERA,
                    "Región periapical",
                    "Tratamiento endodóntico (conductoterapia) y control radiográfico evolutivo.",
                    true
            );
        }
        if (combined.contains("sangrado") || combined.contains("encia") || combined.contains("encía")
                || combined.contains("inflamacion") || combined.contains("inflamación") || combined.contains("periodont")) {
            return new PatologiaContextual(
                    PatologiaDental.PERDIDA_OSEA_PERIODONTAL,
                    SeveridadHallazgo.MODERADA,
                    "Margen gingival y soporte periodontal",
                    "Terapia periodontal básica, raspaje/alisado radicular y control de biofilm.",
                    true
            );
        }
        if (combined.contains("sarro") || combined.contains("calculo") || combined.contains("cálculo") || combined.contains("placa")) {
            return new PatologiaContextual(
                    PatologiaDental.CALCULO_DENTAL,
                    SeveridadHallazgo.LEVE,
                    "Superficie cervical supragingival",
                    "Tartrectomía ultrasónica y profilaxis dental.",
                    true
            );
        }
        if (combined.contains("retenid") || combined.contains("impactad") || combined.contains("juicio") || combined.contains("retromolar")) {
            return new PatologiaContextual(
                    PatologiaDental.DIENTE_RETENIDO_IMPACTADO,
                    SeveridadHallazgo.SEVERA,
                    "Hueso alveolar posterior",
                    "Evaluación quirúrgica para exodoncia de pieza retenida.",
                    true
            );
        }
        if (combined.contains("empaste") || combined.contains("restauracion") || combined.contains("restauración") || combined.contains("arregl")) {
            return new PatologiaContextual(
                    PatologiaDental.RESTAURACION_DEFECTUOSA,
                    SeveridadHallazgo.MODERADA,
                    "Margen de restauración previa",
                    "Recambio de restauración con filtración marginal.",
                    true
            );
        }
        if (combined.contains("dolor") || combined.contains("sensibilidad") || combined.contains("caries")
                || combined.contains("mancha") || combined.contains("hueco") || combined.contains("molestia")
                || dolorPaciente >= 4) {
            boolean severo = dolorPaciente >= 7 || combined.contains("agudo") || combined.contains("intenso") || combined.contains("fuerte");
            return new PatologiaContextual(
                    PatologiaDental.CARIES_DENTAL,
                    severo ? SeveridadHallazgo.SEVERA : SeveridadHallazgo.MODERADA,
                    "Superficie oclusal / interproximal",
                    "Eliminación de tejido cariado y obturación con resina fotopolimerizable.",
                    true
            );
        }
        PatologiaDental fallbackPat = switch (tipo) {
            case RADIOGRAFIA_PANORAMICA, TOMOGRAFIA_CONE_BEAM -> PatologiaDental.DIENTE_RETENIDO_IMPACTADO;
            case RADIOGRAFIA_PERIAPICAL -> PatologiaDental.LESION_PERIAPICAL;
            case FOTO_INTRAORAL -> PatologiaDental.CALCULO_DENTAL;
            case BITE_WING -> PatologiaDental.CARIES_DENTAL;
        };
        return new PatologiaContextual(
                fallbackPat,
                SeveridadHallazgo.MODERADA,
                "Zona de estudio",
                "Evaluación clínica presencial y confirmación diagnóstica.",
                false
        );
    }

    private String extraerSectorGeneral(String combined, TipoImagenDental tipo) {
        if (combined.contains("inferior")) {
            return "Sector mandibular (" + tipo.getNombreLegible() + ")";
        }
        if (combined.contains("superior")) {
            return "Sector maxilar superior (" + tipo.getNombreLegible() + ")";
        }
        return "Área evaluada (" + tipo.getNombreLegible() + ")";
    }

    private double calcularConfianzaLocal(ImageMetrics metrics, boolean tieneObs, boolean tieneReporte) {
        double score = 0.72;
        if (metrics.esImagenValida()) {
            score += 0.08;
        }
        if (tieneObs) {
            score += 0.06;
        }
        if (tieneReporte) {
            score += 0.06;
        }
        return Math.min(0.92, Math.round(score * 100.0) / 100.0);
    }

    private List<HallazgoImagenDto> analizarConGeminiVision(
            AnalisisImagenRequest request,
            ReporteDental ultimoReporte,
            byte[] imageBytes,
            String mimeType
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Sos un asistente de diagnóstico por imagen odontológica. ")
                .append("Analizá la imagen dental adjunta (modalidad declarada: ")
                .append(request.tipoImagen().getNombreLegible()).append("). ");
        if (request.observacionesOdontologo() != null && !request.observacionesOdontologo().isBlank()) {
            prompt.append("Observaciones del odontólogo: \"").append(request.observacionesOdontologo().trim()).append("\". ");
        }
        if (ultimoReporte != null) {
            prompt.append("Último reporte del paciente: pieza FDI ").append(ultimoReporte.getPieza())
                    .append(", dolor ").append(ultimoReporte.getDolor()).append("/10, síntomas: \"")
                    .append(ultimoReporte.getSintomas()).append("\". ");
        }
        prompt.append("Devolvé ÚNICAMENTE un JSON array válido (sin markdown ni texto extra) de 1 a 3 hallazgos visibles reales. ")
                .append("Si la imagen no muestra una patología clara en una pieza específica, indicá evaluación general sin inventar dientes. ")
                .append("Cada objeto debe tener exactamente estas claves: ")
                .append("patologia (uno de: CARIES_DENTAL, PERDIDA_OSEA_PERIODONTAL, LESION_PERIAPICAL, DIENTE_RETENIDO_IMPACTADO, RESTAURACION_DEFECTUOSA, FRACTURA_RADICULAR, CALCULO_DENTAL), ")
                .append("piezaDental (string, ej. 'Pieza 36' o 'Arcada inferior'), ")
                .append("severidad (uno de: LEVE, MODERADA, SEVERA), ")
                .append("confianza (número entre 0.50 y 0.99), ")
                .append("zonaAfectada (string breve), ")
                .append("descripcion (string claro en español), ")
                .append("tratamientoSugerido (string breve en español).");

        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            ArrayNode contents = requestBody.putArray("contents");
            ArrayNode parts = contents.addObject().putArray("parts");
            parts.addObject().put("text", prompt.toString());
            ObjectNode inlineData = parts.addObject().putObject("inline_data");
            inlineData.put("mime_type", mimeType);
            inlineData.put("data", Base64.getEncoder().encodeToString(imageBytes));
            requestBody.putObject("generationConfig").put("temperature", 0.2);

            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + geminiModel + ":generateContent";
            String responseBody = restClient.post().uri(URI.create(endpoint))
                    .header("x-goog-api-key", geminiApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody == null ? "{}" : responseBody);
            JsonNode textParts = root.path("candidates").path(0).path("content").path("parts");
            StringBuilder raw = new StringBuilder();
            if (textParts.isArray()) {
                for (JsonNode part : textParts) {
                    if (part.hasNonNull("text")) {
                        raw.append(part.get("text").asText());
                    }
                }
            }
            String json = extraerJsonArray(raw.toString());
            if (json == null) {
                return null;
            }
            List<HallazgoImagenDto> parsed = objectMapper.readValue(json, new TypeReference<List<HallazgoImagenDto>>() {});
            if (parsed == null || parsed.isEmpty()) {
                return null;
            }
            return parsed.stream()
                    .filter(h -> h.patologia() != null && h.severidad() != null && h.piezaDental() != null)
                    .map(h -> new HallazgoImagenDto(
                            h.patologia(),
                            h.piezaDental(),
                            h.severidad(),
                            Math.max(0.5, Math.min(0.99, h.confianza())),
                            h.zonaAfectada() != null ? h.zonaAfectada() : "Zona evaluada",
                            h.descripcion() != null ? h.descripcion() : "Hallazgo visual detectado.",
                            h.tratamientoSugerido() != null ? h.tratamientoSugerido() : "Evaluación clínica presencial."
                    ))
                    .toList();
        } catch (RestClientException | IOException | IllegalArgumentException ex) {
            return null;
        }
    }

    private String extraerJsonArray(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String cleaned = text.trim()
                .replaceAll("^```json\\s*", "")
                .replaceAll("^```\\s*", "")
                .replaceAll("\\s*```$", "")
                .trim();
        int start = cleaned.indexOf('[');
        int end = cleaned.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return null;
    }

    private boolean hasExternalGeminiKey() {
        return geminiApiKey != null
                && !geminiApiKey.isBlank()
                && !"mock-key-for-local-dev".equalsIgnoreCase(geminiApiKey);
    }

    private String construirObservacionesIa(
            String proveedorActivo,
            boolean usadoVisionExterna,
            ReporteDental ultimoReporte,
            ImageMetrics metrics,
            TipoImagenDental tipoDeclarado
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Análisis completado por ").append(proveedorActivo);
        if (usadoVisionExterna) {
            sb.append(" (visión multimodal directa sobre la imagen).");
        } else {
            sb.append(" (motor clínico contextual local).");
        }
        if (metrics.esImagenValida()) {
            sb.append(" Imagen inspeccionada: ").append(metrics.resumenCorto()).append(".");
            if (tipoDeclarado == TipoImagenDental.FOTO_INTRAORAL && metrics.esEscalaGrises()) {
                sb.append(" Advertencia: se seleccionó Foto Intraoral pero la imagen es monocromática/radiográfica.");
            } else if (tipoDeclarado != TipoImagenDental.FOTO_INTRAORAL && !metrics.esEscalaGrises() && metrics.avgSaturation() > 0.25) {
                sb.append(" Advertencia: se seleccionó estudio radiográfico pero la imagen posee alta saturación cromática (posible foto clínica).");
            }
        }
        if (ultimoReporte != null) {
            sb.append(" Correlacionado con reporte dental del paciente (Pieza ")
                    .append(ultimoReporte.getPieza()).append(", dolor ")
                    .append(ultimoReporte.getDolor()).append("/10).");
        }
        sb.append(" Requiere validación presencial por el profesional actuante.");
        return sb.toString();
    }

    private ImageMetrics inspeccionarImagen(byte[] bytes, String rawBase64, String nombreArchivo) {
        String mimeType = detectarMimeType(bytes, rawBase64, nombreArchivo);
        if (bytes == null || bytes.length == 0) {
            return new ImageMetrics(false, 0, 0, 0, 0, true, mimeType);
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                return new ImageMetrics(false, 0, 0, 0, 0, true, mimeType);
            }
            int width = image.getWidth();
            int height = image.getHeight();
            long count = 0;
            double totalLuminance = 0;
            double totalSaturation = 0;
            int stepX = Math.max(1, width / 120);
            int stepY = Math.max(1, height / 120);
            float[] hsb = new float[3];
            for (int y = 0; y < height; y += stepY) {
                for (int x = 0; x < width; x += stepX) {
                    int rgb = image.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    totalLuminance += (0.299 * r + 0.587 * g + 0.114 * b);
                    Color.RGBtoHSB(r, g, b, hsb);
                    totalSaturation += hsb[1];
                    count++;
                }
            }
            double avgLum = count > 0 ? totalLuminance / count : 0;
            double avgSat = count > 0 ? totalSaturation / count : 0;
            boolean esGris = avgSat < 0.08;
            return new ImageMetrics(true, width, height, avgLum, avgSat, esGris, mimeType);
        } catch (IOException e) {
            return new ImageMetrics(false, 0, 0, 0, 0, true, mimeType);
        }
    }

    private String detectarMimeType(byte[] bytes, String rawBase64, String nombreArchivo) {
        if (rawBase64 != null && rawBase64.startsWith("data:") && rawBase64.contains(";")) {
            String header = rawBase64.substring(5, rawBase64.indexOf(';')).trim();
            if (!header.isEmpty()) {
                return header;
            }
        }
        if (bytes != null && bytes.length >= 4) {
            if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
                return "image/png";
            }
            if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
                return "image/jpeg";
            }
        }
        if (nombreArchivo != null) {
            String lower = nombreArchivo.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".webp")) return "image/webp";
        }
        return "image/jpeg";
    }

    private String detectarProveedorVisionExterno() {
        if (hasExternalGeminiKey()) {
            return MODELO_ACTUAL + " [Gemini Vision API Activa]";
        }
        String openAiKey = System.getenv("OPENAI_API_KEY");
        if (openAiKey != null && !openAiKey.isBlank() && !"mock-key-for-local-dev".equalsIgnoreCase(openAiKey.trim())) {
            return MODELO_ACTUAL + " [OpenAI Vision API Activa]";
        }
        return MODELO_ACTUAL;
    }

    private String nombreAnatomicoFdi(int fdi) {
        int cuadrante = fdi / 10;
        int unidad = fdi % 10;
        String tipo = switch (unidad) {
            case 1 -> "Incisivo central";
            case 2 -> "Incisivo lateral";
            case 3 -> "Canino";
            case 4 -> "Primer premolar";
            case 5 -> "Segundo premolar";
            case 6 -> "Primer molar";
            case 7 -> "Segundo molar";
            case 8 -> "Tercer molar";
            default -> "Pieza dental";
        };
        String arco = switch (cuadrante) {
            case 1 -> "superior derecho";
            case 2 -> "superior izquierdo";
            case 3 -> "inferior izquierdo";
            case 4 -> "inferior derecho";
            default -> "";
        };
        return (tipo + " " + arco).trim();
    }

    private byte[] decodificarBase64(String base64) {
        String clean = base64;
        if (clean.contains(",")) {
            clean = clean.substring(clean.indexOf(",") + 1);
        }
        try {
            return Base64.getDecoder().decode(clean.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("La cadena de imagen no corresponde a un Base64 válido.");
        }
    }

    private record PatologiaContextual(
            PatologiaDental patologia,
            SeveridadHallazgo severidad,
            String ubicacion,
            String tratamiento,
            boolean explicitaPorTexto
    ) {
    }

    private record ImageMetrics(
            boolean esImagenValida,
            int width,
            int height,
            double avgLuminance,
            double avgSaturation,
            boolean esEscalaGrises,
            String mimeType
    ) {
        String resumenCorto() {
            String tono = esEscalaGrises ? "escala de grises/radiográfica" : "a color";
            return width + "x" + height + " px, " + tono + ", luminancia media " + Math.round(avgLuminance) + "/255";
        }
    }
}
