package com.proyectohoussay.odonto.clinicalhistory.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyectohoussay.odonto.clinicalhistory.DiagnosticoRepository;
import com.proyectohoussay.odonto.clinicalhistory.HistoriaClinicaRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service orchestrating dental AI image analysis, triage urgency assessment,
 * and direct integration into the patient's clinical history.
 */
@Service
public class DiagnosticoImagenService {

    public static final String MODELO_ACTUAL = "DR. ANNIE Insights API";

    private final DiagnosticoImagenRepository diagnosticoImagenRepository;
    private final AnnieDentalVisionClient visionClient;
    private final HistoriaClinicaRepository historiaClinicaRepository;
    @Autowired(required = false)
    private DentalImageStorage imageStorage;
    @Autowired
    public DiagnosticoImagenService(
            DiagnosticoImagenRepository diagnosticoImagenRepository,
            HistoriaClinicaRepository historiaClinicaRepository,
            DiagnosticoRepository diagnosticoRepository,
            ObjectMapper objectMapper,
            AnnieDentalVisionClient visionClient
    ) {
        this.diagnosticoImagenRepository = diagnosticoImagenRepository;
        this.visionClient = visionClient;
        this.historiaClinicaRepository = historiaClinicaRepository;
    }

    /** Legacy dependencies are retained for existing test and application wiring. */
    public DiagnosticoImagenService(DiagnosticoImagenRepository diagnosticoImagenRepository,
                                   HistoriaClinicaRepository historiaClinicaRepository,
                                   DiagnosticoRepository diagnosticoRepository,
                                   ObjectMapper objectMapper) {
        this.diagnosticoImagenRepository = diagnosticoImagenRepository;
        this.visionClient = null;
        this.historiaClinicaRepository = historiaClinicaRepository;
    }

    @Transactional
    public AnalisisImagenResponse analizarImagen(AnalisisImagenRequest request) {
        if (request.tipoImagen() == null) {
            throw new IllegalArgumentException("El tipo de imagen dental es obligatorio.");
        }
        if (request.tipoImagen() != TipoImagenDental.RADIOGRAFIA_PANORAMICA) {
            throw new IllegalArgumentException("DR. ANNIE está integrado para radiografías panorámicas. Las demás modalidades aún no están habilitadas.");
        }
        validarImagenUrl(request.imagenUrl());
        if (visionClient == null) {
            throw new IllegalStateException("El proveedor DR. ANNIE no está disponible en esta instancia.");
        }

        String referenciaPaciente = "study-" + java.util.UUID.randomUUID();
        var resultado = visionClient.analyzePanoramic(request.imagenUrl(), referenciaPaciente, LocalDate.now());
        var resultados = resultado.path("Results");
        int totalDetecciones = 0;
        if (resultados.isObject()) {
            var values = resultados.elements();
            while (values.hasNext()) {
                var value = values.next();
                totalDetecciones += value.isArray() ? value.size() : (value.isNumber() ? value.asInt() : 0);
            }
        }
        String resumen = totalDetecciones > 0
                ? "DR. ANNIE devolvió " + totalDetecciones + " detección(es) para revisión profesional."
                : "DR. ANNIE procesó la imagen sin devolver detecciones estructuradas.";
        String observaciones = "Resultado automatizado de apoyo. No es un diagnóstico definitivo ni determina si corresponden brackets o placas. "
                + "Debe revisarlo un odontólogo, idealmente especialista en Ortodoncia.";
        String output = resultado.toString();
        Long historiaId = request.historiaClinicaId();
        if (request.guardarEnHistoria() && historiaId == null && request.pacienteId() != null) {
            historiaId = historiaClinicaRepository.findByPacienteId(request.pacienteId())
                    .orElseGet(() -> historiaClinicaRepository.save(new com.proyectohoussay.odonto.clinicalhistory.HistoriaClinica(
                            LocalDate.now(), request.pacienteId(), "Historia clínica creada desde el análisis de imagen dental.")))
                    .getId();
        }

        DiagnosticoImagen registro = new DiagnosticoImagen(
                LocalDateTime.now(), request.pacienteId(), historiaId, request.tipoImagen(),
                request.nombreArchivo() == null ? "radiografia-panoramica" : request.nombreArchivo(),
                resumen, NivelUrgencia.NO_EVALUADA, 0.0, "{\"detecciones\":" + totalDetecciones + "}", "", request.observacionesOdontologo(),
                observaciones, MODELO_ACTUAL, null);
        var guardado = diagnosticoImagenRepository.save(registro);
        return new AnalisisImagenResponse(guardado.getId(), request.tipoImagen(), resumen, List.of(), List.of(),
                NivelUrgencia.NO_EVALUADA, 0.0, observaciones, MODELO_ACTUAL, guardado.getFechaAnalisis(),
                request.pacienteId(), historiaId, null, resultado);
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
        return analizarArchivo(archivo, tipoImagen, pacienteId, historiaClinicaId, observaciones, guardarEnHistoria, null);
    }

    @Transactional
    public AnalisisImagenResponse analizarArchivo(
            MultipartFile archivo,
            TipoImagenDental tipoImagen,
            Long pacienteId,
            Long historiaClinicaId,
            String observaciones,
            boolean guardarEnHistoria,
            String imagenUrl
    ) {
        if ((archivo == null || archivo.isEmpty()) && (imagenUrl == null || imagenUrl.isBlank())) {
            throw new IllegalArgumentException("Proporcione un archivo o imagenUrl HTTPS accesible por DR. ANNIE.");
        }

        String contentType = archivo == null ? null : archivo.getContentType();
        boolean dicomFile = archivo != null && archivo.getOriginalFilename() != null
                && archivo.getOriginalFilename().toLowerCase().matches(".*\\.(dcm|dicom)$");
        if (archivo != null && contentType != null && !contentType.isBlank()
                && !contentType.startsWith("image/") && !contentType.equals("application/dicom")
                && !(contentType.equals("application/octet-stream") && dicomFile)) {
            throw new IllegalArgumentException("Formato no soportado: " + contentType + ". Ingrese una imagen médica dental válida (PNG, JPEG, WebP, DICOM).");
        }

        if (archivo != null && archivo.getSize() == 0) throw new IllegalArgumentException("El archivo de radiografía está vacío.");
        if (archivo != null && archivo.getSize() > 25L * 1024 * 1024) throw new IllegalArgumentException("La radiografía supera el máximo de 25 MB.");
        String objectKey = null;
        String effectiveUrl = imagenUrl;
        if ((effectiveUrl == null || effectiveUrl.isBlank()) && archivo != null) {
            if (imageStorage == null) {
                throw new IllegalStateException("El almacenamiento privado no está configurado. Configure S3_STORAGE_ENABLED y las variables S3_* para analizar archivos locales.");
            }
            var upload = imageStorage.uploadForAnalysis(archivo);
            effectiveUrl = upload.presignedUrl();
            objectKey = upload.objectKey();
        }

        AnalisisImagenRequest request = new AnalisisImagenRequest(
                tipoImagen,
                null,
                pacienteId,
                historiaClinicaId,
                observaciones,
                guardarEnHistoria,
                archivo == null ? null : archivo.getOriginalFilename(),
                effectiveUrl
        );
        try {
            return analizarImagen(request);
        } finally {
            if (objectKey != null) imageStorage.deleteAfterAnalysis(objectKey);
        }
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
                CatalogoIaDto.UrgenciaInfo.de(NivelUrgencia.NO_EVALUADA),
                CatalogoIaDto.UrgenciaInfo.de(NivelUrgencia.BAJA),
                CatalogoIaDto.UrgenciaInfo.de(NivelUrgencia.MEDIA),
                CatalogoIaDto.UrgenciaInfo.de(NivelUrgencia.ALTA),
                CatalogoIaDto.UrgenciaInfo.de(NivelUrgencia.URGENTE)
        );

        return new CatalogoIaDto(tipos, patologias, urgencias, MODELO_ACTUAL,
                visionClient == null ? "NO_CONFIGURADO" : visionClient.estadoServicio(),
                imageStorage != null);
    }

    private void validarImagenUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("Se requiere imagenUrl HTTPS accesible por DR. ANNIE. El proveedor aún no admite carga directa de archivos en esta integración.");
        }
        try {
            URI uri = URI.create(imageUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new IllegalArgumentException("imagenUrl debe ser una URL HTTPS válida y sin credenciales embebidas.");
            }
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("imagenUrl")) throw e;
            throw new IllegalArgumentException("imagenUrl debe ser una URL HTTPS válida.");
        }
    }

}
