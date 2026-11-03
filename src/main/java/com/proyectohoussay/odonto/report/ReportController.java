package com.proyectohoussay.odonto.report;

import com.proyectohoussay.odonto.dto.InventarioItemDto;
import com.proyectohoussay.odonto.dto.InsumoRequest;
import com.proyectohoussay.odonto.dto.MovimientoFinancieroDto;
import com.proyectohoussay.odonto.service.ReportService;
import com.proyectohoussay.odonto.auth.RequireRoles;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    @RequireRoles({"ADMIN", "RECEPCIONISTA"})
    public ResponseEntity<Map<String, Object>> obtenerInformes() {
        return ResponseEntity.ok(reportService.obtenerResumenInformes());
    }

    @GetMapping("/inventario")
    @RequireRoles({"ADMIN", "RECEPCIONISTA", "ODONTOLOGO"})
    public ResponseEntity<List<InventarioItemDto>> obtenerReporteInventario() {
        return ResponseEntity.ok(reportService.obtenerReporteInventario());
    }

    @PostMapping("/inventario")
    @RequireRoles("ADMIN")
    public ResponseEntity<InventarioItemDto> registrarInsumo(@Valid @RequestBody InsumoRequest request) {
        return ResponseEntity.status(201).body(reportService.registrarInsumo(request));
    }

    @PutMapping("/inventario/{id}")
    @RequireRoles("ADMIN")
    public ResponseEntity<InventarioItemDto> actualizarInsumo(
            @PathVariable Long id, @Valid @RequestBody InsumoRequest request) {
        return ResponseEntity.ok(reportService.actualizarInsumo(id, request));
    }

    @GetMapping("/atenciones")
    @RequireRoles({"ADMIN", "RECEPCIONISTA", "ODONTOLOGO"})
    public ResponseEntity<Map<String, Object>> obtenerReporteAtenciones(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reportService.obtenerReporteAtenciones(desde, hasta));
    }

    @GetMapping("/anual")
    @RequireRoles({"ADMIN", "RECEPCIONISTA"})
    public ResponseEntity<Map<String, Object>> obtenerReporteAnual(@RequestParam int anio) {
        if (anio < 1 || anio > 9999) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "El año debe estar entre 1 y 9999");
        }
        return ResponseEntity.ok(reportService.obtenerReporteAnual(anio));
    }

    @GetMapping("/ganancias")
    @RequireRoles({"ADMIN", "RECEPCIONISTA"})
    public ResponseEntity<Map<String, Object>> obtenerReporteGanancias(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reportService.obtenerReporteGanancias(desde, hasta));
    }

    @GetMapping("/cobros")
    @RequireRoles({"ADMIN", "RECEPCIONISTA"})
    public ResponseEntity<List<MovimientoFinancieroDto>> obtenerCobros(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reportService.obtenerCobros(desde, hasta));
    }
}
