package com.hrbot.service;

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

    public ScanResult scanForFilter(VacancyFilter filter) {
        List<Vacancy> allFound = new ArrayList<>();

        for (String siteKey : filter.getSites()) {
            try {
                SiteParser parser = parserRegistry.getParser(siteKey);
                List<Vacancy> vacancies = parser.parse(filter);
                allFound.addAll(vacancies);

                // Record success
                statusRegistry.recordSuccess(siteKey, vacancies.size());

            } catch (Exception e) {
                log.error("Error parsing site [{}] for filter [{}]: {}", siteKey, filter.getName(), e.getMessage());
                // Record error — visible via /status
                statusRegistry.recordError(siteKey, e.getMessage());
            }
        }

        return diffDetector.detectChanges(allFound);
    }
}
