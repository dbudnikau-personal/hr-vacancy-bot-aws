package com.hrbot.repository;

import com.hrbot.model.Vacancy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class VacancyRepositoryTest {

    @TestConfiguration
    static class ContainerConfig {
        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgres() {
            return new PostgreSQLContainer<>("postgres:16-alpine");
        }
    }

    @Autowired
    VacancyRepository vacancyRepository;

    @Test
    void findByKeyword_matchesTitleSubstring() {
        save("Senior Java Developer", "Acme", "https://example.com/1");
        save("Python Engineer", "Beta", "https://example.com/2");

        Page<Vacancy> result = vacancyRepository.findByKeyword("java", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).contains("Java");
    }

    @Test
    void findByKeyword_matchesCompanySubstring() {
        save("Backend Dev", "Acme Corp", "https://example.com/3");
        save("Backend Dev", "Beta LLC", "https://example.com/4");

        Page<Vacancy> result = vacancyRepository.findByKeyword("acme", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCompany()).contains("Acme");
    }

    @Test
    void findByKeyword_isCaseInsensitive() {
        save("JAVA DEVELOPER", "Company", "https://example.com/5");

        Page<Vacancy> result = vacancyRepository.findByKeyword("java developer", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void findByKeyword_percentSign_treatedAsLiteral_notWildcard() {
        save("100% Remote Java", "Corp", "https://example.com/6");
        save("Python Engineer", "Corp", "https://example.com/7");

        // Without ESCAPE, '%' would match everything; with ESCAPE it matches only the literal '%'
        Page<Vacancy> result = vacancyRepository.findByKeyword("100!%", PageRequest.of(0, 10));

        // "100!%" as LIKE pattern means "100%" literally — should match "100% Remote Java"
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).contains("100%");
    }

    @Test
    void findByKeyword_underscoreSign_treatedAsLiteral_notSingleCharWildcard() {
        save("Java_Spring Dev", "Corp", "https://example.com/8");
        save("Java Spring Dev", "Corp", "https://example.com/9");

        // Without ESCAPE, '_' would match any single character so both rows would match
        Page<Vacancy> result = vacancyRepository.findByKeyword("java!_spring", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).contains("Java_Spring");
    }

    @Test
    void findByKeyword_emptyResults_whenNoMatch() {
        save("Java Developer", "Corp", "https://example.com/10");

        Page<Vacancy> result = vacancyRepository.findByKeyword("cobol", PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void save(String title, String company, String url) {
        vacancyRepository.save(Vacancy.builder()
                .title(title)
                .company(company)
                .url(url)
                .description("desc")
                .siteKey("test")
                .foundAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .contentHash("hash")
                .build());
    }
}
