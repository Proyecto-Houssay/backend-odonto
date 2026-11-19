package com.proyectohoussay.odonto.patient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import com.proyectohoussay.odonto.clinicalhistory.HistoriaClinicaRepository;
import com.proyectohoussay.odonto.repository.TurnoRepository;
import com.proyectohoussay.odonto.odontogram.OdontogramaRepository;
import com.proyectohoussay.odonto.repository.UsuarioRepository;

import java.util.List;
import java.util.Objects;

@Service
public class PacienteService {

    private static final String DUPLICATE_DNI_MESSAGE = "Ya existe un paciente registrado con ese DNI";

    private final PacienteRepository pacienteRepository;
    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final TurnoRepository turnoRepository;
    private final OdontogramaRepository odontogramaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReporteDentalRepository reporteDentalRepository;
    private final ObraSocialRepository obraSocialRepository;

    public PacienteService(PacienteRepository pacienteRepository,
                           HistoriaClinicaRepository historiaClinicaRepository,
                           TurnoRepository turnoRepository, OdontogramaRepository odontogramaRepository,
                           UsuarioRepository usuarioRepository, ReporteDentalRepository reporteDentalRepository) {
        this(pacienteRepository, historiaClinicaRepository, turnoRepository, odontogramaRepository,
                usuarioRepository, reporteDentalRepository, null);
    }

    @Autowired
    public PacienteService(PacienteRepository pacienteRepository,
                           @Autowired(required = false) HistoriaClinicaRepository historiaClinicaRepository,
                           @Autowired(required = false) TurnoRepository turnoRepository,
                           @Autowired(required = false) OdontogramaRepository odontogramaRepository,
                           @Autowired(required = false) UsuarioRepository usuarioRepository,
                           @Autowired(required = false) ReporteDentalRepository reporteDentalRepository,
                           @Autowired(required = false) ObraSocialRepository obraSocialRepository) {
        this.pacienteRepository = pacienteRepository;
        this.historiaClinicaRepository = historiaClinicaRepository;
        this.turnoRepository = turnoRepository;
        this.odontogramaRepository = odontogramaRepository;
        this.usuarioRepository = usuarioRepository;
        this.reporteDentalRepository = reporteDentalRepository;
        this.obraSocialRepository = obraSocialRepository;
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

    @Transactional(readOnly = true)
    public Paciente obtenerPorId(Long id) {
        return obtenerPaciente(id);
    }

    @Transactional(readOnly = true)
    public Paciente obtenerPorDni(String dni) {
        return pacienteRepository.findByDni(dni != null ? dni.trim() : "")
                .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado con DNI: " + dni));
    }

    @Transactional(readOnly = true)
    public List<Paciente> buscar(String termino) {
        if (termino == null || termino.isBlank()) return List.of();
        String query = termino.trim();
        return pacienteRepository.findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCase(query, query);
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
        if (cambios.getContactoEmergencia() != null) {
            actual.setContactoEmergencia(cambios.getContactoEmergencia());
        }
        if (cambios.getAlertasMedicas() != null && !cambios.getAlertasMedicas().isEmpty()) {
            actual.setAlertasMedicas(cambios.getAlertasMedicas());
        }
        try {
            return pacienteRepository.saveAndFlush(actual);
        } catch (DataIntegrityViolationException exception) {
            throw new PacienteDuplicadoException(DUPLICATE_DNI_MESSAGE);
        }
    }

    @Transactional
    public Paciente actualizarCobertura(Long id, Long obraSocialId, String numeroAfiliado, String plan, boolean particular) {
        Paciente paciente = obtenerPaciente(id);
        if (particular) {
            paciente.setObraSocial(null);
            paciente.setNumeroAfiliado(null);
            paciente.setPlanCobertura(null);
            paciente.setParticular(true);
        } else {
            if (obraSocialRepository == null) {
                throw new IllegalStateException("El repositorio de obras sociales no está disponible.");
            }
            if (obraSocialId == null || numeroAfiliado == null || numeroAfiliado.isBlank()) {
                throw new IllegalArgumentException("La obra social y el número de afiliado son obligatorios.");
            }
            ObraSocial obraSocial = obraSocialRepository.findById(obraSocialId)
                    .orElseThrow(() -> new IllegalArgumentException("La obra social indicada no existe."));
            if (!obraSocial.isActivo()) {
                throw new IllegalArgumentException("La obra social está inactiva.");
            }
            paciente.setObraSocial(obraSocial);
            paciente.setNumeroAfiliado(numeroAfiliado.trim());
            paciente.setPlanCobertura(plan == null || plan.isBlank() ? null : plan.trim());
            paciente.setParticular(false);
        }
        return pacienteRepository.save(paciente);
    }

    @Transactional
    public Paciente actualizarAlertasMedicas(Long id, List<String> alertas) {
        if (alertas == null) {
            throw new IllegalArgumentException("La lista de alertas es obligatoria.");
        }
        List<String> normalizadas = alertas.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
        Paciente paciente = obtenerPaciente(id);
        paciente.setAlertasMedicas(normalizadas);
        return pacienteRepository.save(paciente);
    }

    @Transactional
    public void eliminarPaciente(Long id) {
        pacienteRepository.findLockedById(id)
                .orElseThrow(() -> new PacienteNoEncontradoException(id));
        if (historiaClinicaRepository.existsByPacienteId(id) || turnoRepository.existsByPacienteId(id)
                || odontogramaRepository.existsByPacienteId(id) || usuarioRepository.existsByPacienteId(id)
                || reporteDentalRepository.existsByPacienteId(id)) {
            throw new PacienteConDatosVinculadosException();
        }
        pacienteRepository.deleteById(id);
    }
}
