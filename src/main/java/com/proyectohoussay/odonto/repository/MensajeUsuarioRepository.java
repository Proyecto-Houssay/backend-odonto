package com.proyectohoussay.odonto.repository;

import com.proyectohoussay.odonto.model.MensajeUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensajeUsuarioRepository extends JpaRepository<MensajeUsuario, Long> {

    List<MensajeUsuario> findByDestinatarioIdOrderByCreadoEnDescIdDesc(Long destinatarioId);

    long countByDestinatarioIdAndLeidoFalse(Long destinatarioId);
}
