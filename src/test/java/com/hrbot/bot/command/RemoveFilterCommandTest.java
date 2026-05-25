package com.hrbot.bot.command;

import com.hrbot.bot.MessageSender;
import com.hrbot.model.VacancyFilter;
import com.hrbot.service.FilterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveFilterCommandTest {

    @Mock
    MessageSender sender;

    @Mock
    FilterService filterService;

    @InjectMocks
    RemoveFilterCommand command;

    @Test
    void noArgs_sendsUsageHint() {
        command.handle(message(42L), new String[0]);

        verify(sender).sendText(eq(42L), contains("Usage"));
        verify(filterService, never()).deactivate(anyLong());
    }

    @Test
    void nonNumericId_sendsInvalidIdMessage() {
        command.handle(message(42L), new String[]{"abc"});

        verify(sender).sendText(eq(42L), contains("Invalid ID"));
        verify(filterService, never()).deactivate(anyLong());
    }

    @Test
    void filterNotFound_sendsForbidden() {
        when(filterService.findById(99L)).thenReturn(null);

        command.handle(message(42L), new String[]{"99"});

        verify(sender).sendText(eq(42L), contains("not found"));
        verify(filterService, never()).deactivate(anyLong());
    }

    @Test
    void filterBelongsToDifferentChat_sendsForbidden() {
        VacancyFilter filter = VacancyFilter.builder().id(5L).chatId(999L).name("other").active(true).build();
        when(filterService.findById(5L)).thenReturn(filter);

        command.handle(message(42L), new String[]{"5"});

        verify(sender).sendText(eq(42L), contains("not found"));
        verify(filterService, never()).deactivate(anyLong());
    }

    @Test
    void validOwner_deactivatesFilterAndConfirms() {
        VacancyFilter filter = VacancyFilter.builder().id(7L).chatId(42L).name("my-filter").active(true).build();
        when(filterService.findById(7L)).thenReturn(filter);

        command.handle(message(42L), new String[]{"7"});

        verify(filterService).deactivate(7L);
        verify(sender).sendText(eq(42L), contains("deactivated"));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Message message(long chatId) {
        Message message = mock(Message.class);
        when(message.getChatId()).thenReturn(chatId);
        return message;
    }
}
