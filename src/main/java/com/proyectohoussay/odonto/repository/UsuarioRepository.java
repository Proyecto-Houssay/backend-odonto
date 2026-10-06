package com.proyectohoussay.odonto.repository;

import com.proyectohoussay.odonto.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByNombre(String nombre);

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);
}
