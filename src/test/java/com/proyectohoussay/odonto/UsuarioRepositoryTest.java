package com.proyectohoussay.odonto;

import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void testGuardarYBuscarUsuarioPorEmailYNombre() {
        Usuario usuario = new Usuario("Maria", "Gonzalez", "maria.gonzalez@example.com", "PACIENTE", true);
        usuarioRepository.save(usuario);

        Optional<Usuario> porEmail = usuarioRepository.findByEmail("maria.gonzalez@example.com");
        assertThat(porEmail).isPresent();
        assertThat(porEmail.get().getNombre()).isEqualTo("Maria");

        Optional<Usuario> porNombre = usuarioRepository.findByNombre("Maria");
        assertThat(porNombre).isPresent();

        boolean existe = usuarioRepository.existsByEmail("maria.gonzalez@example.com");
        assertThat(existe).isTrue();
    }

    @Test
    void testGuardarYBuscarUsuarioPorUsernameYPassword() {
        Usuario usuario = new Usuario("iris_admin", "iris.dedominicis@example.com", "PasswordSegura123!", "ADMIN");
        usuarioRepository.save(usuario);

        Optional<Usuario> porUsername = usuarioRepository.findByUsername("iris_admin");
        assertThat(porUsername).isPresent();
        assertThat(porUsername.get().getUsername()).isEqualTo("iris_admin");
        assertThat(porUsername.get().getPassword()).isEqualTo("PasswordSegura123!");
        assertThat(porUsername.get().getRol()).isEqualTo("ADMIN");
        assertThat(porUsername.get().getEmail()).isEqualTo("iris.dedominicis@example.com");

        boolean existeUsername = usuarioRepository.existsByUsername("iris_admin");
        assertThat(existeUsername).isTrue();
    }
}
