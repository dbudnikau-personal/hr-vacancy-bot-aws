package com.hrbot.service;

import com.hrbot.model.ScanResult;
import com.hrbot.model.Vacancy;
import com.hrbot.repository.VacancyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiffDetectorService {

    private final VacancyRepository vacancyRepository;

    @Transactional
    public ScanResult detectChanges(List<Vacancy> incoming) {
        List<Vacancy> newVacancies = new ArrayList<>();
        List<Vacancy> updatedVacancies = new ArrayList<>();

        // Deduplicate incoming list by URL before processing
        Map<String, Vacancy> deduped = new LinkedHashMap<>();
        for (Vacancy v : incoming) {
            if (v.getUrl() != null && !v.getUrl().contains("adsrv")) {
                deduped.putIfAbsent(v.getUrl(), v);
            }
        }

        for (Vacancy vacancy : deduped.values()) {
            String hash = computeHash(vacancy);
            vacancy.setContentHash(hash);
            vacancy.setUpdatedAt(LocalDateTime.now());

            Optional<Vacancy> existing = vacancyRepository.findByUrl(vacancy.getUrl());
            if (existing.isEmpty()) {
                vacancy.setFoundAt(LocalDateTime.now());
                newVacancies.add(vacancy);
            } else if (!existing.get().getContentHash().equals(hash)) {
                vacancy.setId(existing.get().getId());
                vacancy.setFoundAt(existing.get().getFoundAt());
                updatedVacancies.add(vacancy);
            }
        }

        vacancyRepository.saveAll(newVacancies);
        vacancyRepository.saveAll(updatedVacancies);

        return ScanResult.builder()
                .newVacancies(newVacancies)
                .updatedVacancies(updatedVacancies)
                .build();
    }

    private String computeHash(Vacancy vacancy) {
        try {
            String content = vacancy.getTitle() + vacancy.getDescription() + vacancy.getSalary();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
}
