package com.hrbot.bot.command;

import com.hrbot.bot.MessageSender;
import com.hrbot.model.VacancyFilter;
import com.hrbot.service.FilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;


/**
 * Usage: /removefilter <id>
 */
@Component
@RequiredArgsConstructor
public class RemoveFilterCommand implements BotCommand {

    private final MessageSender sender;
    private final FilterService filterService;

    private static String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    public String getCommand() { return "/removefilter"; }

    @Override
    public String getDescription() { return "Deactivate a filter by ID. Usage: /removefilter <id>"; }

    @Override
    public void handle(Message message, String[] args) {
        long chatId = message.getChatId();

        if (args.length == 0) {
            sender.sendText(chatId, "❌ Usage: <code>/removefilter &lt;id&gt;</code>");
            return;
        }

        try {
            long filterId = Long.parseLong(args[0].trim());
            VacancyFilter filter = filterService.findById(filterId);
            if (filter == null || !filter.getChatId().equals(chatId)) {
                sender.sendText(chatId, "❌ Filter not found.");
                return;
            }
            filterService.deactivate(filterId);
            sender.sendText(chatId, "✅ Filter <code>%d</code> deactivated.".formatted(filterId));
        } catch (NumberFormatException e) {
            sender.sendText(chatId, "❌ Invalid ID: <code>%s</code>. Use /filters to see IDs.".formatted(escape(args[0])));
        }
    }
}
