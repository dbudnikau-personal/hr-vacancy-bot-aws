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

import java.util.List;
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

        for (VacancyFilter filter : filters) {
            try {
                ScanResult result = vacancyService.scanForFilter(filter);

                log.info("Filter [{}]: {} new, {} updated",
                        filter.getName(),
                        result.getNewVacancies().size(),
                        result.getUpdatedVacancies().size());

                notificationService.notify(filter, result);

            } catch (Exception e) {
                log.error("Scan failed for filter [{}]: {}", filter.getName(), e.getMessage());
            }
        }

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

        int totalNew = 0;
        int totalUpdated = 0;

        for (VacancyFilter filter : filters) {
            messageSender.sendText(chatId, "🔎 <b>%s</b> [sites: <code>%s</code>]"
                    .formatted(filter.getName(), String.join(", ", filter.getSites())));

            for (String siteKey : filter.getSites()) {
                messageSender.sendText(chatId, "⏳ Scanning <code>%s</code>...".formatted(siteKey));
                try {
                    ScanResult result = vacancyService.scanSingleSite(filter, siteKey);
                    int newCount = result.getNewVacancies().size();
                    int updatedCount = result.getUpdatedVacancies().size();
                    totalNew += newCount;
                    totalUpdated += updatedCount;

                    notificationService.notify(filter, result);

                    messageSender.sendText(chatId, "✔️ <code>%s</code>: %d new, %d updated"
                            .formatted(siteKey, newCount, updatedCount));
                } catch (Exception e) {
                    log.error("Scan failed for filter [{}] site [{}]: {}", filter.getName(), siteKey, e.getMessage());
                    messageSender.sendText(chatId, "❌ <code>%s</code>: error — %s".formatted(siteKey, e.getMessage()));
                }
            }
        }

        messageSender.sendText(chatId, "✅ Done: <b>%d new</b>, <b>%d updated</b>".formatted(totalNew, totalUpdated));
    }
}
