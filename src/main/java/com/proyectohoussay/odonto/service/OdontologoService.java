package com.proyectohoussay.odonto.service;

import com.proyectohoussay.odonto.model.Odontologo;
import com.proyectohoussay.odonto.repository.OdontologoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OdontologoService {

    private final OdontologoRepository odontologoRepository;

    public OdontologoService(OdontologoRepository odontologoRepository) {
        this.odontologoRepository = odontologoRepository;
    }

    public List<Odontologo> listarOdontologos() {
        return odontologoRepository.findAll();
    }

    public Odontologo obtenerOdontologo(Long id) {
        return odontologoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Odontólogo no encontrado con ID: " + id));
    }

    public Optional<Odontologo> obtenerPorMatricula(String matricula) {
        return odontologoRepository.findByMatricula(matricula);
    }

    public Odontologo crearOdontologo(Odontologo odontologo) {
        if (odontologo.getMatricula() == null || odontologo.getMatricula().trim().isEmpty()) {
            throw new IllegalArgumentException("La matrícula profesional es obligatoria.");
        }
        if (odontologoRepository.existsByMatricula(odontologo.getMatricula())) {
            throw new IllegalStateException("Ya existe un odontólogo registrado con la matrícula: " + odontologo.getMatricula());
        }
        return odontologoRepository.save(odontologo);
    }

    public Odontologo actualizarOdontologo(Long id, Odontologo details) {
        Odontologo odontologo = obtenerOdontologo(id);

        if (!odontologo.getMatricula().equals(details.getMatricula()) &&
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

        return odontologoRepository.save(odontologo);
    }

    public void eliminarOdontologo(Long id) {
        Odontologo odontologo = obtenerOdontologo(id);
        odontologoRepository.delete(odontologo);
    }
}
