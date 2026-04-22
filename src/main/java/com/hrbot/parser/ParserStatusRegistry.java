package com.hrbot.parser;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ParserStatusRegistry {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM HH:mm");

    private final Map<String, ParserStatus> statuses = new ConcurrentHashMap<>();

    public void recordSuccess(String siteKey, int found) {
        statuses.put(siteKey, ParserStatus.ok(siteKey, found));
    }

    public void recordError(String siteKey, String error) {
        // Preserve previous found count if exists
        ParserStatus prev = statuses.get(siteKey);
        int prevFound = prev != null ? prev.getLastFoundCount() : 0;
        statuses.put(siteKey, ParserStatus.error(siteKey, error, prevFound));
    }

    public Map<String, ParserStatus> getAll() {
        return Map.copyOf(statuses);
    }

    // --- Status model ---

    @Data
    public static class ParserStatus {
        private final String siteKey;
        private final boolean ok;
        private final String errorMessage;
        private final int lastFoundCount;
        private final LocalDateTime lastRunAt;

        public static ParserStatus ok(String siteKey, int found) {
            return new ParserStatus(siteKey, true, null, found, LocalDateTime.now());
        }

        public static ParserStatus error(String siteKey, String error, int prevFound) {
            return new ParserStatus(siteKey, false, error, prevFound, LocalDateTime.now());
        }

        public String toEmoji() {
            return ok ? "✅" : "❌";
        }

        public String format() {
            String time = lastRunAt != null ? lastRunAt.format(FMT) : "never";
            if (ok) {
                return "%s <b>%s</b> — found %d · last run: %s"
                        .formatted(toEmoji(), siteKey, lastFoundCount, time);
            } else {
                String err = errorMessage != null && errorMessage.length() > 100
                        ? errorMessage.substring(0, 100) + "…"
                        : errorMessage;
                return "%s <b>%s</b> — ERROR: <code>%s</code> · last run: %s"
                        .formatted(toEmoji(), siteKey, err, time);
            }
        }
    }
}
