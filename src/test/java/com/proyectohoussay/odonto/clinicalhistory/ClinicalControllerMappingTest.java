package com.proyectohoussay.odonto.clinicalhistory;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClinicalControllerMappingTest {

    @Test
    void clinicalControllersDoNotDeclareDuplicateHttpRoutes() {
        Map<Route, List<String>> handlersByRoute = new LinkedHashMap<>();
        List<Class<?>> controllers = List.of(
                ClinicalRecordController.class,
                HistoriaClinicaController.class,
                DiagnosticoController.class
        );

        for (Class<?> controller : controllers) {
            RequestMapping controllerMapping = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
            List<String> basePaths = paths(controllerMapping);
            for (Method handler : controller.getDeclaredMethods()) {
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(handler, RequestMapping.class);
                if (mapping == null) {
                    continue;
                }

                List<String> methods = methods(mapping);
                List<String> methodPaths = paths(mapping);
                for (String basePath : basePaths) {
                    for (String methodPath : methodPaths) {
                        String fullPath = normalize(basePath, methodPath);
                        for (String httpMethod : methods) {
                            handlersByRoute.computeIfAbsent(new Route(httpMethod, fullPath), ignored -> new ArrayList<>())
                                    .add(controller.getSimpleName() + "#" + handler.getName());
                        }
                    }
                }
            }
        }

        Map<Route, List<String>> duplicates = handlersByRoute.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(LinkedHashMap::new,
                        (result, entry) -> result.put(entry.getKey(), entry.getValue()),
                        LinkedHashMap::putAll);

        assertTrue(duplicates.isEmpty(), () -> "Duplicate clinical HTTP routes: " + duplicates);
    }

    private static List<String> paths(RequestMapping mapping) {
        if (mapping == null) {
            return List.of("");
        }
        String[] value = mapping.value();
        String[] path = mapping.path();
        String[] configuredPaths = value.length > 0 ? value : path;
        return configuredPaths.length > 0 ? List.of(configuredPaths) : List.of("");
    }

    private static List<String> methods(RequestMapping mapping) {
        RequestMethod[] configuredMethods = mapping.method();
        if (configuredMethods.length == 0) {
            return List.of("ANY");
        }
        return java.util.Arrays.stream(configuredMethods).map(Enum::name).toList();
    }

    private static String normalize(String basePath, String methodPath) {
        String combined = (basePath + "/" + methodPath)
                .replaceAll("/{2,}", "/")
                .replaceAll("\\{[^/]+\\}", "{variable}");
        return combined.length() > 1 && combined.endsWith("/")
                ? combined.substring(0, combined.length() - 1)
                : combined;
    }

    private record Route(String method, String path) {
    }
}
