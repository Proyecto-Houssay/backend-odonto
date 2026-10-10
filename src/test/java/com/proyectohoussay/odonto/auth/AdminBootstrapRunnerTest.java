package com.proyectohoussay.odonto.auth;

import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBootstrapRunnerTest {

    @Test
    void doesNotRecreateAdministratorWhenOneAlreadyExistsEvenWithoutBootstrapCredentials() throws Exception {
        UsuarioRepository repository = mock(UsuarioRepository.class);
        given(repository.existsByRolIgnoreCase("ADMINISTRADOR")).willReturn(true);
        AdminBootstrapRunner runner = new AdminBootstrapRunner(
                repository, new BCryptPasswordEncoder(), new MockEnvironment());

        runner.run(new DefaultApplicationArguments());

        verify(repository, never()).save(any(Usuario.class));
    }

    @Test
    void requiresExplicitCredentialsWhenNoAdministratorExists() {
        UsuarioRepository repository = mock(UsuarioRepository.class);
        given(repository.existsByRolIgnoreCase("ADMINISTRADOR")).willReturn(false);
        AdminBootstrapRunner runner = new AdminBootstrapRunner(
                repository, new BCryptPasswordEncoder(), new MockEnvironment());

        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BOOTSTRAP_ADMIN_USERNAME")
                .hasMessageNotContaining("strong-test-password");

        verify(repository, never()).save(any(Usuario.class));
    }

    @Test
    void createsOneAdministratorWithEncodedPasswordFromEnvironment() throws Exception {
        UsuarioRepository repository = mock(UsuarioRepository.class);
        given(repository.existsByRolIgnoreCase("ADMINISTRADOR")).willReturn(false);
        when(repository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MockEnvironment environment = new MockEnvironment()
                .withProperty("BOOTSTRAP_ADMIN_USERNAME", "root")
                .withProperty("BOOTSTRAP_ADMIN_EMAIL", "root@example.com")
                .withProperty("BOOTSTRAP_ADMIN_PASSWORD", "strong-test-password");
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        AdminBootstrapRunner runner = new AdminBootstrapRunner(repository, encoder, environment);

        runner.run(new DefaultApplicationArguments());

        org.mockito.ArgumentCaptor<Usuario> captor = org.mockito.ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(captor.capture());
        Usuario administrator = captor.getValue();
        assertThat(administrator.getUsername()).isEqualTo("root");
        assertThat(administrator.getEmail()).isEqualTo("root@example.com");
        assertThat(administrator.getRol()).isEqualTo("ADMINISTRADOR");
        assertThat(encoder.matches("strong-test-password", administrator.getPassword())).isTrue();
    }
}
