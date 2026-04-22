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
public class WellfoundParser implements SiteParser {

    private static final String BASE_URL   = "https://wellfound.com";
    private static final String SEARCH_URL = BASE_URL + "/role/r/";

    // Vacancy group container — each block contains jobs from one company
    private static final String JOB_GROUP      = "div.mb-4.w-full.px-4";
    // Job title link inside group
    private static final String JOB_LINK       = "a[href*='/jobs/']";
    // Pagination next page
    private static final String PAGINATION_NEXT = "a[aria-label='Next page']";

    private static final int MAX_PAGES = 5;

    @Override
    public String getSiteKey() { return "wellfound"; }

    @Override
    public List<Vacancy> parse(VacancyFilter filter) {
        List<Vacancy> results = new ArrayList<>();
        String url = buildUrl(filter);
        int page = 1;

        while (url != null && page <= MAX_PAGES) {
            try {
                log.debug("Fetching Wellfound page {}: {}", page, url);
                Document doc = fetchPage(url);
                List<Vacancy> pageVacancies = parsePage(doc);

                if (pageVacancies.isEmpty()) break;

                results.addAll(pageVacancies);
                url = getNextPageUrl(doc);
                page++;

                Thread.sleep(2000); // Wellfound is sensitive to fast requests
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

    // --- Private helpers ---

    private String buildUrl(VacancyFilter filter) {
        // Wellfound uses role slug in URL: /role/r/java-developer
        // Map keywords to slug: "Java Developer" -> "java-developer"
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
                .cookie("datadome", "hQ2pV0WKnST5YlTrfuYasyVOC~kmZPaeqT8O9ZojlzHNpNUyrzRDvVSJhcmGSvLx4HXTvSSRo3wj3X4fK94akQiJrIn3oUBgv4dy3hr_wvpb5wqDDcg8GbpgX2~ZKwPq")
                .cookie("cf_clearance", "jTIN0xiH2t7X2TTzbwLhG4lksykrjhN_68CRdir0.6c-1776787042-1.2.1.1-_AZVlUHNvFRvFhKF6oT_qqaEH.kW24g9EQdOeQjLlN_EzRSOuAv0FbsjoQiIUWMqKDbWf4rtavfmv4TuQJRM6FjpC2Jb14pIL48s_C8CQwTOgoQE7UBZo1yBOjmyqnGmNFnKHH2pNJ.MuNdUNk4PGgcnKzjO7cOaQePcV4IN_Jaztcq94t3e7e3sMHilc.t4Ko9tPP2T8LUkVdsFSHdT6ldlTYJeuH14Ib6RneaWa5eeOYK8FOOBxjyvoyU8y70Mkpa_jpdQLjMmB7MxJHD_xD0SDJBh2XDFvWCzYcWbabCUbVqwqyyl.1vHoHogqT.WWxmWXHpYUMjNhoQy6ofjMA")
                .cookie("_wellfound", "aed8f90b70f262fc4db13a6283910118.o")
                .timeout(15_000)
                .get();
    }

    private List<Vacancy> parsePage(Document doc) {
        List<Vacancy> vacancies = new ArrayList<>();
        Elements groups = doc.select(JOB_GROUP);
        log.debug("Wellfound: found {} job groups on page", Optional.of(groups.size()));

        for (Element group : groups) {
            // Company name is in the previous sibling block
            String company = extractCompany(group);

            Elements jobLinks = group.select(JOB_LINK);
            for (Element link : jobLinks) {
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
        // Company block is the previous sibling element
        Element prev = group.previousElementSibling();
        if (prev == null) return "N/A";

        // First line of text is the company name
        String text = prev.text();
        if (text.isBlank()) return "N/A";

        // Company name is before "Actively Hiring" or other badges
        return text.split("\n")[0]
                .split("Actively")[0]
                .split("Top")[0]
                .trim();
    }

    private Vacancy parseJobLink(Element link, String company) {
        String title = link.text().trim();
        String href  = link.attr("href");
        String url   = href.startsWith("http") ? href : BASE_URL + href;

        // Salary is in nearby span — look in parent row
        String salary = extractSalary(link);

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

    private String extractSalary(Element link) {
        // Salary like "$96k – $169k" is in a sibling span within the same row div
        Element row = link.parent();
        if (row == null) return null;

        // Walk up to find salary text
        Element container = row.parent();
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
