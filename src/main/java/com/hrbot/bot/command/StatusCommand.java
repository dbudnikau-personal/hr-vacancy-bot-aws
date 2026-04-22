package com.hrbot.bot.command;

import com.hrbot.bot.MessageSender;
import com.hrbot.parser.ParserStatusRegistry;
import com.hrbot.parser.ParserStatusRegistry.ParserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;


import java.util.Map;

/**
 * Usage: /status
 * Shows last scan result for each parser — ok, error, ban, etc.
 */
@Component
@RequiredArgsConstructor
public class StatusCommand implements BotCommand {

    private final MessageSender sender;
    private final ParserStatusRegistry statusRegistry;

    @Override
    public String getCommand() { return "/status"; }

    @Override
    public String getDescription() { return "Show parser health status"; }

    @Override
    public void handle(Message message, String[] args) {
        long chatId = message.getChatId();
        Map<String, ParserStatus> statuses = statusRegistry.getAll();

        if (statuses.isEmpty()) {
            sender.sendText(chatId, "📭 No scan has been run yet. Use /scan to start.");
            return;
        }

        StringBuilder sb = new StringBuilder("🔧 <b>Parser Status</b>\n\n");
        statuses.values().stream()
                .sorted((a, b) -> Boolean.compare(a.isOk(), b.isOk())) // errors first
                .forEach(s -> sb.append(s.format()).append("\n\n"));

        sender.sendText(chatId, sb.toString());
    }
}
