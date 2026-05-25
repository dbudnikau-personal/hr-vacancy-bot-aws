package com.hrbot.parser.impl;

import com.hrbot.model.Vacancy;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class HhParserTest {

    private HhParser parser;

    @BeforeEach
    void setUp() {
        HhAreaResolver areaResolver = mock(HhAreaResolver.class);
        parser = new HhParser(areaResolver);
    }

    @Test
    void parsePage_singleCard_extractsAllFields() {
        String html = """
                <html><body>
                  <div class="vacancy-card--n77Dj8TY8VIUF0yM">
                    <span data-qa="serp-item__title-text">Java Developer</span>
                    <a data-qa="serp-item__title" href="https://hh.ru/vacancy/123?from=recommend">link</a>
                    <span data-qa="vacancy-serp__vacancy-employer-text">Tech Company</span>
                    <span data-qa="vacancy-serp__vacancy-address">Moscow</span>
                    <span data-qa="vacancy-serp__vacancy-compensation">150 000–200 000 руб.</span>
                  </div>
                </body></html>
                """;
        Document doc = Jsoup.parse(html);

        List<Vacancy> vacancies = parser.parsePage(doc);

        assertThat(vacancies).hasSize(1);
        Vacancy v = vacancies.get(0);
        assertThat(v.getTitle()).isEqualTo("Java Developer");
        assertThat(v.getCompany()).isEqualTo("Tech Company");
        assertThat(v.getLocation()).isEqualTo("Moscow");
        assertThat(v.getSalary()).isEqualTo("150 000–200 000 руб.");
        assertThat(v.getSiteKey()).isEqualTo("hh");
    }

    @Test
    void parsePage_urlQueryParamsAreStripped() {
        String html = """
                <html><body>
                  <div class="vacancy-card--n77Dj8TY8VIUF0yM">
                    <span data-qa="serp-item__title-text">Engineer</span>
                    <a data-qa="serp-item__title" href="https://hh.ru/vacancy/999?from=search&amp;hhtmFrom=vacancy_search_list">link</a>
                    <span data-qa="vacancy-serp__vacancy-employer-text">Corp</span>
                  </div>
                </body></html>
                """;
        Document doc = Jsoup.parse(html);

        List<Vacancy> vacancies = parser.parsePage(doc);

        assertThat(vacancies.get(0).getUrl()).isEqualTo("https://hh.ru/vacancy/999");
    }

    @Test
    void parsePage_fallbackSelector_whenPrimaryCardSelectorMissing() {
        // HhParser falls back to data-qa="vacancy-serp__vacancy" when the CSS-class selector fails
        String html = """
                <html><body>
                  <div data-qa="vacancy-serp__vacancy">
                    <span data-qa="serp-item__title-text">Fallback Job</span>
                    <a data-qa="serp-item__title" href="https://hh.ru/vacancy/55">link</a>
                    <span data-qa="vacancy-serp__vacancy-employer-text">Fallback Corp</span>
                  </div>
                </body></html>
                """;
        Document doc = Jsoup.parse(html);

        List<Vacancy> vacancies = parser.parsePage(doc);

        assertThat(vacancies).hasSize(1);
        assertThat(vacancies.get(0).getTitle()).isEqualTo("Fallback Job");
    }

    @Test
    void parsePage_multipleCards_returnsAll() {
        String html = """
                <html><body>
                  <div class="vacancy-card--n77Dj8TY8VIUF0yM">
                    <span data-qa="serp-item__title-text">Job A</span>
                    <a data-qa="serp-item__title" href="https://hh.ru/vacancy/1">link</a>
                    <span data-qa="vacancy-serp__vacancy-employer-text">Corp A</span>
                  </div>
                  <div class="vacancy-card--n77Dj8TY8VIUF0yM">
                    <span data-qa="serp-item__title-text">Job B</span>
                    <a data-qa="serp-item__title" href="https://hh.ru/vacancy/2">link</a>
                    <span data-qa="vacancy-serp__vacancy-employer-text">Corp B</span>
                  </div>
                </body></html>
                """;
        Document doc = Jsoup.parse(html);

        List<Vacancy> vacancies = parser.parsePage(doc);

        assertThat(vacancies).hasSize(2);
        assertThat(vacancies).extracting(Vacancy::getTitle)
                .containsExactly("Job A", "Job B");
    }

    @Test
    void parsePage_emptyPage_returnsEmptyList() {
        Document doc = Jsoup.parse("<html><body></body></html>");

        List<Vacancy> vacancies = parser.parsePage(doc);

        assertThat(vacancies).isEmpty();
    }

    @Test
    void parsePage_noSalaryAndNoLocation_nullFields() {
        String html = """
                <html><body>
                  <div class="vacancy-card--n77Dj8TY8VIUF0yM">
                    <span data-qa="serp-item__title-text">Minimalist Job</span>
                    <a data-qa="serp-item__title" href="https://hh.ru/vacancy/10">link</a>
                    <span data-qa="vacancy-serp__vacancy-employer-text">Solo Corp</span>
                  </div>
                </body></html>
                """;
        Document doc = Jsoup.parse(html);

        List<Vacancy> vacancies = parser.parsePage(doc);

        assertThat(vacancies).hasSize(1);
        assertThat(vacancies.get(0).getSalary()).isNull();
        assertThat(vacancies.get(0).getLocation()).isNull();
    }
}
