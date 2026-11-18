package com.proyectohoussay.odonto.ai;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;

/** Reads orthodontic price references from the clinic's versioned fee schedule. */
@Component
public class OrthodonticPriceCatalog {

    private static final Set<String> ORTHODONTIC_CODES = Set.of(
            "06.01", "06.02", "06.03", "06.04", "06.05", "06.06");

    private static final Map<String, List<String>> CODES_BY_VISIBLE_DENTITION = Map.of(
            "PRIMARIA", List.of("06.01", "06.02"),
            "MIXTA", List.of("06.01", "06.02"),
            "PERMANENTE", List.of("06.01", "06.03"),
            "NO_DETERMINABLE", List.of("06.01"));

    public List<PriceEstimate> getEstimates() {
        List<PriceEstimate> prices = new ArrayList<>();
        var resource = new ClassPathResource("knowledge/precios-nomenclador.md");
        try (var reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split("\\|");
                if (columns.length < 4) continue;
                String code = columns[1].trim();
                String price = columns[3].trim();
                if (ORTHODONTIC_CODES.contains(code) && price.matches("\\$\\s*[0-9.]+(?:,[0-9]{2})?")) {
                    prices.add(new PriceEstimate(code, columns[2].trim(), price));
                }
            }
        } catch (IOException exception) {
            return List.of();
        }
        return List.copyOf(prices);
    }

    public List<PriceEstimate> getEstimatesForVisibleDentition(String visibleDentition) {
        String normalized = visibleDentition == null ? "NO_DETERMINABLE" : visibleDentition.trim().toUpperCase();
        List<String> selectedCodes = CODES_BY_VISIBLE_DENTITION.getOrDefault(
                normalized, CODES_BY_VISIBLE_DENTITION.get("NO_DETERMINABLE"));
        return getEstimates().stream()
                .filter(estimate -> selectedCodes.contains(estimate.code()))
                .toList();
    }

    public record PriceEstimate(String code, String prestation, String priceArs) { }
}
