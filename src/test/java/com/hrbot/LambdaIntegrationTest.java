package com.hrbot;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrbot.bot.MessageSender;
import com.hrbot.bot.callback.CallbackRouter;
import com.hrbot.bot.command.CommandRouter;
import com.hrbot.lambda.BotHandlerLambda;
import com.hrbot.lambda.ScannerLambda;
import com.hrbot.scheduler.VacancyScanScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class LambdaIntegrationTest {

    @TestConfiguration
    static class TestContainersConfig {
        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgres() {
            return new PostgreSQLContainer<>("postgres:16-alpine");
        }

        // Provide mock here so AppConfig.lambdaClient() is never called
        // (AWS SDK would fail without a configured region in CI)
        @Bean
        LambdaClient lambdaClient() {
            return mock(LambdaClient.class);
        }

        @Bean
        SsmClient ssmClient() {
            SsmClient mock = mock(SsmClient.class);
            when(mock.getParameter(any(GetParameterRequest.class)))
                    .thenThrow(ParameterNotFoundException.builder().message("not found in test").build());
            return mock;
        }

        @Bean
        EventBridgeClient eventBridgeClient() {
            return mock(EventBridgeClient.class);
        }
    }

    @MockitoBean MessageSender messageSender;

    @Autowired CommandRouter commandRouter;
    @Autowired CallbackRouter callbackRouter;
    @Autowired VacancyScanScheduler vacancyScanScheduler;
    @Autowired ObjectMapper objectMapper;

    // ── Context ──────────────────────────────────────────────────────────────

    @Test
    void contextLoads() {
        assertThat(commandRouter).isNotNull();
        assertThat(vacancyScanScheduler).isNotNull();
    }

    @Test
    void allCommandsRegistered() {
        assertThat(commandRouter.getCommands()).containsKeys(
                "/help", "/vacancies", "/report", "/scan",
                "/filters", "/addfilter", "/removefilter", "/status",
                "/stopscan", "/startscan", "/interval"
        );
    }

    // ── BotHandlerLambda ─────────────────────────────────────────────────────

    @Test
    void botHandler_helpCommand_returns200AndSendsMessage() {
        String updateJson = """
                {
                  "update_id": 1,
                  "message": {
                    "message_id": 1,
                    "date": 1700000000,
                    "chat": {"id": 42, "type": "private"},
                    "text": "/help"
                  }
                }
                """;

        BotHandlerLambda handler = new BotHandlerLambda(commandRouter, callbackRouter, objectMapper);
        APIGatewayV2HTTPEvent event = APIGatewayV2HTTPEvent.builder().withBody(updateJson).build();

        APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);

        assertThat(response.getStatusCode()).isEqualTo(200);
        verify(messageSender).sendText(eq(42L), contains("HR Vacancy Bot"));
    }

    @Test
    void botHandler_unknownCommand_returns200WithoutCrashing() {
        String updateJson = """
                {
                  "update_id": 2,
                  "message": {
                    "message_id": 2,
                    "date": 1700000000,
                    "chat": {"id": 42, "type": "private"},
                    "text": "/unknown"
                  }
                }
                """;

        BotHandlerLambda handler = new BotHandlerLambda(commandRouter, callbackRouter, objectMapper);
        APIGatewayV2HTTPEvent event = APIGatewayV2HTTPEvent.builder().withBody(updateJson).build();

        assertThatNoException().isThrownBy(() -> handler.handleRequest(event, null));
    }

    @Test
    void botHandler_malformedBody_returns200WithoutCrashing() {
        APIGatewayV2HTTPEvent event = APIGatewayV2HTTPEvent.builder().withBody("not json").build();
        BotHandlerLambda handler = new BotHandlerLambda(commandRouter, callbackRouter, objectMapper);

        APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);

        assertThat(response.getStatusCode()).isEqualTo(200);
    }

    // ── ScannerLambda ────────────────────────────────────────────────────────

    @Test
    void scannerLambda_scheduledEvent_completesWithNoFilters() {
        ScannerLambda scanner = new ScannerLambda(vacancyScanScheduler);
        assertThatNoException().isThrownBy(() -> scanner.handleRequest(Map.of(), null));
    }

    @Test
    void scannerLambda_manualEvent_completesForUnknownChat() {
        ScannerLambda scanner = new ScannerLambda(vacancyScanScheduler);
        Map<String, Object> event = Map.of("chatId", 99999L, "filterIds", java.util.List.of());
        assertThatNoException().isThrownBy(() -> scanner.handleRequest(event, null));
    }
}
