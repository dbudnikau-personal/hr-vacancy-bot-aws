package com.hrbot.bot.command;

import com.hrbot.bot.MessageSender;
import com.hrbot.service.ScanningStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartScanCommand implements BotCommand {

    private final MessageSender sender;
    private final ScanningStateService scanningStateService;

    @Override
    public String getCommand() { return "/startscan"; }

    @Override
    public String getDescription() { return "Re-enable automatic and manual scanning"; }

    @Override
    public void handle(Message message, String[] args) {
        long chatId = message.getChatId();
        if (!scanningStateService.isPaused()) {
            sender.sendText(chatId, "ℹ️ Scanning is already enabled.");
            return;
        }
        scanningStateService.setPaused(false);
        log.info("Scanning enabled by chatId={}", chatId);
        sender.sendText(chatId, "▶️ Scanning <b>enabled</b>. Automatic scans will resume.\nUse /scan to trigger a manual scan.");
    }
}
