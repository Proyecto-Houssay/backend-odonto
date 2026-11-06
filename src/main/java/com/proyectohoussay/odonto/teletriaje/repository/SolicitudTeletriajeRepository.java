package com.proyectohoussay.odonto.teletriaje.repository;

import com.proyectohoussay.odonto.teletriaje.model.SolicitudTeletriaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudTeletriajeRepository extends JpaRepository<SolicitudTeletriaje, Long> {

    List<SolicitudTeletriaje> findByPacienteId(Long pacienteId);

    @Query("""
            select s from SolicitudTeletriaje s
            order by
                s.derivacionHumana desc,
                case when s.prioridadValidada is not null then s.prioridadValidada else s.prioridadSugerida end asc,
                s.fechaCreacion asc
            """)
    List<SolicitudTeletriaje> obtenerColaPrioridad();

    @Query("""
            select s from SolicitudTeletriaje s
            where (:pacienteId is null or s.paciente.id = :pacienteId)
            order by s.fechaCreacion desc
            """)
    List<SolicitudTeletriaje> buscarPorPacienteOpcional(@Param("pacienteId") Long pacienteId);
}
