package com.proyectohoussay.odonto.service;

import com.proyectohoussay.odonto.dto.AnualReportDto;
import com.proyectohoussay.odonto.dto.AtencionResumenDto;
import com.proyectohoussay.odonto.dto.AtencionesReportDto;
import com.proyectohoussay.odonto.dto.InventarioItemDto;
import com.proyectohoussay.odonto.dto.InsumoRequest;
import com.proyectohoussay.odonto.dto.MesAnualReportDto;
import com.proyectohoussay.odonto.model.Insumo;
import com.proyectohoussay.odonto.model.Turno;
import com.proyectohoussay.odonto.repository.InsumoRepository;
import com.proyectohoussay.odonto.repository.TurnoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private static final String ESTADO_ATENDIDO = "ATENDIDO";

    private final InsumoRepository insumoRepository;
    private final TurnoRepository turnoRepository;
    public ReportService(InsumoRepository insumoRepository, TurnoRepository turnoRepository) {
        this.insumoRepository = insumoRepository;
        this.turnoRepository = turnoRepository;
    }

    public List<InventarioItemDto> obtenerReporteInventario() {
        return insumoRepository.findAll().stream()
                .map(insumo -> new InventarioItemDto(insumo.getId(), insumo.getNombre(), insumo.getCategoria(),
                        insumo.getCantidadDisponible(), insumo.getUnidadMedida(), insumo.getEstado()))
                .toList();
    }

    public InventarioItemDto registrarInsumo(InsumoRequest request) {
        Insumo insumo = new Insumo(request.nombre(), request.categoria(),
                request.cantidadDisponible(), request.unidadMedida(), request.stockMinimo());
        return aDto(insumoRepository.save(insumo));
    }

    public InventarioItemDto actualizarInsumo(Long id, InsumoRequest request) {
        Insumo insumo = insumoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Insumo no encontrado"));
        insumo.setNombre(request.nombre());
        insumo.setCategoria(request.categoria());
        insumo.setCantidadDisponible(request.cantidadDisponible());
        insumo.setUnidadMedida(request.unidadMedida());
        insumo.setStockMinimo(request.stockMinimo());
        return aDto(insumoRepository.save(insumo));
    }

    public AtencionesReportDto obtenerReporteAtenciones(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        List<AtencionResumenDto> atenciones = atencionesEnPeriodo(desde, hasta).stream()
                .map(AtencionResumenDto::de).toList();
        return new AtencionesReportDto("Reporte de Atenciones", desde, hasta, atenciones.size(), atenciones);
    }

    public AnualReportDto obtenerReporteAnual(int anio) {
        if (anio < 1 || anio > 9999) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El año debe estar entre 1 y 9999.");
        }

        Year year = Year.of(anio);
        LocalDate desde = year.atDay(1);
        LocalDate hasta = year.atMonth(12).atEndOfMonth();
        Map<Integer, Long> atencionesPorMes = atencionesEnPeriodo(desde, hasta)
                .stream().collect(Collectors.groupingBy(turno -> turno.getFecha().getMonthValue(), Collectors.counting()));

        List<MesAnualReportDto> meses = java.util.Arrays.stream(Month.values())
                .map(mes -> {
                    return new MesAnualReportDto(mes.getValue(), mes.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es")),
                            atencionesPorMes.getOrDefault(mes.getValue(), 0L));
                }).toList();

        return new AnualReportDto(anio, atencionesPorMes.values().stream().mapToLong(Long::longValue).sum(), meses);
    }

    public Map<String, Object> obtenerResumenInformes() {
        return Map.of(
                "modulo", "Informes y Gestión Odontológica",
                "estado", "DISPONIBLE",
                "totalAtencionesRegistradas", turnoRepository.countByFechaLessThanEqualAndEstadoIgnoreCase(
                        LocalDate.now(), ESTADO_ATENDIDO),
                "totalItemsInventario", insumoRepository.count(),
                "reportesDisponibles", List.of("inventario", "atenciones", "anual")
        );
    }

    private List<Turno> atencionesEnPeriodo(LocalDate desde, LocalDate hasta) {
        LocalDate hoy = LocalDate.now();
        if (desde.isAfter(hoy)) {
            return List.of();
        }
        LocalDate fechaFin = hasta.isAfter(hoy) ? hoy : hasta;
        return turnoRepository.findByFechaBetweenAndEstadoIgnoreCaseOrderByFechaAscHoraAsc(
                desde, fechaFin, ESTADO_ATENDIDO);
    }

    private void validarPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las fechas desde y hasta son obligatorias.");
        }
        if (desde.isAfter(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha desde no puede ser posterior a hasta.");
        }
    }

    private InventarioItemDto aDto(Insumo insumo) {
        return new InventarioItemDto(insumo.getId(), insumo.getNombre(), insumo.getCategoria(),
                insumo.getCantidadDisponible(), insumo.getUnidadMedida(), insumo.getEstado());
    }
}
