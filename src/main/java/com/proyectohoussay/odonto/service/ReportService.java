package com.proyectohoussay.odonto.service;

import com.proyectohoussay.odonto.dto.InventarioItemDto;
import com.proyectohoussay.odonto.repository.TurnoRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final TurnoRepository turnoRepository;

    public ReportService(TurnoRepository turnoRepository) {
        this.turnoRepository = turnoRepository;
    }

    public List<InventarioItemDto> obtenerReporteInventario() {
        return Arrays.asList(
                new InventarioItemDto(1L, "Anestesia local con vasoconstrictor", "Farmacología", 150, "ampollas", "DISPONIBLE"),
                new InventarioItemDto(2L, "Guantes de látex (talle M)", "Bioseguridad", 500, "pares", "DISPONIBLE"),
                new InventarioItemDto(3L, "Resina compuesta fotocurable", "Restauración", 25, "jeringas", "DISPONIBLE"),
                new InventarioItemDto(4L, "Agujas descartables cortas", "Descartables", 80, "unidades", "DISPONIBLE"),
                new InventarioItemDto(5L, "Eyectores de saliva descartables", "Descartables", 15, "paquetes", "BAJO_STOCK")
        );
    }

    public Map<String, Object> obtenerResumenInformes() {
        Map<String, Object> resumen = new HashMap<>();
        long totalTurnos = turnoRepository.count();
        List<InventarioItemDto> inventario = obtenerReporteInventario();

        resumen.put("modulo", "Informes y Gestión Odontológica");
        resumen.put("estado", "DISPONIBLE");
        resumen.put("totalAtencionesRegistradas", totalTurnos);
        resumen.put("totalItemsInventario", inventario.size());
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
