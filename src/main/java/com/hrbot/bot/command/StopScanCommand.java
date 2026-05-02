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
public class StopScanCommand implements BotCommand {

    private final MessageSender sender;
    private final ScanningStateService scanningStateService;

    @Override
    public String getCommand() { return "/stopscan"; }

    @Override
    public String getDescription() { return "Disable automatic and manual scanning"; }

    @Override
    public void handle(Message message, String[] args) {
        long chatId = message.getChatId();
        if (scanningStateService.isPaused()) {
            sender.sendText(chatId, "ℹ️ Scanning is already disabled.");
            return;
        }
        scanningStateService.setPaused(true);
        log.info("Scanning disabled by chatId={}", chatId);
        sender.sendText(chatId, "⏸ Scanning <b>disabled</b>. No automatic or manual scans will run.\nUse /startscan to re-enable.");
    }
}
