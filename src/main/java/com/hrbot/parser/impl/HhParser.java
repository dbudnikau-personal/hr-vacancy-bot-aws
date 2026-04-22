package com.hrbot.parser.impl;

import com.hrbot.model.Vacancy;
import com.hrbot.model.VacancyFilter;
import com.hrbot.parser.SiteParser;
import lombok.RequiredArgsConstructor;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class HhParser implements SiteParser {

    private static final String BASE_URL   = "https://hh.ru";
    private static final String SEARCH_URL = BASE_URL + "/search/vacancy";

    private static final String VACANCY_CARD    = "div.vacancy-card--n77Dj8TY8VIUF0yM";
    private static final String TITLE           = "span[data-qa=serp-item__title-text]";
    private static final String TITLE_LINK      = "a[data-qa=serp-item__title]";
    private static final String COMPANY         = "span[data-qa=vacancy-serp__vacancy-employer-text]";
    private static final String LOCATION        = "span[data-qa=vacancy-serp__vacancy-address]";
    private static final String SALARY          = "span[data-qa=vacancy-serp__vacancy-compensation]";
    private static final String PAGINATION_NEXT = "a[data-qa=pager-next]";

    private static final int MAX_PAGES = 5;

    private final HhAreaResolver areaResolver;

    @Override
    public String getSiteKey() { return "hh"; }

    @Override
    public List<Vacancy> parse(VacancyFilter filter) {
        List<Vacancy> results = new ArrayList<>();
        String url = buildUrl(filter, 0);
        int page = 0;

        while (url != null && page < MAX_PAGES) {
            try {
                log.debug("Fetching HH page {}: {}", page, url);
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
                log.error("HH parse error on page {}: {}", page, e.getMessage());
                break;
            }
        }

        log.info("HH: found {} vacancies for filter [{}]", results.size(), filter.getName());
        return results;
    }

    // --- Private helpers ---

    private String buildUrl(VacancyFilter filter, int page) {
        StringBuilder sb = new StringBuilder(SEARCH_URL).append("?");

        if (filter.getKeywords() != null && !filter.getKeywords().isBlank()) {
            sb.append("text=").append(encode(filter.getKeywords())).append("&");
        }

        // Resolve location name to HH area ID
        if (filter.getLocation() != null && !filter.getLocation().isBlank()) {
            String areaId = areaResolver.resolve(filter.getLocation());
            if (areaId != null) {
                sb.append("area=").append(areaId).append("&");
            }
        }

        if (filter.getSalaryMin() != null && !filter.getSalaryMin().isBlank()) {
            sb.append("salary=").append(filter.getSalaryMin()).append("&");
            sb.append("only_with_salary=true&");
        }

        sb.append("experience=doesNotMatter&");
        sb.append("order_by=relevance&");
        sb.append("search_period=0&");
        sb.append("items_on_page=50&");
        sb.append("L_save_area=true&");
        sb.append("ored_clusters=true&");
        sb.append("hhtmFrom=vacancy_search_list&");

        if (page > 0) {
            sb.append("page=").append(page);
        }

        return sb.toString();
    }

    private Document fetchPage(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Referer", "https://hh.ru/")
                .timeout(15_000)
                .get();
    }

    private List<Vacancy> parsePage(Document doc) {
        List<Vacancy> vacancies = new ArrayList<>();
        Elements cards = doc.select(VACANCY_CARD);

        if (cards.isEmpty()) {
            cards = doc.select("[data-qa=vacancy-serp__vacancy]");
        }

        log.debug("HH: found {} cards on page", cards.size());

        for (Element card : cards) {
            try {
                vacancies.add(parseCard(card));
            } catch (Exception e) {
                log.warn("HH: failed to parse card: {}", e.getMessage());
            }
        }
        return vacancies;
    }

    private Vacancy parseCard(Element card) {
        Element titleEl    = card.selectFirst(TITLE);
        Element linkEl     = card.selectFirst(TITLE_LINK);
        Element companyEl  = card.selectFirst(COMPANY);
        Element locationEl = card.selectFirst(LOCATION);
        Element salaryEl   = card.selectFirst(SALARY);

        String title    = titleEl    != null ? titleEl.text().trim()    : "N/A";
        String company  = companyEl  != null ? companyEl.text().trim()  : "N/A";
        String location = locationEl != null ? locationEl.text().trim() : null;
        String salary   = salaryEl   != null ? salaryEl.text().trim()   : null;

        String href = linkEl != null ? linkEl.attr("href") : "";
        String url  = href.contains("?") ? href.substring(0, href.indexOf("?")) : href;
        if (!url.startsWith("http")) url = BASE_URL + url;

        return Vacancy.builder()
                .title(title)
                .company(company)
                .url(url)
                .salary(salary)
                .location(location)
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
        return href.startsWith("http") ? href : BASE_URL + href;
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value.trim(), java.nio.charset.StandardCharsets.UTF_8);
    }
}
