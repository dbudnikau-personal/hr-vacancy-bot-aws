package com.hrbot.bot.command;

import com.hrbot.bot.MessageSender;
import com.hrbot.model.VacancyFilter;
import com.hrbot.service.FilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;


import java.util.List;

/**
 * Usage: /filters
 * Lists all active filters for current chat.
 */
@Component
@RequiredArgsConstructor
public class ListFiltersCommand implements BotCommand {

    private final MessageSender sender;
    private final FilterService filterService;

    @Override
    public String getCommand() { return "/filters"; }

    @Override
    public String getDescription() { return "List all active vacancy filters"; }

    @Override
    public void handle(Message message, String[] args) {
        long chatId = message.getChatId();
        List<VacancyFilter> filters = filterService.getActiveFiltersForChat(chatId);

        if (filters.isEmpty()) {
            sender.sendText(chatId, "📭 No active filters. Use /addfilter to create one.");
            return;
        }

        StringBuilder sb = new StringBuilder("📋 <b>Active filters:</b>\n\n");
        for (VacancyFilter f : filters) {
            sb.append("🔹 <b>%s</b> [ID: <code>%d</code>]\n".formatted(f.getName(), f.getId()));
            sb.append("   🔍 <code>%s</code>\n".formatted(f.getKeywords()));
            if (f.getLocation() != null)
                sb.append("   📍 %s\n".formatted(f.getLocation()));
            if (f.getSalaryMin() != null)
                sb.append("   💰 from %s\n".formatted(f.getSalaryMin()));
            sb.append("   🌐 %s\n".formatted(String.join(", ", f.getSites())));
            sb.append("\n");
        }

        sb.append("Use /removefilter &lt;ID&gt; to deactivate a filter.");
        sender.sendText(chatId, sb.toString());
    }
}
