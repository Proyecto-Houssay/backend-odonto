package com.proyectohoussay.odonto.chatbot;

import com.proyectohoussay.odonto.auth.UserRole;
import com.proyectohoussay.odonto.chatbot.dto.AgendaAsistenteDto;
import com.proyectohoussay.odonto.chatbot.dto.ChatResponse;
import com.proyectohoussay.odonto.chatbot.dto.DoctorDisponibleDto;
import com.proyectohoussay.odonto.chatbot.dto.ReservaChatbotRequest;
import com.proyectohoussay.odonto.chatbot.dto.ReservaChatbotResponse;
import com.proyectohoussay.odonto.chatbot.dto.TurnoSlotDto;
import com.proyectohoussay.odonto.dto.DisponibilidadAgendaResponse;
import com.proyectohoussay.odonto.dto.TurnoRequest;
import com.proyectohoussay.odonto.model.Odontologo;
import com.proyectohoussay.odonto.model.Turno;
import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.repository.OdontologoRepository;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import com.proyectohoussay.odonto.service.TurnoService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conecta el asistente virtual con la agenda real del consultorio:
 * consulta odontólogos activos, calcula horarios libres reales y registra turnos persistidos.
 */
@Service
public class ChatbotAgendaService {

    private static final Locale ES_AR = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("EEE dd/MM", ES_AR);
    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES_AR);
    private static final DateTimeFormatter HORA_CORTA = DateTimeFormatter.ofPattern("HH:mm");

    private static final Pattern ISO_DATE = Pattern.compile("\\b(\\d{4})-(\\d{2})-(\\d{2})\\b");
    private static final Pattern DMY_DATE = Pattern.compile("\\b(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})\\b");
    private static final Pattern DM_DATE = Pattern.compile("\\b(\\d{1,2})/(\\d{1,2})\\b");
    private static final Pattern TIME_COLON = Pattern.compile("\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b");
    private static final Pattern TIME_WORDS = Pattern.compile("\\b(?:a\\s+las|las)\\s+([01]?\\d|2[0-3])(?:\\s*hs?)?\\b");
    private static final Pattern TIME_HS = Pattern.compile("\\b([01]?\\d|2[0-3])\\s*hs?\\b");
    private static final Pattern MOTIVO_PATTERN = Pattern.compile("\\b(?:por|motivo(?:\\s+de)?)\\s+(?!favor\\b|la\\s+manana\\b|la\\s+tarde\\b)([^,.!?;\\n]{3,120})");

    private static final Map<String, DayOfWeek> DIAS_SEMANA = Map.of(
            "lunes", DayOfWeek.MONDAY,
            "martes", DayOfWeek.TUESDAY,
            "miercoles", DayOfWeek.WEDNESDAY,
            "jueves", DayOfWeek.THURSDAY,
            "viernes", DayOfWeek.FRIDAY,
            "sabado", DayOfWeek.SATURDAY,
            "domingo", DayOfWeek.SUNDAY
    );

    private final OdontologoRepository odontologoRepository;
    private final TurnoService turnoService;
    private final UsuarioRepository usuarioRepository;

    public ChatbotAgendaService(OdontologoRepository odontologoRepository,
                                TurnoService turnoService,
                                UsuarioRepository usuarioRepository) {
        this.odontologoRepository = odontologoRepository;
        this.turnoService = turnoService;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<DoctorDisponibleDto> listarDoctoresDisponibles() {
        return odontologoRepository.findAll().stream()
                .filter(Odontologo::isActivo)
                .sorted(Comparator.comparing(Odontologo::getApellido, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Odontologo::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(this::aDoctorDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TurnoSlotDto> buscarTurnosDisponibles(Long odontologoId, LocalDate fechaBase, int diasABuscar, int limiteTotal) {
        LocalDate inicio = (fechaBase == null || fechaBase.isBefore(LocalDate.now()))
                ? LocalDate.now()
                : fechaBase;
        int dias = Math.max(1, Math.min(diasABuscar, 14));
        int limite = Math.max(1, limiteTotal);

        List<DoctorDisponibleDto> doctores;
        if (odontologoId != null) {
            DoctorDisponibleDto elegido = listarDoctoresDisponibles().stream()
                    .filter(doc -> doc.id().equals(odontologoId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("El odontólogo seleccionado no está disponible."));
            doctores = List.of(elegido);
        } else {
            doctores = listarDoctoresDisponibles();
        }

        if (doctores.isEmpty()) {
            return List.of();
        }

        List<TurnoSlotDto> resultados = new ArrayList<>();
        int maxPorDoctorYDia = dias == 1 ? limite : 4;

        for (int offset = 0; offset < dias && resultados.size() < limite; offset++) {
            LocalDate fecha = inicio.plusDays(offset);
            for (DoctorDisponibleDto doctor : doctores) {
                DisponibilidadAgendaResponse disponibilidad = turnoService.listarHorariosDisponibles(doctor.id(), fecha);
                int agregadosHoy = 0;
                for (LocalTime hora : disponibilidad.horariosDisponibles()) {
                    resultados.add(new TurnoSlotDto(
                            doctor.id(),
                            doctor.nombreCompleto(),
                            doctor.especialidad(),
                            fecha,
                            hora,
                            etiquetaFecha(fecha),
                            hora.format(HORA_CORTA)
                    ));
                    agregadosHoy++;
                    if (resultados.size() >= limite || agregadosHoy >= maxPorDoctorYDia) {
                        break;
                    }
                }
                if (resultados.size() >= limite) {
                    break;
                }
            }
        }
        return List.copyOf(resultados);
    }

    @Transactional
    public ReservaChatbotResponse reservarDesdeChat(ReservaChatbotRequest request, Long userId, String role) {
        Long pacienteId = resolverPacienteId(request.pacienteId(), userId, role);

        DoctorDisponibleDto doctor = listarDoctoresDisponibles().stream()
                .filter(d -> d.id().equals(request.odontologoId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El odontólogo seleccionado no está activo o no existe."));

        DisponibilidadAgendaResponse agendaDia = turnoService.listarHorariosDisponibles(doctor.id(), request.fecha());
        if (!agendaDia.horariosDisponibles().contains(request.hora())) {
            throw new IllegalStateException("Ese horario ya no está disponible para Dr./Dra. "
                    + doctor.nombreCompleto() + ". Elegí otra franja libre.");
        }

        Turno turno = turnoService.crearTurno(new TurnoRequest(
                request.fecha(),
                request.hora(),
                request.motivo().trim(),
                "PENDIENTE",
                pacienteId,
                doctor.id()
        ));

        String nombrePaciente = turno.getPaciente().getNombre() + " " + turno.getPaciente().getApellido();
        String fechaTexto = formatearFechaLarga(turno.getFecha());
        String horaTexto = turno.getHora().format(HORA_CORTA);
        String mensaje = "¡Listo! Tu turno quedó confirmado para el " + fechaTexto
                + " a las " + horaTexto + " hs con Dr./Dra. " + doctor.nombreCompleto()
                + " (" + doctor.especialidad() + "). Motivo: " + turno.getMotivo()
                + " (Turno #" + turno.getId() + "). Ya aparece registrado en la sección Turnos.";

        return new ReservaChatbotResponse(
                turno.getId(),
                turno.getFecha(),
                turno.getHora(),
                turno.getMotivo(),
                turno.getEstado(),
                doctor.id(),
                doctor.nombreCompleto(),
                turno.getPaciente().getId(),
                nombrePaciente.trim(),
                mensaje
        );
    }

    @Transactional(readOnly = true)
    public ChatResponse responderConsultaDoctores(Long userId, String role) {
        List<DoctorDisponibleDto> doctores = listarDoctoresDisponibles();
        EstadoUsuario estadoUsuario = evaluarEstadoUsuario(userId, role);
        List<String> acciones = accionesParaRol(role);

        if (doctores.isEmpty()) {
            AgendaAsistenteDto agendaVacia = new AgendaAsistenteDto(
                    estadoUsuario.requiereFichaPaciente(),
                    estadoUsuario.esStaff(),
                    List.of(),
                    List.of(),
                    LocalDate.now(),
                    null,
                    null,
                    null,
                    null
            );
            return ChatResponse.conAgenda(
                    "Por el momento no hay doctores activos cargados en la agenda del consultorio. Consultá con recepción o pedile a administración que habilite los odontólogos en la sección Odontólogos.",
                    "doctores",
                    acciones,
                    agendaVacia
            );
        }

        List<TurnoSlotDto> proximosTurnos = buscarTurnosDisponibles(null, LocalDate.now(), 7, 16);
        Map<Long, TurnoSlotDto> primerTurnoPorDoctor = new LinkedHashMap<>();
        for (TurnoSlotDto slot : proximosTurnos) {
            primerTurnoPorDoctor.putIfAbsent(slot.odontologoId(), slot);
        }

        StringBuilder texto = new StringBuilder("¡Hola! Sí, estos son los doctores disponibles en NovaDent:\n");
        for (DoctorDisponibleDto doctor : doctores) {
            texto.append("\n• Dr./Dra. ").append(doctor.nombreCompleto())
                    .append(" — ").append(doctor.especialidad());
            TurnoSlotDto primero = primerTurnoPorDoctor.get(doctor.id());
            if (primero != null) {
                texto.append(" (próximo turno libre: ")
                        .append(primero.etiquetaFecha()).append(" a las ")
                        .append(primero.etiquetaHora()).append(" hs)");
            }
        }
        texto.append("\n\nElegí un profesional y horario acá abajo para sacar tu turno directamente.");

        TurnoSlotDto primerSlot = proximosTurnos.isEmpty() ? null : proximosTurnos.get(0);
        AgendaAsistenteDto agenda = new AgendaAsistenteDto(
                estadoUsuario.requiereFichaPaciente(),
                estadoUsuario.esStaff(),
                doctores,
                proximosTurnos,
                primerSlot != null ? primerSlot.fecha() : LocalDate.now(),
                primerSlot != null ? primerSlot.odontologoId() : doctores.get(0).id(),
                null,
                null,
                null
        );
        return ChatResponse.conAgenda(texto.toString(), "doctores", acciones, agenda);
    }

    @Transactional
    public ChatResponse responderConsultaOReservaTurnos(String mensaje, Long userId, String role) {
        List<DoctorDisponibleDto> doctores = listarDoctoresDisponibles();
        EstadoUsuario estadoUsuario = evaluarEstadoUsuario(userId, role);
        List<String> acciones = accionesParaRol(role);

        if (doctores.isEmpty()) {
            AgendaAsistenteDto agendaVacia = new AgendaAsistenteDto(
                    estadoUsuario.requiereFichaPaciente(),
                    estadoUsuario.esStaff(),
                    List.of(),
                    List.of(),
                    LocalDate.now(),
                    null,
                    null,
                    null,
                    null
            );
            return ChatResponse.conAgenda(
                    "Por el momento no hay odontólogos activos ni horarios disponibles cargados en la agenda. Consultá tus turnos registrados o comunicate con recepción.",
                    "turno",
                    acciones,
                    agendaVacia
            );
        }

        Optional<LocalDate> fechaPedidaOpt = extraerFecha(mensaje);
        Optional<LocalTime> horaPedidaOpt = extraerHora(mensaje);
        Optional<DoctorDisponibleDto> doctorPedidoOpt = extraerDoctor(mensaje, doctores);
        Optional<String> motivoPedidoOpt = extraerMotivo(mensaje);
        boolean quiereReservarAhora = tieneVerboReserva(mensaje);

        LocalDate hoy = LocalDate.now();
        boolean fechaEraPasada = fechaPedidaOpt.isPresent() && fechaPedidaOpt.get().isBefore(hoy);
        LocalDate fechaBusqueda = fechaPedidaOpt.filter(f -> !f.isBefore(hoy)).orElse(hoy);

        // Si pidió explícitamente reservar con fecha, hora y doctor inequívoco y es un paciente con ficha, intentamos reservar directo.
        if (quiereReservarAhora && !fechaEraPasada && fechaPedidaOpt.isPresent() && horaPedidaOpt.isPresent()) {
            DoctorDisponibleDto doctorElegido = doctorPedidoOpt.orElse(doctores.size() == 1 ? doctores.get(0) : null);
            LocalTime horaElegida = horaPedidaOpt.get();

            if (doctorElegido != null && !estadoUsuario.esStaff() && !estadoUsuario.requiereFichaPaciente() && userId != null) {
                DisponibilidadAgendaResponse disponibilidadDia = turnoService.listarHorariosDisponibles(doctorElegido.id(), fechaBusqueda);
                if (disponibilidadDia.horariosDisponibles().contains(horaElegida)) {
                    String motivoFinal = motivoPedidoOpt.orElse("Consulta odontológica");
                    ReservaChatbotResponse confirmacion = reservarDesdeChat(
                            new ReservaChatbotRequest(doctorElegido.id(), fechaBusqueda, horaElegida, motivoFinal, null),
                            userId,
                            role
                    );
                    AgendaAsistenteDto agendaConfirmada = new AgendaAsistenteDto(
                            false,
                            false,
                            doctores,
                            List.of(),
                            fechaBusqueda,
                            doctorElegido.id(),
                            horaElegida.format(HORA_CORTA),
                            motivoFinal,
                            confirmacion
                    );
                    return ChatResponse.conAgenda(confirmacion.mensaje(), "turno", acciones, agendaConfirmada);
                }

                // El horario pedido no estaba libre: mostramos los que sí están libres.
                List<TurnoSlotDto> alternativas = buscarTurnosDisponibles(doctorElegido.id(), fechaBusqueda, 7, 16);
                String textoNoLibre = construirMensajeHorariosAlternativos(doctorElegido, fechaBusqueda, horaElegida, alternativas);
                AgendaAsistenteDto agendaAlt = new AgendaAsistenteDto(
                        false,
                        false,
                        doctores,
                        alternativas,
                        alternativas.isEmpty() ? fechaBusqueda : alternativas.get(0).fecha(),
                        doctorElegido.id(),
                        null,
                        motivoPedidoOpt.orElse(null),
                        null
                );
                return ChatResponse.conAgenda(textoNoLibre, "turno", acciones, agendaAlt);
            }
        }

        Long doctorFiltroId = doctorPedidoOpt.map(DoctorDisponibleDto::id).orElse(null);
        List<TurnoSlotDto> slots = fechaPedidaOpt.isPresent() && !fechaEraPasada
                ? buscarTurnosDisponibles(doctorFiltroId, fechaBusqueda, 1, 24)
                : buscarTurnosDisponibles(doctorFiltroId, fechaBusqueda, 7, 16);

        boolean sinLugaresEnFechaPedida = false;
        if (slots.isEmpty() && fechaPedidaOpt.isPresent() && !fechaEraPasada) {
            sinLugaresEnFechaPedida = true;
            slots = buscarTurnosDisponibles(doctorFiltroId, fechaBusqueda.plusDays(1), 7, 16);
        }

        StringBuilder respuesta = new StringBuilder();
        if (fechaEraPasada) {
            respuesta.append("Esa fecha ya pasó, así que busqué los próximos horarios desde hoy.\n\n");
        } else if (sinLugaresEnFechaPedida) {
            respuesta.append("¡Hola! Para el ").append(etiquetaFecha(fechaBusqueda))
                    .append(" ya no quedan horarios libres, pero mirá los próximos turnos que sí tenemos disponibles:\n");
        } else {
            respuesta.append("¡Hola! Sí, mirá, tenemos estos turnos disponibles:\n");
        }

        if (slots.isEmpty()) {
            respuesta.append("\nNo encontramos franjas libres en los próximos 7 días. Probá elegir otra fecha en el calendario de abajo o consultá con recepción.");
        } else {
            respuesta.append(formatearResumenSlots(slots));
            respuesta.append("\n\nDoctores disponibles: ");
            respuesta.append(doctores.stream()
                    .map(d -> "Dr./Dra. " + d.nombreCompleto() + " (" + d.especialidad() + ")")
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));
            if (estadoUsuario.requiereFichaPaciente()) {
                respuesta.append("\n\nPara confirmar un turno completá tu DNI y fecha de nacimiento en el formulario de abajo y elegí tu horario.");
            } else {
                respuesta.append("\n\nElegí el doctor, día y horario acá abajo para sacar tu turno y que quede registrado en Turnos.");
            }
        }

        LocalDate fechaInicial = !slots.isEmpty() ? slots.get(0).fecha() : fechaBusqueda;
        Long doctorInicial = doctorPedidoOpt.map(DoctorDisponibleDto::id)
                .orElse(!slots.isEmpty() ? slots.get(0).odontologoId() : doctores.get(0).id());

        AgendaAsistenteDto agenda = new AgendaAsistenteDto(
                estadoUsuario.requiereFichaPaciente(),
                estadoUsuario.esStaff(),
                doctores,
                slots,
                fechaInicial,
                doctorInicial,
                horaPedidaOpt.map(h -> h.format(HORA_CORTA)).orElse(null),
                motivoPedidoOpt.orElse(null),
                null
        );
        return ChatResponse.conAgenda(respuesta.toString(), "turno", acciones, agenda);
    }

    private String construirMensajeHorariosAlternativos(DoctorDisponibleDto doctor, LocalDate fecha, LocalTime hora,
                                                        List<TurnoSlotDto> alternativas) {
        StringBuilder sb = new StringBuilder();
        sb.append("El horario de las ").append(hora.format(HORA_CORTA)).append(" hs el ")
                .append(etiquetaFecha(fecha)).append(" con Dr./Dra. ").append(doctor.nombreCompleto())
                .append(" no está disponible.");
        if (alternativas.isEmpty()) {
            sb.append(" No quedan turnos libres con este profesional en los próximos días; podés elegir otro doctor abajo.");
        } else {
            sb.append(" ¡Mirá! Tenemos estos turnos disponibles que sí podés reservar:\n")
                    .append(formatearResumenSlots(alternativas))
                    .append("\n\nHacé clic en uno de los horarios de abajo para confirmarlo.");
        }
        return sb.toString();
    }

    private String formatearResumenSlots(List<TurnoSlotDto> slots) {
        // Agrupamos por profesional + fecha para que la burbuja de chat sea clara y concisa.
        Map<String, List<String>> agrupados = new LinkedHashMap<>();
        for (TurnoSlotDto slot : slots) {
            String key = "• Dr./Dra. " + slot.odontologoNombre() + " (" + slot.especialidad() + ") — " + slot.etiquetaFecha();
            agrupados.computeIfAbsent(key, ignored -> new ArrayList<>());
            List<String> horas = agrupados.get(key);
            if (horas.size() < 5) {
                horas.add(slot.etiquetaHora());
            }
        }

        StringBuilder sb = new StringBuilder();
        int lineas = 0;
        for (Map.Entry<String, List<String>> entry : agrupados.entrySet()) {
            sb.append("\n").append(entry.getKey()).append(": ").append(String.join(", ", entry.getValue()));
            lineas++;
            if (lineas >= 5) {
                break;
            }
        }
        return sb.toString();
    }

    private Long resolverPacienteId(Long pacienteIdSolicitado, Long userId, String role) {
        boolean esPaciente = role == null || UserRole.PACIENTE.name().equalsIgnoreCase(role);
        if (esPaciente) {
            if (userId == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión de paciente inválida.");
            }
            Usuario usuario = usuarioRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La cuenta no existe."));
            if (!usuario.isActivo()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta está desactivada.");
            }
            if (usuario.getPaciente() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Todavía no tenés una ficha de paciente vinculada. Completá tus datos para poder reservar un turno.");
            }
            // Siempre usamos el paciente autenticado en el token para evitar reservas sobre terceros.
            return usuario.getPaciente().getId();
        }

        if (pacienteIdSolicitado == null || pacienteIdSolicitado <= 0) {
            throw new IllegalArgumentException("Seleccioná un paciente para registrar el turno.");
        }
        return pacienteIdSolicitado;
    }

    private EstadoUsuario evaluarEstadoUsuario(Long userId, String role) {
        if (role != null && !UserRole.PACIENTE.name().equalsIgnoreCase(role)) {
            return new EstadoUsuario(false, true);
        }
        if (userId == null) {
            return new EstadoUsuario(false, false);
        }
        boolean sinFicha = usuarioRepository.findById(userId)
                .map(usuario -> usuario.getPaciente() == null)
                .orElse(true);
        return new EstadoUsuario(sinFicha, false);
    }

    private List<String> accionesParaRol(String role) {
        if (role != null && !UserRole.PACIENTE.name().equalsIgnoreCase(role)) {
            return List.of("/turnos", "/odontologos");
        }
        return List.of("/mis-turnos");
    }

    private DoctorDisponibleDto aDoctorDto(Odontologo odontologo) {
        String especialidad = odontologo.getEspecialidad() != null
                ? odontologo.getEspecialidad().getNombre()
                : "Odontología General";
        String nombreCompleto = (odontologo.getNombre() + " " + odontologo.getApellido()).trim();
        return new DoctorDisponibleDto(
                odontologo.getId(),
                odontologo.getNombre(),
                odontologo.getApellido(),
                nombreCompleto,
                especialidad
        );
    }

    private String etiquetaFecha(LocalDate fecha) {
        LocalDate hoy = LocalDate.now();
        if (fecha.equals(hoy)) {
            return "hoy " + fecha.format(DateTimeFormatter.ofPattern("dd/MM"));
        }
        if (fecha.equals(hoy.plusDays(1))) {
            return "mañana " + fecha.format(DateTimeFormatter.ofPattern("dd/MM"));
        }
        return fecha.format(FECHA_CORTA);
    }

    private String formatearFechaLarga(LocalDate fecha) {
        return fecha.format(FECHA_LARGA);
    }

    static Optional<LocalDate> extraerFecha(String texto) {
        String normalizado = normalizar(texto);
        LocalDate hoy = LocalDate.now();

        Matcher iso = ISO_DATE.matcher(normalizado);
        if (iso.find()) {
            try {
                return Optional.of(LocalDate.of(
                        Integer.parseInt(iso.group(1)),
                        Integer.parseInt(iso.group(2)),
                        Integer.parseInt(iso.group(3))));
            } catch (DateTimeException ignored) { }
        }

        Matcher dmy = DMY_DATE.matcher(normalizado);
        if (dmy.find()) {
            try {
                return Optional.of(LocalDate.of(
                        Integer.parseInt(dmy.group(3)),
                        Integer.parseInt(dmy.group(2)),
                        Integer.parseInt(dmy.group(1))));
            } catch (DateTimeException ignored) { }
        }

        Matcher dm = DM_DATE.matcher(normalizado);
        if (dm.find()) {
            try {
                LocalDate candidata = LocalDate.of(
                        hoy.getYear(),
                        Integer.parseInt(dm.group(2)),
                        Integer.parseInt(dm.group(1)));
                if (candidata.isBefore(hoy.minusMonths(2))) {
                    candidata = candidata.plusYears(1);
                }
                return Optional.of(candidata);
            } catch (DateTimeException ignored) { }
        }

        if (normalizado.contains("pasado manana")) {
            return Optional.of(hoy.plusDays(2));
        }
        if (Pattern.compile("\\bmanana\\b").matcher(normalizado).find()) {
            return Optional.of(hoy.plusDays(1));
        }
        if (Pattern.compile("\\bhoy\\b").matcher(normalizado).find()) {
            return Optional.of(hoy);
        }

        for (Map.Entry<String, DayOfWeek> entry : DIAS_SEMANA.entrySet()) {
            if (Pattern.compile("\\b" + entry.getKey() + "\\b").matcher(normalizado).find()) {
                int diff = (entry.getValue().getValue() - hoy.getDayOfWeek().getValue() + 7) % 7;
                return Optional.of(hoy.plusDays(diff));
            }
        }
        return Optional.empty();
    }

    static Optional<LocalTime> extraerHora(String texto) {
        String normalizado = normalizar(texto);
        Matcher colon = TIME_COLON.matcher(normalizado);
        if (colon.find()) {
            return Optional.of(LocalTime.of(Integer.parseInt(colon.group(1)), Integer.parseInt(colon.group(2))));
        }
        Matcher words = TIME_WORDS.matcher(normalizado);
        if (words.find()) {
            return Optional.of(LocalTime.of(Integer.parseInt(words.group(1)), 0));
        }
        Matcher hs = TIME_HS.matcher(normalizado);
        if (hs.find()) {
            return Optional.of(LocalTime.of(Integer.parseInt(hs.group(1)), 0));
        }
        return Optional.empty();
    }

    static Optional<DoctorDisponibleDto> extraerDoctor(String texto, List<DoctorDisponibleDto> doctores) {
        String normalizado = normalizar(texto);

        for (DoctorDisponibleDto doctor : doctores) {
            String completo = normalizar(doctor.nombreCompleto());
            if (completo.length() >= 4 && normalizado.contains(completo)) {
                return Optional.of(doctor);
            }
        }

        List<DoctorDisponibleDto> porApellido = doctores.stream()
                .filter(d -> {
                    String apellido = normalizar(d.apellido()).trim();
                    return apellido.length() >= 3
                            && Pattern.compile("\\b" + Pattern.quote(apellido) + "\\b").matcher(normalizado).find();
                })
                .toList();
        if (porApellido.size() == 1) {
            return Optional.of(porApellido.get(0));
        }

        List<DoctorDisponibleDto> porNombre = doctores.stream()
                .filter(d -> {
                    String nombre = normalizar(d.nombre()).trim();
                    return nombre.length() >= 3
                            && Pattern.compile("\\b" + Pattern.quote(nombre) + "\\b").matcher(normalizado).find();
                })
                .toList();
        if (porNombre.size() == 1) {
            return Optional.of(porNombre.get(0));
        }

        return Optional.empty();
    }

    static Optional<String> extraerMotivo(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = MOTIVO_PATTERN.matcher(texto.trim());
        while (matcher.find()) {
            String raw = matcher.group(1).trim();
            // Evitamos capturar restos de "por favor" o frases horarias
            String norm = normalizar(raw);
            if (norm.startsWith("favor") || norm.startsWith("la manana") || norm.startsWith("la tarde")
                    || norm.startsWith("la noche")) {
                continue;
            }
            raw = raw.replaceAll("(?i)\\s+(?:para\\s+)?(?:hoy|mañana|manana|pasado\\s+mañana|pasado\\s+manana|el\\s+(?:lunes|martes|mi[eé]rcoles|jueves|viernes|s[aá]bado|domingo|\\d{1,2}[/-]\\d{1,2})|a\\s+las\\s+\\d{1,2}|con\\s+(?:el\\s+|la\\s+)?dr).*$", "").trim();
            if (raw.length() >= 3) {
                return Optional.of(Character.toUpperCase(raw.charAt(0)) + raw.substring(1));
            }
        }
        return Optional.empty();
    }

    private static boolean tieneVerboReserva(String texto) {
        String normalizado = normalizar(texto);
        return Pattern.compile("\\b(sacar|sacame|reservar|reservame|agendar|agendame|confirmar|anotame)\\b")
                .matcher(normalizado).find();
    }

    private static String normalizar(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
    }

    private record EstadoUsuario(boolean requiereFichaPaciente, boolean esStaff) {
    }
}
