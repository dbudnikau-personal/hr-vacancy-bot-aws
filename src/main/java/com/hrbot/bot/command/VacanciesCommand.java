package com.hrbot.bot.command;

import com.hrbot.bot.MessageSender;
import com.hrbot.model.Vacancy;
import com.hrbot.repository.VacancyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;


import java.util.List;

/**
 * Usage:
 *   /vacancies              — last 10 vacancies across all filters
 *   /vacancies <filter_id>  — last 10 vacancies for specific filter
 *   /vacancies <keyword>    — search by keyword in title or company
 *   /vacancies page <N>     — pagination
 */
@Component
@RequiredArgsConstructor
public class VacanciesCommand implements BotCommand {

    private static final int PAGE_SIZE = 5;

    private final MessageSender sender;
    private final VacancyRepository vacancyRepository;

    @Override
    public String getCommand() { return "/vacancies"; }

    @Override
    public String getDescription() { return "Browse vacancies from DB. Usage: /vacancies [filter_id|keyword] [page N]"; }

    @Override
    public void handle(Message message, String[] args) {
        long chatId = message.getChatId();

        // Parse args
        String keyword = null;
        Long filterId = null;
        int page = 0;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("page") && i + 1 < args.length) {
                try {
                    page = Math.max(0, Integer.parseInt(args[i + 1]) - 1);
                } catch (NumberFormatException ignored) {}
                i++; // skip next
            } else {
                try {
                    filterId = Long.parseLong(args[i]);
                } catch (NumberFormatException e) {
                    keyword = args[i];
                }
            }
        }

        // Query
        List<Vacancy> vacancies;
        long total;
        String context;

        PageRequest pageRequest = PageRequest.of(page, PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "foundAt"));

        if (keyword != null) {
            String kw = keyword;
            Page<Vacancy> result = vacancyRepository.findByTitleContainingIgnoreCaseOrCompanyContainingIgnoreCase(
                    kw, kw, pageRequest);
            vacancies = result.getContent();
            total = result.getTotalElements();
            context = "🔍 keyword: <code>" + escape(kw) + "</code>";
        } else if (filterId != null) {
            Page<Vacancy> result = vacancyRepository.findBySiteKey(filterId.toString(), pageRequest);
            vacancies = result.getContent();
            total = result.getTotalElements();
            context = "🔖 filter ID: <code>" + filterId + "</code>";
        } else {
            Page<Vacancy> result = vacancyRepository.findAll(pageRequest);
            vacancies = result.getContent();
            total = result.getTotalElements();
            context = "📋 all vacancies";
        }

        if (vacancies.isEmpty()) {
            sender.sendText(chatId, "📭 No vacancies found.");
            return;
        }

        // Build response
        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
        StringBuilder sb = new StringBuilder();
        sb.append("💼 <b>Vacancies</b> (%s) — page %d/%d\n\n"
                .formatted(context, page + 1, totalPages));

        for (Vacancy v : vacancies) {
            sb.append("▪️ <a href=\"%s\">%s</a>\n".formatted(v.getUrl(), escape(v.getTitle())));
            sb.append("   🏢 %s".formatted(escape(v.getCompany())));
            if (v.getSalary() != null) sb.append(" · 💰 %s".formatted(escape(v.getSalary())));
            if (v.getLocation() != null) sb.append(" · 📍 %s".formatted(escape(v.getLocation())));
            sb.append(" · <i>%s</i>".formatted(v.getSiteKey()));
            sb.append("\n\n");
        }

        // Pagination hint
        if (totalPages > 1) {
            sb.append("📄 Page %d of %d · Total: %d\n".formatted(page + 1, totalPages, total));
            if (page + 1 < totalPages) {
                sb.append("➡️ Next: <code>/vacancies");
                if (keyword != null) sb.append(" ").append(keyword);
                if (filterId != null) sb.append(" ").append(filterId);
                sb.append(" page %d</code>".formatted(page + 2));
            }
        }

        sender.sendText(chatId, sb.toString());
    }

    private String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
