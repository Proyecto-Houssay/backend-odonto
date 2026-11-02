package com.proyectohoussay.odonto.teletriaje;

import com.proyectohoussay.odonto.model.Odontologo;
import com.proyectohoussay.odonto.model.Turno;
import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.patient.Paciente;
import com.proyectohoussay.odonto.patient.PacienteRepository;
import com.proyectohoussay.odonto.repository.OdontologoRepository;
import com.proyectohoussay.odonto.repository.TurnoRepository;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import com.proyectohoussay.odonto.teletriaje.dto.ReservarTeletriajeRequest;
import com.proyectohoussay.odonto.teletriaje.dto.SolicitudTeletriajeRequest;
import com.proyectohoussay.odonto.teletriaje.dto.ValidarPrioridadRequest;
import com.proyectohoussay.odonto.teletriaje.exception.AccesoDenegadoTeletriajeException;
import com.proyectohoussay.odonto.teletriaje.exception.ConflictoReservaException;
import com.proyectohoussay.odonto.teletriaje.model.*;
import com.proyectohoussay.odonto.teletriaje.repository.CitaTeletriajeRepository;
import com.proyectohoussay.odonto.teletriaje.repository.SolicitudTeletriajeRepository;
import com.proyectohoussay.odonto.teletriaje.service.TeletriajeService;
import com.proyectohoussay.odonto.teletriaje.service.TeletriajeVideoProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeletriajeServiceTest {

    @Mock
    private SolicitudTeletriajeRepository solicitudRepository;

    @Mock
    private CitaTeletriajeRepository citaRepository;

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private OdontologoRepository odontologoRepository;

    @Mock
    private TurnoRepository turnoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private TeletriajeVideoProvider videoProviderDisabled;
    private TeletriajeVideoProvider videoProviderEnabled;

    private TeletriajeService service;

    private Paciente paciente1;
    private Paciente paciente2;
    private Odontologo odontologo;

    @BeforeEach
    void setUp() {
        videoProviderDisabled = new TeletriajeVideoProvider(false, "https://telemedicina.test/sala", 60);
        videoProviderEnabled = new TeletriajeVideoProvider(true, "https://telemedicina.test/sala", 60);

        service = new TeletriajeService(
                solicitudRepository,
                citaRepository,
                pacienteRepository,
                odontologoRepository,
                turnoRepository,
                usuarioRepository,
                videoProviderDisabled
        );

        paciente1 = new Paciente();
        paciente1.setId(10L);
        paciente1.setNombre("Ana");
        paciente1.setApellido("Gomez");
        paciente1.setDni("30111222");
        paciente1.setEmail("ana@example.com");

        paciente2 = new Paciente();
        paciente2.setId(20L);
        paciente2.setNombre("Carlos");
        paciente2.setApellido("Perez");
        paciente2.setDni("35333444");
        paciente2.setEmail("carlos@example.com");

        odontologo = new Odontologo();
        odontologo.setId(5L);
        odontologo.setNombre("Dr. Martin");
        odontologo.setApellido("Lopez");
        odontologo.setMatricula("MN-9988");
    }

    @Test
    @DisplayName("Crear solicitud con signos de alarma activa derivación humana y estado DERIVADA_URGENTE")
    void crearSolicitudConSignosDeAlarma() {
        when(pacienteRepository.findById(10L)).thenReturn(Optional.of(paciente1));
        when(solicitudRepository.save(any(SolicitudTeletriaje.class))).thenAnswer(inv -> inv.getArgument(0));

        var req = new SolicitudTeletriajeRequest(10L, "Dolor punzante y fiebre", "Fiebre 39 y trismo", "ROJO", true, "WEB");
        SolicitudTeletriaje guardada = service.crearSolicitud(req);

        assertThat(guardada.isSignosAlarma()).isTrue();
        assertThat(guardada.isDerivacionHumana()).isTrue();
        assertThat(guardada.getEstado()).isEqualTo(EstadoSolicitud.DERIVADA_URGENTE);
        assertThat(guardada.getEstadoValidacion()).isEqualTo(EstadoValidacion.SUGERIDA);
    }

    @Test
    @DisplayName("Validación profesional actualiza prioridad, estado y observaciones clínicas")
    void validarPrioridadProfesional() {
        var solicitud = new SolicitudTeletriaje(paciente1, "Sensibilidad dental", "Leve", NivelPrioridad.VERDE, false, OrigenSolicitud.WEB);
        solicitud.setId(1L);

        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(solicitudRepository.save(any(SolicitudTeletriaje.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new ValidarPrioridadRequest("AMARILLO", "Se observa riesgo de caries profunda en sector molar");
        SolicitudTeletriaje res = service.validarPrioridad(1L, request, 99L, "Dr. Martinez");

        assertThat(res.getEstadoValidacion()).isEqualTo(EstadoValidacion.VALIDADA);
        assertThat(res.getPrioridadValidada()).isEqualTo(NivelPrioridad.AMARILLO);
        assertThat(res.getPrioridadEfectiva()).isEqualTo(NivelPrioridad.AMARILLO);
        assertThat(res.getProfesionalValidadorId()).isEqualTo(99L);
        assertThat(res.getProfesionalValidadorNombre()).isEqualTo("Dr. Martinez");
        assertThat(res.getObservacionesClinicas()).contains("caries profunda");
        assertThat(res.getFechaValidacion()).isNotNull();
    }

    @Test
    @DisplayName("Reserva transaccional rechaza slot conflictivo si odontólogo ya está ocupado")
    void reservarRechazaSiSlotOcupado() {
        var solicitud = new SolicitudTeletriaje(paciente1, "Dolor", "Moderado", NivelPrioridad.AMARILLO, false, OrigenSolicitud.WEB);
        solicitud.setId(1L);

        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(odontologoRepository.findById(5L)).thenReturn(Optional.of(odontologo));

        LocalDate maniana = LocalDate.now().plusDays(1);
        LocalTime hora = LocalTime.of(10, 0);

        when(turnoRepository.existeTurnoActivoEnHorario(5L, maniana, hora, null)).thenReturn(true);

        var req = new ReservarTeletriajeRequest(5L, maniana, hora, "Consulta", true, true);

        assertThatThrownBy(() -> service.reservarTurno(1L, req, 1L, "ADMINISTRADOR"))
                .isInstanceOf(ConflictoReservaException.class)
                .hasMessageContaining("disponibilidad");

        verify(turnoRepository, never()).save(any());
        verify(citaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reserva requiere consentimiento informado obligatorio")
    void reservarRequiereConsentimiento() {
        var solicitud = new SolicitudTeletriaje(paciente1, "Dolor", "Moderado", NivelPrioridad.AMARILLO, false, OrigenSolicitud.WEB);
        solicitud.setId(1L);

        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));

        LocalDate maniana = LocalDate.now().plusDays(1);
        LocalTime hora = LocalTime.of(10, 0);

        var req = new ReservarTeletriajeRequest(5L, maniana, hora, "Consulta", false, true);

        assertThatThrownBy(() -> service.reservarTurno(1L, req, 1L, "ADMINISTRADOR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consentimiento informado es obligatorio");
    }

    @Test
    @DisplayName("Sin proveedor de video configurado, enlace permanece nulo y sala en PENDIENTE_PROVEEDOR")
    void reservarSinProveedorMantienePendiente() {
        var solicitud = new SolicitudTeletriaje(paciente1, "Dolor", "Moderado", NivelPrioridad.AMARILLO, false, OrigenSolicitud.WEB);
        solicitud.setId(1L);

        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(odontologoRepository.findById(5L)).thenReturn(Optional.of(odontologo));

        LocalDate maniana = LocalDate.now().plusDays(1);
        LocalTime hora = LocalTime.of(11, 0);

        when(turnoRepository.existeTurnoActivoEnHorario(5L, maniana, hora, null)).thenReturn(false);
        when(turnoRepository.save(any(Turno.class))).thenAnswer(inv -> {
            Turno t = inv.getArgument(0);
            t.setId(100L);
            return t;
        });
        when(citaRepository.save(any(CitaTeletriaje.class))).thenAnswer(inv -> {
            CitaTeletriaje c = inv.getArgument(0);
            c.setId(200L);
            return c;
        });

        var req = new ReservarTeletriajeRequest(5L, maniana, hora, "Consulta", true, true);
        CitaTeletriaje cita = service.reservarTurno(1L, req, 1L, "ADMINISTRADOR");

        assertThat(cita.getEnlaceVideo()).isNull();
        assertThat(cita.getEstadoSala()).isEqualTo(EstadoSalaTeletriaje.PENDIENTE_PROVEEDOR);
        assertThat(cita.isEnlaceFuncional()).isFalse();
        assertThat(cita.isGrabacionHabilitada()).isFalse(); // no guardar video por defecto
        assertThat(cita.isConsentimientoInformado()).isTrue();
    }

    @Test
    @DisplayName("Con proveedor de video habilitado, genera enlace temporal con fecha de expiración")
    void reservarConProveedorGeneraEnlaceTemporal() {
        var serviceConVideo = new TeletriajeService(
                solicitudRepository,
                citaRepository,
                pacienteRepository,
                odontologoRepository,
                turnoRepository,
                usuarioRepository,
                videoProviderEnabled
        );

        var solicitud = new SolicitudTeletriaje(paciente1, "Dolor", "Moderado", NivelPrioridad.AMARILLO, false, OrigenSolicitud.WEB);
        solicitud.setId(1L);

        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(odontologoRepository.findById(5L)).thenReturn(Optional.of(odontologo));

        LocalDate maniana = LocalDate.now().plusDays(1);
        LocalTime hora = LocalTime.of(14, 0);

        when(turnoRepository.existeTurnoActivoEnHorario(5L, maniana, hora, null)).thenReturn(false);
        when(turnoRepository.save(any(Turno.class))).thenAnswer(inv -> {
            Turno t = inv.getArgument(0);
            t.setId(101L);
            return t;
        });
        when(citaRepository.save(any(CitaTeletriaje.class))).thenAnswer(inv -> {
            CitaTeletriaje c = inv.getArgument(0);
            c.setId(201L);
            return c;
        });

        var req = new ReservarTeletriajeRequest(5L, maniana, hora, "Consulta", true, true);
        CitaTeletriaje cita = serviceConVideo.reservarTurno(1L, req, 1L, "ADMINISTRADOR");

        assertThat(cita.getEnlaceVideo()).isNotNull().startsWith("https://telemedicina.test/sala");
        assertThat(cita.getEnlaceExpiraEn()).isAfter(LocalDateTime.now());
        assertThat(cita.getEstadoSala()).isEqualTo(EstadoSalaTeletriaje.DISPONIBLE);
        assertThat(cita.isEnlaceFuncional()).isTrue();
    }

    @Test
    @DisplayName("Paciente solo accede a su propia cita; 403 Forbidden ante paciente ajeno")
    void controlAccesoPorRolPaciente() {
        var turno = new Turno(LocalDate.now().plusDays(1), LocalTime.of(10, 0), "Motivo", "CONFIRMADO", paciente1, odontologo);
        var cita = new CitaTeletriaje(null, turno, paciente1, odontologo, true);
        cita.setId(10L);

        when(citaRepository.findById(10L)).thenReturn(Optional.of(cita));

        var usuarioAjeno = new Usuario("carlos", "Carlos", "Perez", "carlos@example.com", "pass", "PACIENTE", true);
        usuarioAjeno.setId(77L);
        when(usuarioRepository.findById(77L)).thenReturn(Optional.of(usuarioAjeno));

        // Intento de acceso de un paciente a la cita de otro paciente
        assertThatThrownBy(() -> service.consultarAccesoCita(10L, 77L, "PACIENTE", "carlos"))
                .isInstanceOf(AccesoDenegadoTeletriajeException.class)
                .hasMessageContaining("solo puede consultar sus propias citas");

        // Personal profesional sí tiene acceso
        CitaTeletriaje citaOk = service.consultarAccesoCita(10L, 99L, "ODONTOLOGO", "dr_odontologo");
        assertThat(citaOk).isNotNull();
    }

    @Test
    @DisplayName("Revocación y caducidad del enlace temporal")
    void revocacionYCaducidadEnlace() {
        var turno = new Turno(LocalDate.now().plusDays(1), LocalTime.of(10, 0), "Motivo", "CONFIRMADO", paciente1, odontologo);
        var cita = new CitaTeletriaje(null, turno, paciente1, odontologo, true);
        cita.setId(15L);
        cita.setEstadoSala(EstadoSalaTeletriaje.DISPONIBLE);
        cita.setEnlaceVideo("https://telemedicina.test/sala/uuid");
        cita.setEnlaceExpiraEn(LocalDateTime.now().minusMinutes(5)); // Ya expirada

        when(citaRepository.findById(15L)).thenReturn(Optional.of(cita));
        when(citaRepository.save(any(CitaTeletriaje.class))).thenAnswer(inv -> inv.getArgument(0));

        // Al consultar una cita expirada, pasa a EXPIRADA y deja de ser funcional
        CitaTeletriaje consultada = service.consultarAccesoCita(15L, 1L, "ADMINISTRADOR", "admin");
        assertThat(consultada.getEstadoSala()).isEqualTo(EstadoSalaTeletriaje.EXPIRADA);
        assertThat(consultada.isEnlaceFuncional()).isFalse();

        // Revocación manual
        CitaTeletriaje revocada = service.revocarEnlace(15L);
        assertThat(revocada.getEstadoSala()).isEqualTo(EstadoSalaTeletriaje.REVOCADA);
        assertThat(revocada.getEnlaceVideo()).isNull();
        assertThat(revocada.isEnlaceFuncional()).isFalse();
    }

    @Test
    @DisplayName("Carrera de reserva simultánea: dos hilos concurrentes compitiendo por el mismo slot no duplican turnos")
    void carreraDeReservaSimultaneaConcurrente() throws Exception {
        LocalDate fecha = LocalDate.now().plusDays(2);
        LocalTime hora = LocalTime.of(16, 0);

        var sol1 = new SolicitudTeletriaje(paciente1, "Molestia 1", "Sintoma 1", NivelPrioridad.AMARILLO, false, OrigenSolicitud.WEB);
        sol1.setId(101L);
        var sol2 = new SolicitudTeletriaje(paciente2, "Molestia 2", "Sintoma 2", NivelPrioridad.AMARILLO, false, OrigenSolicitud.WEB);
        sol2.setId(102L);

        when(solicitudRepository.findById(101L)).thenReturn(Optional.of(sol1));
        when(solicitudRepository.findById(102L)).thenReturn(Optional.of(sol2));
        when(odontologoRepository.findById(5L)).thenReturn(Optional.of(odontologo));

        // Simular que en la primera verificación existeTurnoActivoEnHorario da false,
        // pero una vez guardado el primer turno, las verificaciones posteriores devuelven true
        AtomicInteger guardados = new AtomicInteger(0);
        when(turnoRepository.existeTurnoActivoEnHorario(eq(5L), eq(fecha), eq(hora), any()))
                .thenAnswer(inv -> guardados.get() > 0);

        when(turnoRepository.save(any(Turno.class))).thenAnswer(inv -> {
            guardados.incrementAndGet();
            Turno t = inv.getArgument(0);
            t.setId(500L + guardados.get());
            return t;
        });
        when(citaRepository.save(any(CitaTeletriaje.class))).thenAnswer(inv -> {
            CitaTeletriaje c = inv.getArgument(0);
            c.setId(600L);
            return c;
        });

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger exitos = new AtomicInteger(0);
        AtomicInteger conflictos = new AtomicInteger(0);

        var req1 = new ReservarTeletriajeRequest(5L, fecha, hora, "Motivo 1", true, false);
        var req2 = new ReservarTeletriajeRequest(5L, fecha, hora, "Motivo 2", true, false);

        executor.submit(() -> {
            try {
                startLatch.await();
                service.reservarTurno(101L, req1, 1L, "ADMINISTRADOR");
                exitos.incrementAndGet();
            } catch (ConflictoReservaException e) {
                conflictos.incrementAndGet();
            } catch (Exception e) {
                // error inesperado
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                service.reservarTurno(102L, req2, 2L, "ADMINISTRADOR");
                exitos.incrementAndGet();
            } catch (ConflictoReservaException e) {
                conflictos.incrementAndGet();
            } catch (Exception e) {
                // error inesperado
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Exactamente una solicitud obtiene el turno y la otra recibe conflicto 409
        assertThat(exitos.get()).isEqualTo(1);
        assertThat(conflictos.get()).isEqualTo(1);
        assertThat(guardados.get()).isEqualTo(1);
    }
}
