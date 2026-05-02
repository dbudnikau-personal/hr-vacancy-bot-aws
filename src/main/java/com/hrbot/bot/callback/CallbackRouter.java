package com.hrbot.bot.callback;

import com.hrbot.bot.MessageSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CallbackRouter {

    private final Map<String, CallbackHandler> handlers;
    private final MessageSender messageSender;

    public CallbackRouter(List<CallbackHandler> handlerList, MessageSender messageSender) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(CallbackHandler::getPrefix, Function.identity()));
        this.messageSender = messageSender;
    }

    public void route(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        String prefix = data.contains(":") ? data.substring(0, data.indexOf(":")) : data;
        CallbackHandler handler = handlers.get(prefix);
        if (handler != null) {
            log.debug("Routing callback [{}] from chatId={}", prefix, callbackQuery.getMessage().getChatId());
            handler.handle(callbackQuery, data);
        } else {
            log.debug("Unknown callback prefix [{}], answering noop", prefix);
            messageSender.answerCallback(callbackQuery.getId());
        }
    }
}
