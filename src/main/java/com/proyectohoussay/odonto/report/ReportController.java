package com.proyectohoussay.odonto.report;

import com.proyectohoussay.odonto.dto.InventarioItemDto;
import com.proyectohoussay.odonto.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> obtenerInformes() {
        return ResponseEntity.ok(reportService.obtenerResumenInformes());
    }

    @GetMapping("/inventario")
    public ResponseEntity<List<InventarioItemDto>> obtenerReporteInventario() {
        return ResponseEntity.ok(reportService.obtenerReporteInventario());
    }

    @GetMapping("/atenciones")
    public ResponseEntity<Map<String, Object>> obtenerReporteAtenciones() {
        return ResponseEntity.ok(reportService.obtenerReporteAtenciones());
    }
}
