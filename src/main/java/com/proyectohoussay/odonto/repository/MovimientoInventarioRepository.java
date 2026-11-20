package com.proyectohoussay.odonto.repository;

import com.proyectohoussay.odonto.model.MovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
    List<MovimientoInventario> findAllByInsumoIdOrderByFechaHoraDesc(Long insumoId);
}
