package com.hrbot.scheduler;

import com.hrbot.model.ScanResult;
import com.hrbot.model.VacancyFilter;
import com.hrbot.service.FilterService;
import com.hrbot.service.NotificationService;
import com.hrbot.service.VacancyService;
import com.hrbot.bot.MessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class VacancyScanScheduler {

    private final FilterService filterService;
    private final VacancyService vacancyService;
    private final NotificationService notificationService;
    private final MessageSender messageSender;

    @Scheduled(cron = "${bot.scan.cron:0 0 */2 * * *}")
    public void scan() {
        List<VacancyFilter> filters = filterService.getAllActiveFilters();
        log.info("Starting vacancy scan for {} active filters", filters.size());

        Map<Long, int[]> chatTotals = new LinkedHashMap<>();
        Map<Long, List<String>> chatFilterLines = new LinkedHashMap<>();

        for (VacancyFilter filter : filters) {
            long chatId = filter.getChatId();
            int filterFound = 0, filterNew = 0, filterUpdated = 0;
            List<String> siteParts = new ArrayList<>();

            for (String siteKey : filter.getSites()) {
                try {
                    ScanResult result = vacancyService.scanSingleSite(filter, siteKey);
                    int f = result.getTotalFound();
                    int n = result.getNewVacancies().size();
                    int u = result.getUpdatedVacancies().size();
                    filterFound += f;
                    filterNew += n;
                    filterUpdated += u;
                    notificationService.notify(filter, result);
                    siteParts.add("<code>%s</code> %d→%d🆕%d🔄".formatted(siteKey, f, n, u));
                    log.info("Filter [{}] site [{}]: {} found, {} new, {} updated", filter.getName(), siteKey, f, n, u);
                } catch (Exception e) {
                    log.error("Scan failed for filter [{}] site [{}]: {}", filter.getName(), siteKey, e.getMessage());
                    siteParts.add("<code>%s</code> ❌".formatted(siteKey));
                }
            }

            chatFilterLines.computeIfAbsent(chatId, k -> new ArrayList<>())
                    .add("• <b>%s</b>: %s".formatted(filter.getName(), String.join(" · ", siteParts)));

            int[] totals = chatTotals.computeIfAbsent(chatId, k -> new int[3]);
            totals[0] += filterFound;
            totals[1] += filterNew;
            totals[2] += filterUpdated;
        }

        chatTotals.forEach((chatId, totals) -> {
            String header = "✅ Scan complete: <b>%d found</b>, %d new, %d updated"
                    .formatted(totals[0], totals[1], totals[2]);
            String details = String.join("\n", chatFilterLines.getOrDefault(chatId, List.of()));
            messageSender.sendText(chatId, header + "\n\n" + details);
        });

        log.info("Scan complete");
    }

    public void scanForChat(long chatId, List<Long> filterIds) {
        List<VacancyFilter> filters = filterIds.isEmpty()
                ? filterService.getActiveFiltersForChat(chatId)
                : filterIds.stream()
                        .map(filterService::findById)
                        .filter(f -> f != null && f.isActive())
                        .collect(Collectors.toList());

        log.info("Manual scan for chatId={}: {} filter(s)", chatId, filters.size());

        int totalFound = 0;
        int totalNew = 0;
        int totalUpdated = 0;

        for (VacancyFilter filter : filters) {
            messageSender.sendText(chatId, "🔎 <b>%s</b> [sites: <code>%s</code>]"
                    .formatted(filter.getName(), String.join(", ", filter.getSites())));

            for (String siteKey : filter.getSites()) {
                messageSender.sendText(chatId, "⏳ Scanning <code>%s</code>...".formatted(siteKey));
                try {
                    ScanResult result = vacancyService.scanSingleSite(filter, siteKey);
                    int foundCount = result.getTotalFound();
                    int newCount = result.getNewVacancies().size();
                    int updatedCount = result.getUpdatedVacancies().size();
                    totalFound += foundCount;
                    totalNew += newCount;
                    totalUpdated += updatedCount;

                    notificationService.notify(filter, result);

                    messageSender.sendText(chatId, "✔️ <code>%s</code>: %d found → %d new, %d updated"
                            .formatted(siteKey, foundCount, newCount, updatedCount));
                } catch (Exception e) {
                    log.error("Scan failed for filter [{}] site [{}]: {}", filter.getName(), siteKey, e.getMessage());
                    messageSender.sendText(chatId, "❌ <code>%s</code>: error — %s".formatted(siteKey, e.getMessage()));
                }
            }
        }

        messageSender.sendText(chatId, "✅ Done: <b>%d found</b>, %d new, %d updated"
                .formatted(totalFound, totalNew, totalUpdated));
    }
}
