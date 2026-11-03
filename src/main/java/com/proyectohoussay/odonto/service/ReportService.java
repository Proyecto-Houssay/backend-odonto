package com.proyectohoussay.odonto.service;

import com.proyectohoussay.odonto.dto.InventarioItemDto;
import com.proyectohoussay.odonto.dto.InsumoRequest;
import com.proyectohoussay.odonto.model.Insumo;
import com.proyectohoussay.odonto.model.Turno;
import com.proyectohoussay.odonto.repository.InsumoRepository;
import com.proyectohoussay.odonto.repository.TurnoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final InsumoRepository insumoRepository;
    private final TurnoRepository turnoRepository;

    public ReportService(InsumoRepository insumoRepository, TurnoRepository turnoRepository) {
        this.insumoRepository = insumoRepository;
        this.turnoRepository = turnoRepository;
    }

    public List<InventarioItemDto> obtenerReporteInventario() {
        return insumoRepository.findAll().stream()
                .map(insumo -> new InventarioItemDto(
                        insumo.getId(),
                        insumo.getNombre(),
                        insumo.getCategoria(),
                        insumo.getCantidadDisponible(),
                        insumo.getUnidadMedida(),
                        insumo.getEstado()
                ))
                .toList();
    }

    public InventarioItemDto registrarInsumo(InsumoRequest request) {
        Insumo insumo = new Insumo(request.nombre(), request.categoria(),
                request.cantidadDisponible(), request.unidadMedida(), request.stockMinimo());
        insumo = insumoRepository.save(insumo);
        return aDto(insumo);
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

    private InventarioItemDto aDto(Insumo insumo) {
        return new InventarioItemDto(insumo.getId(), insumo.getNombre(), insumo.getCategoria(),
                insumo.getCantidadDisponible(), insumo.getUnidadMedida(), insumo.getEstado());
    }

    public Map<String, Object> obtenerResumenInformes() {
        Map<String, Object> resumen = new HashMap<>();
        long totalTurnos = turnoRepository.count();
        long totalItems = insumoRepository.count();

        resumen.put("modulo", "Informes y Gestión Odontológica");
        resumen.put("estado", "DISPONIBLE");
        resumen.put("totalTurnosRegistrados", totalTurnos);
        resumen.put("totalItemsInventario", totalItems);
        resumen.put("reportesDisponibles", List.of(
                "inventario",
                "turnos-por-periodo",
                "resumen-anual-de-turnos"
        ));
        resumen.put("reportesFinancierosDisponibles", false);
        return resumen;
    }

    public Map<String, Object> obtenerReporteAtenciones(LocalDate desde, LocalDate hasta) {
        List<Turno> turnos = obtenerTurnos(desde, hasta);
        Map<String, Object> reporte = new HashMap<>();
        reporte.put("tipo", "Turnos registrados");
        reporte.put("desde", desde);
        reporte.put("hasta", hasta);
        reporte.put("totalTurnos", turnos.size());
        reporte.put("porEstado", turnos.stream().collect(Collectors.groupingBy(
                turno -> turno.getEstado() == null || turno.getEstado().isBlank() ? "SIN_ESTADO" : turno.getEstado(),
                Collectors.counting())));
        reporte.put("detalle", "Conteo de turnos registrados; el sistema no distingue una atención efectivamente realizada.");
        return reporte;
    }

    public Map<String, Object> obtenerReporteAnual(int anio) {
        LocalDate desde = LocalDate.of(anio, 1, 1);
        LocalDate hasta = LocalDate.of(anio, 12, 31);
        Map<String, Object> reporte = obtenerReporteAtenciones(desde, hasta);
        reporte.put("tipo", "Resumen anual de turnos registrados");
        reporte.put("anio", anio);
        reporte.put("reportesFinancierosDisponibles", false);
        return reporte;
    }

    private List<Turno> obtenerTurnos(LocalDate desde, LocalDate hasta) {
        if (desde == null && hasta == null) return turnoRepository.findAll();
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe indicar desde y hasta, y desde no puede ser posterior a hasta");
        }
        return turnoRepository.findByFechaBetween(desde, hasta);
    }
}
