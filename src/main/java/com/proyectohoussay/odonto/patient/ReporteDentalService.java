package com.proyectohoussay.odonto.patient;

import com.proyectohoussay.odonto.auth.UserRole;
import com.proyectohoussay.odonto.model.MensajeUsuario;
import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.repository.MensajeUsuarioRepository;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ReporteDentalService {
    private final ReporteDentalRepository reportes;
    private final PacienteCuentaService cuentas;
    private final UsuarioRepository usuarios;
    private final MensajeUsuarioRepository mensajes;

    public ReporteDentalService(ReporteDentalRepository reportes, PacienteCuentaService cuentas,
                                UsuarioRepository usuarios) {
        this(reportes, cuentas, usuarios, null);
    }

    @Autowired
    public ReporteDentalService(ReporteDentalRepository reportes, PacienteCuentaService cuentas,
                                UsuarioRepository usuarios,
                                @Autowired(required = false) MensajeUsuarioRepository mensajes) {
        this.reportes = reportes;
        this.cuentas = cuentas;
        this.usuarios = usuarios;
        this.mensajes = mensajes;
    }

    @Transactional
    public ReporteDental crear(Long userId, int pieza, int dolor, String sintomas,
                               String fotoBase64, boolean consentimientoFoto) {
        if (pieza / 10 < 1 || pieza / 10 > 4 || pieza % 10 < 1 || pieza % 10 > 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Número FDI de pieza inválido");
        }
        Paciente paciente = cuentas.pacientePropio(userId);
        if (fotoBase64 != null && !fotoBase64.isBlank() && !consentimientoFoto) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debés consentir el almacenamiento de la foto");
        }
        String foto = FotoClinica.normalizar(fotoBase64);
        return reportes.save(new ReporteDental(paciente, pieza, dolor, sintomas.trim(), foto, foto != null && consentimientoFoto));
    }

    @Transactional
    public List<ReporteDental> propios(Long userId) {
        return reportes.findByPacienteIdOrderByCreadoEnDesc(cuentas.pacientePropio(userId).getId());
    }

    @Transactional
    public ReporteDental propio(Long userId, Long id) {
        Long pacienteId = cuentas.pacientePropio(userId).getId();
        ReporteDental reporte = reportes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte no encontrado"));
        if (!reporte.getPaciente().getId().equals(pacienteId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte no encontrado");
        }
        return reporte;
    }

    @Transactional(readOnly = true)
    public List<ReporteDental> clinicos(Long pacienteId) {
        return pacienteId == null ? reportes.findAllByOrderByCreadoEnDesc()
                : reportes.findByPacienteIdOrderByCreadoEnDesc(pacienteId);
    }

    @Transactional(readOnly = true)
    public ReporteDental clinico(Long id) {
        return reportes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte no encontrado"));
    }

    @Transactional
    public ReporteDental revisar(Long id, Long profesionalId, String revision) {
        Usuario profesional = usuarios.findById(profesionalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cuenta no encontrada"));
        UserRole role = UserRole.from(profesional.getRol()).orElse(null);
        if (!profesional.isActivo() || (role != UserRole.ODONTOLOGO && role != UserRole.ADMINISTRADOR)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el personal clínico puede revisar reportes");
        }
        ReporteDental reporte = reportes.findLockedById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte no encontrado"));
        if (reporte.getRevisadoEn() != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "El reporte ya fue revisado");
        reporte.revisar(revision.trim(), profesionalId);

        if (mensajes != null && reporte.getPaciente() != null && reporte.getPaciente().getId() != null) {
            usuarios.findByPacienteId(reporte.getPaciente().getId()).ifPresent(cuentaPaciente -> {
                String nombreRemitente = ((profesional.getNombre() != null ? profesional.getNombre() : "")
                        + " " + (profesional.getApellido() != null ? profesional.getApellido() : "")).trim();
                if (nombreRemitente.isBlank()) {
                    nombreRemitente = profesional.getUsername() != null ? profesional.getUsername() : "Odontología NovaDent";
                } else {
                    nombreRemitente = "Dr./Dra. " + nombreRemitente;
                }
                String asunto = "Revisión clínica de tu reporte dental (Pieza " + reporte.getPieza() + ")";
                String resumenRevision = revision.trim();
                if (resumenRevision.length() > 750) {
                    resumenRevision = resumenRevision.substring(0, 750) + "...";
                }
                String contenido = "El profesional revisó tu reporte dental de la pieza " + reporte.getPieza()
                        + ":\n\"" + resumenRevision + "\"\nPodés consultar el detalle completo en la sección Mi seguimiento.";
                mensajes.save(new MensajeUsuario(
                        cuentaPaciente.getId(),
                        profesional.getId(),
                        nombreRemitente,
                        asunto,
                        contenido
                ));
            });
        }

        return reporte;
    }
}
