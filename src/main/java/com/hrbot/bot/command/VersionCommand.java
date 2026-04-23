package com.hrbot.bot.command;

import com.hrbot.bot.MessageSender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class VersionCommand implements BotCommand {

    private final MessageSender sender;

    @Value("${bot.version:local}")
    private String version;

    @Override
    public String getCommand() { return "/version"; }

    @Override
    public String getDescription() { return "Show current bot version"; }

    @Override
    public void handle(Message message, String[] args) {
        sender.sendText(message.getChatId(), "Version: <code>" + version + "</code>");
    }
}
