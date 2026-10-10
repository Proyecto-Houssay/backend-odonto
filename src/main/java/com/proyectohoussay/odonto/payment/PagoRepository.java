package com.proyectohoussay.odonto.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByEstadoAndFechaBetweenOrderByFechaAscIdAsc(
            PagoEstado estado, LocalDate desde, LocalDate hasta);
}
