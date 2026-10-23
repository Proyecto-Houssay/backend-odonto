package com.proyectohoussay.odonto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
public class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void testGuardarYBuscarUsuarioPorEmailYNombre() {
        Usuario usuario = new Usuario("maria_g", "Maria", "Gonzalez", "maria.gonzalez@example.com",
                "UnaClaveSegura123!", "RECEPCIONISTA", true);
        usuarioRepository.save(usuario);

        Optional<Usuario> porEmail = usuarioRepository.findByEmail("maria.gonzalez@example.com");
        assertThat(porEmail).isPresent();
        assertThat(porEmail.get().getNombre()).isEqualTo("Maria");
        assertThat(passwordEncoder.matches("UnaClaveSegura123!", porEmail.get().getPassword())).isTrue();

        Optional<Usuario> porNombre = usuarioRepository.findByNombre("Maria");
        assertThat(porNombre).isPresent();

        boolean existe = usuarioRepository.existsByEmail("maria.gonzalez@example.com");
        assertThat(existe).isTrue();
    }

    @Test
    void testGuardarYBuscarUsuarioPorUsernameYPassword() throws Exception {
        Usuario usuario = new Usuario("iris_admin", "Iris", "De Dominicis", "iris.dedominicis@example.com",
                "PasswordSegura123!", "ADMINISTRADOR", true);
        usuarioRepository.save(usuario);

        Optional<Usuario> porUsername = usuarioRepository.findByUsername("iris_admin");
        assertThat(porUsername).isPresent();
        assertThat(porUsername.get().getUsername()).isEqualTo("iris_admin");
        assertThat(passwordEncoder.matches("PasswordSegura123!", porUsername.get().getPassword())).isTrue();
        assertThat(new ObjectMapper().writeValueAsString(porUsername.get())).doesNotContain("password");
        assertThat(porUsername.get().getRol()).isEqualTo("ADMINISTRADOR");
        assertThat(porUsername.get().getEmail()).isEqualTo("iris.dedominicis@example.com");

        boolean existeUsername = usuarioRepository.existsByUsername("iris_admin");
        assertThat(existeUsername).isTrue();
    }

    @Test
    void rechazaUsernameDuplicado() {
        usuarioRepository.saveAndFlush(usuario("iris_admin", "primero@example.com"));

        assertThatThrownBy(() -> usuarioRepository.saveAndFlush(usuario("iris_admin", "segundo@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechazaEmailDuplicado() {
        usuarioRepository.saveAndFlush(usuario("iris_admin", "iris@example.com"));

        assertThatThrownBy(() -> usuarioRepository.saveAndFlush(usuario("otro_usuario", "iris@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechazaCamposObligatoriosVacios() {
        Usuario usuario = usuario("", "invalido@example.com");

        assertThatThrownBy(() -> usuarioRepository.saveAndFlush(usuario))
                .isInstanceOf(RuntimeException.class);
    }

    private Usuario usuario(String username, String email) {
        return new Usuario(username, "Iris", "De Dominicis", email, "PasswordSegura123!", "ADMINISTRADOR", true);
    }
}
