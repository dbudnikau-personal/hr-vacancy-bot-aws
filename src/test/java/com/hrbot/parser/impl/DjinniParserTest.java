package com.hrbot.parser.impl;

import com.hrbot.model.Vacancy;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DjinniParserTest {

    private DjinniParser parser;

    @BeforeEach
    void setUp() {
        parser = new DjinniParser();
    }

    @Test
    void parsePage_singleCard_extractsAllFields() {
        String html = """
                <html><body>
                  <div class="job-item">
                    <h2 class="job-item__position">Senior Java Developer</h2>
                    <a class="job_item__header-link" href="/jobs/123-senior-java">View job</a>
                    <span class="small text-gray-800">Acme Corp</span>
                    <span class="text-body-tertiary fw-medium">$5 000–$7 000</span>
                  </div>
                </body></html>
                """;
        Document doc = Jsoup.parse(html);

        List<Vacancy> vacancies = parser.parsePage(doc);

        assertThat(vacancies).hasSize(1);
        Vacancy v = vacancies.get(0);
        assertThat(v.getTitle()).isEqualTo("Senior Java Developer");
        assertThat(v.getCompany()).isEqualTo("Acme Corp");
        assertThat(v.getUrl()).isEqualTo("https://djinni.co/jobs/123-senior-java");
        assertThat(v.getSalary()).isEqualTo("$5 000–$7 000");
        assertThat(v.getSiteKey()).isEqualTo("djinni");
    }

    @Test
    void parsePage_multipleCards_returnsAll() {
        String html = """
                <html><body>
                  <div class="job-item">
                    <h2 class="job-item__position">Backend Engineer</h2>
                    <a class="job_item__header-link" href="/jobs/1">link</a>
                    <span class="small text-gray-800">Company A</span>
                  </div>
                  <div class="job-item">
                    <h2 class="job-item__position">Frontend Engineer</h2>
                    <a class="job_item__header-link" href="/jobs/2">link</a>
                    <span class="small text-gray-800">Company B</span>
                  </div>
                </body></html>
                """;
        Document doc = Jsoup.parse(html);

        List<Vacancy> vacancies = parser.parsePage(doc);

        assertThat(vacancies).hasSize(2);
        assertThat(vacancies).extracting(Vacancy::getTitle)
                .containsExactly("Backend Engineer", "Frontend Engineer");
    }

    @Test
    void parsePage_emptyPage_returnsEmptyList() {
        Document doc = Jsoup.parse("<html><body></body></html>");

        List<Vacancy> vacancies = parser.parsePage(doc);

        assertThat(vacancies).isEmpty();
    }

    @Test
    void parsePage_noSalary_salaryIsNull() {
        String html = """
                <html><body>
                  <div class="job-item">
                    <h2 class="job-item__position">DevOps Engineer</h2>
                    <a class="job_item__header-link" href="/jobs/99">link</a>
                    <span class="small text-gray-800">Cloud Inc</span>
                  </div>
                </body></html>
                """;
        Document doc = Jsoup.parse(html);

        List<Vacancy> vacancies = parser.parsePage(doc);

        assertThat(vacancies).hasSize(1);
        assertThat(vacancies.get(0).getSalary()).isNull();
    }

    @Test
    void parsePage_missingTitleElement_fallsBackToNA() {
        String html = """
                <html><body>
                  <div class="job-item">
                    <a class="job_item__header-link" href="/jobs/77">link</a>
                    <span class="small text-gray-800">Some Corp</span>
                  </div>
                </body></html>
                """;
        Document doc = Jsoup.parse(html);

        List<Vacancy> vacancies = parser.parsePage(doc);

        assertThat(vacancies).hasSize(1);
        assertThat(vacancies.get(0).getTitle()).isEqualTo("N/A");
    }

    @Test
    void parsePage_absoluteUrlInHref_isNotPrefixed() {
        String html = """
                <html><body>
                  <div class="job-item">
                    <h2 class="job-item__position">QA Engineer</h2>
                    <a class="job_item__header-link" href="https://djinni.co/jobs/200-qa">link</a>
                    <span class="small text-gray-800">QA House</span>
                  </div>
                </body></html>
                """;
        Document doc = Jsoup.parse(html);

        List<Vacancy> vacancies = parser.parsePage(doc);

        assertThat(vacancies.get(0).getUrl()).isEqualTo("https://djinni.co/jobs/200-qa");
    }
}
