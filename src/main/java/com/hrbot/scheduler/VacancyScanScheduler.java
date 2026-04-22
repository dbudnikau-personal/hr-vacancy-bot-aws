package com.hrbot.scheduler;

import com.hrbot.model.ScanResult;
import com.hrbot.model.VacancyFilter;
import com.hrbot.service.FilterService;
import com.hrbot.service.NotificationService;
import com.hrbot.service.VacancyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VacancyScanScheduler {

    private final FilterService filterService;
    private final VacancyService vacancyService;
    private final NotificationService notificationService;

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
}
