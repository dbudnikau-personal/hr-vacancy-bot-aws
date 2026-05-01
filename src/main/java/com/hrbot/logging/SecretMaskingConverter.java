package com.hrbot.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Pattern;

public class SecretMaskingConverter extends ClassicConverter {

    private static final Pattern[] PATTERNS = {
        // Telegram bot token in URLs: /bot123456:AAFxxx.../
        Pattern.compile("(bot\\d+:)[A-Za-z0-9_-]+"),
        // Bearer authorization header value
        Pattern.compile("((?i)Bearer\\s+)\\S+"),
        // Password in JDBC/HTTP URLs: scheme://user:PASSWORD@host
        Pattern.compile("(://[^:/@\\s]+:)[^@\\s]+(?=@)"),
    };

    @Override
    public String convert(ILoggingEvent event) {
        String msg = event.getFormattedMessage();
        for (Pattern pattern : PATTERNS) {
            msg = pattern.matcher(msg).replaceAll("$1***");
        }
        return msg;
    }
}
