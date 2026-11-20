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
        List<Especialidad> existentes = especialidadRepository.findAll();
        if (existentes.isEmpty()) {
            List<Especialidad> porDefecto = List.of(
                    new Especialidad("Odontología General", "Atención odontológica integral, prevención, diagnóstico y restauraciones."),
                    new Especialidad("Ortodoncia", "Corrección de maloclusiones, alineación dental y ortopedia maxilar."),
                    new Especialidad("Endodoncia", "Tratamiento de conductos radiculares y patologías pulpares."),
                    new Especialidad("Implantología", "Rehabilitación oral mediante implantes osteointegrados y prótesis."),
                    new Especialidad("Periodoncia", "Prevención, diagnóstico y tratamiento de encías y tejidos de soporte."),
                    new Especialidad("Odontopediatría", "Atención odontológica integral para niños y adolescentes."),
                    new Especialidad("Cirugía Maxilofacial", "Extracciones complejas, terceros molares retenidos y cirugía oral.")
            );
            return especialidadRepository.saveAll(porDefecto);
        }
        return existentes;
    }

    public Especialidad obtenerPorId(Long id) {
        return especialidadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Especialidad no encontrada con ID: " + id));
    }

    public Especialidad crearEspecialidad(Especialidad especialidad) {
        if (especialidad.getNombre() == null || especialidad.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre de la especialidad es obligatorio.");
        }
        String nombre = especialidad.getNombre().trim();
        if (especialidadRepository.existsByNombre(nombre)) {
            throw new IllegalStateException("Ya existe una especialidad con el nombre: " + nombre);
        }
        especialidad.setNombre(nombre);
        return especialidadRepository.save(especialidad);
    }

    public void eliminarEspecialidad(Long id) {
        especialidadRepository.deleteById(id);
    }
}
