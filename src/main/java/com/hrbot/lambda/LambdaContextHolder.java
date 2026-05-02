package com.hrbot.lambda;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrbot.HrVacancyBotApplication;
import com.hrbot.bot.MessageSender;
import com.hrbot.bot.callback.CallbackRouter;
import com.hrbot.bot.command.CommandRouter;
import com.hrbot.scheduler.VacancyScanScheduler;
import lombok.extern.slf4j.Slf4j;
import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

import java.util.Map;

@Slf4j
public class LambdaContextHolder implements Resource {

    private static final Map<String, String> SSM_TO_PROPERTY = Map.of(
            "/hrbot/datasource/url",      "SPRING_DATASOURCE_URL",
            "/hrbot/datasource/username", "SPRING_DATASOURCE_USERNAME",
            "/hrbot/datasource/password", "SPRING_DATASOURCE_PASSWORD",
            "/hrbot/bot/token",           "BOT_TOKEN",
            "/hrbot/deepseek/api-key",    "DEEPSEEK_API_KEY",
            "/hrbot/admin/chat-id",       "ADMIN_CHAT_ID"
    );

    static final CommandRouter commandRouter;
    static final CallbackRouter callbackRouter;
    static final ObjectMapper objectMapper;
    static final VacancyScanScheduler vacancyScanScheduler;
    private static final MessageSender messageSender;
    static {
        Core.getGlobalContext().register(new LambdaContextHolder());
        loadSecretsFromSsm();
        log.info("Initializing Spring context...");
        ConfigurableApplicationContext context = new SpringApplicationBuilder(HrVacancyBotApplication.class)
                .web(WebApplicationType.NONE)
                .run();
        commandRouter = context.getBean(CommandRouter.class);
        callbackRouter = context.getBean(CallbackRouter.class);
        objectMapper = context.getBean(ObjectMapper.class);
        vacancyScanScheduler = context.getBean(VacancyScanScheduler.class);
        messageSender = context.getBean(MessageSender.class);
        log.info("Spring context initialized");
    }

    @Override
    public void beforeCheckpoint(Context<? extends Resource> ctx) {}

    @Override
    public void afterRestore(Context<? extends Resource> ctx) {
        loadSecretsFromSsm();
        messageSender.reloadToken(System.getProperty("BOT_TOKEN"));
    }

    private static void loadSecretsFromSsm() {
        try (SsmClient ssm = SsmClient.builder()
                .httpClient(UrlConnectionHttpClient.create())
                .build()) {

            GetParametersResponse response = ssm.getParameters(
                    GetParametersRequest.builder()
                            .names(SSM_TO_PROPERTY.keySet())
                            .withDecryption(true)
                            .build());

            if (!response.invalidParameters().isEmpty()) {
                throw new IllegalStateException("Missing SSM parameters: " + response.invalidParameters());
            }

            for (Parameter param : response.parameters()) {
                String property = SSM_TO_PROPERTY.get(param.name());
                if (property != null) {
                    System.setProperty(property, param.value());
                }
            }
        }
        log.info("Secrets loaded from SSM");
    }
}
