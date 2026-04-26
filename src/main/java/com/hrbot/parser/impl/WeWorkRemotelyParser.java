package com.hrbot.parser.impl;

import com.hrbot.model.Vacancy;
import com.hrbot.model.VacancyFilter;
import com.hrbot.parser.SiteParser;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class WeWorkRemotelyParser implements SiteParser {

    private static final String FEED_URL = "https://weworkremotely.com/remote-jobs.rss";

    @Override
    public String getSiteKey() { return "weworkremotely"; }

    @Override
    public List<Vacancy> parse(VacancyFilter filter) {
        List<Vacancy> results = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(FEED_URL)
                    .parser(Parser.xmlParser())
                    .userAgent("Mozilla/5.0 (compatible; job-aggregator/1.0)")
                    .header("Accept", "application/rss+xml, application/xml, text/xml")
                    .timeout(15_000)
                    .get();

            Elements items = doc.select("item");
            String keywords = filter.getKeywords();

            for (Element item : items) {
                try {
                    Vacancy v = parseItem(item);
                    if (matchesKeywords(v, keywords)) {
                        results.add(v);
                    }
                } catch (Exception e) {
                    log.warn("WeWorkRemotely: failed to parse item: {}", e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("WeWorkRemotely parse error: {}", e.getMessage());
        }

        log.info("WeWorkRemotely: found {} vacancies for filter [{}]", results.size(), filter.getName());
        return results;
    }

    private Vacancy parseItem(Element item) {
        // title format: "Company: Job Title"
        String rawTitle = text(item, "title");
        String link     = text(item, "link");
        String region   = text(item, "region");
        String desc     = Jsoup.parse(text(item, "description")).text();

        String company = "N/A";
        String title   = rawTitle;
        if (rawTitle.contains(":")) {
            String[] parts = rawTitle.split(":", 2);
            company = parts[0].trim();
            title   = parts[1].trim();
        }

        return Vacancy.builder()
                .title(title)
                .company(company)
                .url(link != null ? link : "")
                .location(region != null && !region.isBlank() ? region : "Remote")
                .description(desc)
                .siteKey(getSiteKey())
                .foundAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .contentHash("")
                .build();
    }

    private boolean matchesKeywords(Vacancy v, String keywords) {
        if (keywords == null || keywords.isBlank()) return true;
        String kw = keywords.toLowerCase();
        String searchable = (v.getTitle() + " " + v.getDescription()).toLowerCase();
        return searchable.contains(kw);
    }

    private String text(Element parent, String tag) {
        Element el = parent.selectFirst(tag);
        return el != null ? el.text().trim() : "";
    }
}
