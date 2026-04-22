package com.hrbot.ai;

import com.hrbot.model.Vacancy;
import com.hrbot.model.VacancyFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VacancyRelevanceService {

    private static final String SYSTEM_PROMPT = """
            You are a vacancy relevance evaluator. Given a job vacancy and filter criteria,
            respond with only "yes" if the vacancy is relevant, or "no" if it is not.
            Be strict: the vacancy must match the keywords and requirements.
            """;

    private final DeepSeekClient deepSeekClient;

    public boolean isRelevant(Vacancy vacancy, VacancyFilter filter) {
        String userMessage = buildMessage(vacancy, filter);
        String response = deepSeekClient.chat(SYSTEM_PROMPT, userMessage);

        if (response == null) {
            log.warn("DeepSeek returned null for vacancy [{}], treating as relevant", vacancy.getUrl());
            return true;
        }

        boolean relevant = response.trim().toLowerCase().startsWith("yes");
        log.debug("Vacancy [{}] relevance: {} (filter: {})", vacancy.getTitle(), relevant, filter.getName());
        return relevant;
    }

    private String buildMessage(Vacancy vacancy, VacancyFilter filter) {
        return """
                Filter criteria:
                - Keywords: %s
                - Location: %s
                - Minimum salary: %s

                Vacancy:
                - Title: %s
                - Company: %s
                - Location: %s
                - Salary: %s
                - Description: %s

                Is this vacancy relevant to the filter criteria?
                """.formatted(
                filter.getKeywords(),
                filter.getLocation() != null ? filter.getLocation() : "any",
                filter.getSalaryMin() != null ? filter.getSalaryMin() : "any",
                vacancy.getTitle(),
                vacancy.getCompany(),
                vacancy.getLocation(),
                vacancy.getSalary() != null ? vacancy.getSalary() : "not specified",
                truncate(vacancy.getDescription(), 500)
        );
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
