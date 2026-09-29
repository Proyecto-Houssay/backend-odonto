package com.proyectohoussay.odonto.service;

import com.proyectohoussay.odonto.model.Especialidad;
import com.proyectohoussay.odonto.repository.EspecialidadRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EspecialidadService {

    private final EspecialidadRepository especialidadRepository;

    public EspecialidadService(EspecialidadRepository especialidadRepository) {
        this.especialidadRepository = especialidadRepository;
    }

    public List<Especialidad> listarEspecialidades() {
        return especialidadRepository.findAll();
    }

    public Especialidad obtenerPorId(Long id) {
        return especialidadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Especialidad no encontrada con ID: " + id));
    }

    public Especialidad crearEspecialidad(Especialidad especialidad) {
        return especialidadRepository.save(especialidad);
    }

    public void eliminarEspecialidad(Long id) {
        especialidadRepository.deleteById(id);
    }
}
