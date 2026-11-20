package com.proyectohoussay.odonto.report;

import com.proyectohoussay.odonto.dto.InventarioItemDto;
import com.proyectohoussay.odonto.dto.InsumoRequest;
import com.proyectohoussay.odonto.dto.MovimientoInventarioDto;
import com.proyectohoussay.odonto.dto.MovimientoInventarioRequest;
import com.proyectohoussay.odonto.dto.AnualReportDto;
import com.proyectohoussay.odonto.dto.AtencionesReportDto;
import com.proyectohoussay.odonto.dto.MovimientoFinancieroDto;
import com.proyectohoussay.odonto.service.ReportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
@SecurityRequirement(name = "bearerAuth")
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

    @GetMapping("/inventario/bajo-stock")
    public ResponseEntity<List<InventarioItemDto>> obtenerInsumosBajoStock() {
        return ResponseEntity.ok(reportService.obtenerInsumosBajoStock());
    }

    @GetMapping("/inventario/{id}/movimientos")
    public ResponseEntity<List<MovimientoInventarioDto>> obtenerMovimientos(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.obtenerMovimientos(id));
    }

    @PostMapping("/inventario/{id}/movimientos")
    public ResponseEntity<MovimientoInventarioDto> registrarMovimiento(
            @PathVariable Long id, @Valid @RequestBody MovimientoInventarioRequest request) {
        return ResponseEntity.status(201).body(reportService.registrarMovimiento(id, request));
    }

    @PostMapping("/inventario")
    public ResponseEntity<InventarioItemDto> registrarInsumo(@Valid @RequestBody InsumoRequest request) {
        return ResponseEntity.status(201).body(reportService.registrarInsumo(request));
    }

    @PutMapping("/inventario/{id}")
    public ResponseEntity<InventarioItemDto> actualizarInsumo(
            @PathVariable Long id, @Valid @RequestBody InsumoRequest request) {
        return ResponseEntity.ok(reportService.actualizarInsumo(id, request));
    }

    @GetMapping("/atenciones")
    public ResponseEntity<AtencionesReportDto> obtenerReporteAtenciones(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate inicio = desde != null ? desde : LocalDate.now().withDayOfYear(1);
        LocalDate fin = hasta != null ? hasta : LocalDate.now().withMonth(12).withDayOfMonth(31);
        return ResponseEntity.ok(reportService.obtenerReporteAtenciones(inicio, fin));
    }

    @GetMapping("/anual")
    public ResponseEntity<AnualReportDto> obtenerReporteAnual(
            @RequestParam(required = false) Integer anio) {
        int targetYear = anio != null ? anio : LocalDate.now().getYear();
        return ResponseEntity.ok(reportService.obtenerReporteAnual(targetYear));
    }

    @GetMapping("/ganancias")
    public ResponseEntity<Map<String, Object>> obtenerReporteGanancias(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate inicio = desde != null ? desde : LocalDate.now().withDayOfYear(1);
        LocalDate fin = hasta != null ? hasta : LocalDate.now().withMonth(12).withDayOfMonth(31);
        return ResponseEntity.ok(reportService.obtenerReporteGanancias(inicio, fin));
    }

    @GetMapping("/cobros")
    public ResponseEntity<List<MovimientoFinancieroDto>> obtenerCobros(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate inicio = desde != null ? desde : LocalDate.now().withDayOfYear(1);
        LocalDate fin = hasta != null ? hasta : LocalDate.now().withMonth(12).withDayOfMonth(31);
        return ResponseEntity.ok(reportService.obtenerCobros(inicio, fin));
    }
}
