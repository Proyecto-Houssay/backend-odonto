package com.proyectohoussay.odonto.patient;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class PacienteService {

    private static final String DUPLICATE_DNI_MESSAGE = "Ya existe un paciente registrado con ese DNI";

    private final PacienteRepository pacienteRepository;
    private final ObraSocialRepository obraSocialRepository;

    public PacienteService(PacienteRepository pacienteRepository) {
        this(pacienteRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public PacienteService(PacienteRepository pacienteRepository, ObraSocialRepository obraSocialRepository) {
        this.pacienteRepository = pacienteRepository;
        this.obraSocialRepository = obraSocialRepository;
    }

    public Paciente registrarPaciente(Paciente paciente) {
        if (paciente.getDni() != null) paciente.setDni(paciente.getDni().trim());
        if (pacienteRepository.existsByDni(paciente.getDni())) {
            throw new PacienteDuplicadoException(DUPLICATE_DNI_MESSAGE);
        }

        return pacienteRepository.save(paciente);
    }

    @Transactional(readOnly = true)
    public List<Paciente> listarPacientes() { return pacienteRepository.findAll(); }

    @Transactional(readOnly = true)
    public Paciente obtenerPorId(Long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado con ID: " + id));
    }

    @Transactional(readOnly = true)
    public Paciente obtenerPorDni(String dni) {
        return pacienteRepository.findByDni(dni.trim())
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
        Paciente paciente = obtenerPorId(id);
        if (cambios.getDni() != null && !cambios.getDni().trim().equals(paciente.getDni())
                && pacienteRepository.existsByDniAndIdNot(cambios.getDni().trim(), id)) {
            throw new PacienteDuplicadoException(DUPLICATE_DNI_MESSAGE);
        }
        if (cambios.getDni() != null) paciente.setDni(cambios.getDni().trim());
        if (cambios.getNombre() != null) paciente.setNombre(cambios.getNombre().trim());
        if (cambios.getApellido() != null) paciente.setApellido(cambios.getApellido().trim());
        if (cambios.getEmail() != null) paciente.setEmail(cambios.getEmail().trim());
        if (cambios.getTelefono() != null) paciente.setTelefono(cambios.getTelefono().trim());
        if (cambios.getFechaNacimiento() != null) paciente.setFechaNacimiento(cambios.getFechaNacimiento());
        if (cambios.getContactoEmergencia() != null) paciente.setContactoEmergencia(cambios.getContactoEmergencia());
        if (cambios.getAlertasMedicas() != null) paciente.setAlertasMedicas(cambios.getAlertasMedicas());
        return pacienteRepository.save(paciente);
    }

    @Transactional
    public Paciente actualizarCobertura(Long id, Long obraSocialId, String numeroAfiliado, String plan, boolean particular) {
        Paciente paciente = obtenerPorId(id);
        if (particular) {
            paciente.setObraSocial(null);
            paciente.setNumeroAfiliado(null);
            paciente.setPlanCobertura(null);
            paciente.setParticular(true);
        } else {
            if (obraSocialRepository == null) throw new IllegalStateException("El repositorio de obras sociales no está disponible.");
            if (obraSocialId == null || numeroAfiliado == null || numeroAfiliado.isBlank()) {
                throw new IllegalArgumentException("La obra social y el número de afiliado son obligatorios.");
            }
            ObraSocial obraSocial = obraSocialRepository.findById(obraSocialId)
                    .orElseThrow(() -> new IllegalArgumentException("La obra social indicada no existe."));
            if (!obraSocial.isActivo()) throw new IllegalArgumentException("La obra social está inactiva.");
            paciente.setObraSocial(obraSocial);
            paciente.setNumeroAfiliado(numeroAfiliado.trim());
            paciente.setPlanCobertura(plan == null || plan.isBlank() ? null : plan.trim());
            paciente.setParticular(false);
        }
        return pacienteRepository.save(paciente);
    }

    @Transactional
    public Paciente actualizarAlertasMedicas(Long id, List<String> alertas) {
        if (alertas == null) throw new IllegalArgumentException("La lista de alertas es obligatoria.");
        List<String> normalizadas = alertas.stream().filter(Objects::nonNull).map(String::trim)
                .filter(s -> !s.isBlank()).distinct().toList();
        Paciente paciente = obtenerPorId(id);
        paciente.setAlertasMedicas(normalizadas);
        return pacienteRepository.save(paciente);
    }
}
