package com.proyectohoussay.odonto.clinicalhistory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TratamientoRepository extends JpaRepository<Tratamiento, Long> {

    List<Tratamiento> findAllByDiagnosticoId(Long diagnosticoId);

    List<Tratamiento> findAllByEstado(EstadoTratamiento estado);

    @Query("""
            select t from Tratamiento t
            join t.diagnostico d
            join d.historiaClinica h
            where h.pacienteId = :pacienteId
            order by t.fecha desc, t.id desc
            """)
    List<Tratamiento> findByPacienteIdNewestFirst(@Param("pacienteId") Long pacienteId);
}
