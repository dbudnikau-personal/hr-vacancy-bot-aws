package com.hrbot.parser.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrbot.model.Vacancy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RemotiveParserTest {

    private RemotiveParser parser;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        parser = new RemotiveParser();
    }

    @Test
    void parseJob_fullEntry_extractsAllFields() throws Exception {
        String json = """
                {
                  "title": "Senior Backend Engineer",
                  "company_name": "Remote Corp",
                  "url": "https://remotive.com/jobs/1234",
                  "candidate_required_location": "Europe",
                  "salary": "$120k–$160k",
                  "description": "<p>Build amazing things</p>"
                }
                """;
        JsonNode job = objectMapper.readTree(json);

        Vacancy v = parser.parseJob(job);

        assertThat(v.getTitle()).isEqualTo("Senior Backend Engineer");
        assertThat(v.getCompany()).isEqualTo("Remote Corp");
        assertThat(v.getUrl()).isEqualTo("https://remotive.com/jobs/1234");
        assertThat(v.getLocation()).isEqualTo("Europe");
        assertThat(v.getSalary()).isEqualTo("$120k–$160k");
        assertThat(v.getDescription()).isEqualTo("Build amazing things");
        assertThat(v.getSiteKey()).isEqualTo("remotive");
    }

    @Test
    void parseJob_htmlDescription_isStrippedByJsoup() throws Exception {
        String json = """
                {
                  "title": "Dev",
                  "company_name": "Corp",
                  "url": "https://remotive.com/jobs/1",
                  "description": "<ul><li>Write code</li><li>Review PRs</li></ul>"
                }
                """;
        JsonNode job = objectMapper.readTree(json);

        Vacancy v = parser.parseJob(job);

        assertThat(v.getDescription()).doesNotContain("<", ">");
        assertThat(v.getDescription()).contains("Write code");
    }

    @Test
    void parseJob_missingCompany_fallsBackToNA() throws Exception {
        String json = """
                {
                  "title": "Mystery Role",
                  "url": "https://remotive.com/jobs/2"
                }
                """;
        JsonNode job = objectMapper.readTree(json);

        Vacancy v = parser.parseJob(job);

        assertThat(v.getCompany()).isEqualTo("N/A");
    }

    @Test
    void parseJob_emptyLocation_defaultsToRemote() throws Exception {
        String json = """
                {
                  "title": "Remote Dev",
                  "company_name": "Global Corp",
                  "url": "https://remotive.com/jobs/3",
                  "candidate_required_location": ""
                }
                """;
        JsonNode job = objectMapper.readTree(json);

        Vacancy v = parser.parseJob(job);

        assertThat(v.getLocation()).isEqualTo("Remote");
    }

    @Test
    void parseJob_blankSalary_salaryIsNull() throws Exception {
        String json = """
                {
                  "title": "Dev",
                  "company_name": "Corp",
                  "url": "https://remotive.com/jobs/4",
                  "salary": "   "
                }
                """;
        JsonNode job = objectMapper.readTree(json);

        Vacancy v = parser.parseJob(job);

        assertThat(v.getSalary()).isNull();
    }

    @Test
    void parseJob_missingSalaryField_salaryIsNull() throws Exception {
        String json = """
                {
                  "title": "Dev",
                  "company_name": "Corp",
                  "url": "https://remotive.com/jobs/5"
                }
                """;
        JsonNode job = objectMapper.readTree(json);

        Vacancy v = parser.parseJob(job);

        assertThat(v.getSalary()).isNull();
    }
}
