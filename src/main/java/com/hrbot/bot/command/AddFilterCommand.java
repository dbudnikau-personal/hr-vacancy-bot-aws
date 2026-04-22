package com.hrbot.bot.command;

import com.hrbot.bot.MessageSender;
import com.hrbot.bot.TelegramBot;
import com.hrbot.model.VacancyFilter;
import com.hrbot.parser.ParserRegistry;
import com.hrbot.service.FilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;


import java.util.Arrays;
import java.util.List;

/**
 * Usage: /addfilter <name> <keywords> [location] [salaryMin] [site1,site2]
 *
 * Examples:
 *   /addfilter java-remote "Java Spring" remote 3000 djinni,hh
 *   /addfilter backend-kyiv "Java Senior" 1 5000 hh
 *   /addfilter all-java "Java" "" "" djinni,hh,linkedin
 */
@Component
@RequiredArgsConstructor
public class AddFilterCommand implements BotCommand {

    private final MessageSender sender;
    private final FilterService filterService;
    private final ParserRegistry parserRegistry;

    @Override
    public String getCommand() { return "/addfilter"; }

    @Override
    public String getDescription() { return "Add vacancy filter. Usage: /addfilter <name> <keywords> [location] [salaryMin] [sites]"; }

    @Override
    public void handle(Message message, String[] args) {
        long chatId = message.getChatId();

        if (args.length < 2) {
            sender.sendText(chatId, """
                    ❌ <b>Usage:</b>
                    <code>/addfilter &lt;name&gt; &lt;keywords&gt; [location] [salaryMin] [sites]</code>

                    <b>Examples:</b>
                    <code>/addfilter java-remote "Java Spring" remote 3000 djinni,hh</code>
                    <code>/addfilter backend Java 1 5000 hh</code>

                    <b>Available sites:</b> %s
                    """.formatted(String.join(", ", parserRegistry.availableSites())));
            return;
        }

        // Re-parse from raw message text to support quoted args
        String rawText = message.getText().trim();
        String[] parsed = parseArgs(rawText);

        if (parsed.length < 3) {
            sender.sendText(chatId, "❌ Provide at least <name> and <keywords>.");
            return;
        }

        String name      = parsed[1];
        String keywords  = parsed[2];
        String location  = parsed.length > 3 ? emptyToNull(parsed[3]) : null;
        String salaryMin = parsed.length > 4 ? emptyToNull(parsed[4]) : null;
        List<String> sites = parsed.length > 5
                ? Arrays.asList(parsed[5].split(","))
                : parserRegistry.availableSites();

        VacancyFilter filter = VacancyFilter.builder()
                .chatId(chatId)
                .name(name)
                .keywords(keywords)
                .location(location)
                .salaryMin(salaryMin)
                .sites(sites)
                .active(true)
                .build();

        VacancyFilter saved = filterService.save(filter);

        sender.sendText(chatId, """
                ✅ <b>Filter saved!</b>
                🏷 Name: <code>%s</code>
                🔍 Keywords: <code>%s</code>
                📍 Location: <code>%s</code>
                💰 Min salary: <code>%s</code>
                🌐 Sites: <code>%s</code>
                🆔 Filter ID: <code>%d</code>
                """.formatted(
                saved.getName(),
                saved.getKeywords(),
                saved.getLocation() != null ? saved.getLocation() : "any",
                saved.getSalaryMin() != null ? saved.getSalaryMin() : "any",
                String.join(", ", saved.getSites()),
                saved.getId()
        ));
    }

    // Splits by whitespace but respects "quoted strings"
    private String[] parseArgs(String text) {
        List<String> tokens = new java.util.ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"([^\"]*)\"|'([^']*)'|(\\S+)")
                .matcher(text);
        while (m.find()) {
            if (m.group(1) != null)      tokens.add(m.group(1));
            else if (m.group(2) != null) tokens.add(m.group(2));
            else                          tokens.add(m.group(3));
        }
        return tokens.toArray(new String[0]);
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank() || s.equals("\"\"") || s.equals("''")) ? null : s;
    }
}
