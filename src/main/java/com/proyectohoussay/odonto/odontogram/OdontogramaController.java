package com.proyectohoussay.odonto.odontogram;

import com.proyectohoussay.odonto.model.MensajeUsuario;
import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.patient.PacienteNoEncontradoException;
import com.proyectohoussay.odonto.auth.UserRole;
import com.proyectohoussay.odonto.repository.MensajeUsuarioRepository;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/odontograma")
@SecurityRequirement(name = "bearerAuth")
public class OdontogramaController {
    private final OdontogramaService service;
    private final UsuarioRepository usuarioRepository;
    private final MensajeUsuarioRepository mensajeUsuarioRepository;

    public OdontogramaController(OdontogramaService service) {
        this(service, null, null);
    }

    @Autowired
    public OdontogramaController(OdontogramaService service,
                                 @Autowired(required = false) UsuarioRepository usuarioRepository,
                                 @Autowired(required = false) MensajeUsuarioRepository mensajeUsuarioRepository) {
        this.service = service;
        this.usuarioRepository = usuarioRepository;
        this.mensajeUsuarioRepository = mensajeUsuarioRepository;
    }

    @GetMapping("/catalogo") public OdontogramaCatalogo.Catalogo catalogo() { return OdontogramaCatalogo.actual(); }
    @PostMapping
    public ResponseEntity<OdontogramaResponse> crear(@Valid @RequestBody OdontogramaRequest request, @AuthenticationPrincipal Jwt jwt) {
        Long creator;
        try { creator = Long.valueOf(jwt.getClaimAsString("userId")); }
        catch (RuntimeException exception) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "El token no contiene un usuario válido"); }
        if (creator <= 0) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "El token no contiene un usuario válido");
        var snapshot = service.crear(request, creator);
        return ResponseEntity.created(URI.create("/api/odontograma/" + snapshot.id())).body(snapshot);
    }
    @GetMapping("/{id}")
    public OdontogramaResponse obtener(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        OdontogramaResponse response = service.obtener(id);
        return UserRole.from(jwt.getClaimAsString("role"))
                .filter(role -> role == UserRole.ADMINISTRADOR || role == UserRole.ODONTOLOGO)
                .isPresent() ? response : response.sinFirma();
    }
    @GetMapping public List<OdontogramaResumen> listar(@RequestParam Long pacienteId) { return service.listar(pacienteId); }

    @PostMapping("/{id}/enviar-paciente")
    public ResponseEntity<Map<String, Object>> enviarAlPaciente(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        OdontogramaResponse odonto = service.obtener(id);
        Long remitenteId = null;
        String remitenteNombre = "Odontología NovaDent";
        if (jwt != null) {
            try {
                remitenteId = Long.valueOf(jwt.getClaimAsString("userId"));
            } catch (RuntimeException ignored) {
            }
            if (remitenteId != null && usuarioRepository != null) {
                Optional<Usuario> remitenteOpt = usuarioRepository.findById(remitenteId);
                if (remitenteOpt.isPresent()) {
                    Usuario rem = remitenteOpt.get();
                    String full = ((rem.getNombre() != null ? rem.getNombre() : "")
                            + " " + (rem.getApellido() != null ? rem.getApellido() : "")).trim();
                    if (!full.isBlank()) {
                        remitenteNombre = "Dr./Dra. " + full;
                    } else if (rem.getUsername() != null && !rem.getUsername().isBlank()) {
                        remitenteNombre = rem.getUsername();
                    }
                }
            }
        }

        Optional<Usuario> cuentaPacienteOpt = (usuarioRepository != null && odonto.pacienteId() != null)
                ? usuarioRepository.findByPacienteId(odonto.pacienteId())
                : Optional.empty();

        if (cuentaPacienteOpt.isEmpty() || mensajeUsuarioRepository == null) {
            return ResponseEntity.ok(Map.of(
                    "enviado", false,
                    "odontogramaId", odonto.id(),
                    "pacienteId", odonto.pacienteId(),
                    "mensaje", "Odontograma #" + odonto.id() + " registrado. El paciente aún no posee una cuenta vinculada en el portal para recibir la notificación en su buzón."
            ));
        }

        Usuario cuentaPaciente = cuentaPacienteOpt.get();
        int cantidadPiezas = odonto.dientes() != null ? odonto.dientes().size() : 0;
        String detallePiezas = odonto.dientes() != null && !odonto.dientes().isEmpty()
                ? odonto.dientes().stream()
                        .limit(8)
                        .map(d -> "Pieza " + d.numero() + " (" + d.nombre() + ": " + d.tratamiento() + ")")
                        .collect(Collectors.joining(", "))
                : "Sin piezas detalladas";

        String asunto = "Nuevo odontograma y diagnóstico firmado (#" + odonto.id() + ")";
        String contenido = "Se registró y firmó tu odontograma clínico (#" + odonto.id() + ") con "
                + cantidadPiezas + " pieza(s) evaluada(s) (" + detallePiezas + ") y un presupuesto estimado de $"
                + odonto.totalPresupuesto() + " " + odonto.moneda()
                + ". Podés consultarlo en la sección Mi odontograma de tu portal.";
        if (contenido.length() > 950) {
            contenido = contenido.substring(0, 947) + "...";
        }

        mensajeUsuarioRepository.save(new MensajeUsuario(
                cuentaPaciente.getId(),
                remitenteId,
                remitenteNombre,
                asunto,
                contenido
        ));

        return ResponseEntity.ok(Map.of(
                "enviado", true,
                "odontogramaId", odonto.id(),
                "pacienteId", odonto.pacienteId(),
                "destinatarioUsuarioId", cuentaPaciente.getId(),
                "mensaje", "Diagnóstico y odontograma #" + odonto.id() + " enviados al buzón del paciente."
        ));
    }

    @ExceptionHandler(PacienteNoEncontradoException.class)
    ResponseEntity<String> noPatient(PacienteNoEncontradoException exception) { return ResponseEntity.status(404).body(exception.getMessage()); }
    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    ResponseEntity<String> invalid(Exception exception) {
        return ResponseEntity.badRequest().body(exception instanceof IllegalArgumentException ? exception.getMessage() : "El cuerpo de la solicitud no es válido");
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<String> invalidFields(MethodArgumentNotValidException exception) {
        String details = exception.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    String message = error.getDefaultMessage() == null ? "valor inválido" : error.getDefaultMessage();
                    return error instanceof org.springframework.validation.FieldError fieldError
                            ? fieldError.getField() + ": " + message
                            : message;
                })
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining("; "));
        if (details.isBlank()) details = "Revise los campos de la solicitud";
        return ResponseEntity.badRequest().body(details);
    }
}
