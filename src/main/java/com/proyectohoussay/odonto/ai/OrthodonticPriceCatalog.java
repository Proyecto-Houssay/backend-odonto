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

/** Reads orthodontic price references from the clinic's versioned fee schedule. */
@Component
public class OrthodonticPriceCatalog {

    private static final Set<String> ORTHODONTIC_CODES = Set.of("O001", "O002", "O003", "O004");

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
                if (ORTHODONTIC_CODES.contains(code) && price.matches("\\$\\s*[0-9.]+")) {
                    prices.add(new PriceEstimate(code, columns[2].trim(), price));
                }
            }
        } catch (IOException exception) {
            return List.of();
        }
        return List.copyOf(prices);
    }

    public record PriceEstimate(String code, String prestation, String priceArs) { }
}
