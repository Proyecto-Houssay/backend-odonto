package com.proyectohoussay.odonto.clinicalhistory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TratamientoRepository extends JpaRepository<Tratamiento, Long> {

    List<Tratamiento> findAllByDiagnosticoId(Long diagnosticoId);

    List<Tratamiento> findAllByEstado(EstadoTratamiento estado);
}
