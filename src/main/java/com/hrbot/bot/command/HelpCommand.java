package com.hrbot.bot.command;

import com.hrbot.bot.MessageSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;


@Component
@RequiredArgsConstructor
public class HelpCommand implements BotCommand {

    private final MessageSender sender;

    @Override
    public String getCommand() { return "/help"; }

    @Override
    public String getDescription() { return "Show available commands"; }

    @Override
    public void handle(Message message, String[] args) {
        sender.sendText(message.getChatId(), """
                🤖 <b>HR Vacancy Bot</b>

                <b>Filters:</b>
                • <code>/addfilter &lt;name&gt; &lt;keywords&gt; [location] [salaryMin] [sites]</code>
                  <i>Add vacancy filter</i>
                • <code>/filters</code>
                  <i>List active filters</i>
                • <code>/removefilter &lt;id&gt;</code>
                  <i>Deactivate filter by ID</i>

                <b>Search:</b>
                • <code>/vacancies</code> — browse all vacancies (inline Prev/Next)
                • <code>/vacancies &lt;keyword&gt;</code> — search by keyword
                • <code>/report &lt;site&gt;</code> — export vacancies to CSV

                <b>Control:</b>
                • <code>/scan</code> — scan all active filters
                • <code>/scan &lt;filter_id&gt;</code> — scan specific filter
                • <code>/status</code> — parser health status
                • <code>/help</code> — this message

                <b>Available sites:</b>
                <code>djinni, hh, getmatch, wellfound</code>

                <b>Examples:</b>
                <code>/addfilter java-remote "Java Spring" "" 3000 djinni,hh,getmatch</code>
                <code>/scan 1</code>
                <code>/vacancies java</code>
                """);
    }
}
