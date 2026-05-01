package com.hrbot.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.InputStream;

@Slf4j
@Component
public class MessageSender {

    private volatile TelegramClient telegramClient;

    public MessageSender(@Value("${bot.token}") String botToken) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
    }

    public void reloadToken(String botToken) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
    }

    public boolean sendText(Long chatId, String html) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(html)
                .parseMode("HTML")
                .disableWebPagePreview(true)
                .build();
        try {
            telegramClient.execute(message);
            return true;
        } catch (TelegramApiException e) {
            log.error("Failed to send message to {}: {}", chatId, e.getMessage());
            return false;
        }
    }

    public Integer sendWithKeyboard(Long chatId, String html, InlineKeyboardMarkup keyboard) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(html)
                .parseMode("HTML")
                .disableWebPagePreview(true)
                .replyMarkup(keyboard)
                .build();
        try {
            return telegramClient.execute(message).getMessageId();
        } catch (TelegramApiException e) {
            log.error("Failed to send message to {}: {}", chatId, e.getMessage());
            return null;
        }
    }

    public void editMessage(Long chatId, Integer messageId, String html, InlineKeyboardMarkup keyboard) {
        EditMessageText edit = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(html)
                .parseMode("HTML")
                .disableWebPagePreview(true)
                .replyMarkup(keyboard)
                .build();
        try {
            telegramClient.execute(edit);
        } catch (TelegramApiException e) {
            log.error("Failed to edit message {} in chat {}: {}", messageId, chatId, e.getMessage());
        }
    }

    public void sendDocument(Long chatId, InputStream content, String filename, String caption) {
        SendDocument doc = SendDocument.builder()
                .chatId(chatId)
                .document(new InputFile(content, filename))
                .caption(caption)
                .build();
        try {
            telegramClient.execute(doc);
        } catch (TelegramApiException e) {
            log.error("Failed to send document to {}: {}", chatId, e.getMessage());
        }
    }

    public void answerCallback(String callbackQueryId) {
        try {
            telegramClient.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQueryId)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to answer callback {}: {}", callbackQueryId, e.getMessage());
        }
    }
}
