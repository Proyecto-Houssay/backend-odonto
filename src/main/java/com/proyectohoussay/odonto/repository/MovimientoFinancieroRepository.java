package com.proyectohoussay.odonto.repository;

import com.proyectohoussay.odonto.model.MovimientoFinanciero;
import com.proyectohoussay.odonto.model.TipoMovimientoFinanciero;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface MovimientoFinancieroRepository extends JpaRepository<MovimientoFinanciero, Long> {
    List<MovimientoFinanciero> findByFechaBetween(LocalDate desde, LocalDate hasta);
    List<MovimientoFinanciero> findByFechaBetweenAndTipo(LocalDate desde, LocalDate hasta,
                                                         TipoMovimientoFinanciero tipo);
}
