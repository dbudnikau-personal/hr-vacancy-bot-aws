package com.hrbot.bot.command;


import org.telegram.telegrambots.meta.api.objects.message.Message;

public interface BotCommand {
    String getCommand();       // e.g. "/addfilter"
    String getDescription();   // shown in /help
    void handle(Message message, String[] args);
}
