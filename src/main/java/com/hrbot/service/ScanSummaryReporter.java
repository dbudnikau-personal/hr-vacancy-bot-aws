package com.hrbot.service;

import com.hrbot.bot.MessageSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Composes and sends the post-scan summary message to each chat.
 * Extracted from {@link com.hrbot.scheduler.VacancyScanScheduler} to keep
 * the scheduler focused on orchestration rather than message formatting.
 */
@Service
@RequiredArgsConstructor
public class ScanSummaryReporter {

    private final MessageSender messageSender;

    public void report(Map<Long, int[]> chatTotals, Map<Long, List<String>> chatFilterLines) {
        chatTotals.forEach((chatId, totals) -> {
            String header = "✅ Scan complete: <b>%d found</b>, %d new, %d updated"
                    .formatted(totals[0], totals[1], totals[2]);
            String details = String.join("\n", chatFilterLines.getOrDefault(chatId, List.of()));
            messageSender.sendText(chatId, header + "\n\n" + details);
        });
    }
}
