package com.hrbot.parser.impl;

import com.hrbot.model.Vacancy;
import com.hrbot.model.VacancyFilter;
import com.hrbot.parser.SiteParser;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class DjinniParser implements SiteParser {

    private static final String BASE_URL  = "https://djinni.co";
    private static final String JOBS_URL  = BASE_URL + "/jobs/";

    private static final String JOB_CARD       = "div.job-item";
    private static final String JOB_TITLE      = "h2.job-item__position";
    private static final String JOB_LINK       = "a.job_item__header-link";
    private static final String JOB_COMPANY    = "span.small.text-gray-800";
    private static final String JOB_SALARY     = "span.text-body-tertiary.fw-medium";

    private static final String PAGINATION_NEXT = "ul.pagination li.page-item:last-child a.page-link";

    private static final int MAX_PAGES = 3;

    @Override
    public String getSiteKey() { return "djinni"; }

    @Override
    public List<Vacancy> parse(VacancyFilter filter) {
        List<Vacancy> results = new ArrayList<>();
        String url = buildUrl(filter);
        int page = 1;

        while (url != null && page <= MAX_PAGES) {
            try {
                log.debug("Fetching Djinni page {}: {}", Optional.of(page), url);
                Document doc = fetchPage(url);
                List<Vacancy> pageVacancies = parsePage(doc);

                if (pageVacancies.isEmpty()) break;

                results.addAll(pageVacancies);
                url = getNextPageUrl(doc);
                page++;

                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Djinni parse error on page {}: {}", Optional.of(page), e.getMessage());
                break;
            }
        }

        log.info("Djinni: found {} vacancies for filter [{}]", Optional.of(results.size()), filter.getName());
        return results;
    }

    // --- Private helpers ---

    private String buildUrl(VacancyFilter filter) {
        StringBuilder sb = new StringBuilder(JOBS_URL).append("?");

        if (filter.getKeywords() != null && !filter.getKeywords().isBlank()) {
            sb.append("primary_keyword=")
                    .append(encode(filter.getKeywords()))
                    .append("&");
        }

        if (filter.getLocation() != null && !filter.getLocation().isBlank()) {
            sb.append("location=")
                    .append(encode(filter.getLocation()))
                    .append("&");
        }

        return sb.toString();
    }

    private Document fetchPage(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Referer", "https://djinni.co/")
                .timeout(15_000)
                .get();
    }

    private List<Vacancy> parsePage(Document doc) {
        List<Vacancy> vacancies = new ArrayList<>();
        Elements cards = doc.select(JOB_CARD);
        log.debug("Djinni: found {} cards on page", Optional.of(cards.size()));

        for (Element card : cards) {
            try {
                vacancies.add(parseCard(card));
            } catch (Exception e) {
                log.warn("Djinni: failed to parse card: {}", e.getMessage());
            }
        }
        return vacancies;
    }

    private Vacancy parseCard(Element card) {
        Element titleEl   = card.selectFirst(JOB_TITLE);
        Element linkEl    = card.selectFirst(JOB_LINK);
        Element companyEl = card.selectFirst(JOB_COMPANY);
        Element salaryEl  = card.selectFirst(JOB_SALARY);

        String title   = titleEl   != null ? titleEl.text().trim()   : "N/A";
        String company = companyEl != null ? companyEl.text().trim() : "N/A";
        String salary  = salaryEl  != null ? salaryEl.text().trim()  : null;

        String href = linkEl != null ? linkEl.attr("href") : "";
        String url  = href.startsWith("http") ? href : BASE_URL + href;

        return Vacancy.builder()
                .title(title)
                .company(company)
                .url(url)
                .salary(salary)
                .siteKey(getSiteKey())
                .foundAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .contentHash("")
                .build();
    }

    private String getNextPageUrl(Document doc) {
        Element next = doc.selectFirst(PAGINATION_NEXT);
        if (next == null) return null;
        String href = next.attr("href");
        // Skip if it's disabled (#)
        if (href.equals("#") || href.isBlank()) return null;
        return href.startsWith("http") ? href : BASE_URL + "/jobs/" + href;
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value.trim(), java.nio.charset.StandardCharsets.UTF_8);
    }
}
