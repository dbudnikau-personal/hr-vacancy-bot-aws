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
public class IndeedParser implements SiteParser {

    private static final String RSS_URL   = "https://ca.indeed.com/rss";
    private static final int    LIMIT     = 10;
    private static final int    MAX_PAGES = 5;

    @Override
    public String getSiteKey() { return "indeed"; }

    @Override
    public List<Vacancy> parse(VacancyFilter filter) {
        List<Vacancy> results = new ArrayList<>();

        for (int page = 0; page < MAX_PAGES; page++) {
            String url = buildUrl(filter, page * LIMIT);
            try {
                log.debug("Fetching Indeed RSS page {}: {}", page, url);
                Document doc = fetchFeed(url);
                Elements items = doc.select("item");

                if (items.isEmpty()) {
                    log.warn("Indeed: 0 items on page {}, doc title='{}', body snippet='{}'",
                            page, doc.title(), doc.text().substring(0, Math.min(200, doc.text().length())));
                    break;
                }

                for (Element item : items) {
                    try {
                        results.add(parseItem(item));
                    } catch (Exception e) {
                        log.warn("Indeed: failed to parse item: {}", e.getMessage());
                    }
                }

                if (items.size() < LIMIT) break; // last page

                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Indeed parse error on page {}: {}", page, e.getMessage());
                break;
            }
        }

        log.info("Indeed: found {} vacancies for filter [{}]", results.size(), filter.getName());
        return results;
    }

    private Document fetchFeed(String url) throws IOException {
        return Jsoup.connect(url)
                .parser(Parser.xmlParser())
                .userAgent("Mozilla/5.0 (compatible; RSS reader)")
                .header("Accept", "application/rss+xml, application/xml, text/xml")
                .timeout(10_000)
                .get();
    }

    private Vacancy parseItem(Element item) {
        String title   = text(item, "title");
        String guid    = text(item, "guid");
        String company = text(item, "source");

        // Location and salary are embedded in the <description> HTML table
        String rawDesc = text(item, "description");
        Document desc  = Jsoup.parse(rawDesc);
        String location = extractField(desc, "Location:");
        String salary   = extractField(desc, "Salary:");
        String snippet  = extractField(desc, "Description:");

        return Vacancy.builder()
                .title(title)
                .company(company.isBlank() ? "N/A" : company)
                .url(guid)
                .location(location.isBlank() ? null : location)
                .salary(salary.isBlank() ? null : salary)
                .description(snippet.isBlank() ? null : snippet)
                .siteKey(getSiteKey())
                .foundAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .contentHash("")
                .build();
    }

    /**
     * Extracts the value of a labeled row from the Indeed description HTML table.
     * <pre>{@code
     * <tr>
     *   <td><b>Location:</b></td>
     *   <td>Toronto, ON</td>
     * </tr>
     * }</pre>
     */
    private String extractField(Document html, String label) {
        for (Element b : html.select("b")) {
            if (b.text().trim().equalsIgnoreCase(label)) {
                Element labelTd = b.parent();
                if (labelTd != null) {
                    Element valueTd = labelTd.nextElementSibling();
                    if (valueTd != null) return valueTd.text().trim();
                }
            }
        }
        return "";
    }

    private String text(Element parent, String tag) {
        Element el = parent.selectFirst(tag);
        return el != null ? el.text().trim() : "";
    }

    private String buildUrl(VacancyFilter filter, int start) {
        StringBuilder sb = new StringBuilder(RSS_URL).append("?");

        if (filter.getKeywords() != null && !filter.getKeywords().isBlank()) {
            sb.append("q=").append(encode(filter.getKeywords())).append("&");
        }

        String location = (filter.getLocation() != null && !filter.getLocation().isBlank())
                ? filter.getLocation()
                : "Canada";
        sb.append("l=").append(encode(location)).append("&");

        if (filter.getSalaryMin() != null && !filter.getSalaryMin().isBlank()) {
            sb.append("salaryMin=").append(encode(filter.getSalaryMin())).append("&");
        }

        sb.append("sort=date&");
        sb.append("limit=").append(LIMIT).append("&");
        sb.append("start=").append(start);

        return sb.toString();
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value.trim(), java.nio.charset.StandardCharsets.UTF_8);
    }
}
