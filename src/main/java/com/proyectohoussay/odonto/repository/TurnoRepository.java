package com.proyectohoussay.odonto.repository;

import com.proyectohoussay.odonto.model.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {

    List<Turno> findByFecha(LocalDate fecha);

    List<Turno> findByOdontologoId(Long odontologoId);

    List<Turno> findByPacienteId(Long pacienteId);

    List<Turno> findByFechaAndOdontologoId(LocalDate fecha, Long odontologoId);

    @Query("""
            select case when count(t) > 0 then true else false end
            from Turno t
            where t.odontologo.id = :odontologoId
              and t.fecha = :fecha
              and t.hora = :hora
              and (t.estado is null or upper(t.estado) <> 'CANCELADO')
              and (:turnoExcluidoId is null or t.id <> :turnoExcluidoId)
            """)
    boolean existeTurnoActivoEnHorario(@Param("odontologoId") Long odontologoId,
                                       @Param("fecha") LocalDate fecha,
                                       @Param("hora") LocalTime hora,
                                       @Param("turnoExcluidoId") Long turnoExcluidoId);
}
