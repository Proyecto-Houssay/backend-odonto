package com.proyectohoussay.odonto.payment;

import com.proyectohoussay.odonto.dto.PagoRequest;
import com.proyectohoussay.odonto.dto.PagoResponse;
import com.proyectohoussay.odonto.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final ReportService reportService;

    public PagoController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<PagoResponse> registrar(@Valid @RequestBody PagoRequest request) {
        return ResponseEntity.status(201).body(reportService.registrarPago(request));
    }
}
