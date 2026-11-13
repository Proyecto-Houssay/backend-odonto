package com.proyectohoussay.odonto.odontogram;

import com.proyectohoussay.odonto.patient.PacienteCuentaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PacienteOdontogramaService {
    private final PacienteCuentaService cuentas;
    private final OdontogramaRepository odontogramas;

    public PacienteOdontogramaService(PacienteCuentaService cuentas, OdontogramaRepository odontogramas) {
        this.cuentas = cuentas;
        this.odontogramas = odontogramas;
    }

    @Transactional(readOnly = true)
    public PacienteOdontogramaResponse propio(Long userId) {
        Long pacienteId = cuentas.pacientePropio(userId).getId();
        return odontogramas.findFirstByPacienteIdOrderByCreadoEnDescIdDesc(pacienteId)
                .map(Odontograma::respuestaPaciente)
                .orElseGet(() -> new PacienteOdontogramaResponse(null, List.of()));
    }
}
