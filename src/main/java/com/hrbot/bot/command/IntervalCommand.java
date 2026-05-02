package com.hrbot.bot.command;

import com.hrbot.bot.MessageSender;
import com.hrbot.service.ScanScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntervalCommand implements BotCommand {

    private final MessageSender sender;
    private final ScanScheduleService scanScheduleService;

    @Override
    public String getCommand() { return "/interval"; }

    @Override
    public String getDescription() { return "Show or set the scan schedule. Usage: /interval [Nm|Nh|Nd]"; }

    @Override
    public void handle(Message message, String[] args) {
        long chatId = message.getChatId();

        if (args.length == 0) {
            try {
                String current = scanScheduleService.getCurrentRate();
                sender.sendText(chatId, "⏱ Current scan schedule: <code>%s</code>\nChange with <code>/interval 1d</code> (m/h/d)".formatted(current));
            } catch (Exception e) {
                log.error("Failed to read current schedule: {}", e.getMessage(), e);
                sender.sendText(chatId, "❌ Failed to read schedule: " + e.getMessage());
            }
            return;
        }

        String rate;
        try {
            rate = ScanScheduleService.parseUserInputToRate(args[0]);
        } catch (IllegalArgumentException e) {
            sender.sendText(chatId, "❌ " + e.getMessage());
            return;
        }

        try {
            scanScheduleService.setRate(rate);
            log.info("Scan schedule changed to {} by chatId={}", rate, chatId);
            sender.sendText(chatId, "✅ Scan schedule set to <code>%s</code>.\n<i>Note: a redeploy resets it to the template default.</i>".formatted(rate));
        } catch (Exception e) {
            log.error("Failed to set schedule: {}", e.getMessage(), e);
            sender.sendText(chatId, "❌ Failed to update schedule: " + e.getMessage());
        }
    }
}
