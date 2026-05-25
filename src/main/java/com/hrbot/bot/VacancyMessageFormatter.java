package com.hrbot.bot;

import com.hrbot.model.Vacancy;
import org.springframework.stereotype.Component;

/**
 * Formats a {@link Vacancy} into an HTML string suitable for sending via Telegram
 * (parseMode=HTML). Keeps formatting logic out of service and command classes.
 */
@Component
public class VacancyMessageFormatter {

    public String format(Vacancy vacancy, boolean isUpdate) {
        String header = isUpdate
                ? "🔄 <b>Updated vacancy</b>"
                : "🆕 <b>New vacancy</b>";

        StringBuilder sb = new StringBuilder();
        sb.append(header).append("\n\n");
        sb.append("💼 <b>%s</b>\n".formatted(TelegramEscape.html(vacancy.getTitle())));
        sb.append("🏢 %s\n".formatted(TelegramEscape.html(vacancy.getCompany())));

        if (vacancy.getLocation() != null && !vacancy.getLocation().isBlank()) {
            sb.append("📍 %s\n".formatted(TelegramEscape.html(vacancy.getLocation())));
        }

        if (vacancy.getSalary() != null && !vacancy.getSalary().isBlank()) {
            sb.append("💰 %s\n".formatted(TelegramEscape.html(vacancy.getSalary())));
        }

        if (vacancy.getDescription() != null && !vacancy.getDescription().isBlank()) {
            String desc = vacancy.getDescription().length() > 300
                    ? vacancy.getDescription().substring(0, 300) + "…"
                    : vacancy.getDescription();
            sb.append("\n📝 %s\n".formatted(TelegramEscape.html(desc)));
        }

        sb.append("\n🌐 <a href=\"%s\">Open vacancy</a>".formatted(vacancy.getUrl()));
        sb.append(" · <i>%s</i>".formatted(vacancy.getSiteKey()));

        return sb.toString();
    }
}
