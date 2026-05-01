package com.hrbot.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeploymentNotifier {

    private final MessageSender messageSender;

    @Value("${bot.version:unknown}")
    private String version;

    @Value("${admin.chat.id:0}")
    private long adminChatId;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (adminChatId == 0) {
            log.debug("DeploymentNotifier: admin.chat.id not set, skipping notification");
            return;
        }
        try {
            boolean sent = messageSender.sendText(adminChatId, "HR Vacancy Bot v" + version + " deployed");
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
