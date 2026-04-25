package com.hrbot.bot.command;

import com.hrbot.bot.MessageSender;
import com.hrbot.model.Vacancy;
import com.hrbot.repository.VacancyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReportCommand implements BotCommand {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String CSV_HEADER = "id,title,company,location,salary,url,found_at,updated_at\n";

    private final MessageSender sender;
    private final VacancyRepository vacancyRepository;

    @Override
    public String getCommand() { return "/report"; }

    @Override
    public String getDescription() { return "Export vacancies to CSV. Usage: /report <site>"; }

    @Override
    public void handle(Message message, String[] args) {
        long chatId = message.getChatId();

        if (args.length == 0) {
            sender.sendText(chatId, "Usage: <code>/report &lt;site&gt;</code>\nExample: <code>/report hh</code>");
            return;
        }

        String siteKey = args[0].toLowerCase();
        List<Vacancy> vacancies = vacancyRepository.findBySiteKeyOrderByFoundAtDesc(siteKey);

        if (vacancies.isEmpty()) {
            sender.sendText(chatId, "📭 No vacancies found for site <code>%s</code>.".formatted(siteKey));
            return;
        }

        byte[] csv = buildCsv(vacancies);
        String filename = "vacancies_%s.csv".formatted(siteKey);
        String caption = "📊 <b>%s</b>: %d vacancies".formatted(siteKey, vacancies.size());

        sender.sendDocument(chatId, new ByteArrayInputStream(csv), filename, caption);
    }

    private byte[] buildCsv(List<Vacancy> vacancies) {
        StringBuilder sb = new StringBuilder(CSV_HEADER);
        for (Vacancy v : vacancies) {
            sb.append(csv(String.valueOf(v.getId()))).append(',');
            sb.append(csv(v.getTitle())).append(',');
            sb.append(csv(v.getCompany())).append(',');
            sb.append(csv(v.getLocation())).append(',');
            sb.append(csv(v.getSalary())).append(',');
            sb.append(csv(v.getUrl())).append(',');
            sb.append(v.getFoundAt() != null ? v.getFoundAt().format(DATE_FMT) : "").append(',');
            sb.append(v.getUpdatedAt() != null ? v.getUpdatedAt().format(DATE_FMT) : "");
            sb.append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String csv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")
                ? "\"" + escaped + "\""
                : escaped;
    }
}
