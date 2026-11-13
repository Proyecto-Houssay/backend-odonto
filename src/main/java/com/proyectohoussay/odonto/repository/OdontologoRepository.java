package com.proyectohoussay.odonto.repository;

import com.proyectohoussay.odonto.model.Odontologo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import com.proyectohoussay.odonto.model.DiaSemana;
import java.time.LocalTime;

@Repository
public interface OdontologoRepository extends JpaRepository<Odontologo, Long> {

    Optional<Odontologo> findByMatricula(String matricula);

    boolean existsByMatricula(String matricula);

    Optional<Odontologo> findByUsuarioId(Long usuarioId);

    Optional<Odontologo> findByEmailIgnoreCase(String email);

    List<Odontologo> findAllByActivoTrue();

    @org.springframework.data.jpa.repository.Query("select distinct o from Odontologo o left join o.especialidadesAdicionales ea where o.especialidad.id = :especialidadId or ea.id = :especialidadId")
    List<Odontologo> findAllByEspecialidad(@org.springframework.data.repository.query.Param("especialidadId") Long especialidadId);

    @org.springframework.data.jpa.repository.Query("""
            select case when count(o) > 0 then true else false end
            from Odontologo o join o.disponibilidades d
            where o.id <> coalesce(:excludeId, -1)
              and o.consultorio = :consultorio
              and o.estadoProfesional = com.proyectohoussay.odonto.model.EstadoProfesional.ACTIVO
              and d.dia = :dia
              and d.horaInicio < :horaFin and d.horaFin > :horaInicio
            """)
    boolean existeSolapamientoConsultorio(@org.springframework.data.repository.query.Param("consultorio") String consultorio,
                                           @org.springframework.data.repository.query.Param("dia") DiaSemana dia,
                                           @org.springframework.data.repository.query.Param("horaInicio") LocalTime horaInicio,
                                           @org.springframework.data.repository.query.Param("horaFin") LocalTime horaFin,
                                           @org.springframework.data.repository.query.Param("excludeId") Long excludeId);
}
