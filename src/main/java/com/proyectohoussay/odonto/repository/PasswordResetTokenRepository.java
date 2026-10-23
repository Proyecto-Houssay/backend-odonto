package com.proyectohoussay.odonto.repository;

import com.proyectohoussay.odonto.model.PasswordResetToken;
import com.proyectohoussay.odonto.model.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from PasswordResetToken token join fetch token.usuario where token.tokenHash = :hash")
    Optional<PasswordResetToken> findByHashForUpdate(@Param("hash") String hash);

    void deleteAllByUsuario(Usuario usuario);
}
