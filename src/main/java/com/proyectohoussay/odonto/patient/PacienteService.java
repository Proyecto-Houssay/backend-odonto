package com.proyectohoussay.odonto.patient;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import com.proyectohoussay.odonto.clinicalhistory.HistoriaClinicaRepository;
import com.proyectohoussay.odonto.repository.TurnoRepository;
import com.proyectohoussay.odonto.odontogram.OdontogramaRepository;

import java.util.List;

@Service
public class PacienteService {

    private static final String DUPLICATE_DNI_MESSAGE = "Ya existe un paciente registrado con ese DNI";

    private final PacienteRepository pacienteRepository;
    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final TurnoRepository turnoRepository;
    private final OdontogramaRepository odontogramaRepository;

    public PacienteService(PacienteRepository pacienteRepository,
                           HistoriaClinicaRepository historiaClinicaRepository,
                           TurnoRepository turnoRepository, OdontogramaRepository odontogramaRepository) {
        this.pacienteRepository = pacienteRepository;
        this.historiaClinicaRepository = historiaClinicaRepository;
        this.turnoRepository = turnoRepository;
        this.odontogramaRepository = odontogramaRepository;
    }

    public Paciente registrarPaciente(Paciente paciente) {
        if (pacienteRepository.existsByDni(paciente.getDni())) {
            throw new PacienteDuplicadoException(DUPLICATE_DNI_MESSAGE);
        }

        try {
            return pacienteRepository.saveAndFlush(paciente);
        } catch (DataIntegrityViolationException exception) {
            throw new PacienteDuplicadoException(DUPLICATE_DNI_MESSAGE);
        }
    }

    public List<Paciente> listarPacientes() {
        return pacienteRepository.findAll();
    }

    public Paciente obtenerPaciente(Long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new PacienteNoEncontradoException(id));
    }

    @Transactional
    public Paciente actualizarPaciente(Long id, Paciente cambios) {
        Paciente actual = obtenerPaciente(id);
        if (pacienteRepository.existsByDniAndIdNot(cambios.getDni(), id)) {
            throw new PacienteDuplicadoException(DUPLICATE_DNI_MESSAGE);
        }
        actual.setNombre(cambios.getNombre());
        actual.setApellido(cambios.getApellido());
        actual.setDni(cambios.getDni());
        actual.setTelefono(cambios.getTelefono());
        actual.setEmail(cambios.getEmail());
        actual.setFechaNacimiento(cambios.getFechaNacimiento());
        try {
            return pacienteRepository.saveAndFlush(actual);
        } catch (DataIntegrityViolationException exception) {
            throw new PacienteDuplicadoException(DUPLICATE_DNI_MESSAGE);
        }
    }

    @Transactional
    public void eliminarPaciente(Long id) {
        pacienteRepository.findLockedById(id)
                .orElseThrow(() -> new PacienteNoEncontradoException(id));
        if (historiaClinicaRepository.existsByPacienteId(id) || turnoRepository.existsByPacienteId(id)
                || odontogramaRepository.existsByPacienteId(id)) {
            throw new PacienteConDatosVinculadosException();
        }
        pacienteRepository.deleteById(id);
    }
}
