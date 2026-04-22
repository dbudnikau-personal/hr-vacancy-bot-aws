package com.hrbot.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.hrbot.scheduler.VacancyScanScheduler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ScannerLambda implements RequestHandler<ScheduledEvent, Void> {

    private final VacancyScanScheduler scanner;

    public ScannerLambda() {
        this.scanner = LambdaContextHolder.getBean(VacancyScanScheduler.class);
    }

    @Override
    public Void handleRequest(ScheduledEvent event, Context context) {
        log.info("Scanner triggered by EventBridge: {}", event.getTime());
        scanner.scan();
        return null;
    }
}
