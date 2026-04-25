package com.hrbot.service;

import com.hrbot.ai.VacancyRelevanceService;
import com.hrbot.model.ScanResult;
import com.hrbot.model.Vacancy;
import com.hrbot.model.VacancyFilter;
import com.hrbot.parser.ParserRegistry;
import com.hrbot.parser.ParserStatusRegistry;
import com.hrbot.parser.SiteParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VacancyService {

    private final ParserRegistry parserRegistry;
    private final DiffDetectorService diffDetector;
    private final ParserStatusRegistry statusRegistry;
    private final VacancyRelevanceService relevanceService;

    public ScanResult scanForFilter(VacancyFilter filter) {
        List<Vacancy> allFound = new ArrayList<>();
        for (String siteKey : filter.getSites()) {
            try {
                SiteParser parser = parserRegistry.getParser(siteKey);
                List<Vacancy> vacancies = parser.parse(filter);
                List<Vacancy> relevant = filterByRelevance(vacancies, filter);
                allFound.addAll(relevant);
                log.info("Site [{}]: {} found, {} relevant after AI filter", siteKey, vacancies.size(), relevant.size());
                statusRegistry.recordSuccess(siteKey, relevant.size());
            } catch (Exception e) {
                log.error("Error parsing site [{}] for filter [{}]: {}", siteKey, filter.getName(), e.getMessage());
                statusRegistry.recordError(siteKey, e.getMessage());
            }
        }
        ScanResult result = diffDetector.detectChanges(allFound);
        result.setTotalFound(allFound.size());
        return result;
    }

    public ScanResult scanSingleSite(VacancyFilter filter, String siteKey) {
        SiteParser parser = parserRegistry.getParser(siteKey);
        List<Vacancy> vacancies = parser.parse(filter);
        List<Vacancy> relevant = filterByRelevance(vacancies, filter);
        log.info("Site [{}]: {} found, {} relevant after AI filter", siteKey, vacancies.size(), relevant.size());
        statusRegistry.recordSuccess(siteKey, relevant.size());
        ScanResult result = diffDetector.detectChanges(relevant);
        result.setTotalFound(relevant.size());
        return result;
    }

    private List<Vacancy> filterByRelevance(List<Vacancy> vacancies, VacancyFilter filter) {
        if (filter.getKeywords() == null || filter.getKeywords().isBlank()) {
            return vacancies;
        }
        return vacancies.stream()
                .filter(v -> relevanceService.isRelevant(v, filter))
                .toList();
    }
}
