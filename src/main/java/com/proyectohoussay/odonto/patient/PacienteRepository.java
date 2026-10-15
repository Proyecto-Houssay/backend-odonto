package com.proyectohoussay.odonto.patient;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    boolean existsByDni(String dni);

    boolean existsByDniAndIdNot(String dni, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Paciente p where p.id = :id")
    Optional<Paciente> findLockedById(@Param("id") Long id);
}
