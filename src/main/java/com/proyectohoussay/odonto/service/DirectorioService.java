package com.proyectohoussay.odonto.service;

import com.proyectohoussay.odonto.dto.DirectorioItemDto;
import com.proyectohoussay.odonto.model.Odontologo;
import com.proyectohoussay.odonto.model.Personal;
import com.proyectohoussay.odonto.patient.Paciente;
import com.proyectohoussay.odonto.patient.PacienteRepository;
import com.proyectohoussay.odonto.repository.OdontologoRepository;
import com.proyectohoussay.odonto.repository.PersonalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Service
public class DirectorioService {
    private final PersonalRepository personalRepository;
    private final OdontologoRepository odontologoRepository;
    private final PacienteRepository pacienteRepository;

    public DirectorioService(PersonalRepository personalRepository, OdontologoRepository odontologoRepository,
                             PacienteRepository pacienteRepository) {
        this.personalRepository = personalRepository;
        this.odontologoRepository = odontologoRepository;
        this.pacienteRepository = pacienteRepository;
    }

    @Transactional(readOnly = true)
    public List<DirectorioItemDto> buscar(String termino, String tipo) {
        if (termino == null || termino.isBlank()) throw new IllegalArgumentException("El término de búsqueda es obligatorio.");
        String query = termino.trim().toLowerCase(Locale.ROOT);
        String filtroTipo = normalizarFiltroTipo(tipo);

        List<DirectorioItemDto> items = new ArrayList<>();
        if (filtroTipo == null || "PERSONAL".equals(filtroTipo)) {
            personalRepository.findAll().stream().map(this::de).filter(item -> coincide(item, query)).forEach(items::add);
        }
        if (filtroTipo == null || "MEDICO".equals(filtroTipo)) {
            odontologoRepository.findAll().stream().map(this::de).filter(item -> coincide(item, query)).forEach(items::add);
        }
        if (filtroTipo == null || "PACIENTE".equals(filtroTipo)) {
            pacienteRepository.findAll().stream().map(this::de).filter(item -> coincide(item, query)).forEach(items::add);
        }
        return items;
    }

    @Transactional(readOnly = true)
    public List<DirectorioItemDto> listar(String tipo) {
        String filtroTipo = normalizarFiltroTipo(tipo);
        List<DirectorioItemDto> items = new ArrayList<>();
        if (filtroTipo == null || "PERSONAL".equals(filtroTipo)) {
            personalRepository.findAll().stream().map(this::de).forEach(items::add);
        }
        if (filtroTipo == null || "MEDICO".equals(filtroTipo)) {
            odontologoRepository.findAll().stream().map(this::de).forEach(items::add);
        }
        if (filtroTipo == null || "PACIENTE".equals(filtroTipo)) {
            pacienteRepository.findAll().stream().map(this::de).forEach(items::add);
        }
        return items;
    }

    private String normalizarFiltroTipo(String tipo) {
        String filtroTipo = tipo == null || tipo.isBlank() || "TODOS".equalsIgnoreCase(tipo.trim())
                ? null
                : tipo.trim().toUpperCase(Locale.ROOT);
        if (filtroTipo != null && !List.of("PERSONAL", "MEDICO", "PACIENTE").contains(filtroTipo)) {
            throw new IllegalArgumentException("El tipo debe ser PERSONAL, MEDICO o PACIENTE.");
        }
        return filtroTipo;
    }

    private DirectorioItemDto de(Personal p) {
        return new DirectorioItemDto("PERSONAL", p.getId(), nombre(p.getNombre(), p.getApellido()), p.getDni(),
                p.getTelefono(), p.getEmail(), p.getCargo() == null ? null : p.getCargo().getNombreLegible(), p.isActivo());
    }

    private DirectorioItemDto de(Odontologo o) {
        String especialidad = Stream.concat(Stream.ofNullable(o.getEspecialidad()), o.getEspecialidadesAdicionales().stream())
                .map(e -> e.getNombre()).distinct().reduce((a, b) -> a + ", " + b).orElse(null);
        String detail = "Matrícula " + o.getMatricula() + (especialidad == null ? "" : " · " + especialidad);
        return new DirectorioItemDto("MEDICO", o.getId(), nombre(o.getNombre(), o.getApellido()), null,
                o.getTelefono(), o.getEmail(), detail, o.isActivo());
    }

    private DirectorioItemDto de(Paciente p) {
        String detail = p.isParticular() ? "Particular" : (p.getObraSocial() == null ? "Cobertura" : p.getObraSocial().getNombre());
        if (p.getPlanCobertura() != null) detail += " · " + p.getPlanCobertura();
        return new DirectorioItemDto("PACIENTE", p.getId(), nombre(p.getNombre(), p.getApellido()), p.getDni(),
                p.getTelefono(), p.getEmail(), detail, true);
    }

    private String nombre(String nombre, String apellido) { return (nombre + " " + apellido).trim(); }
    private boolean coincide(DirectorioItemDto item, String query) {
        return contiene(item.nombreCompleto(), query) || contiene(item.dni(), query) || contiene(item.telefono(), query)
                || contiene(item.email(), query) || contiene(item.detalle(), query);
    }
    private boolean contiene(String value, String query) { return value != null && value.toLowerCase(Locale.ROOT).contains(query); }
}
