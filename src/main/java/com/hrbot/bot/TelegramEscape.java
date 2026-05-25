package com.hrbot.bot;

/**
 * Utility for escaping HTML special characters in Telegram HTML parse-mode messages.
 */
public final class TelegramEscape {

    private TelegramEscape() {}

    public static String html(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
