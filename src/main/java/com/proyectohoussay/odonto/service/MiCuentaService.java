package com.proyectohoussay.odonto.service;

import com.proyectohoussay.odonto.dto.ActualizarPerfilRequest;
import com.proyectohoussay.odonto.dto.EnviarMensajeRequest;
import com.proyectohoussay.odonto.dto.MensajeUsuarioDto;
import com.proyectohoussay.odonto.dto.PerfilUsuarioResponse;
import com.proyectohoussay.odonto.model.MensajeUsuario;
import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.repository.MensajeUsuarioRepository;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MiCuentaService {

    private static final Pattern DATA_URL_PATTERN =
            Pattern.compile("^data:image/(png|jpeg|jpg);base64,([A-Za-z0-9+/=\\r\\n]+)$");
    private static final int MAX_IMAGE_BYTES = 1_048_576; // 1 MB
    private static final int MAX_ENCODED_LENGTH = 1_450_000;
    private static final int MAX_DIMENSION = 2048;

    private final UsuarioRepository usuarioRepository;
    private final MensajeUsuarioRepository mensajeUsuarioRepository;

    public MiCuentaService(
            UsuarioRepository usuarioRepository,
            MensajeUsuarioRepository mensajeUsuarioRepository) {
        this.usuarioRepository = usuarioRepository;
        this.mensajeUsuarioRepository = mensajeUsuarioRepository;
    }

    public Usuario resolverUsuarioAutenticado(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticación requerida.");
        }

        Object rawUserId = jwt.getClaims().get("userId");
        if (rawUserId != null) {
            try {
                long userId = Long.parseLong(String.valueOf(rawUserId));
                if (userId > 0) {
                    Optional<Usuario> porId = usuarioRepository.findById(userId);
                    if (porId.isPresent()) {
                        return porId.get();
                    }
                }
            } catch (NumberFormatException ignored) {
                // fallback to subject lookup
            }
        }

        String subject = jwt.getSubject();
        if (subject != null && !subject.isBlank()) {
            Optional<Usuario> porSubject =
                    usuarioRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(subject.trim(), subject.trim());
            if (porSubject.isPresent()) {
                return porSubject.get();
            }
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token sin usuario válido.");
    }

    public PerfilUsuarioResponse obtenerPerfilPropio(Jwt jwt) {
        Usuario usuario = resolverUsuarioAutenticado(jwt);
        return PerfilUsuarioResponse.de(usuario);
    }

    @Transactional
    public PerfilUsuarioResponse actualizarPerfilPropio(Jwt jwt, ActualizarPerfilRequest request) {
        Usuario usuario = resolverUsuarioAutenticado(jwt);
        if (request == null || request.nombreMostrar() == null || request.nombreMostrar().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre para mostrar es obligatorio.");
        }
        String nombreMostrar = request.nombreMostrar().trim();
        if (nombreMostrar.length() > 120) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre para mostrar no puede superar los 120 caracteres.");
        }
        usuario.setNombreMostrar(nombreMostrar);

        if (request.fotoBase64() != null && !request.fotoBase64().isBlank()) {
            usuario.setFotoBase64(validarYNormalizarFoto(request.fotoBase64()));
        }

        return PerfilUsuarioResponse.de(usuarioRepository.saveAndFlush(usuario));
    }

    @Transactional
    public PerfilUsuarioResponse actualizarFotoPropia(Jwt jwt, String fotoBase64) {
        Usuario usuario = resolverUsuarioAutenticado(jwt);
        if (fotoBase64 == null || fotoBase64.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La foto es obligatoria.");
        }
        String fotoNormalizada = validarYNormalizarFoto(fotoBase64);
        usuario.setFotoBase64(fotoNormalizada);
        return PerfilUsuarioResponse.de(usuarioRepository.saveAndFlush(usuario));
    }

    @Transactional
    public PerfilUsuarioResponse eliminarFotoPropia(Jwt jwt) {
        Usuario usuario = resolverUsuarioAutenticado(jwt);
        usuario.setFotoBase64(null);
        return PerfilUsuarioResponse.de(usuarioRepository.saveAndFlush(usuario));
    }

    public List<MensajeUsuarioDto> listarMensajesPropios(Jwt jwt) {
        Usuario usuario = resolverUsuarioAutenticado(jwt);
        return mensajeUsuarioRepository
                .findByDestinatarioIdOrderByCreadoEnDescIdDesc(usuario.getId())
                .stream()
                .map(MensajeUsuarioDto::de)
                .toList();
    }

    @Transactional
    public MensajeUsuarioDto enviarMensaje(Jwt jwt, EnviarMensajeRequest request) {
        Usuario remitente = resolverUsuarioAutenticado(jwt);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los datos del mensaje son obligatorios.");
        }
        if (request.asunto() == null || request.asunto().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El asunto es obligatorio.");
        }
        if (request.contenido() == null || request.contenido().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El contenido del mensaje es obligatorio.");
        }

        Usuario destinatario = resolverDestinatario(request);
        String remitenteNombre = PerfilUsuarioResponse.de(remitente).nombreMostrar();

        MensajeUsuario mensaje = new MensajeUsuario(
                destinatario.getId(),
                remitente.getId(),
                remitenteNombre,
                request.asunto().trim(),
                request.contenido().trim()
        );

        return MensajeUsuarioDto.de(mensajeUsuarioRepository.saveAndFlush(mensaje));
    }

    @Transactional
    public MensajeUsuarioDto marcarMensajeLeido(Jwt jwt, Long mensajeId) {
        Usuario usuario = resolverUsuarioAutenticado(jwt);
        MensajeUsuario mensaje = mensajeUsuarioRepository.findById(mensajeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mensaje no encontrado."));

        if (!mensaje.getDestinatarioId().equals(usuario.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tenés permiso para acceder o modificar un mensaje de otro usuario.");
        }

        if (!mensaje.isLeido()) {
            mensaje.setLeido(true);
            mensaje.setLeidoEn(LocalDateTime.now());
            mensaje = mensajeUsuarioRepository.saveAndFlush(mensaje);
        }

        return MensajeUsuarioDto.de(mensaje);
    }

    private Usuario resolverDestinatario(EnviarMensajeRequest request) {
        if (request.destinatarioId() != null) {
            return usuarioRepository.findById(request.destinatarioId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "No se encontró el usuario destinatario."));
        }
        if (request.destinatarioUsername() != null && !request.destinatarioUsername().isBlank()) {
            String target = request.destinatarioUsername().trim();
            return usuarioRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(target, target)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "No se encontró el usuario destinatario: " + target));
        }
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Debés indicar el destinatario del mensaje.");
    }

    public static String validarYNormalizarFoto(String rawFoto) {
        String trimmed = rawFoto.trim();
        if (trimmed.length() > MAX_ENCODED_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La foto supera el tamaño máximo permitido de 1 MB.");
        }

        String declaredFormat = null;
        String base64Payload;
        if (trimmed.startsWith("data:")) {
            Matcher matcher = DATA_URL_PATTERN.matcher(trimmed);
            if (!matcher.matches()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Formato de foto inválido. Solo se permiten imágenes PNG o JPEG.");
            }
            declaredFormat = matcher.group(1).toLowerCase(Locale.ROOT);
            if ("jpg".equals(declaredFormat)) {
                declaredFormat = "jpeg";
            }
            base64Payload = matcher.group(2).replaceAll("\\s+", "");
        } else {
            base64Payload = trimmed.replaceAll("\\s+", "");
        }

        final byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Payload);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La codificación Base64 de la foto no es válida.");
        }

        if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La foto supera el tamaño máximo permitido de 1 MB.");
        }

        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El archivo enviado no es una imagen válida.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El archivo enviado no contiene una imagen PNG o JPEG válida.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String detectedFormat = reader.getFormatName().toLowerCase(Locale.ROOT);
                if ("jpg".equals(detectedFormat)) {
                    detectedFormat = "jpeg";
                }
                if (!"png".equals(detectedFormat) && !"jpeg".equals(detectedFormat)) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Formato de imagen no soportado. Solo se aceptan PNG o JPEG.");
                }
                if (declaredFormat != null && !declaredFormat.equals(detectedFormat)) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "El formato declarado de la foto no coincide con el contenido de la imagen.");
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Las dimensiones de la foto deben ser de hasta 2048x2048 píxeles.");
                }
                BufferedImage bufferedImage = reader.read(0);
                if (bufferedImage == null) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "No se pudo decodificar la imagen enviada.");
                }
                return "data:image/" + detectedFormat + ";base64," + Base64.getEncoder().encodeToString(bytes);
            } finally {
                reader.dispose();
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se pudo procesar la imagen enviada.");
        }
    }
}
