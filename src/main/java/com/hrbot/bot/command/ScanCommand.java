package com.hrbot.bot.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrbot.bot.MessageSender;
import com.hrbot.bot.TelegramEscape;
import com.hrbot.model.VacancyFilter;
import com.hrbot.service.AsyncTaskDispatcher;
import com.hrbot.service.FilterService;
import com.hrbot.service.ScanningStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScanCommand implements BotCommand {

    private final MessageSender sender;
    private final FilterService filterService;
    private final AsyncTaskDispatcher taskDispatcher;
    private final ObjectMapper objectMapper;
    private final ScanningStateService scanningStateService;

    @Value("${scanner.function.name}")
    private String scannerFunctionName;

    @Override
    public String getCommand() { return "/scan"; }

    @Override
    public String getDescription() { return "Trigger vacancy scan. Usage: /scan [filter_id]"; }

    @Override
    public void handle(Message message, String[] args) {
        long chatId = message.getChatId();

        if (scanningStateService.isPaused()) {
            sender.sendText(chatId, "⏸ Scanning is currently disabled. Use /startscan to re-enable.");
            return;
        }

        List<VacancyFilter> filters;

        if (args.length > 0) {
            try {
                long filterId = Long.parseLong(args[0].trim());
                VacancyFilter filter = filterService.findById(filterId);

                if (filter == null || !filter.getChatId().equals(chatId)) {
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
                        .formatted(TelegramEscape.html(args[0])));
                return;
            }
        } else {
            filters = filterService.getActiveFiltersForChat(chatId);
        }

        if (filters.isEmpty()) {
            sender.sendText(chatId, "📭 No active filters. Use /addfilter to create one.");
            return;
        }

        sender.sendText(chatId, "🔍 Scanning %d filter(s) in background...".formatted(filters.size()));

        try {
            List<Long> filterIds = filters.stream().map(VacancyFilter::getId).toList();
            String payload = objectMapper.writeValueAsString(Map.of("chatId", chatId, "filterIds", filterIds));
            taskDispatcher.dispatch(scannerFunctionName, payload);
            log.info("Async scan triggered for chatId={}, filters={}", chatId, filterIds);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize scan payload: {}", e.getMessage(), e);
            sender.sendText(chatId, "❌ Failed to start scan: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to trigger async scan: {}", e.getMessage(), e);
            sender.sendText(chatId, "❌ Failed to start scan: " + e.getMessage());
        }
    }
}
