package com.hrbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.DescribeRuleRequest;
import software.amazon.awssdk.services.eventbridge.model.DescribeRuleResponse;
import software.amazon.awssdk.services.eventbridge.model.PutRuleRequest;
import software.amazon.awssdk.services.eventbridge.model.RuleState;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ScanScheduleService {

    private static final Pattern USER_INPUT = Pattern.compile("^(\\d+)\\s*([mhd])$", Pattern.CASE_INSENSITIVE);
    private static final Duration MIN_INTERVAL = Duration.ofMinutes(1);

    private final EventBridgeClient eventBridgeClient;
    private final String ruleName;

    public ScanScheduleService(EventBridgeClient eventBridgeClient,
                               @Value("${SCAN_SCHEDULE_RULE_NAME:hr-vacancy-bot-scan-schedule}") String ruleName) {
        this.eventBridgeClient = eventBridgeClient;
        this.ruleName = ruleName;
    }

    public String getCurrentRate() {
        DescribeRuleResponse response = eventBridgeClient.describeRule(DescribeRuleRequest.builder()
                .name(ruleName)
                .build());
        return response.scheduleExpression();
    }

    public void setRate(String rateExpression) {
        eventBridgeClient.putRule(PutRuleRequest.builder()
                .name(ruleName)
                .scheduleExpression(rateExpression)
                .state(RuleState.ENABLED)
                .build());
        log.info("Scan schedule updated to {}", rateExpression);
    }

    public static String parseUserInputToRate(String input) {
        Matcher m = USER_INPUT.matcher(input.trim());
        if (!m.matches()) {
            throw new IllegalArgumentException("Use format like 30m, 6h, 1d");
        }
        long value = Long.parseLong(m.group(1));
        String unit = m.group(2).toLowerCase();
        Duration duration = switch (unit) {
            case "m" -> Duration.ofMinutes(value);
            case "h" -> Duration.ofHours(value);
            case "d" -> Duration.ofDays(value);
            default -> throw new IllegalArgumentException("Unknown unit: " + unit);
        };
        if (duration.compareTo(MIN_INTERVAL) < 0) {
            throw new IllegalArgumentException("Minimum interval is 1 minute");
        }
        String unitName = switch (unit) {
            case "m" -> value == 1 ? "minute" : "minutes";
            case "h" -> value == 1 ? "hour" : "hours";
            case "d" -> value == 1 ? "day" : "days";
            default -> throw new IllegalArgumentException("Unknown unit: " + unit);
        };
        return "rate(" + value + " " + unitName + ")";
    }
}
