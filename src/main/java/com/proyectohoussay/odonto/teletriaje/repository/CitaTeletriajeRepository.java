package com.proyectohoussay.odonto.teletriaje.repository;

import com.proyectohoussay.odonto.teletriaje.model.CitaTeletriaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CitaTeletriajeRepository extends JpaRepository<CitaTeletriaje, Long> {

    List<CitaTeletriaje> findByPacienteId(Long pacienteId);

    Optional<CitaTeletriaje> findByTurnoId(Long turnoId);

    Optional<CitaTeletriaje> findBySolicitudId(Long solicitudId);

    @Query("""
            select c from CitaTeletriaje c
            join fetch c.turno t
            left join fetch c.paciente p
            left join fetch c.odontologo o
            where t.fecha between :desde and :hasta
              and (:odontologoId is null or o.id = :odontologoId)
            order by t.fecha asc, t.hora asc
            """)
    List<CitaTeletriaje> listarAgenda(@Param("desde") LocalDate desde,
                                      @Param("hasta") LocalDate hasta,
                                      @Param("odontologoId") Long odontologoId);
}
