package com.hrbot.lambda;

import com.hrbot.HrVacancyBotApplication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

import java.util.List;
import java.util.Map;

@Slf4j
public class LambdaContextHolder {

    private static final Map<String, String> SSM_TO_PROPERTY = Map.of(
            "/hrbot/datasource/url",      "SPRING_DATASOURCE_URL",
            "/hrbot/datasource/username", "SPRING_DATASOURCE_USERNAME",
            "/hrbot/datasource/password", "SPRING_DATASOURCE_PASSWORD",
            "/hrbot/bot/token",           "BOT_TOKEN",
            "/hrbot/deepseek/api-key",    "DEEPSEEK_API_KEY"
    );

    private static final ConfigurableApplicationContext context;

    static {
        loadSecretsFromSsm();
        log.info("Initializing Spring context...");
        context = new SpringApplicationBuilder(HrVacancyBotApplication.class)
                .web(WebApplicationType.NONE)
                .run();
        log.info("Spring context initialized");
    }

    public static <T> T getBean(Class<T> type) {
        return context.getBean(type);
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
