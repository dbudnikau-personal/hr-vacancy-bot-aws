package com.hrbot.parser.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrbot.model.Vacancy;
import com.hrbot.model.VacancyFilter;
import com.hrbot.parser.SiteParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetmatchParser implements SiteParser {

    private static final String BASE_URL    = "https://getmatch.ru";
    private static final String API_URL     = BASE_URL + "/api/offers";
    private static final String VACANCY_URL = BASE_URL + "/vacancies/";
    private static final int    LIMIT       = 20;
    private static final int    MAX_PAGES   = 5;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String getSiteKey() { return "getmatch"; }

    @Override
    public List<Vacancy> parse(VacancyFilter filter) {
        List<Vacancy> results = new ArrayList<>();

        for (int page = 0; page < MAX_PAGES; page++) {
            try {
                int offset = page * LIMIT;
                String url = buildUrl(filter, offset);
                log.debug("Fetching Getmatch page {}: {}", page + 1, url);

                JsonNode response = fetchJson(url);
                JsonNode offers = response.path("offers");

                if (!offers.isArray() || offers.isEmpty()) break;

                for (JsonNode offer : offers) {
                    try {
                        results.add(mapVacancy(offer));
                    } catch (Exception e) {
                        log.warn("Getmatch: failed to map offer: {}", e.getMessage());
                    }
                }

                int total = response.path("meta").path("total").asInt(0);
                if (offset + LIMIT >= total) break;

                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Getmatch API error on page {}: {}", page + 1, e.getMessage());
                break;
            }
        }

        log.info("Getmatch: found {} vacancies for filter [{}]", results.size(), filter.getName());
        return results;
    }

    // --- Private helpers ---

    private String buildUrl(VacancyFilter filter, int offset) {
        StringBuilder sb = new StringBuilder(API_URL).append("?");

        if (filter.getKeywords() != null && !filter.getKeywords().isBlank()) {
            sb.append("q=").append(encode(filter.getKeywords())).append("&");
        }

        if (filter.getSalaryMin() != null && !filter.getSalaryMin().isBlank()) {
            sb.append("sa=").append(filter.getSalaryMin()).append("&");
        }

        sb.append("pa=all&");
        sb.append("limit=").append(LIMIT).append("&");
        sb.append("offset=").append(offset);

        return sb.toString();
    }

    private JsonNode fetchJson(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/124.0.0.0 Safari/537.36")
                .header("Referer", "https://getmatch.ru/vacancies")
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Getmatch API returned HTTP " + response.statusCode());
        }

        return objectMapper.readTree(response.body());
    }

    private Vacancy mapVacancy(JsonNode offer) {
        String id = offer.path("id").asText();

        // Use url field from response if available, fallback to id
        String urlPath = offer.path("url").asText(null);
        String url = (urlPath != null && !urlPath.isBlank())
                ? BASE_URL + urlPath
                : VACANCY_URL + id;

        String title   = offer.path("position").asText("N/A");
        String company = offer.path("company").path("name").asText("N/A");

        // Description — strip HTML tags
        String description = offer.path("offer_description").asText(null);
        if (description != null) {
            description = description.replaceAll("<[^>]+>", "").trim();
            if (description.length() > 300) description = description.substring(0, 300) + "…";
        }

        // Location from location_items
        String location = extractLocation(offer);
        String salary   = formatSalary(offer);

        return Vacancy.builder()
                .title(title)
                .company(company)
                .url(url)
                .salary(salary)
                .location(location)
                .description(description)
                .siteKey(getSiteKey())
                .foundAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .contentHash("")
                .build();
    }

    private String extractLocation(JsonNode offer) {
        JsonNode items = offer.path("location_items");
        if (!items.isArray() || items.isEmpty()) return null;

        List<String> locations = new ArrayList<>();
        for (JsonNode item : items) {
            String label  = item.path("label").asText(null);
            String format = item.path("format").asText(null);
            if (label != null) {
                locations.add(label + (format != null ? " (" + format + ")" : ""));
            }
        }
        return locations.isEmpty() ? null : String.join(", ", locations);
    }

    private String formatSalary(JsonNode offer) {
        if (offer.path("salary_hidden").asBoolean(false)) return null;

        JsonNode from    = offer.path("salary_display_from");
        JsonNode to      = offer.path("salary_display_to");
        String currency  = offer.path("salary_currency").asText("RUB");

        if (from.isNull() && to.isNull()) return null;

        String fromStr = from.isNull() ? null : String.valueOf(from.asInt());
        String toStr   = to.isNull()   ? null : String.valueOf(to.asInt());

        if (fromStr != null && toStr != null) return fromStr + "–" + toStr + " " + currency;
        if (fromStr != null) return "from " + fromStr + " " + currency;
        if (toStr   != null) return "up to " + toStr + " " + currency;
        return null;
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value.trim(), java.nio.charset.StandardCharsets.UTF_8);
    }
}
