package com.hrbot.parser.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrbot.model.HhArea;
import com.hrbot.repository.HhAreaRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HhAreaResolver {

    private static final String AREAS_API  = "https://api.hh.ru/areas";
    private static final String USER_AGENT = "hr-vacancy-bot/1.0 (your@email.com)";

    private final ObjectMapper objectMapper;
    private final HhAreaRepository areaRepository;

    @PostConstruct
    public void init() {
        if (areaRepository.existsBy()) {
            log.info("HH areas already loaded from DB — skipping fetch");
            return;
        }

        try {
            log.info("Loading HH areas from API...");
            List<HhArea> areas = fetchAllAreas();
            areaRepository.saveAll(areas);
            log.info("HH areas saved to DB: {} entries", areas.size());
        } catch (Exception e) {
            log.error("Failed to load HH areas: {}", e.getMessage());
        }
    }

    /**
     * Resolves location string to HH area ID.
     * Input: "Georgia", "Tbilisi", "28", etc.
     * Returns null if not resolved.
     */
    public String resolve(String location) {
        if (location == null || location.isBlank()) return null;

        // Already numeric ID — use as-is
        if (location.matches("\\d+")) return location;

        return areaRepository.findByNameLower(location.trim().toLowerCase())
                .map(HhArea::getId)
                .orElseGet(() -> {
                    log.warn("HH: area not found for '{}'. Use numeric ID or check spelling.", location);
                    return null;
                });
    }

    // --- Private helpers ---

    private List<HhArea> fetchAllAreas() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AREAS_API))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("HH areas API returned " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        List<HhArea> areas = new ArrayList<>();
        collectAreas(root, null, areas);
        return areas;
    }

    private void collectAreas(JsonNode node, String parentId, List<HhArea> result) {
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectAreas(child, parentId, result);
            }
        } else if (node.isObject()) {
            String id   = node.path("id").asText(null);
            String name = node.path("name").asText(null);

            if (id != null && name != null) {
                result.add(HhArea.builder()
                        .id(id)
                        .name(name)
                        .parentId(parentId)
                        .nameLower(name.toLowerCase())
                        .build());
            }

            JsonNode areas = node.path("areas");
            if (areas.isArray()) {
                collectAreas(areas, id, result);
            }
        }
    }
}
