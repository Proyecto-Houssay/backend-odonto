package com.proyectohoussay.odonto.repository;

import com.proyectohoussay.odonto.model.Insumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;

@Repository
public interface InsumoRepository extends JpaRepository<Insumo, Long> {

    List<Insumo> findByCategoria(String categoria);

    List<Insumo> findByCantidadDisponibleLessThan(int stockMinimo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Insumo i where i.id = :id")
    java.util.Optional<Insumo> findByIdForUpdate(@Param("id") Long id);
}
