package com.hrbot.bot.callback;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

public interface CallbackHandler {
    String getPrefix();
    void handle(CallbackQuery callbackQuery, String data);
}
