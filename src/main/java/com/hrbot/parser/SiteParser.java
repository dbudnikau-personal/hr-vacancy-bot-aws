package com.hrbot.parser;

import com.hrbot.model.Vacancy;
import com.hrbot.model.VacancyFilter;
import java.util.List;

public interface SiteParser {
    String getSiteKey();
    List<Vacancy> parse(VacancyFilter filter);
    default boolean supportsPlaywright() { return false; }
}
