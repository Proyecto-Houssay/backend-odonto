package com.proyectohoussay.odonto.repository;

import com.proyectohoussay.odonto.model.CargoPersonal;
import com.proyectohoussay.odonto.model.Personal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonalRepository extends JpaRepository<Personal, Long> {

    Optional<Personal> findByDni(String dni);

    boolean existsByDni(String dni);

    Optional<Personal> findByUsuarioId(Long usuarioId);

    Optional<Personal> findByEmailIgnoreCase(String email);

    List<Personal> findAllByCargo(CargoPersonal cargo);

    List<Personal> findAllByActivoTrue();

    List<Personal> findAllByCargoAndActivo(CargoPersonal cargo, boolean activo);
}
