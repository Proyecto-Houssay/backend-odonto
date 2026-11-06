package com.proyectohoussay.odonto.patient;

import com.proyectohoussay.odonto.clinicalhistory.TratamientoRepository;
import com.proyectohoussay.odonto.repository.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PacienteActividadService {
    private final PacienteCuentaService cuentas;
    private final TurnoRepository turnos;
    private final TratamientoRepository tratamientos;

    public PacienteActividadService(PacienteCuentaService cuentas, TurnoRepository turnos,
                                    TratamientoRepository tratamientos) {
        this.cuentas = cuentas;
        this.turnos = turnos;
        this.tratamientos = tratamientos;
    }

    @Transactional(readOnly = true)
    public List<TurnoPropioResponse> turnosPropios(Long userId) {
        Long pacienteId = cuentas.pacientePropio(userId).getId();
        return turnos.findByPacienteIdOrderByFechaAscHoraAscIdAsc(pacienteId).stream()
                .map(TurnoPropioResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public List<TratamientoPropioResponse> tratamientosPropios(Long userId) {
        Long pacienteId = cuentas.pacientePropio(userId).getId();
        return tratamientos.findByPacienteIdNewestFirst(pacienteId).stream()
                .map(TratamientoPropioResponse::de).toList();
    }
}
