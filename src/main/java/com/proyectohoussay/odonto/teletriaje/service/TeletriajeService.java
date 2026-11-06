package com.proyectohoussay.odonto.teletriaje.service;

import com.proyectohoussay.odonto.model.Odontologo;
import com.proyectohoussay.odonto.model.Turno;
import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.patient.Paciente;
import com.proyectohoussay.odonto.patient.PacienteNoEncontradoException;
import com.proyectohoussay.odonto.patient.PacienteRepository;
import com.proyectohoussay.odonto.repository.OdontologoRepository;
import com.proyectohoussay.odonto.repository.TurnoRepository;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import com.proyectohoussay.odonto.teletriaje.dto.*;
import com.proyectohoussay.odonto.teletriaje.exception.AccesoDenegadoTeletriajeException;
import com.proyectohoussay.odonto.teletriaje.exception.CitaTeletriajeNoEncontradaException;
import com.proyectohoussay.odonto.teletriaje.exception.ConflictoReservaException;
import com.proyectohoussay.odonto.teletriaje.exception.SolicitudTeletriajeNoEncontradaException;
import com.proyectohoussay.odonto.teletriaje.model.*;
import com.proyectohoussay.odonto.teletriaje.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TeletriajeService {

    private final SolicitudTeletriajeRepository solicitudRepository;
    private final CitaTeletriajeRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final OdontologoRepository odontologoRepository;
    private final TurnoRepository turnoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TeletriajeVideoProvider videoProvider;

    // Mutex map for slot reservation to guarantee thread-safe transactional reservations
    private final ConcurrentHashMap<String, Object> lockMap = new ConcurrentHashMap<>();

    public TeletriajeService(
            SolicitudTeletriajeRepository solicitudRepository,
            CitaTeletriajeRepository citaRepository,
            PacienteRepository pacienteRepository,
            OdontologoRepository odontologoRepository,
            TurnoRepository turnoRepository,
            UsuarioRepository usuarioRepository,
            TeletriajeVideoProvider videoProvider) {
        this.solicitudRepository = solicitudRepository;
        this.citaRepository = citaRepository;
        this.pacienteRepository = pacienteRepository;
        this.odontologoRepository = odontologoRepository;
        this.turnoRepository = turnoRepository;
        this.usuarioRepository = usuarioRepository;
        this.videoProvider = videoProvider;
    }

    public SolicitudTeletriaje crearSolicitud(SolicitudTeletriajeRequest request) {
        Paciente paciente = pacienteRepository.findById(request.pacienteId())
                .orElseThrow(() -> new PacienteNoEncontradoException(request.pacienteId()));

        NivelPrioridad prioridadSugerida = NivelPrioridad.fromString(request.prioridadSugerida());
        boolean signosAlarma = Boolean.TRUE.equals(request.signosAlarma());
        OrigenSolicitud origen = OrigenSolicitud.fromString(request.origen());

        SolicitudTeletriaje solicitud = new SolicitudTeletriaje(
                paciente,
                request.motivoConsulta(),
                request.descripcionSintomas(),
                prioridadSugerida,
                signosAlarma,
                origen
        );

        return solicitudRepository.save(solicitud);
    }

    /** Crea la solicitud y reserva el turno dentro de una única transacción. */
    @Transactional
    public CitaTeletriaje crearYReservar(ReservarTeletriajeCompletoRequest request,
                                        Long currentUserId, String currentRole) {
        SolicitudTeletriaje solicitud = crearSolicitud(request.solicitud());
        return reservarTurno(solicitud.getId(), request.reserva(), currentUserId, currentRole);
    }

    public List<SolicitudTeletriaje> obtenerColaPrioridad() {
        return solicitudRepository.obtenerColaPrioridad();
    }

    public SolicitudTeletriaje obtenerSolicitud(Long id) {
        return solicitudRepository.findById(id)
                .orElseThrow(() -> new SolicitudTeletriajeNoEncontradaException(id));
    }

    public List<SolicitudTeletriaje> listarSolicitudesPaciente(Long pacienteId) {
        return solicitudRepository.findByPacienteId(pacienteId);
    }

    @Transactional
    public SolicitudTeletriaje validarPrioridad(Long solicitudId, ValidarPrioridadRequest request,
                                               Long profesionalId, String profesionalNombre) {
        SolicitudTeletriaje solicitud = obtenerSolicitud(solicitudId);
        NivelPrioridad nuevaPrioridad = NivelPrioridad.fromString(request.nuevaPrioridad());

        solicitud.setPrioridadValidada(nuevaPrioridad);
        solicitud.setEstadoValidacion(EstadoValidacion.VALIDADA);
        solicitud.setFechaValidacion(LocalDateTime.now());
        solicitud.setProfesionalValidadorId(profesionalId);
        solicitud.setProfesionalValidadorNombre(profesionalNombre);
        if (request.observacionesClinicas() != null) {
            solicitud.setObservacionesClinicas(request.observacionesClinicas());
        }

        // Si se valida como ROJO o signos de alarma, marcar derivacion humana
        if (nuevaPrioridad == NivelPrioridad.ROJO || solicitud.isSignosAlarma()) {
            solicitud.setDerivacionHumana(true);
            if (solicitud.getEstado() == EstadoSolicitud.PENDIENTE) {
                solicitud.setEstado(EstadoSolicitud.DERIVADA_URGENTE);
            }
        }

        return solicitudRepository.save(solicitud);
    }

    @Transactional
    public CitaTeletriaje reservarTurno(Long solicitudId, ReservarTeletriajeRequest request,
                                        Long currentUserId, String currentRole) {
        SolicitudTeletriaje solicitud = obtenerSolicitud(solicitudId);

        if (solicitud.getEstado() == EstadoSolicitud.RESERVADA) {
            throw new ConflictoReservaException("La solicitud ya cuenta con un turno reservado previamente.");
        }

        if (request.fecha().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("No se permiten turnos en fechas anteriores a la actual.");
        }

        if (!Boolean.TRUE.equals(request.consentimientoInformado())) {
            throw new IllegalArgumentException("El consentimiento informado es obligatorio para confirmar la cita de teletriaje.");
        }

        Odontologo odontologo = odontologoRepository.findById(request.odontologoId())
                .orElseThrow(() -> new IllegalArgumentException("El odontólogo indicado no existe: " + request.odontologoId()));

        // Concurrencia transaccional: lock por slot
        String slotKey = request.odontologoId() + "_" + request.fecha() + "_" + request.hora();
        Object slotLock = lockMap.computeIfAbsent(slotKey, k -> new Object());

        synchronized (slotLock) {
            if (turnoRepository.existeTurnoActivoEnHorario(odontologo.getId(), request.fecha(), request.hora(), null)) {
                throw new ConflictoReservaException("El profesional ya no cuenta con disponibilidad en la fecha y hora seleccionadas.");
            }

            String motivo = request.motivo() != null && !request.motivo().isBlank()
                    ? request.motivo()
                    : "Teletriaje: " + solicitud.getMotivoConsulta();

            Turno turno = new Turno(
                    request.fecha(),
                    request.hora(),
                    motivo,
                    "CONFIRMADO",
                    solicitud.getPaciente(),
                    odontologo
            );
            Turno turnoGuardado = turnoRepository.save(turno);

            CitaTeletriaje cita = new CitaTeletriaje(
                    solicitud,
                    turnoGuardado,
                    solicitud.getPaciente(),
                    odontologo,
                    true
            );

            // Proveedor de video según configuración (TR-49)
            boolean requiereVideo = !Boolean.FALSE.equals(request.requiereVideollamada());
            if (requiereVideo && videoProvider.isEnabled()) {
                var enlaceOpt = videoProvider.generarEnlaceTemporal(cita.getId(), solicitud.getPaciente().getId());
                if (enlaceOpt.isPresent()) {
                    cita.setEnlaceVideo(enlaceOpt.get().url());
                    cita.setEnlaceExpiraEn(enlaceOpt.get().expiraEn());
                    cita.setEstadoSala(EstadoSalaTeletriaje.DISPONIBLE);
                } else {
                    cita.setEnlaceVideo(null);
                    cita.setEstadoSala(EstadoSalaTeletriaje.PENDIENTE_PROVEEDOR);
                }
            } else {
                // Sin proveedor de video o fuera de P0: pendiente, no llamada funcional
                cita.setEnlaceVideo(null);
                cita.setEstadoSala(EstadoSalaTeletriaje.PENDIENTE_PROVEEDOR);
            }

            CitaTeletriaje citaGuardada = citaRepository.save(cita);

            solicitud.setEstado(EstadoSolicitud.RESERVADA);
            solicitud.setTurno(turnoGuardado);
            solicitudRepository.save(solicitud);

            return citaGuardada;
        }
    }

    public CitaTeletriaje obtenerCita(Long id) {
        return citaRepository.findById(id)
                .orElseThrow(() -> new CitaTeletriajeNoEncontradaException(id));
    }

    public List<CitaTeletriaje> listarCitasPaciente(Long pacienteId) {
        return citaRepository.findByPacienteId(pacienteId);
    }

    public List<CitaTeletriaje> listarAgenda(LocalDate desde, LocalDate hasta, Long odontologoId) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new IllegalArgumentException("El rango de fechas de la agenda no es válido.");
        }
        if (hasta.isAfter(desde.plusDays(45))) {
            throw new IllegalArgumentException("La agenda admite rangos de hasta 46 días.");
        }
        return citaRepository.listarAgenda(desde, hasta, odontologoId);
    }

    @Transactional
    public CitaTeletriaje consultarAccesoCita(Long citaId, Long usuarioId, String rol, String username) {
        CitaTeletriaje cita = obtenerCita(citaId);

        // Control de acceso por rol: paciente ve solo su cita
        if ("PACIENTE".equalsIgnoreCase(rol)) {
            validarPropiedadPaciente(cita.getPaciente(), usuarioId, username);
        }

        // Revisar caducidad del enlace temporal
        if (cita.estaExpirada() && cita.getEstadoSala() == EstadoSalaTeletriaje.DISPONIBLE) {
            cita.setEstadoSala(EstadoSalaTeletriaje.EXPIRADA);
            cita = citaRepository.save(cita);
        }

        return cita;
    }

    @Transactional
    public CitaTeletriaje revocarEnlace(Long citaId) {
        CitaTeletriaje cita = obtenerCita(citaId);
        cita.setEstadoSala(EstadoSalaTeletriaje.REVOCADA);
        cita.setEnlaceVideo(null);
        return citaRepository.save(cita);
    }

    private void validarPropiedadPaciente(Paciente paciente, Long usuarioId, String username) {
        if (paciente == null) {
            throw new AccesoDenegadoTeletriajeException("Acceso no autorizado: cita sin paciente asociado.");
        }

        // Si el usuario autenticado tiene email o username, buscar usuario correspondiente
        if (usuarioId != null) {
            Optional<Usuario> userOpt = usuarioRepository.findById(usuarioId);
            if (userOpt.isPresent()) {
                Usuario user = userOpt.get();
                boolean coincideEmail = user.getEmail() != null && user.getEmail().equalsIgnoreCase(paciente.getEmail());
                boolean coincideUsername = user.getUsername() != null && user.getUsername().equalsIgnoreCase(paciente.getDni());
                if (!coincideEmail && !coincideUsername) {
                    throw new AccesoDenegadoTeletriajeException("Acceso denegado: el paciente solo puede consultar sus propias citas.");
                }
                return;
            }
        }

        // Si no hay match con el usuario del token
        if (username != null && !username.equalsIgnoreCase(paciente.getDni()) && !username.equalsIgnoreCase(paciente.getEmail())) {
            throw new AccesoDenegadoTeletriajeException("Acceso denegado: el paciente solo puede consultar sus propias citas.");
        }
    }
}
