package com.proyectohoussay.odonto.service;

import com.proyectohoussay.odonto.dto.InventarioItemDto;
import com.proyectohoussay.odonto.model.Insumo;
import com.proyectohoussay.odonto.repository.InsumoRepository;
import com.proyectohoussay.odonto.repository.TurnoRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

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

    @PostConstruct
    public void inicializarInventarioSiVacio() {
        if (insumoRepository.count() == 0) {
            insumoRepository.saveAll(Arrays.asList(
                    new Insumo("Anestesia local con vasoconstrictor", "Farmacología", 150, "ampollas", 30),
                    new Insumo("Guantes de látex (talle M)", "Bioseguridad", 500, "pares", 100),
                    new Insumo("Resina compuesta fotocurable", "Restauración", 25, "jeringas", 10),
                    new Insumo("Agujas descartables cortas", "Descartables", 80, "unidades", 20),
                    new Insumo("Eyectores de saliva descartables", "Descartables", 15, "paquetes", 20)
            ));
        }
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

    public Insumo registrarInsumo(Insumo insumo) {
        return insumoRepository.save(insumo);
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
