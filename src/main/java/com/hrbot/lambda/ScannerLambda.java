package com.hrbot.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.hrbot.scheduler.VacancyScanScheduler;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class ScannerLambda implements RequestHandler<Map<String, Object>, Void> {

    private final VacancyScanScheduler scanner;

    public ScannerLambda() {
        this(LambdaContextHolder.vacancyScanScheduler);
    }

    public ScannerLambda(VacancyScanScheduler scanner) {
        this.scanner = scanner;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Void handleRequest(Map<String, Object> event, Context context) {
        if (event != null && event.containsKey("chatId")) {
            long chatId = ((Number) event.get("chatId")).longValue();
            List<Long> filterIds = event.containsKey("filterIds")
                    ? ((List<Number>) event.get("filterIds")).stream().map(Number::longValue).toList()
                    : List.of();
            log.info("Manual scan triggered for chatId={}, filterIds={}", chatId, filterIds);
            scanner.scanForChat(chatId, filterIds);
        } else {
            log.info("Scheduled scan triggered");
            scanner.scan();
        }
        return null;
    }
}
