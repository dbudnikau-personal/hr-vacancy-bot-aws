package com.hrbot.parser.impl;

import com.hrbot.model.Vacancy;
import com.hrbot.service.AsyncTaskDispatcher;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ssm.SsmClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WellfoundParserTest {

    private WellfoundParser parser;

    @BeforeEach
    void setUp() {
        parser = new WellfoundParser(mock(AsyncTaskDispatcher.class), mock(SsmClient.class));
    }

    @Test
    void parsePage_singleJobGroup_extractsJobLinks() {
        // Wellfound page structure: company info is in the previous sibling of the job group div.
        // The parser calls group.previousElementSibling() to get the company name.
        String html = """
                <html><body>
                  <div class="company-header">Wellfound Company</div>
                  <div class="mb-4 w-full px-4">
                    <a href="/jobs/1234-backend-engineer">Backend Engineer</a>
                  </div>
                </body></html>
                """;
        Document doc = Jsoup.parse(html);

        List<Vacancy> vacancies = parser.parsePage(doc);

        assertThat(vacancies).hasSize(1);
        Vacancy v = vacancies.get(0);
        assertThat(v.getTitle()).isEqualTo("Backend Engineer");
        assertThat(v.getCompany()).isEqualTo("Wellfound Company");
        assertThat(v.getUrl()).isEqualTo("https://wellfound.com/jobs/1234-backend-engineer");
        assertThat(v.getSiteKey()).isEqualTo("wellfound");
    }

    @Test
    void parsePage_multipleJobsInGroup_returnsAll() {
        String html = """
                <html><body>
                  <div class="company-header">Multi Corp</div>
                  <div class="mb-4 w-full px-4">
                    <a href="/jobs/1">Job One</a>
                    <a href="/jobs/2">Job Two</a>
                    <a href="/jobs/3">Job Three</a>
                  </div>
                </body></html>
                """;
        Document doc = Jsoup.parse(html);

        List<Vacancy> vacancies = parser.parsePage(doc);

        assertThat(vacancies).hasSize(3);
        assertThat(vacancies).extracting(Vacancy::getTitle)
                .containsExactly("Job One", "Job Two", "Job Three");
    }

    @Test
    void parsePage_emptyPage_returnsEmptyList() {
        Document doc = Jsoup.parse("<html><body></body></html>");

        List<Vacancy> vacancies = parser.parsePage(doc);

        assertThat(vacancies).isEmpty();
    }

    @Test
    void parsePage_noPreviousSibling_companyIsNA() {
        // When the job group has no previous sibling, company should fall back to "N/A"
        String html = """
                <html><body>
                  <div class="mb-4 w-full px-4">
                    <a href="/jobs/5">Solo Job</a>
                  </div>
                </body></html>
                """;
        Document doc = Jsoup.parse(html);

        List<Vacancy> vacancies = parser.parsePage(doc);

        assertThat(vacancies).hasSize(1);
        assertThat(vacancies.get(0).getCompany()).isEqualTo("N/A");
    }

    @Test
    void parsePage_absoluteJobUrl_isNotPrefixed() {
        String html = """
                <html><body>
                  <div class="company-header">Absolute Corp</div>
                  <div class="mb-4 w-full px-4">
                    <a href="https://wellfound.com/jobs/999-role">Full Stack Dev</a>
                  </div>
                </body></html>
                """;
        Document doc = Jsoup.parse(html);

        List<Vacancy> vacancies = parser.parsePage(doc);

        assertThat(vacancies.get(0).getUrl()).isEqualTo("https://wellfound.com/jobs/999-role");
    }
}
