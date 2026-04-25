package com.hrbot.bot.command;

import com.hrbot.bot.MessageSender;
import com.hrbot.bot.callback.CallbackHandler;
import com.hrbot.model.Vacancy;
import com.hrbot.repository.VacancyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class VacanciesCommand implements BotCommand, CallbackHandler {

    private static final int PAGE_SIZE = 5;
    private static final String PREFIX = "vac";

    private final MessageSender sender;
    private final VacancyRepository vacancyRepository;

    @Override
    public String getCommand() { return "/vacancies"; }

    @Override
    public String getPrefix() { return PREFIX; }

    @Override
    public String getDescription() { return "Browse vacancies from DB. Usage: /vacancies [keyword]"; }

    @Override
    public void handle(Message message, String[] args) {
        String keyword = args.length > 0 ? args[0] : null;
        showPage(message.getChatId(), null, 0, keyword);
    }

    @Override
    public void handle(CallbackQuery callbackQuery, String data) {
        // data format: "vac:PAGE" or "vac:PAGE:kw:KEYWORD"
        String[] parts = data.split(":", 4);
        int page = Integer.parseInt(parts[1]);
        String keyword = parts.length == 4 && "kw".equals(parts[2]) ? parts[3] : null;

        long chatId = callbackQuery.getMessage().getChatId();
        int messageId = callbackQuery.getMessage().getMessageId();
        sender.answerCallback(callbackQuery.getId());
        showPage(chatId, messageId, page, keyword);
    }

    private void showPage(long chatId, Integer messageId, int page, String keyword) {
        PageRequest pageRequest = PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "foundAt"));

        Page<Vacancy> result = keyword != null
                ? vacancyRepository.findByTitleContainingIgnoreCaseOrCompanyContainingIgnoreCase(keyword, keyword, pageRequest)
                : vacancyRepository.findAll(pageRequest);

        if (result.isEmpty()) {
            sender.sendText(chatId, "📭 No vacancies found.");
            return;
        }

        int totalPages = result.getTotalPages();
        long total = result.getTotalElements();
        String context = keyword != null ? "🔍 <code>" + escape(keyword) + "</code>" : "📋 all";

        StringBuilder sb = new StringBuilder();
        sb.append("💼 <b>Vacancies</b> (%s) — page %d/%d, total: %d\n\n"
                .formatted(context, page + 1, totalPages, total));

        for (Vacancy v : result.getContent()) {
            sb.append("▪️ <a href=\"%s\">%s</a>\n".formatted(v.getUrl(), escape(v.getTitle())));
            sb.append("   🏢 %s".formatted(escape(v.getCompany())));
            if (v.getSalary() != null) sb.append(" · 💰 %s".formatted(escape(v.getSalary())));
            if (v.getLocation() != null) sb.append(" · 📍 %s".formatted(escape(v.getLocation())));
            sb.append(" · <i>%s</i>".formatted(v.getSiteKey()));
            sb.append("\n\n");
        }

        InlineKeyboardMarkup keyboard = buildKeyboard(page, totalPages, keyword);

        if (messageId != null) {
            sender.editMessage(chatId, messageId, sb.toString(), keyboard);
        } else {
            sender.sendWithKeyboard(chatId, sb.toString(), keyboard);
        }
    }

    private InlineKeyboardMarkup buildKeyboard(int page, int totalPages, String keyword) {
        String base = PREFIX + ":" + "%d" + (keyword != null ? ":kw:" + keyword : "");
        List<InlineKeyboardButton> buttons = new ArrayList<>();

        if (page > 0) {
            buttons.add(InlineKeyboardButton.builder()
                    .text("◀ Prev")
                    .callbackData(base.formatted(page - 1))
                    .build());
        }
        buttons.add(InlineKeyboardButton.builder()
                .text((page + 1) + "/" + totalPages)
                .callbackData("noop")
                .build());
        if (page + 1 < totalPages) {
            buttons.add(InlineKeyboardButton.builder()
                    .text("Next ▶")
                    .callbackData(base.formatted(page + 1))
                    .build());
        }

        return new InlineKeyboardMarkup(List.of(new InlineKeyboardRow(buttons)));
    }

    private String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
