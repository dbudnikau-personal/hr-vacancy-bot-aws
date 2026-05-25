package com.hrbot.bot.command;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Slf4j
@Component
public class CommandRouter {

    private final Map<String, BotCommand> commands;

    public CommandRouter(List<BotCommand> commandList) {
        this.commands = commandList.stream()
                .collect(Collectors.toMap(BotCommand::getCommand, Function.identity()));
    }

    public void route(Message message) {
        String text = message.getText().trim();
        String[] parts = text.split("\\s+", 2);
        String command = parts[0].toLowerCase();

        // Strip bot mention: /start@mybotname -> /start
        if (command.contains("@")) {
            command = command.substring(0, command.indexOf("@"));
        }

        String[] args = parts.length > 1
                ? parts[1].split("\\s+")
                : new String[0];

        BotCommand handler = commands.get(command);
        if (handler != null) {
            log.debug("Routing command [{}] from chatId={}", command, message.getChatId());
            handler.handle(message, args);
        } else {
            log.debug("Unknown command [{}] from chatId={}", command, message.getChatId());
        }
    }

}
