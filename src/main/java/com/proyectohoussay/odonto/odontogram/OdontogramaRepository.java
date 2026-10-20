package com.proyectohoussay.odonto.odontogram;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface OdontogramaRepository extends JpaRepository<Odontograma, Long> {
    boolean existsByPacienteId(Long pacienteId);

    @Query("select new com.proyectohoussay.odonto.odontogram.OdontogramaResumen(o.id, o.paciente.id, o.creadoEn, o.creadoPorId, o.moneda, o.versionCatalogo, o.totalPresupuesto, o.versionConsentimiento) from Odontograma o where o.paciente.id = :pacienteId order by o.creadoEn desc, o.id desc")
    List<OdontogramaResumen> resumenes(@Param("pacienteId") Long pacienteId);
}
