package com.proyectohoussay.odonto.service;

import com.proyectohoussay.odonto.dto.AnualReportDto;
import com.proyectohoussay.odonto.dto.AtencionResumenDto;
import com.proyectohoussay.odonto.dto.AtencionesReportDto;
import com.proyectohoussay.odonto.dto.InventarioItemDto;
import com.proyectohoussay.odonto.dto.InsumoRequest;
import com.proyectohoussay.odonto.dto.MovimientoInventarioDto;
import com.proyectohoussay.odonto.dto.MovimientoInventarioRequest;
import com.proyectohoussay.odonto.dto.MesAnualReportDto;
import com.proyectohoussay.odonto.model.Insumo;
import com.proyectohoussay.odonto.model.MovimientoInventario;
import com.proyectohoussay.odonto.model.TipoMovimientoInventario;
import com.proyectohoussay.odonto.model.Turno;
import com.proyectohoussay.odonto.repository.InsumoRepository;
import com.proyectohoussay.odonto.repository.MovimientoInventarioRepository;
import com.proyectohoussay.odonto.repository.TurnoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
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
    private final MovimientoInventarioRepository movimientoInventarioRepository;

    @Autowired
    public ReportService(InsumoRepository insumoRepository, TurnoRepository turnoRepository,
                         MovimientoInventarioRepository movimientoInventarioRepository) {
        this.insumoRepository = insumoRepository;
        this.turnoRepository = turnoRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
    }

    /** Retained for existing direct callers and unit fixtures. */
    public ReportService(InsumoRepository insumoRepository, TurnoRepository turnoRepository) {
        this.insumoRepository = insumoRepository;
        this.turnoRepository = turnoRepository;
        this.movimientoInventarioRepository = null;
    }

    public List<InventarioItemDto> obtenerReporteInventario() {
        return insumoRepository.findAll().stream()
                .map(this::aDto)
                .toList();
    }

    public List<InventarioItemDto> obtenerInsumosBajoStock() {
        return insumoRepository.findAll().stream()
                .filter(insumo -> insumo.getCantidadDisponible() <= insumo.getStockMinimo())
                .map(this::aDto)
                .toList();
    }

    @Transactional
    public MovimientoInventarioDto registrarMovimiento(Long insumoId, MovimientoInventarioRequest request) {
        if (request.tipo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tipo de movimiento es obligatorio.");
        }
        Insumo insumo = insumoRepository.findByIdForUpdate(insumoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Insumo no encontrado"));
        int anterior = insumo.getCantidadDisponible();
        int nuevoStock;
        if (request.tipo() == TipoMovimientoInventario.INGRESO) {
            try {
                nuevoStock = Math.addExact(anterior, request.cantidad());
            } catch (ArithmeticException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad excede el máximo de stock permitido.");
            }
        } else {
            if (request.cantidad() > anterior) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuficiente para registrar el consumo.");
            }
            nuevoStock = anterior - request.cantidad();
        }
        insumo.setCantidadDisponible(nuevoStock);
        insumoRepository.save(insumo);
        MovimientoInventario movimiento = movimientoInventarioRepository.save(new MovimientoInventario(
                insumo, request.tipo(), request.cantidad(), anterior, nuevoStock, request.motivo()));
        return aMovimientoDto(movimiento);
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventarioDto> obtenerMovimientos(Long insumoId) {
        if (!insumoRepository.existsById(insumoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Insumo no encontrado");
        }
        return movimientoInventarioRepository.findAllByInsumoIdOrderByFechaHoraDesc(insumoId).stream()
                .map(this::aMovimientoDto)
                .toList();
    }

    @Transactional
    public InventarioItemDto registrarInsumo(InsumoRequest request) {
        Insumo insumo = new Insumo(request.nombre(), request.categoria(),
                request.cantidadDisponible(), request.unidadMedida(), request.stockMinimo());
        Insumo guardado = insumoRepository.save(insumo);
        if (movimientoInventarioRepository != null && guardado.getCantidadDisponible() > 0) {
            movimientoInventarioRepository.save(new MovimientoInventario(guardado, TipoMovimientoInventario.INGRESO,
                    guardado.getCantidadDisponible(), 0, guardado.getCantidadDisponible(), "Stock inicial al registrar el insumo"));
        }
        return aDto(guardado);
    }

    @Transactional
    public InventarioItemDto actualizarInsumo(Long id, InsumoRequest request) {
        Insumo insumo = insumoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Insumo no encontrado"));
        int stockAnterior = insumo.getCantidadDisponible();
        insumo.setNombre(request.nombre());
        insumo.setCategoria(request.categoria());
        insumo.setCantidadDisponible(request.cantidadDisponible());
        insumo.setUnidadMedida(request.unidadMedida());
        insumo.setStockMinimo(request.stockMinimo());
        Insumo actualizado = insumoRepository.save(insumo);
        int delta = request.cantidadDisponible() - stockAnterior;
        if (movimientoInventarioRepository != null && delta != 0) {
            TipoMovimientoInventario tipo = delta > 0 ? TipoMovimientoInventario.INGRESO : TipoMovimientoInventario.CONSUMO;
            movimientoInventarioRepository.save(new MovimientoInventario(actualizado, tipo, Math.abs(delta),
                    stockAnterior, actualizado.getCantidadDisponible(), "Ajuste de stock desde edición del insumo"));
        }
        return aDto(actualizado);
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
                insumo.getCantidadDisponible(), insumo.getUnidadMedida(), insumo.getStockMinimo(), insumo.getEstado());
    }

    private MovimientoInventarioDto aMovimientoDto(MovimientoInventario movimiento) {
        return new MovimientoInventarioDto(movimiento.getId(), movimiento.getInsumo().getId(),
                movimiento.getInsumo().getNombre(), movimiento.getTipo(), movimiento.getCantidad(),
                movimiento.getStockAnterior(), movimiento.getStockResultante(), movimiento.getMotivo(),
                movimiento.getFechaHora());
    }
}
