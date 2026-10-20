package com.proyectohoussay.odonto.odontogram;

import com.proyectohoussay.odonto.patient.PacienteNoEncontradoException;
import com.proyectohoussay.odonto.patient.PacienteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.HashSet;
import java.util.List;

@Service
public class OdontogramaService {
    private final OdontogramaRepository repository;
    private final PacienteRepository patients;
    public OdontogramaService(OdontogramaRepository repository, PacienteRepository patients) {
        this.repository = repository; this.patients = patients;
    }

    @Transactional
    public OdontogramaResponse crear(OdontogramaRequest request, Long creator) {
        if (!OdontogramaCatalogo.VERSION.equals(request.versionConsentimiento()))
            throw new IllegalArgumentException("La versión del consentimiento no está vigente");
        var seen = new HashSet<Integer>();
        var teeth = request.dientes().stream().map(tooth -> {
            int n = tooth.numero();
            if (n / 10 < 1 || n / 10 > 4 || n % 10 < 1 || n % 10 > 8)
                throw new IllegalArgumentException("La numeración FDI no es válida");
            if (!seen.add(n)) throw new IllegalArgumentException("No se puede repetir un diente");
            return new OdontogramaDiente(n, OdontogramaCatalogo.buscar(tooth.afeccion()));
        }).toList();
        FirmaPng.validar(request.firmaBase64());
        var patient = patients.findLockedById(request.pacienteId())
                .orElseThrow(() -> new PacienteNoEncontradoException(request.pacienteId()));
        return repository.saveAndFlush(new Odontograma(patient, creator, teeth, request.firmaBase64())).response();
    }
    @Transactional(readOnly = true)
    public OdontogramaResponse obtener(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Odontograma no encontrado")).response();
    }
    @Transactional(readOnly = true)
    public List<OdontogramaResumen> listar(Long pacienteId) {
        if (pacienteId == null || pacienteId <= 0) throw new IllegalArgumentException("El ID de paciente debe ser positivo");
        if (!patients.existsById(pacienteId)) throw new PacienteNoEncontradoException(pacienteId);
        return repository.resumenes(pacienteId);
    }
}
