package com.hrbot.parser.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrbot.model.Vacancy;
import com.hrbot.model.VacancyFilter;
import com.hrbot.parser.SiteParser;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class RemotiveParser implements SiteParser {

    private static final String API_URL = "https://remotive.com/api/remote-jobs";
    private static final int    LIMIT   = 100;

    private final HttpClient    http   = HttpClient.newHttpClient();
    private final ObjectMapper  mapper = new ObjectMapper();

    @Override
    public String getSiteKey() { return "remotive"; }

    @Override
    public List<Vacancy> parse(VacancyFilter filter) {
        List<Vacancy> results = new ArrayList<>();
        String url = buildUrl(filter);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .header("User-Agent", "Mozilla/5.0 (compatible; job-aggregator/1.0)")
                    .GET()
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Remotive API returned status {}", response.statusCode());
                return results;
            }

            JsonNode root = mapper.readTree(response.body());
            JsonNode jobs = root.path("jobs");

            for (JsonNode job : jobs) {
                try {
                    results.add(parseJob(job));
                } catch (Exception e) {
                    log.warn("Remotive: failed to parse job: {}", e.getMessage());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Remotive parse error: {}", e.getMessage());
        }

        log.info("Remotive: found {} vacancies for filter [{}]", results.size(), filter.getName());
        return results;
    }

    private Vacancy parseJob(JsonNode job) {
        String title    = job.path("title").asText();
        String company  = job.path("company_name").asText("N/A");
        String url      = job.path("url").asText();
        String location = job.path("candidate_required_location").asText("Remote");
        String salary   = job.path("salary").asText(null);
        String descHtml = job.path("description").asText("");
        String desc     = Jsoup.parse(descHtml).text();

        return Vacancy.builder()
                .title(title)
                .company(company)
                .url(url)
                .location(location.isBlank() ? "Remote" : location)
                .salary(salary != null && !salary.isBlank() ? salary : null)
                .description(desc)
                .siteKey(getSiteKey())
                .foundAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .contentHash("")
                .build();
    }

    private String buildUrl(VacancyFilter filter) {
        StringBuilder sb = new StringBuilder(API_URL).append("?limit=").append(LIMIT);
        if (filter.getKeywords() != null && !filter.getKeywords().isBlank()) {
            sb.append("&search=")
              .append(URLEncoder.encode(filter.getKeywords().trim(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
