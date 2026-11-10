package com.proyectohoussay.odonto.patient;

import com.proyectohoussay.odonto.auth.UserRole;
import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
public class PacienteCuentaService {
    private final UsuarioRepository usuarios;
    private final PacienteRepository pacientes;

    public PacienteCuentaService(UsuarioRepository usuarios, PacienteRepository pacientes) {
        this.usuarios = usuarios;
        this.pacientes = pacientes;
    }

    @Transactional(readOnly = true)
    public PacienteResponse propio(Long userId) {
        Paciente paciente = cuentaPaciente(userId).getPaciente();
        if (paciente == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Completá tu ficha de paciente");
        return PacienteResponse.de(paciente);
    }

    @Transactional
    public PacienteResponse crear(Long userId, String dni, String telefono, LocalDate fechaNacimiento) {
        Usuario cuenta = usuarios.findLockedById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La cuenta no existe"));
        validarCuentaPaciente(cuenta);
        if (cuenta.getPaciente() != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta ya tiene una ficha de paciente");
        String dniNormalizado = dni.trim();
        if (pacientes.existsByDni(dniNormalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El DNI ya figura en el sistema. Pedí al administrador que vincule tu cuenta con esa ficha");
        }
        Paciente paciente = new Paciente();
        paciente.setNombre(cuenta.getNombre());
        paciente.setApellido(cuenta.getApellido());
        paciente.setEmail(cuenta.getEmail());
        paciente.setDni(dniNormalizado);
        paciente.setTelefono(telefono == null ? null : telefono.trim());
        paciente.setFechaNacimiento(fechaNacimiento);
        try {
            pacientes.saveAndFlush(paciente);
            cuenta.setPaciente(paciente);
            usuarios.saveAndFlush(cuenta);
            return PacienteResponse.de(paciente);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La ficha o su vínculo ya existen");
        }
    }

    @Transactional
    public PacienteResponse vincular(Long userId, Long pacienteId, String dniConfirmado) {
        Usuario cuenta = usuarios.findLockedById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));
        if (UserRole.from(cuenta.getRol()).orElse(null) != UserRole.PACIENTE || !cuenta.isActivo()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cuenta debe ser de un paciente activo");
        }
        if (cuenta.getPaciente() != null || usuarios.existsByPacienteId(pacienteId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta o la ficha ya están vinculadas");
        }
        Paciente paciente = pacientes.findById(pacienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente no encontrado"));
        if (!paciente.getDni().equals(dniConfirmado.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El DNI confirmado no coincide con la ficha");
        }
        try {
            cuenta.setPaciente(paciente);
            usuarios.saveAndFlush(cuenta);
            return PacienteResponse.de(paciente);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La ficha ya está vinculada a otra cuenta");
        }
    }

    @Transactional
    public PacienteResponse actualizarTelefono(Long userId, String telefono) {
        Paciente paciente = pacientePropio(userId);
        paciente.setTelefono(telefono == null || telefono.isBlank() ? null : telefono.trim());
        return PacienteResponse.de(pacientes.save(paciente));
    }

    @Transactional(readOnly = true)
    public Paciente pacientePropio(Long userId) {
        Paciente paciente = cuentaPaciente(userId).getPaciente();
        if (paciente == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Completá tu ficha antes de cargar un reporte");
        return paciente;
    }

    private Usuario cuentaPaciente(Long userId) {
        Usuario user = usuarios.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La cuenta no existe"));
        validarCuentaPaciente(user);
        return user;
    }

    private void validarCuentaPaciente(Usuario user) {
        if (!user.isActivo() || UserRole.from(user.getRol()).orElse(null) != UserRole.PACIENTE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta no tiene acceso al portal de pacientes");
        }
    }
}
