package com.proyectohoussay.odonto;

import com.proyectohoussay.odonto.auth.AuthController;
import com.proyectohoussay.odonto.config.OpenApiConfiguration;
import com.proyectohoussay.odonto.controller.TurnoController;
import com.proyectohoussay.odonto.controller.UsuarioController;
import com.proyectohoussay.odonto.health.HealthController;
import com.proyectohoussay.odonto.patient.PacienteController;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiSecurityIntegrationTest {

    private static final String BEARER_AUTH = "bearerAuth";

    @Test
    void declaresHttpBearerJwtSecurityScheme() {
        SecurityScheme scheme = OpenApiConfiguration.class.getDeclaredAnnotation(SecurityScheme.class);

        assertNotNull(scheme);
        assertEquals(BEARER_AUTH, scheme.name());
        assertEquals(SecuritySchemeType.HTTP, scheme.type());
        assertEquals("bearer", scheme.scheme());
        assertEquals("JWT", scheme.bearerFormat());
    }

    @Test
    void patientCreationRequiresBearerAuth() {
        assertTrue(mapsPost(PacienteController.class, "/api/pacientes"));
        assertBearerAuthRequired(PacienteController.class);
    }

    @Test
    void appointmentCreationRequiresBearerAuth() {
        assertTrue(mapsPost(TurnoController.class, "/api/turnos"));
        assertBearerAuthRequired(TurnoController.class);
    }

    @Test
    void userManagementRequiresBearerAuth() {
        assertTrue(mapsPost(UsuarioController.class, "/api/usuarios"));
        assertBearerAuthRequired(UsuarioController.class);
    }

    @Test
    void loginIsPublicAndDoesNotDeclareBearerAuth() {
        assertTrue(mapsPost(AuthController.class, "/api/auth/login"));
        assertFalse(hasBearerAuthRequirement(AuthController.class));
    }

    @Test
    void healthCheckIsPublicAndDoesNotDeclareBearerAuth() {
        assertTrue(mapsGet(HealthController.class, "/api/health"));
        assertFalse(hasBearerAuthRequirement(HealthController.class));
    }

    private static void assertBearerAuthRequired(Class<?> controller) {
        assertTrue(hasBearerAuthRequirement(controller),
                () -> controller.getSimpleName() + " must declare @SecurityRequirement(name = \""
                        + BEARER_AUTH + "\")");
    }

    private static boolean hasBearerAuthRequirement(Class<?> controller) {
        return Arrays.stream(controller.getDeclaredAnnotationsByType(SecurityRequirement.class))
                .anyMatch(requirement -> BEARER_AUTH.equals(requirement.name()));
    }

    private static boolean mapsPost(Class<?> controller, String expectedPath) {
        return maps(controller, expectedPath, (basePath, method) -> {
            PostMapping mapping = method.getDeclaredAnnotation(PostMapping.class);
            return mapping == null ? Stream.empty() : combinePaths(basePath, resolve(mapping.value(), mapping.path()));
        });
    }

    private static boolean mapsGet(Class<?> controller, String expectedPath) {
        return maps(controller, expectedPath, (basePath, method) -> {
            GetMapping mapping = method.getDeclaredAnnotation(GetMapping.class);
            return mapping == null ? Stream.empty() : combinePaths(basePath, resolve(mapping.value(), mapping.path()));
        });
    }

    private static boolean maps(Class<?> controller, String expectedPath, MethodPathResolver resolver) {
        RequestMapping requestMapping = controller.getDeclaredAnnotation(RequestMapping.class);
        if (requestMapping == null) {
            return Arrays.stream(controller.getDeclaredMethods())
                    .flatMap(method -> resolver.resolve("", method))
                    .anyMatch(expectedPath::equals);
        }

        return resolve(requestMapping.value(), requestMapping.path())
                .flatMap(basePath -> Arrays.stream(controller.getDeclaredMethods())
                        .flatMap(method -> resolver.resolve(basePath, method)))
                .anyMatch(expectedPath::equals);
    }

    private static Stream<String> combinePaths(String basePath, Stream<String> methodPaths) {
        return methodPaths.map(methodPath -> {
            if (methodPath.isEmpty()) {
                return basePath.isEmpty() ? "/" : basePath;
            }
            String combined = basePath + "/" + methodPath;
            return combined.replaceAll("/{2,}", "/");
        });
    }

    private static Stream<String> resolve(String[] value, String[] path) {
        if (value.length > 0) {
            return Arrays.stream(value);
        }
        if (path.length > 0) {
            return Arrays.stream(path);
        }
        return Stream.of("");
    }

    @FunctionalInterface
    private interface MethodPathResolver {
        Stream<String> resolve(String basePath, Method method);
    }
}
