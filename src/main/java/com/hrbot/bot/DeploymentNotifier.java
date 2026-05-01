package com.hrbot.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeploymentNotifier {

    private final MessageSender messageSender;

    @Value("${bot.version:unknown}")
    private String version;

    public void notifyDeployment() {
        String rawChatId = System.getProperty("ADMIN_CHAT_ID", "0");
        long adminChatId;
        try {
            adminChatId = Long.parseLong(rawChatId);
        } catch (NumberFormatException e) {
            log.warn("DeploymentNotifier: invalid ADMIN_CHAT_ID '{}', skipping", rawChatId);
            return;
        }
        if (adminChatId == 0) {
            log.debug("DeploymentNotifier: ADMIN_CHAT_ID not set, skipping notification");
            return;
        }
        try {
            boolean sent = messageSender.sendText(adminChatId, "HR Vacancy Bot v" + version + " started");
            if (sent) {
                log.info("Deployment notification sent to chat {}", adminChatId);
            } else {
                log.warn("Failed to send deployment notification to chat {}", adminChatId);
            }
        } catch (Exception e) {
            log.warn("Failed to send deployment notification: {}", e.getMessage());
        }
    }
}
