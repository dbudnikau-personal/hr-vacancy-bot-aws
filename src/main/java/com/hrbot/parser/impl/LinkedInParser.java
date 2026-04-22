package com.hrbot.parser.impl;

import com.hrbot.model.Vacancy;
import com.hrbot.model.VacancyFilter;
import com.hrbot.parser.SiteParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LinkedIn parser — not yet implemented.
 *
 * LinkedIn aggressively protects against scraping and requires authentication.
 * Planned implementation: Playwright-based browser automation with session cookies.
 *
 * To implement:
 * 1. Log in once manually, export cookies
 * 2. Use Playwright to load cookies and fetch job search pages
 * 3. Parse job cards from the authenticated DOM
 */
@Slf4j
@Component
public class LinkedInParser implements SiteParser {

    @Override
    public String getSiteKey() { return "linkedin"; }

    @Override
    public boolean supportsPlaywright() { return true; }

    @Override
    public List<Vacancy> parse(VacancyFilter filter) {
        log.warn("LinkedIn parser is not yet implemented. Skipping filter [{}].", filter.getName());
        return List.of();
    }
}
