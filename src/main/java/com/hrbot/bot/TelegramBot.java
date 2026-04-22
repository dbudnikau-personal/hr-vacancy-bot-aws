package com.hrbot.bot;

import com.hrbot.bot.command.CommandRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Component
public class TelegramBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final String botToken;
    private final CommandRouter commandRouter;

    public TelegramBot(
            @Value("${bot.token}") String botToken,
            CommandRouter commandRouter) {
        this.botToken = botToken;
        this.commandRouter = commandRouter;
    }

    @Override
    public String getBotToken() { return botToken; }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() { return this; }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;
        String text = update.getMessage().getText().trim();
        if (text.startsWith("/")) {
            commandRouter.route(update.getMessage());
        }
    }
}
