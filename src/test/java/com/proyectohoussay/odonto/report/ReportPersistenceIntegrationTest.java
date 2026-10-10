package com.proyectohoussay.odonto.report;

import com.proyectohoussay.odonto.model.Insumo;
import com.proyectohoussay.odonto.payment.Pago;
import com.proyectohoussay.odonto.payment.PagoEstado;
import com.proyectohoussay.odonto.payment.PagoRepository;
import com.proyectohoussay.odonto.model.Especialidad;
import com.proyectohoussay.odonto.model.Odontologo;
import com.proyectohoussay.odonto.model.Turno;
import com.proyectohoussay.odonto.patient.Paciente;
import com.proyectohoussay.odonto.patient.PacienteRepository;
import com.proyectohoussay.odonto.repository.EspecialidadRepository;
import com.proyectohoussay.odonto.repository.InsumoRepository;
import com.proyectohoussay.odonto.repository.OdontologoRepository;
import com.proyectohoussay.odonto.repository.TurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ReportPersistenceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InsumoRepository insumoRepository;

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private EspecialidadRepository especialidadRepository;

    @Autowired
    private OdontologoRepository odontologoRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @BeforeEach
    void setUp() {
        pagoRepository.deleteAll();
        turnoRepository.deleteAll();
        odontologoRepository.deleteAll();
        pacienteRepository.deleteAll();
        especialidadRepository.deleteAll();
        insumoRepository.deleteAll();
        insumoRepository.save(new Insumo("Fresas de diamante", "Instrumental", 40, "unidades", 10));
        insumoRepository.save(new Insumo("Baberos descartables", "Descartables", 5, "paquetes", 10));
    }

    @Test
    void getInventarioRetornaDatosPersistidosReales() throws Exception {
        mockMvc.perform(get("/api/reports/inventario")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].nombre").value("Fresas de diamante"))
                .andExpect(jsonPath("$[0].cantidadDisponible").value(40))
                .andExpect(jsonPath("$[0].estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$[1].nombre").value("Baberos descartables"))
                .andExpect(jsonPath("$[1].cantidadDisponible").value(5))
                .andExpect(jsonPath("$[1].estado").value("BAJO_STOCK"));
    }

    @Test
    void getInventarioVacioRetornaListaVacia() throws Exception {
        insumoRepository.deleteAll();

        mockMvc.perform(get("/api/reports/inventario")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getResumenReflejaCantidadDeItemsPersistidos() throws Exception {
        mockMvc.perform(get("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modulo").value("Informes y Gestión Odontológica"))
                .andExpect(jsonPath("$.totalItemsInventario").value(2));
    }

    @Test
    void reportesFinancierosYAnualesEstanDisponiblesConPeriodo() throws Exception {
        mockMvc.perform(get("/api/reports/ganancias")
                        .param("desde", "2026-04-01")
                        .param("hasta", "2026-04-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desde").value("2026-04-01"))
                .andExpect(jsonPath("$.hasta").value("2026-04-30"))
                .andExpect(jsonPath("$.totalIngresosBrutos").value(0));

        mockMvc.perform(get("/api/reports/cobros")
                        .param("desde", "2026-04-01")
                        .param("hasta", "2026-04-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCobrado").value(0))
                .andExpect(jsonPath("$.cobros").isArray());

        mockMvc.perform(get("/api/reports/anual").param("anio", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anio").value(2026))
                .andExpect(jsonPath("$.totalAtenciones").value(0))
                .andExpect(jsonPath("$.meses.length()").value(12));
    }

    @Test
    void rechazaPeriodosInvertidosYParametrosAusentes() throws Exception {
        mockMvc.perform(get("/api/reports/ganancias")
                        .param("desde", "2026-04-30")
                        .param("hasta", "2026-04-01"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/reports/cobros"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resumenSoloAnunciaInformesConEndpointImplementado() throws Exception {
        mockMvc.perform(get("/api/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportesDisponibles").value(org.hamcrest.Matchers.containsInAnyOrder(
                        "inventario", "atenciones", "ganancias", "cobros", "anual")));
    }

    @Test
    void reportesDeCobrosYGananciasUsanCobrosPagadosYPeriodoInclusivo() throws Exception {
        registrarPago("10.50", "2026-04-01", "COBRADO");
        registrarPago("20.00", "2026-04-30", "COBRADO");
        registrarPago("99.00", "2026-04-15", "PENDIENTE");
        registrarPago("80.00", "2026-03-31", "COBRADO");

        mockMvc.perform(get("/api/reports/ganancias")
                        .param("desde", "2026-04-01")
                        .param("hasta", "2026-04-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIngresosBrutos").value(30.5))
                .andExpect(jsonPath("$.cantidadCobros").value(2));

        mockMvc.perform(get("/api/reports/cobros")
                        .param("desde", "2026-04-01")
                        .param("hasta", "2026-04-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCobrado").value(30.5))
                .andExpect(jsonPath("$.cobros.length()").value(2));
    }

    @Test
    void informeDeAtencionesCuentaSoloTurnosAtendidosDentroDeFechasInclusivas() throws Exception {
        Paciente paciente = new Paciente();
        paciente.setNombre("Ana");
        paciente.setApellido("Lopez");
        paciente.setDni("report-301");
        paciente.setEmail("report-ana@example.com");
        paciente.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        paciente = pacienteRepository.save(paciente);
        Especialidad especialidad = especialidadRepository.save(new Especialidad("General", "Atención general"));
        Odontologo odontologo = odontologoRepository.save(new Odontologo(
                "Laura", "Rios", "REPORT-MN-1", "report-laura@example.com", "111111", especialidad));

        guardarTurno(LocalDate.of(2026, 4, 1), LocalTime.of(9, 0), "Inicio", "ATENDIDO", paciente, odontologo);
        guardarTurno(LocalDate.of(2026, 4, 30), LocalTime.of(10, 0), "Fin", "atendido", paciente, odontologo);
        guardarTurno(LocalDate.of(2026, 4, 15), LocalTime.of(11, 0), "Cancelado", "CANCELADO", paciente, odontologo);
        guardarTurno(LocalDate.of(2026, 3, 31), LocalTime.of(12, 0), "Anterior", "ATENDIDO", paciente, odontologo);

        mockMvc.perform(get("/api/reports/atenciones")
                        .param("desde", "2026-04-01")
                        .param("hasta", "2026-04-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAtenciones").value(2))
                .andExpect(jsonPath("$.detalle.length()").value(2));
    }

    @Test
    void informeAnualAgrupaCobrosYAtencionesPorMesYExcluyeElAnioSiguiente() throws Exception {
        Paciente paciente = crearPaciente();
        Especialidad especialidad = especialidadRepository.save(new Especialidad("General", "Atención general"));
        Odontologo odontologo = odontologoRepository.save(new Odontologo(
                "Laura", "Rios", "REPORT-MN-2", "report-laura2@example.com", "111112", especialidad));

        registrarPago("100.00", "2025-01-01", "COBRADO");
        registrarPago("50.00", "2025-12-31", "COBRADO");
        registrarPago("200.00", "2026-01-01", "COBRADO");
        guardarTurno(LocalDate.of(2025, 1, 1), LocalTime.NOON, "Enero", "ATENDIDO", paciente, odontologo);
        guardarTurno(LocalDate.of(2025, 12, 31), LocalTime.NOON, "Diciembre", "ATENDIDO", paciente, odontologo);
        guardarTurno(LocalDate.of(2026, 1, 1), LocalTime.NOON, "Siguiente año", "ATENDIDO", paciente, odontologo);

        mockMvc.perform(get("/api/reports/anual").param("anio", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCobrado").value(150.0))
                .andExpect(jsonPath("$.totalAtenciones").value(2))
                .andExpect(jsonPath("$.meses[0].totalCobrado").value(100.0))
                .andExpect(jsonPath("$.meses[0].totalAtenciones").value(1))
                .andExpect(jsonPath("$.meses[11].totalCobrado").value(50.0))
                .andExpect(jsonPath("$.meses[11].totalAtenciones").value(1));
    }

    @Test
    void registrarPagoRechazaMontoNuloOCeroYEstadoDesconocido() throws Exception {
        mockMvc.perform(post("/api/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto": 0, "fecha": "2026-04-15", "estado": "COBRADO", "metodo": "EFECTIVO"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto": 10, "fecha": "2026-04-15", "estado": "PAGADO", "metodo": "EFECTIVO"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrarPagoRechazaMontoConMasDeDosDecimalesSinPersistirlo() throws Exception {
        mockMvc.perform(post("/api/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto": 1.239, "fecha": "2026-04-15", "estado": "COBRADO", "metodo": "EFECTIVO"}
                                """))
                .andExpect(status().isBadRequest());

        org.assertj.core.api.Assertions.assertThat(pagoRepository.count()).isZero();
    }

    @Test
    void registrarPagoRechazaCobradoConFechaFuturaPeroPermitePendienteFuturo() throws Exception {
        LocalDate futuro = LocalDate.now().plusDays(1);

        mockMvc.perform(post("/api/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto": 20.00, "fecha": "%s", "estado": "COBRADO", "metodo": "EFECTIVO"}
                                """.formatted(futuro)))
                .andExpect(status().isBadRequest());

        org.assertj.core.api.Assertions.assertThat(pagoRepository.count()).isZero();

        mockMvc.perform(post("/api/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto": 20.00, "fecha": "%s", "estado": "PENDIENTE", "metodo": "EFECTIVO"}
                                """.formatted(futuro)))
                .andExpect(status().isCreated());
    }

    @Test
    void reportesFinancierosYAnualIgnoranPagosCobradosFuturosYaPersistidos() throws Exception {
        LocalDate hoy = LocalDate.now();
        LocalDate futuro = hoy.plusDays(1);
        pagoRepository.save(new Pago(new java.math.BigDecimal("99.99"), futuro, PagoEstado.COBRADO, "EFECTIVO"));

        mockMvc.perform(get("/api/reports/cobros")
                        .param("desde", hoy.toString())
                        .param("hasta", futuro.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCobrado").value(0))
                .andExpect(jsonPath("$.cantidadCobros").value(0))
                .andExpect(jsonPath("$.cobros").isEmpty());

        mockMvc.perform(get("/api/reports/ganancias")
                        .param("desde", hoy.toString())
                        .param("hasta", futuro.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIngresosBrutos").value(0))
                .andExpect(jsonPath("$.cantidadCobros").value(0));

        mockMvc.perform(get("/api/reports/anual").param("anio", String.valueOf(futuro.getYear())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCobrado").value(0))
                .andExpect(jsonPath("$.cantidadCobros").value(0))
                .andExpect(jsonPath("$.meses[" + (futuro.getMonthValue() - 1) + "].totalCobrado").value(0));
    }

    @Test
    void reportesDeAtencionesExcluyenTurnosMarcadosAtendidosConFechaFutura() throws Exception {
        Paciente paciente = crearPaciente();
        Especialidad especialidad = especialidadRepository.save(new Especialidad("General", "Atención general"));
        Odontologo odontologo = odontologoRepository.save(new Odontologo(
                "Laura", "Rios", "REPORT-MN-FUTURE", "report-laura-future@example.com", "111113", especialidad));
        LocalDate hoy = LocalDate.now();
        guardarTurno(hoy, LocalTime.of(9, 0), "Hoy", "ATENDIDO", paciente, odontologo);
        guardarTurno(hoy.plusDays(1), LocalTime.of(10, 0), "Futuro", "ATENDIDO", paciente, odontologo);

        mockMvc.perform(get("/api/reports/atenciones")
                        .param("desde", hoy.toString())
                        .param("hasta", hoy.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAtenciones").value(1));

        mockMvc.perform(get("/api/reports/anual").param("anio", String.valueOf(hoy.getYear())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAtenciones").value(1));
    }

    private Paciente crearPaciente() {
        Paciente paciente = new Paciente();
        paciente.setNombre("Ana");
        paciente.setApellido("Lopez");
        paciente.setDni("report-301");
        paciente.setEmail("report-ana@example.com");
        paciente.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        return pacienteRepository.save(paciente);
    }

    private void guardarTurno(LocalDate fecha, LocalTime hora, String motivo, String estado,
                              Paciente paciente, Odontologo odontologo) {
        turnoRepository.save(new Turno(fecha, hora, motivo, estado, paciente, odontologo));
    }

    private void registrarPago(String monto, String fecha, String estado) throws Exception {
        mockMvc.perform(post("/api/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto": %s, "fecha": "%s", "estado": "%s", "metodo": "EFECTIVO"}
                                """.formatted(monto, fecha, estado)))
                .andExpect(status().isCreated());
    }
}
