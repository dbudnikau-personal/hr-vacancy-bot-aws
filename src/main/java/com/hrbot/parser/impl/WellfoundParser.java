package com.hrbot.parser.impl;

import com.hrbot.model.Vacancy;
import com.hrbot.model.VacancyFilter;
import com.hrbot.parser.SiteParser;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class WellfoundParser implements SiteParser {

    private static final String BASE_URL    = "https://wellfound.com";
    private static final String SEARCH_URL  = BASE_URL + "/role/r/";

    private static final String JOB_GROUP       = "div.mb-4.w-full.px-4";
    private static final String JOB_LINK        = "a[href*='/jobs/']";
    private static final String PAGINATION_NEXT = "a[aria-label='Next page']";

    private static final String SSM_DATADOME    = "/hrbot/wellfound/datadome";
    private static final String SSM_CF_CLEARANCE = "/hrbot/wellfound/cf-clearance";

    private static final int MAX_PAGES = 2;

    @Value("${aws.region:eu-central-1}")
    private String awsRegion;

    private SsmClient ssmClient;

    // In-memory cache — refreshed once per Lambda warm instance
    private String cachedDatadome;
    private String cachedCfClearance;

    @PostConstruct
    void init() {
        ssmClient = SsmClient.builder()
                .region(Region.of(awsRegion))
                .httpClient(UrlConnectionHttpClient.create())
                .build();
    }

    @Override
    public String getSiteKey() { return "wellfound"; }

    @Override
    public List<Vacancy> parse(VacancyFilter filter) {
        if (!loadCookies()) {
            return List.of();
        }

        List<Vacancy> results = new ArrayList<>();
        String url = buildUrl(filter);
        int page = 1;

        while (url != null && page <= MAX_PAGES) {
            try {
                log.debug("Fetching Wellfound page {}: {}", page, url);
                Document doc = fetchPage(url);

                if (isBlocked(doc)) {
                    log.warn("Wellfound: bot-detection challenge on page {} — cookies may be expired", page);
                    break;
                }

                List<Vacancy> pageVacancies = parsePage(doc);
                if (pageVacancies.isEmpty()) break;

                results.addAll(pageVacancies);
                url = getNextPageUrl(doc);
                page++;

                if (url != null) Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Wellfound parse error on page {}: {}", page, e.getMessage());
                break;
            }
        }

        log.info("Wellfound: found {} vacancies for filter [{}]", results.size(), filter.getName());
        return results;
    }

    private boolean loadCookies() {
        if (cachedDatadome != null && cachedCfClearance != null) {
            return true;
        }
        try {
            cachedDatadome    = getParam(SSM_DATADOME);
            cachedCfClearance = getParam(SSM_CF_CLEARANCE);
            log.info("Wellfound cookies loaded from SSM");
            return true;
        } catch (ParameterNotFoundException e) {
            log.warn("Wellfound SSM cookies not found — run cookie-refresher Lambda first");
            return false;
        } catch (Exception e) {
            log.error("Failed to load Wellfound cookies from SSM: {}", e.getMessage());
            return false;
        }
    }

    private String getParam(String name) {
        return ssmClient.getParameter(GetParameterRequest.builder()
                .name(name)
                .withDecryption(true)
                .build())
                .parameter()
                .value();
    }

    private boolean isBlocked(Document doc) {
        String title = doc.title().toLowerCase();
        return title.contains("just a moment")
                || title.contains("attention required")
                || title.contains("access denied")
                || (doc.selectFirst(JOB_GROUP) == null
                    && !doc.title().toLowerCase().contains("wellfound"));
    }

    private String buildUrl(VacancyFilter filter) {
        String slug = filter.getKeywords() != null
                ? filter.getKeywords().trim().toLowerCase().replace(" ", "-")
                : "software-engineer";

        StringBuilder sb = new StringBuilder(SEARCH_URL).append(slug);

        if (filter.getLocation() != null && !filter.getLocation().isBlank()) {
            sb.append("?location=").append(encode(filter.getLocation()));
        }

        return sb.toString();
    }

    private Document fetchPage(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.6 Safari/605.1.15")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-GB,en;q=0.9")
                .header("Referer", "https://wellfound.com/role/r/software-engineer")
                .header("Sec-Fetch-Site", "same-origin")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Dest", "document")
                .cookie("datadome", cachedDatadome)
                .cookie("cf_clearance", cachedCfClearance)
                .cookie("_wellfound", "aed8f90b70f262fc4db13a6283910118.o")
                .timeout(8_000)
                .get();
    }

    private List<Vacancy> parsePage(Document doc) {
        List<Vacancy> vacancies = new ArrayList<>();
        Elements groups = doc.select(JOB_GROUP);
        log.debug("Wellfound: found {} job groups on page", Optional.of(groups.size()));

        for (Element group : groups) {
            String company = extractCompany(group);
            for (Element link : group.select(JOB_LINK)) {
                try {
                    vacancies.add(parseJobLink(link, company));
                } catch (Exception e) {
                    log.warn("Wellfound: failed to parse job: {}", e.getMessage());
                }
            }
        }

        return vacancies;
    }

    private String extractCompany(Element group) {
        Element prev = group.previousElementSibling();
        if (prev == null) return "N/A";
        String text = prev.text();
        if (text.isBlank()) return "N/A";
        return text.split("\n")[0].split("Actively")[0].split("Top")[0].trim();
    }

    private Vacancy parseJobLink(Element link, String company) {
        String title = link.text().trim();
        String href  = link.attr("href");
        String url   = href.startsWith("http") ? href : BASE_URL + href;

        return Vacancy.builder()
                .title(title)
                .company(company)
                .url(url)
                .salary(extractSalary(link))
                .siteKey(getSiteKey())
                .foundAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .contentHash("")
                .build();
    }

    private String extractSalary(Element link) {
        Element container = link.parent() != null ? link.parent().parent() : null;
        if (container == null) return null;
        for (Element el : container.getAllElements()) {
            String text = el.ownText().trim();
            if (text.matches(".*\\$\\d+.*")) return text;
        }
        return null;
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
