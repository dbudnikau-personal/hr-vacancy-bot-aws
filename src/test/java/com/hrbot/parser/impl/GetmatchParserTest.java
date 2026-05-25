package com.hrbot.parser.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrbot.model.Vacancy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GetmatchParserTest {

    private GetmatchParser parser;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        parser = new GetmatchParser(objectMapper);
    }

    // ── mapVacancy ────────────────────────────────────────────────────────────

    @Test
    void mapVacancy_fullOffer_extractsAllFields() throws Exception {
        String json = """
                {
                  "id": "42",
                  "url": "/vacancies/42",
                  "position": "Backend Engineer",
                  "company": {"name": "Startup Inc"},
                  "offer_description": "<p>Great job opportunity</p>",
                  "location_items": [{"label": "Remote", "format": "remote"}],
                  "salary_hidden": false,
                  "salary_display_from": 100000,
                  "salary_display_to": 150000,
                  "salary_currency": "RUB"
                }
                """;
        JsonNode offer = objectMapper.readTree(json);

        Vacancy v = parser.mapVacancy(offer);

        assertThat(v.getTitle()).isEqualTo("Backend Engineer");
        assertThat(v.getCompany()).isEqualTo("Startup Inc");
        assertThat(v.getUrl()).isEqualTo("https://getmatch.ru/vacancies/42");
        assertThat(v.getSalary()).isEqualTo("100000–150000 RUB");
        assertThat(v.getLocation()).isEqualTo("Remote (remote)");
        assertThat(v.getDescription()).isEqualTo("Great job opportunity");
        assertThat(v.getSiteKey()).isEqualTo("getmatch");
    }

    @Test
    void mapVacancy_noUrlField_fallsBackToIdPath() throws Exception {
        String json = """
                {
                  "id": "99",
                  "position": "QA Engineer",
                  "company": {"name": "QA Corp"},
                  "salary_hidden": true
                }
                """;
        JsonNode offer = objectMapper.readTree(json);

        Vacancy v = parser.mapVacancy(offer);

        assertThat(v.getUrl()).isEqualTo("https://getmatch.ru/vacancies/99");
    }

    @Test
    void mapVacancy_longDescription_isTruncatedAt300() throws Exception {
        String longText = "A".repeat(400);
        String json = """
                {
                  "id": "1",
                  "position": "Dev",
                  "company": {"name": "Corp"},
                  "offer_description": "%s",
                  "salary_hidden": true
                }
                """.formatted(longText);
        JsonNode offer = objectMapper.readTree(json);

        Vacancy v = parser.mapVacancy(offer);

        assertThat(v.getDescription()).hasSize(301); // 300 chars + "…"
        assertThat(v.getDescription()).endsWith("…");
    }

    @Test
    void mapVacancy_htmlInDescription_isStripped() throws Exception {
        String json = """
                {
                  "id": "2",
                  "position": "Dev",
                  "company": {"name": "Corp"},
                  "offer_description": "<strong>Exciting</strong> <em>role</em>",
                  "salary_hidden": true
                }
                """;
        JsonNode offer = objectMapper.readTree(json);

        Vacancy v = parser.mapVacancy(offer);

        assertThat(v.getDescription()).isEqualTo("Exciting role");
    }

    // ── formatSalary ─────────────────────────────────────────────────────────

    @Test
    void formatSalary_hiddenSalary_returnsNull() throws Exception {
        JsonNode offer = objectMapper.readTree("""
                {"salary_hidden": true}
                """);

        assertThat(parser.formatSalary(offer)).isNull();
    }

    @Test
    void formatSalary_bothFromAndTo_returnsRange() throws Exception {
        JsonNode offer = objectMapper.readTree("""
                {
                  "salary_hidden": false,
                  "salary_display_from": 80000,
                  "salary_display_to": 120000,
                  "salary_currency": "USD"
                }
                """);

        assertThat(parser.formatSalary(offer)).isEqualTo("80000–120000 USD");
    }

    @Test
    void formatSalary_onlyFrom_returnsFromLabel() throws Exception {
        JsonNode offer = objectMapper.readTree("""
                {
                  "salary_hidden": false,
                  "salary_display_from": 50000,
                  "salary_display_to": null,
                  "salary_currency": "RUB"
                }
                """);

        assertThat(parser.formatSalary(offer)).isEqualTo("from 50000 RUB");
    }

    @Test
    void formatSalary_onlyTo_returnsUpToLabel() throws Exception {
        JsonNode offer = objectMapper.readTree("""
                {
                  "salary_hidden": false,
                  "salary_display_from": null,
                  "salary_display_to": 200000,
                  "salary_currency": "RUB"
                }
                """);

        assertThat(parser.formatSalary(offer)).isEqualTo("up to 200000 RUB");
    }

    @Test
    void formatSalary_bothNull_returnsNull() throws Exception {
        JsonNode offer = objectMapper.readTree("""
                {
                  "salary_hidden": false,
                  "salary_display_from": null,
                  "salary_display_to": null
                }
                """);

        assertThat(parser.formatSalary(offer)).isNull();
    }

    // ── extractLocation ───────────────────────────────────────────────────────

    @Test
    void extractLocation_multipleItems_joinedWithComma() throws Exception {
        JsonNode offer = objectMapper.readTree("""
                {
                  "location_items": [
                    {"label": "Saint Petersburg", "format": "office"},
                    {"label": "Remote", "format": "remote"}
                  ]
                }
                """);

        assertThat(parser.extractLocation(offer))
                .isEqualTo("Saint Petersburg (office), Remote (remote)");
    }

    @Test
    void extractLocation_emptyArray_returnsNull() throws Exception {
        JsonNode offer = objectMapper.readTree("""
                {"location_items": []}
                """);

        assertThat(parser.extractLocation(offer)).isNull();
    }

    @Test
    void extractLocation_itemWithoutFormat_omitsParentheses() throws Exception {
        JsonNode offer = objectMapper.readTree("""
                {"location_items": [{"label": "Worldwide"}]}
                """);

        assertThat(parser.extractLocation(offer)).isEqualTo("Worldwide");
    }

    @Test
    void extractLocation_missingField_returnsNull() throws Exception {
        JsonNode offer = objectMapper.readTree("{}");

        assertThat(parser.extractLocation(offer)).isNull();
    }
}
