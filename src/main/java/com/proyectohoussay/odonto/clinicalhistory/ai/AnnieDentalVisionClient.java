package com.proyectohoussay.odonto.clinicalhistory.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;

/** Client for the dental-specific DR. ANNIE Insights panoramic findings API. */
@Component
public class AnnieDentalVisionClient {
    private final RestClient restClient;
    private final String apiKey;
    private volatile String estadoServicio;

    public AnnieDentalVisionClient(
            RestClient.Builder builder,
            @Value("${annie.api-key:}") String apiKey,
            @Value("${annie.base-url:https://api.annie-insights.com/api/v2}") String baseUrl
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.estadoServicio = this.apiKey.isBlank() ? "NO_CONFIGURADO" : "CLAVE_SIN_VALIDAR";
        var requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(60));
        this.restClient = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public String estadoServicio() {
        return estadoServicio;
    }

    public JsonNode analyzePanoramic(String imageUrl, String patientReference, LocalDate date) {
        if (!isConfigured()) {
            throw new IllegalStateException("El proveedor DR. ANNIE no está configurado. Defina ANNIE_API_KEY.");
        }
        try {
            JsonNode result = restClient.post()
                    .uri("/panos-findings")
                    .header("x-api-key", apiKey)
                    .body(Map.of(
                            "PatientID", patientReference,
                            "TimeStamp", date.toString(),
                            "ImageName", imageUrl
                    ))
                    .retrieve()
                    .onStatus(status -> status.value() == 401, (request, response) -> {
                        estadoServicio = "CREDENCIAL_RECHAZADA";
                        throw new IllegalStateException("DR. ANNIE rechazó ANNIE_API_KEY (HTTP 401). La clave cargada no es válida o no está activa para Annie Insights API v2. Confirme la clave con HCDS y reinicie el backend.");
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        estadoServicio = "ERROR_PROVEEDOR";
                        throw new IllegalStateException("El proveedor DR. ANNIE rechazó el análisis (HTTP " + response.getStatusCode().value() + ").");
                    })
                    .body(JsonNode.class);
            if (result == null) {
                throw new IllegalStateException("El proveedor DR. ANNIE devolvió una respuesta vacía.");
            }
            estadoServicio = "OPERATIVO_PANORAMICAS";
            return result;
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("No se pudo completar el análisis con DR. ANNIE (HTTP " + e.getStatusCode().value() + ").");
        } catch (RestClientException e) {
            estadoServicio = "ERROR_CONEXION";
            throw new IllegalStateException("No se pudo conectar con el proveedor DR. ANNIE.");
        }
    }
}
