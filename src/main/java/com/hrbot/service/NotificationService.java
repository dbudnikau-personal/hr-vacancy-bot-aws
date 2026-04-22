package com.hrbot.service;

import com.hrbot.bot.MessageSender;
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

    private static final int MAX_NOTIFY = 10; // max per scan

    private final MessageSender sender;

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

    // --- Private helpers ---
    private void sendBatch(Long chatId, List<Vacancy> vacancies, boolean isUpdate) {
        List<Vacancy> toSend = vacancies.size() > MAX_NOTIFY
                ? vacancies.subList(0, MAX_NOTIFY)
                : vacancies;

        for (Vacancy v : toSend) {
            try {
                sender.sendText(chatId, formatVacancy(v, isUpdate));
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

    private String formatVacancy(Vacancy v, boolean isUpdate) {
        String header = isUpdate
                ? "🔄 <b>Updated vacancy</b>"
                : "🆕 <b>New vacancy</b>";

        StringBuilder sb = new StringBuilder();
        sb.append(header).append("\n\n");
        sb.append("💼 <b>%s</b>\n".formatted(escape(v.getTitle())));
        sb.append("🏢 %s\n".formatted(escape(v.getCompany())));

        if (v.getLocation() != null && !v.getLocation().isBlank()) {
            sb.append("📍 %s\n".formatted(escape(v.getLocation())));
        }

        if (v.getSalary() != null && !v.getSalary().isBlank()) {
            sb.append("💰 %s\n".formatted(escape(v.getSalary())));
        }

        if (v.getDescription() != null && !v.getDescription().isBlank()) {
            String desc = v.getDescription().length() > 300
                    ? v.getDescription().substring(0, 300) + "…"
                    : v.getDescription();
            sb.append("\n📝 %s\n".formatted(escape(desc)));
        }

        sb.append("\n🌐 <a href=\"%s\">Open vacancy</a>".formatted(v.getUrl()));
        sb.append(" · <i>%s</i>".formatted(v.getSiteKey()));

        return sb.toString();
    }

    // Escape HTML special chars for Telegram parseMode=HTML
    private String escape(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
