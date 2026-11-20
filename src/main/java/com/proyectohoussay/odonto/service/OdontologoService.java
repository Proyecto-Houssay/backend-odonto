package com.proyectohoussay.odonto.service;

import com.proyectohoussay.odonto.dto.OdontologoAltaCompletaRequest;
import com.proyectohoussay.odonto.model.Especialidad;
import com.proyectohoussay.odonto.model.Odontologo;
import com.proyectohoussay.odonto.model.DisponibilidadHoraria;
import com.proyectohoussay.odonto.model.EstadoProfesional;
import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.repository.EspecialidadRepository;
import com.proyectohoussay.odonto.repository.OdontologoRepository;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Service
public class OdontologoService {

    private final OdontologoRepository odontologoRepository;
    private final EspecialidadRepository especialidadRepository;
    private UsuarioRepository usuarioRepository;
    private PasswordEncoder passwordEncoder;

    public OdontologoService(OdontologoRepository odontologoRepository, EspecialidadRepository especialidadRepository) {
        this.odontologoRepository = odontologoRepository;
        this.especialidadRepository = especialidadRepository;
    }

    @Autowired(required = false)
    public void setUsuarioRepository(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Autowired(required = false)
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public List<Odontologo> listarOdontologos() {
        return odontologoRepository.findAll();
    }

    public List<Odontologo> listarPorEspecialidad(Long especialidadId) {
        if (especialidadId == null || !especialidadRepository.existsById(especialidadId)) {
            throw new IllegalArgumentException("La especialidad indicada no existe.");
        }
        return odontologoRepository.findAllByEspecialidad(especialidadId);
    }

    public Odontologo obtenerOdontologo(Long id) {
        return odontologoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Odontólogo no encontrado con ID: " + id));
    }

    public Optional<Odontologo> obtenerPorMatricula(String matricula) {
        return odontologoRepository.findByMatricula(matricula);
    }

    public Odontologo crearOdontologo(Odontologo odontologo) {
        validarEspecialidad(odontologo);
        validarMatricula(odontologo.getMatricula());
        if (odontologoRepository.existsByMatricula(odontologo.getMatricula())) {
            throw new IllegalStateException("Ya existe un odontólogo registrado con la matrícula: " + odontologo.getMatricula());
        }
        return odontologoRepository.save(odontologo);
    }

    @Transactional
    public Odontologo crearOdontologoAltaCompleta(OdontologoAltaCompletaRequest request) {
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del odontólogo es obligatorio.");
        }
        if (request.apellido() == null || request.apellido().isBlank()) {
            throw new IllegalArgumentException("El apellido del odontólogo es obligatorio.");
        }
        validarMatricula(request.matricula());
        String matricula = request.matricula().trim();
        if (odontologoRepository.existsByMatricula(matricula)) {
            throw new IllegalStateException("Ya existe un odontólogo registrado con la matrícula: " + matricula);
        }

        Especialidad especialidad;
        if (request.nuevaEspecialidadNombre() != null && !request.nuevaEspecialidadNombre().isBlank()) {
            String nombreEsp = request.nuevaEspecialidadNombre().trim();
            especialidad = especialidadRepository.findAll().stream()
                    .filter(e -> e.getNombre() != null && e.getNombre().equalsIgnoreCase(nombreEsp))
                    .findFirst()
                    .orElseGet(() -> especialidadRepository.save(new Especialidad(
                            nombreEsp,
                            request.nuevaEspecialidadDescripcion() != null && !request.nuevaEspecialidadDescripcion().isBlank()
                                    ? request.nuevaEspecialidadDescripcion().trim()
                                    : "Especialidad clínica en " + nombreEsp)));
        } else if (request.especialidadId() != null) {
            especialidad = especialidadRepository.findById(request.especialidadId())
                    .orElseThrow(() -> new IllegalArgumentException("La especialidad indicada no existe."));
        } else {
            throw new IllegalArgumentException("El odontólogo debe tener una especialidad asociada.");
        }

        Odontologo odontologo = new Odontologo(
                request.nombre().trim(),
                request.apellido().trim(),
                matricula,
                request.email() != null && !request.email().isBlank() ? request.email().trim() : null,
                request.telefono() != null && !request.telefono().isBlank() ? request.telefono().trim() : null,
                especialidad
        );
        boolean activo = request.activo() == null || request.activo();
        odontologo.setActivo(activo);
        odontologo.setEstadoProfesional(activo ? EstadoProfesional.ACTIVO : EstadoProfesional.INACTIVO);
        if (request.consultorio() != null && !request.consultorio().isBlank()) {
            odontologo.setConsultorio(request.consultorio().trim());
        }

        if (Boolean.TRUE.equals(request.crearCuentaUsuario()) && usuarioRepository != null && passwordEncoder != null) {
            String username = request.username() != null ? request.username().trim() : "";
            String password = request.password() != null ? request.password() : "";
            if (username.isBlank()) {
                throw new IllegalArgumentException("El nombre de usuario es obligatorio para crear la cuenta de acceso.");
            }
            if (password.length() < 6) {
                throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres.");
            }
            if (usuarioRepository.existsByUsernameIgnoreCase(username)) {
                throw new IllegalStateException("Ya existe una cuenta de usuario con el username: " + username);
            }
            String userEmail = odontologo.getEmail() != null && !odontologo.getEmail().isBlank()
                    ? odontologo.getEmail()
                    : username.toLowerCase() + "@novadent.local";
            if (usuarioRepository.existsByEmailIgnoreCase(userEmail)) {
                throw new IllegalStateException("Ya existe una cuenta de usuario con el correo: " + userEmail);
            }
            Usuario cuenta = new Usuario();
            cuenta.setUsername(username);
            cuenta.setNombre(odontologo.getNombre());
            cuenta.setApellido(odontologo.getApellido());
            cuenta.setNombreMostrar("Dr./Dra. " + odontologo.getNombre() + " " + odontologo.getApellido());
            cuenta.setEmail(userEmail);
            cuenta.setPassword(passwordEncoder.encode(password));
            cuenta.setRol("ODONTOLOGO");
            cuenta.setActivo(true);
            Usuario guardada = usuarioRepository.save(cuenta);
            odontologo.setUsuarioId(guardada.getId());
        }

        return odontologoRepository.save(odontologo);
    }

    public Odontologo actualizarOdontologo(Long id, Odontologo details) {
        Odontologo odontologo = obtenerOdontologo(id);
        validarEspecialidad(details);
        validarMatricula(details.getMatricula());

        if (!details.getMatricula().equals(odontologo.getMatricula()) &&
                odontologoRepository.existsByMatricula(details.getMatricula())) {
            throw new IllegalStateException("La matrícula " + details.getMatricula() + " ya está registrada para otro odontólogo.");
        }

        odontologo.setNombre(details.getNombre());
        odontologo.setApellido(details.getApellido());
        odontologo.setMatricula(details.getMatricula());
        odontologo.setEmail(details.getEmail());
        odontologo.setTelefono(details.getTelefono());
        odontologo.setEspecialidad(details.getEspecialidad());
        odontologo.setActivo(details.isActivo());
        odontologo.setConsultorio(details.getConsultorio());
        odontologo.setEstadoProfesional(details.getEstadoProfesional() == null
                ? (details.isActivo() ? EstadoProfesional.ACTIVO : EstadoProfesional.INACTIVO)
                : details.getEstadoProfesional());
        odontologo.setDisponibilidades(details.getDisponibilidades());
        odontologo.setEspecialidadesAdicionales(details.getEspecialidadesAdicionales());

        return odontologoRepository.save(odontologo);
    }

    public void eliminarOdontologo(Long id) {
        Odontologo odontologo = obtenerOdontologo(id);
        odontologo.setActivo(false);
        odontologo.setEstadoProfesional(EstadoProfesional.INACTIVO);
        odontologoRepository.save(odontologo);
    }

    public List<DisponibilidadHoraria> obtenerDisponibilidad(Long id) {
        return new ArrayList<>(obtenerOdontologo(id).getDisponibilidades());
    }

    public Odontologo actualizarDisponibilidad(Long id, List<DisponibilidadHoraria> disponibilidad) {
        Odontologo odontologo = obtenerOdontologo(id);
        if (odontologo.getEstadoProfesional() != EstadoProfesional.ACTIVO || !odontologo.isActivo()) {
            throw new IllegalStateException("Solo se puede asignar disponibilidad a un odontólogo activo.");
        }
        validarDisponibilidad(odontologo, disponibilidad);
        odontologo.setDisponibilidades(disponibilidad);
        return odontologoRepository.save(odontologo);
    }

    public Odontologo actualizarEspecialidades(Long id, List<Long> ids) {
        Odontologo odontologo = obtenerOdontologo(id);
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("Debe indicar al menos una especialidad.");
        }
        Set<Long> unicos = new HashSet<>(ids);
        if (unicos.size() != ids.size()) {
            throw new IllegalArgumentException("No se permiten especialidades duplicadas.");
        }
        var especialidades = especialidadRepository.findAllById(ids);
        if (especialidades.size() != unicos.size()) {
            throw new IllegalArgumentException("Una o más especialidades indicadas no existen.");
        }
        odontologo.setEspecialidad(especialidades.get(0));
        odontologo.setEspecialidadesAdicionales(especialidades.size() > 1
                ? especialidades.subList(1, especialidades.size()) : List.of());
        return odontologoRepository.save(odontologo);
    }

    private void validarDisponibilidad(Odontologo odontologo, List<DisponibilidadHoraria> disponibilidad) {
        if (disponibilidad == null) {
            throw new IllegalArgumentException("La disponibilidad es obligatoria.");
        }
        for (int i = 0; i < disponibilidad.size(); i++) {
            DisponibilidadHoraria slot = disponibilidad.get(i);
            if (slot == null || slot.getDia() == null || slot.getHoraInicio() == null || slot.getHoraFin() == null
                    || !slot.getHoraInicio().isBefore(slot.getHoraFin())) {
                throw new IllegalArgumentException("Cada franja requiere día y horas válidas; el inicio debe ser anterior al fin.");
            }
            if (slot.getDuracionPromedioMinutos() != null && slot.getDuracionPromedioMinutos() <= 0) {
                throw new IllegalArgumentException("La duración promedio debe ser mayor a cero.");
            }
            for (int j = 0; j < i; j++) {
                DisponibilidadHoraria anterior = disponibilidad.get(j);
                if (anterior.getDia() == slot.getDia()
                        && anterior.getHoraInicio().isBefore(slot.getHoraFin())
                        && anterior.getHoraFin().isAfter(slot.getHoraInicio())) {
                    throw new IllegalArgumentException("Las franjas de un odontólogo no pueden superponerse.");
                }
            }
            if (odontologo.getConsultorio() != null && !odontologo.getConsultorio().isBlank()
                    && odontologoRepository.existeSolapamientoConsultorio(odontologo.getConsultorio(), slot.getDia(),
                    slot.getHoraInicio(), slot.getHoraFin(), odontologo.getId())) {
                throw new IllegalStateException("El consultorio ya está ocupado en esa franja horaria.");
            }
        }
    }

    private void validarEspecialidad(Odontologo odontologo) {
        if (odontologo.getEspecialidad() == null || odontologo.getEspecialidad().getId() == null
                || !especialidadRepository.existsById(odontologo.getEspecialidad().getId())) {
            throw new IllegalArgumentException("La especialidad indicada no existe.");
        }
    }

    private void validarMatricula(String matricula) {
        if (matricula == null || matricula.isBlank()) {
            throw new IllegalArgumentException("La matrícula profesional es obligatoria.");
        }
        if (!matricula.trim().matches("^[A-Za-z0-9][A-Za-z0-9./-]{1,49}$")) {
            throw new IllegalArgumentException("La matrícula debe contener entre 2 y 50 caracteres alfanuméricos, guiones, puntos o barras.");
        }
    }
}
