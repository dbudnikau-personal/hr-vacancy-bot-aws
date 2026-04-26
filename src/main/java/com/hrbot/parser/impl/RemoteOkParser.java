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
public class RemoteOkParser implements SiteParser {

    private static final String FEED_URL = "https://remoteok.com/remote-jobs.rss";

    @Override
    public String getSiteKey() { return "remoteok"; }

    @Override
    public List<Vacancy> parse(VacancyFilter filter) {
        List<Vacancy> results = new ArrayList<>();
        String url = buildUrl(filter);

        try {
            Document doc = Jsoup.connect(url)
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
                    log.warn("RemoteOK: failed to parse item: {}", e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("RemoteOK parse error: {}", e.getMessage());
        }

        log.info("RemoteOK: found {} vacancies for filter [{}]", results.size(), filter.getName());
        return results;
    }

    private String buildUrl(VacancyFilter filter) {
        // RemoteOK supports tag-based URLs: /remote-java-jobs.rss
        // Fall back to generic feed if keywords are multi-word or absent
        if (filter.getKeywords() != null && !filter.getKeywords().isBlank()) {
            String tag = filter.getKeywords().trim().toLowerCase()
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("-+", "-")
                    .replaceAll("^-|-$", "");
            return "https://remoteok.com/remote-" + tag + "-jobs.rss";
        }
        return FEED_URL;
    }

    private Vacancy parseItem(Element item) {
        String title   = text(item, "title");
        String link    = text(item, "link");
        String company = text(item, "author");
        String location = text(item, "location");
        String salary  = text(item, "salary");
        String desc    = Jsoup.parse(text(item, "description")).text();

        // title format is often "Company | Role" — extract company if author is missing
        if ((company == null || company.isBlank()) && title.contains("|")) {
            String[] parts = title.split("\\|", 2);
            company = parts[0].trim();
            title   = parts[1].trim();
        }

        return Vacancy.builder()
                .title(title)
                .company(company != null && !company.isBlank() ? company : "N/A")
                .url(link != null ? link : "")
                .location(location != null && !location.isBlank() ? location : "Remote")
                .salary(salary != null && !salary.isBlank() ? salary : null)
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
