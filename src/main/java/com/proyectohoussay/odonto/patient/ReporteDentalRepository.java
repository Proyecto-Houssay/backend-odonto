package com.proyectohoussay.odonto.patient;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface ReporteDentalRepository extends JpaRepository<ReporteDental, Long> {
    List<ReporteDental> findByPacienteIdOrderByCreadoEnDesc(Long pacienteId);
    List<ReporteDental> findAllByOrderByCreadoEnDesc();
    boolean existsByPacienteId(Long pacienteId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReporteDental r where r.id = :id")
    Optional<ReporteDental> findLockedById(@Param("id") Long id);
}
