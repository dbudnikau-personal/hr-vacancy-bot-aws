package com.hrbot.service;

import com.hrbot.bot.MessageSender;
import com.hrbot.bot.VacancyMessageFormatter;
import com.hrbot.model.ScanResult;
import com.hrbot.model.Vacancy;
import com.hrbot.model.VacancyFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int MAX_NOTIFY = 10;

    private final MessageSender sender;
    private final VacancyMessageFormatter formatter;

    public void notify(VacancyFilter filter, ScanResult result) {
        List<Vacancy> newVacancies     = result.getNewVacancies();
        List<Vacancy> updatedVacancies = result.getUpdatedVacancies();

        if (newVacancies.isEmpty() && updatedVacancies.isEmpty()) {
            log.debug("No changes for filter [{}], skipping notification", filter.getName());
            return;
        }

        if (!newVacancies.isEmpty()) {
            sendBatch(filter.getChatId(), newVacancies, false);
        }

        if (!updatedVacancies.isEmpty()) {
            sendBatch(filter.getChatId(), updatedVacancies, true);
        }
    }

    private void sendBatch(Long chatId, List<Vacancy> vacancies, boolean isUpdate) {
        List<Vacancy> toSend = vacancies.size() > MAX_NOTIFY
                ? vacancies.subList(0, MAX_NOTIFY)
                : vacancies;

        for (Vacancy vacancy : toSend) {
            try {
                sender.sendText(chatId, formatter.format(vacancy, isUpdate));
                Thread.sleep(300); // Telegram rate limit: 30 msg/sec
            } catch (Exception e) {
                log.error("Failed to notify: {}", e.getMessage());
            }
        }

        if (vacancies.size() > MAX_NOTIFY) {
            sender.sendText(chatId, "📊 ... and %d more. Refine your filter."
                    .formatted(vacancies.size() - MAX_NOTIFY));
        }
    }
}
