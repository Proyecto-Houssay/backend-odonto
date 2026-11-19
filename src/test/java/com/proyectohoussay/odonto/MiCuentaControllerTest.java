package com.proyectohoussay.odonto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyectohoussay.odonto.model.MensajeUsuario;
import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.repository.MensajeUsuarioRepository;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MiCuentaControllerTest {

    private static final String PASSWORD = "test-password-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MensajeUsuarioRepository mensajeUsuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private Usuario userA;
    private Usuario userB;

    @BeforeEach
    void setUp() {
        mensajeUsuarioRepository.deleteAll();
        usuarioRepository.deleteAll();

        userA = usuarioRepository.save(new Usuario(
                "user.a",
                "Ana",
                "Gómez",
                "ana.gomez@novadent.com",
                passwordEncoder.encode(PASSWORD),
                "RECEPCIONISTA",
                true
        ));

        userB = usuarioRepository.save(new Usuario(
                "user.b",
                "Bruno",
                "Díaz",
                "bruno.diaz@novadent.com",
                passwordEncoder.encode(PASSWORD),
                "PACIENTE",
                true
        ));
    }

    @Test
    void endpointsDeMiCuentaRequierenAutenticacion401() throws Exception {
        mockMvc.perform(get("/api/mi-cuenta/perfil"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/mi-cuenta/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreMostrar\":\"Intento Anonimo\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/mi-cuenta/mensajes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void actualizaNombreMostrarYFotoPngValidaSinPermitirCambioDeRol() throws Exception {
        String tokenA = loginAndGetToken("user.a");
        String validPngDataUrl = generateValidPngDataUrl();

        // Lectura inicial del perfil
        mockMvc.perform(get("/api/mi-cuenta/perfil")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user.a"))
                .andExpect(jsonPath("$.nombreMostrar").value("Ana Gómez"))
                .andExpect(jsonPath("$.rol").value("RECEPCIONISTA"));

        // Intentar actualizar nombreMostrar e inyectar un cambio de rol a ADMINISTRADOR
        mockMvc.perform(put("/api/mi-cuenta/perfil")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombreMostrar": "Ana M. Gómez (Recepción)",
                                  "rol": "ADMINISTRADOR"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreMostrar").value("Ana M. Gómez (Recepción)"))
                .andExpect(jsonPath("$.rol").value("RECEPCIONISTA"));

        // Subir foto PNG válida
        mockMvc.perform(put("/api/mi-cuenta/foto")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fotoBase64": "%s"
                                }
                                """.formatted(validPngDataUrl)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoBase64").value(validPngDataUrl));

        // Verificar persistencia real en base de datos
        Usuario persisted = usuarioRepository.findById(userA.getId()).orElseThrow();
        assertThat(persisted.getNombreMostrar()).isEqualTo("Ana M. Gómez (Recepción)");
        assertThat(persisted.getRol()).isEqualTo("RECEPCIONISTA");
        assertThat(persisted.getFotoBase64()).isEqualTo(validPngDataUrl);
    }

    @Test
    void rechazaFotoConFormatoInvalidoONoImagenCon400() throws Exception {
        String tokenA = loginAndGetToken("user.a");

        // Formato MIME no permitido (PDF)
        String fakePdf = "data:application/pdf;base64,"
                + Base64.getEncoder().encodeToString("not-an-image".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(put("/api/mi-cuenta/foto")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fotoBase64": "%s"
                                }
                                """.formatted(fakePdf)))
                .andExpect(status().isBadRequest());

        // Prefijo PNG pero bytes de texto plano
        String fakePngBytes = "data:image/png;base64,"
                + Base64.getEncoder().encodeToString("texto-plano-no-imagen".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(put("/api/mi-cuenta/foto")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fotoBase64": "%s"
                                }
                                """.formatted(fakePngBytes)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void buzonAisladoPorUsuarioOrdenadoCronologicamenteYBloqueaLecturaCruzadaCon403() throws Exception {
        String tokenA = loginAndGetToken("user.a");
        String tokenB = loginAndGetToken("user.b");

        // Buzón vacío devuelve lista vacía
        mockMvc.perform(get("/api/mi-cuenta/mensajes")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Crear mensajes para User A (dos mensajes en distintos momentos) y uno para User B
        MensajeUsuario antiguoA = new MensajeUsuario(
                userA.getId(),
                userB.getId(),
                "Bruno Díaz",
                "Consulta de turno",
                "Hola, quería consultar disponibilidad para el viernes."
        );
        antiguoA.setCreadoEn(LocalDateTime.now().minusHours(2));
        antiguoA = mensajeUsuarioRepository.saveAndFlush(antiguoA);

        MensajeUsuario recienteA = new MensajeUsuario(
                userA.getId(),
                userB.getId(),
                "Administración NovaDent",
                "Actualización de protocolo",
                "Recordatorio de cierre de agenda mensual."
        );
        recienteA.setCreadoEn(LocalDateTime.now().minusMinutes(10));
        recienteA = mensajeUsuarioRepository.saveAndFlush(recienteA);

        MensajeUsuario mensajePrivadoB = new MensajeUsuario(
                userB.getId(),
                userA.getId(),
                "Ana Gómez",
                "Confirmación de turno",
                "Tu turno quedó confirmado para mañana a las 10:00 hs."
        );
        mensajePrivadoB = mensajeUsuarioRepository.saveAndFlush(mensajePrivadoB);

        // User A solo ve sus 2 mensajes en orden descendente (más reciente primero)
        mockMvc.perform(get("/api/mi-cuenta/mensajes")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(recienteA.getId()))
                .andExpect(jsonPath("$[0].asunto").value("Actualización de protocolo"))
                .andExpect(jsonPath("$[0].leido").value(false))
                .andExpect(jsonPath("$[1].id").value(antiguoA.getId()))
                .andExpect(jsonPath("$[1].asunto").value("Consulta de turno"));

        // User A marca su propio mensaje como leído -> 200 OK y persiste leido = true
        mockMvc.perform(put("/api/mi-cuenta/mensajes/" + recienteA.getId() + "/leido")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leido").value(true))
                .andExpect(jsonPath("$.leidoEn").isNotEmpty());

        assertThat(mensajeUsuarioRepository.findById(recienteA.getId()).orElseThrow().isLeido()).isTrue();

        // User A intenta marcar como leído el mensaje privado de User B -> 403 Forbidden
        mockMvc.perform(put("/api/mi-cuenta/mensajes/" + mensajePrivadoB.getId() + "/leido")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());

        // User B sigue viendo su mensaje como NO leído
        mockMvc.perform(get("/api/mi-cuenta/mensajes")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(mensajePrivadoB.getId()))
                .andExpect(jsonPath("$[0].leido").value(false));
    }

    @Test
    void permiteEnviarMensajeInternoAUsuarioDestinatarioPorUsername() throws Exception {
        String tokenA = loginAndGetToken("user.a");
        String tokenB = loginAndGetToken("user.b");

        mockMvc.perform(post("/api/mi-cuenta/mensajes")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "destinatarioUsername": "user.b",
                                  "asunto": "Recordatorio de control",
                                  "contenido": "Te esperamos el lunes a las 09:30 hs."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.destinatarioId").value(userB.getId()))
                .andExpect(jsonPath("$.asunto").value("Recordatorio de control"));

        mockMvc.perform(get("/api/mi-cuenta/mensajes")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].asunto").value("Recordatorio de control"));
    }

    private String loginAndGetToken(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usernameOrEmail":"%s","password":"%s"}
                                """.formatted(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private static String generateValidPngDataUrl() throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}
