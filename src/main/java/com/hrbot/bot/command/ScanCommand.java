package com.hrbot.bot.command;

import com.hrbot.bot.MessageSender;
import com.hrbot.model.ScanResult;
import com.hrbot.model.VacancyFilter;
import com.hrbot.service.FilterService;
import com.hrbot.service.NotificationService;
import com.hrbot.service.VacancyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;


import java.util.List;

/**
 * Usage:
 * /scan           — scan all active filters for this chat
 * /scan <id>      — scan specific filter by ID
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScanCommand implements BotCommand {

    private final MessageSender sender;
    private final FilterService filterService;
    private final VacancyService vacancyService;
    private final NotificationService notificationService;

    @Override
    public String getCommand() {
        return "/scan";
    }

    @Override
    public String getDescription() {
        return "Trigger vacancy scan. Usage: /scan [filter_id]";
    }

    @Override
    public void handle(Message message, String[] args) {
        long chatId = message.getChatId();

        List<VacancyFilter> filters;

        if (args.length > 0) {
            // Scan specific filter by ID
            try {
                long filterId = Long.parseLong(args[0].trim());
                VacancyFilter filter = filterService.findById(filterId);

                if (filter == null) {
                    sender.sendText(chatId, "❌ Filter <code>%d</code> not found.".formatted(filterId));
                    return;
                }

                if (!filter.isActive()) {
                    sender.sendText(chatId, "⚠️ Filter <code>%d</code> is inactive.".formatted(filterId));
                    return;
                }

                filters = List.of(filter);
            } catch (NumberFormatException e) {
                sender.sendText(chatId, "❌ Invalid filter ID: <code>%s</code>. Use /filters to see IDs."
                        .formatted(args[0]));
                return;
            }
        } else {
            // Scan all active filters for this chat
            filters = filterService.getActiveFiltersForChat(chatId);
        }

        if (filters.isEmpty()) {
            sender.sendText(chatId, "📭 No active filters. Use /addfilter to create one.");
            return;
        }

        sender.sendText(chatId, "🔍 Scanning %d filter(s)...".formatted(filters.size()));

        int totalNew = 0;
        int totalUpdated = 0;

        for (VacancyFilter filter : filters) {
            try {
                sender.sendText(chatId, "⏳ <b>%s</b> [ID: <code>%d</code>] — sites: <code>%s</code>"
                        .formatted(filter.getName(), filter.getId(),
                                String.join(", ", filter.getSites())));

                ScanResult result = vacancyService.scanForFilter(filter);
                notificationService.notify(filter, result);

                totalNew += result.getNewVacancies().size();
                totalUpdated += result.getUpdatedVacancies().size();

                sender.sendText(chatId, "✔️ <b>%s</b>: %d new, %d updated"
                        .formatted(filter.getName(),
                                result.getNewVacancies().size(),
                                result.getUpdatedVacancies().size()));

            } catch (Exception e) {
                log.error("Manual scan failed for filter [{}]: {}", filter.getName(), e.getMessage());
                sender.sendText(chatId, "❌ Error scanning <code>%s</code>: %s"
                        .formatted(filter.getName(), e.getMessage()));
            }
        }

        sender.sendText(chatId, "✅ Done: <b>%d new</b>, <b>%d updated</b>"
                .formatted(totalNew, totalUpdated));
    }
}
