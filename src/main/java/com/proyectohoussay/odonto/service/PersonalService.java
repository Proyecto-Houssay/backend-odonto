package com.proyectohoussay.odonto.service;

import com.proyectohoussay.odonto.dto.PersonalRequest;
import com.proyectohoussay.odonto.model.CargoPersonal;
import com.proyectohoussay.odonto.model.Personal;
import com.proyectohoussay.odonto.repository.PersonalRepository;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PersonalService {

    private final PersonalRepository personalRepository;
    private final UsuarioRepository usuarioRepository;

    public PersonalService(PersonalRepository personalRepository, UsuarioRepository usuarioRepository) {
        this.personalRepository = personalRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Personal crearPersonal(PersonalRequest request) {
        if (request.dni() == null || request.dni().isBlank()) {
            throw new IllegalArgumentException("El DNI es obligatorio.");
        }
        if (personalRepository.existsByDni(request.dni().trim())) {
            throw new IllegalStateException("Ya existe un miembro del personal registrado con el DNI: " + request.dni());
        }

        if (request.usuarioId() != null) {
            validarUsuarioExiste(request.usuarioId());
        }

        LocalDate fecha = request.fechaIngreso() != null ? request.fechaIngreso() : LocalDate.now();
        Personal personal = new Personal(
                request.nombre(),
                request.apellido(),
                request.dni().trim(),
                request.email() != null ? request.email().trim() : null,
                request.telefono() != null ? request.telefono().trim() : null,
                request.cargo(),
                fecha,
                request.horarioLaboral(),
                request.usuarioId()
        );
        if (request.activo() != null) {
            personal.setActivo(request.activo());
        }

        return personalRepository.save(personal);
    }

    @Transactional
    public Personal actualizarPersonal(Long id, PersonalRequest request) {
        Personal personal = obtenerPorId(id);

        if (request.dni() != null && !request.dni().trim().equalsIgnoreCase(personal.getDni())) {
            if (personalRepository.existsByDni(request.dni().trim())) {
                throw new IllegalStateException("Ya existe otro miembro del personal con el DNI: " + request.dni());
            }
            personal.setDni(request.dni().trim());
        }

        if (request.usuarioId() != null && !request.usuarioId().equals(personal.getUsuarioId())) {
            validarUsuarioExiste(request.usuarioId());
            personal.setUsuarioId(request.usuarioId());
        }

        if (request.nombre() != null && !request.nombre().isBlank()) {
            personal.setNombre(request.nombre().trim());
        }
        if (request.apellido() != null && !request.apellido().isBlank()) {
            personal.setApellido(request.apellido().trim());
        }
        if (request.email() != null) {
            personal.setEmail(request.email().trim());
        }
        if (request.telefono() != null) {
            personal.setTelefono(request.telefono().trim());
        }
        if (request.cargo() != null) {
            personal.setCargo(request.cargo());
        }
        if (request.fechaIngreso() != null) {
            personal.setFechaIngreso(request.fechaIngreso());
        }
        if (request.horarioLaboral() != null) {
            personal.setHorarioLaboral(request.horarioLaboral());
        }
        if (request.activo() != null) {
            personal.setActivo(request.activo());
        }

        return personalRepository.save(personal);
    }

    @Transactional(readOnly = true)
    public List<Personal> listarPersonal(CargoPersonal cargo, Boolean activo) {
        if (cargo != null && activo != null) {
            return personalRepository.findAllByCargoAndActivo(cargo, activo);
        }
        if (cargo != null) {
            return personalRepository.findAllByCargo(cargo);
        }
        if (activo != null && activo) {
            return personalRepository.findAllByActivoTrue();
        }
        return personalRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Personal obtenerPorId(Long id) {
        return personalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Personal con ID " + id + " no encontrado."));
    }

    @Transactional(readOnly = true)
    public Personal obtenerPorDni(String dni) {
        return personalRepository.findByDni(dni)
                .orElseThrow(() -> new IllegalArgumentException("Personal con DNI " + dni + " no encontrado."));
    }

    @Transactional
    public void darDeBaja(Long id) {
        Personal personal = obtenerPorId(id);
        personal.setActivo(false);
        personalRepository.save(personal);
    }

    private void validarUsuarioExiste(Long usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new IllegalArgumentException("El usuario con ID " + usuarioId + " no existe.");
        }
    }
}
