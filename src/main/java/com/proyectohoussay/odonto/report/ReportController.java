package com.proyectohoussay.odonto.report;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @GetMapping
    public String obtenerInformes() {
        return "Módulo de informes disponible";
    }
}
