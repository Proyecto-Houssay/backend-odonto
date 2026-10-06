package com.proyectohoussay.odonto.repository;

import com.proyectohoussay.odonto.model.Insumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InsumoRepository extends JpaRepository<Insumo, Long> {

    List<Insumo> findByCategoria(String categoria);

    List<Insumo> findByCantidadDisponibleLessThan(int stockMinimo);
}
