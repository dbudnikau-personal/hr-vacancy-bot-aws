package com.hrbot.service;

import com.hrbot.model.Vacancy;
import com.hrbot.model.ScanResult;
import com.hrbot.repository.VacancyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiffDetectorServiceTest {

    @Mock
    VacancyRepository vacancyRepository;

    @InjectMocks
    DiffDetectorService diffDetectorService;

    @Test
    void newVacancy_isSavedAndReturnedAsNew() {
        Vacancy incoming = vacancy("https://example.com/job/1", "Java Dev", "Desc", null);
        when(vacancyRepository.findByUrl(incoming.getUrl())).thenReturn(Optional.empty());
        when(vacancyRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        ScanResult result = diffDetectorService.detectChanges(List.of(incoming));

        assertThat(result.getNewVacancies()).hasSize(1);
        assertThat(result.getUpdatedVacancies()).isEmpty();
        verify(vacancyRepository).saveAll(argThat(list -> !((List<?>) list).isEmpty()));
    }

    @Test
    void existingVacancy_contentUnchanged_isNotSaved() {
        Vacancy incoming = vacancy("https://example.com/job/2", "Java Dev", "Desc", null);
        Vacancy existing = vacancy("https://example.com/job/2", "Java Dev", "Desc", null);
        existing.setId(10L);

        // Pre-compute the same hash that the service will compute
        String hash = computeExpectedHash(incoming);
        existing.setContentHash(hash);

        when(vacancyRepository.findByUrl(incoming.getUrl())).thenReturn(Optional.of(existing));
        when(vacancyRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        ScanResult result = diffDetectorService.detectChanges(List.of(incoming));

        assertThat(result.getNewVacancies()).isEmpty();
        assertThat(result.getUpdatedVacancies()).isEmpty();
    }

    @Test
    void existingVacancy_contentChanged_isUpdatedAndPreservesFoundAt() {
        LocalDateTime originalFoundAt = LocalDateTime.of(2024, 1, 1, 0, 0);
        Vacancy incoming = vacancy("https://example.com/job/3", "Java Dev", "New description", "5000");
        Vacancy existing = vacancy("https://example.com/job/3", "Java Dev", "Old description", null);
        existing.setId(20L);
        existing.setFoundAt(originalFoundAt);
        existing.setContentHash("old-hash");

        when(vacancyRepository.findByUrl(incoming.getUrl())).thenReturn(Optional.of(existing));
        when(vacancyRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        ScanResult result = diffDetectorService.detectChanges(List.of(incoming));

        assertThat(result.getUpdatedVacancies()).hasSize(1);
        assertThat(result.getNewVacancies()).isEmpty();

        Vacancy updated = result.getUpdatedVacancies().get(0);
        assertThat(updated.getId()).isEqualTo(20L);
        assertThat(updated.getFoundAt()).isEqualTo(originalFoundAt);
    }

    @Test
    void duplicateUrls_inIncomingList_deduplicatedBeforeProcessing() {
        Vacancy v1 = vacancy("https://example.com/job/4", "Java Dev", "Desc", null);
        Vacancy v2 = vacancy("https://example.com/job/4", "Java Dev", "Desc", null);
        when(vacancyRepository.findByUrl(v1.getUrl())).thenReturn(Optional.empty());
        when(vacancyRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        ScanResult result = diffDetectorService.detectChanges(List.of(v1, v2));

        assertThat(result.getNewVacancies()).hasSize(1);
    }

    @Test
    void adServerUrls_areFiltered_andNeverSaved() {
        Vacancy adVacancy = vacancy("https://adsrv.example.com/track/job/5", "Spam", "Spam", null);
        when(vacancyRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        ScanResult result = diffDetectorService.detectChanges(List.of(adVacancy));

        assertThat(result.getNewVacancies()).isEmpty();
        verify(vacancyRepository, never()).findByUrl(any());
    }

    @Test
    void nullUrl_isFiltered() {
        Vacancy noUrl = vacancy(null, "Job", "Desc", null);
        when(vacancyRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        ScanResult result = diffDetectorService.detectChanges(List.of(noUrl));

        assertThat(result.getNewVacancies()).isEmpty();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Vacancy vacancy(String url, String title, String description, String salary) {
        return Vacancy.builder()
                .url(url)
                .title(title)
                .company("Test Corp")
                .description(description)
                .salary(salary)
                .siteKey("test")
                .foundAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .contentHash("")
                .build();
    }

    private String computeExpectedHash(Vacancy v) {
        try {
            String content = v.getTitle() + v.getDescription() + v.getSalary();
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
