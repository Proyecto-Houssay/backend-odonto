package com.proyectohoussay.odonto.service;

import com.proyectohoussay.odonto.dto.InventarioItemDto;
import com.proyectohoussay.odonto.dto.InsumoRequest;
import com.proyectohoussay.odonto.model.Insumo;
import com.proyectohoussay.odonto.repository.InsumoRepository;
import com.proyectohoussay.odonto.repository.TurnoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        resumen.put("totalAtencionesRegistradas", totalTurnos);
        resumen.put("totalItemsInventario", totalItems);
        resumen.put("reportesDisponibles", Arrays.asList(
                "inventario",
                "atenciones",
                "ganancias",
                "cobros",
                "anual"
        ));
        return resumen;
    }

    public Map<String, Object> obtenerReporteAtenciones() {
        Map<String, Object> reporte = new HashMap<>();
        reporte.put("tipo", "Reporte de Atenciones");
        reporte.put("totalAtenciones", turnoRepository.count());
        reporte.put("detalle", "Atenciones y turnos contabilizados en el sistema");
        return reporte;
    }
}
