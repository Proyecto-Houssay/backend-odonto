package com.proyectohoussay.odonto.auth;

import com.proyectohoussay.odonto.dto.UserProfileResponse;
import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.patient.PacienteRepository;
import com.proyectohoussay.odonto.repository.OdontologoRepository;
import com.proyectohoussay.odonto.repository.PersonalRepository;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class AuthService {

    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final PersonalRepository personalRepository;
    private final OdontologoRepository odontologoRepository;
    private final PacienteRepository pacienteRepository;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this(usuarioRepository, passwordEncoder, null, null, null);
    }

    @Autowired
    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Autowired(required = false) PersonalRepository personalRepository,
            @Autowired(required = false) OdontologoRepository odontologoRepository,
            @Autowired(required = false) PacienteRepository pacienteRepository
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.personalRepository = personalRepository;
        this.odontologoRepository = odontologoRepository;
        this.pacienteRepository = pacienteRepository;
    }

    public Optional<Usuario> authenticate(String usernameOrEmail, String password) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()
                || password == null || password.isBlank()
                || password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES) {
            return Optional.empty();
        }

        String identifier = usernameOrEmail.trim();
        return usuarioRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, identifier)
                .filter(Usuario::isActivo)
                .filter(user -> UserRole.from(user.getRol()).isPresent())
                .filter(user -> matches(password, user.getPassword()));
    }

    public Optional<UserProfileResponse> obtenerPerfilUsuario(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(username, username);
        if (usuarioOpt.isEmpty()) {
            return Optional.empty();
        }
        Usuario usuario = usuarioOpt.get();

        UserProfileResponse.PerfilVinculadoDto perfilVinculado = resolverPerfilVinculado(usuario);
        return Optional.of(UserProfileResponse.de(usuario, perfilVinculado));
    }

    private UserProfileResponse.PerfilVinculadoDto resolverPerfilVinculado(Usuario usuario) {
        String rol = usuario.getRol();

        if (odontologoRepository != null && "ODONTOLOGO".equalsIgnoreCase(rol)) {
            var odontologoOpt = odontologoRepository.findByUsuarioId(usuario.getId())
                    .or(() -> odontologoRepository.findByEmailIgnoreCase(usuario.getEmail()));
            if (odontologoOpt.isPresent()) {
                var o = odontologoOpt.get();
                String espNombre = o.getEspecialidad() != null ? o.getEspecialidad().getNombre() : null;
                return new UserProfileResponse.PerfilVinculadoDto(
                        "ODONTOLOGO",
                        o.getId(),
                        null,
                        o.getMatricula(),
                        espNombre
                );
            }
        }

        if (personalRepository != null) {
            var personalOpt = personalRepository.findByUsuarioId(usuario.getId())
                    .or(() -> personalRepository.findByEmailIgnoreCase(usuario.getEmail()));
            if (personalOpt.isPresent()) {
                var p = personalOpt.get();
                return new UserProfileResponse.PerfilVinculadoDto(
                        "PERSONAL",
                        p.getId(),
                        p.getCargo() != null ? p.getCargo().name() : null,
                        null,
                        null
                );
            }
        }

        if (pacienteRepository != null) {
            var pacienteOpt = pacienteRepository.findByEmailIgnoreCase(usuario.getEmail());
            if (pacienteOpt.isPresent()) {
                var pac = pacienteOpt.get();
                return new UserProfileResponse.PerfilVinculadoDto(
                        "PACIENTE",
                        pac.getId(),
                        null,
                        null,
                        null
                );
            }
        }

        return new UserProfileResponse.PerfilVinculadoDto(
                rol != null ? rol.toUpperCase() : "USUARIO",
                null,
                null,
                null,
                null
        );
    }

    private boolean matches(String rawPassword, String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            return false;
        }
        try {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    @Transactional
    public void changePassword(String authenticatedUsername, String currentPassword, String newPassword) {
        if (authenticatedUsername == null || authenticatedUsername.isBlank()) {
            throw new InvalidCurrentPasswordException();
        }
        Usuario user = usuarioRepository.findByUsername(authenticatedUsername)
                .filter(Usuario::isActivo)
                .orElseThrow(InvalidCurrentPasswordException::new);
        if (currentPassword == null || currentPassword.isBlank()
                || currentPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES
                || !matches(currentPassword, user.getPassword())) {
            throw new InvalidCurrentPasswordException();
        }
        if (newPassword == null || newPassword.isBlank() || newPassword.length() < 8
                || newPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES
                || passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser diferente y tener entre 8 caracteres y 72 bytes UTF-8.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        usuarioRepository.save(user);
    }
}
