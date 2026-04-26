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

/**
 * Parses Canadian Job Bank (jobbank.gc.ca) via Atom feed.
 * Feed URL: /jobsearch/feed/jobSearchRSSfeed?searchstring=...&locationstring=...&distance=...
 */
@Slf4j
@Component
public class JobBankParser implements SiteParser {

    private static final String BASE_URL = "https://www.jobbank.gc.ca";
    private static final String FEED_URL = BASE_URL + "/jobsearch/feed/jobSearchRSSfeed";
    private static final int    MAX_PAGES = 3;
    private static final int    PAGE_SIZE = 25;

    @Override
    public String getSiteKey() { return "jobbank"; }

    @Override
    public List<Vacancy> parse(VacancyFilter filter) {
        List<Vacancy> results = new ArrayList<>();

        for (int page = 0; page < MAX_PAGES; page++) {
            String url = buildUrl(filter, page * PAGE_SIZE);
            try {
                log.debug("Fetching Job Bank page {}: {}", page, url);
                Document doc = fetchFeed(url);
                Elements entries = doc.select("entry");

                if (entries.isEmpty()) {
                    log.debug("Job Bank: no entries on page {}", page);
                    break;
                }

                for (Element entry : entries) {
                    try {
                        results.add(parseEntry(entry));
                    } catch (Exception e) {
                        log.warn("Job Bank: failed to parse entry: {}", e.getMessage());
                    }
                }

                if (entries.size() < PAGE_SIZE) break; // last page

                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Job Bank parse error on page {}: {}", page, e.getMessage());
                break;
            }
        }

        log.info("Job Bank: found {} vacancies for filter [{}]", results.size(), filter.getName());
        return results;
    }

    private Document fetchFeed(String url) throws IOException {
        return Jsoup.connect(url)
                .parser(Parser.xmlParser())
                .userAgent("Mozilla/5.0 (compatible; job-aggregator/1.0)")
                .header("Accept", "application/atom+xml, application/xml, text/xml")
                .timeout(10_000)
                .get();
    }

    private Vacancy parseEntry(Element entry) {
        String title = text(entry, "title");
        String url   = attr(entry, "link", "href");
        if (url != null && url.startsWith("/")) url = BASE_URL + url;

        // <summary> contains HTML with job details
        String rawSummary = text(entry, "summary");
        Document summary  = Jsoup.parse(rawSummary);

        String location = extractLabel(summary, "Location:");
        String employer = extractLabel(summary, "Employer:");
        String salary   = extractLabel(summary, "Salary:");

        return Vacancy.builder()
                .title(title)
                .company(employer != null ? employer : "N/A")
                .url(url != null ? url : "")
                .location(location)
                .salary(salary)
                .siteKey(getSiteKey())
                .foundAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .contentHash("")
                .build();
    }

    /**
     * Extracts value after a label like "Location: Toronto, ON" from summary HTML.
     */
    private String extractLabel(Document html, String label) {
        for (Element el : html.getAllElements()) {
            String text = el.ownText().trim();
            if (text.startsWith(label)) {
                String value = text.substring(label.length()).trim();
                if (!value.isEmpty()) return value;
                // value might be in next sibling
                Element sibling = el.nextElementSibling();
                if (sibling != null) return sibling.text().trim();
            }
        }
        return null;
    }

    private String text(Element parent, String tag) {
        Element el = parent.selectFirst(tag);
        return el != null ? el.text().trim() : "";
    }

    private String attr(Element parent, String tag, String attrName) {
        Element el = parent.selectFirst(tag);
        return el != null ? el.attr(attrName) : null;
    }

    private String buildUrl(VacancyFilter filter, int start) {
        StringBuilder sb = new StringBuilder(FEED_URL).append("?");

        if (filter.getKeywords() != null && !filter.getKeywords().isBlank()) {
            sb.append("searchstring=").append(encode(filter.getKeywords())).append("&");
        }

        boolean hasLocation = filter.getLocation() != null && !filter.getLocation().isBlank();
        String location = hasLocation ? filter.getLocation() : "Canada";
        int distance    = hasLocation ? 50 : 500;
        sb.append("locationstring=").append(encode(location)).append("&");
        sb.append("distance=").append(distance).append("&");
        sb.append("sort=D");  // sort by date

        if (start > 0) {
            sb.append("&start=").append(start);
        }

        return sb.toString();
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value.trim(), java.nio.charset.StandardCharsets.UTF_8);
    }
}
