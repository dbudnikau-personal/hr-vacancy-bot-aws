package com.hrbot.lambda;

import com.hrbot.HrVacancyBotApplication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
public class LambdaContextHolder {

    private static final ConfigurableApplicationContext context;

    static {
        log.info("Initializing Spring context...");
        context = new SpringApplicationBuilder(HrVacancyBotApplication.class)
                .web(WebApplicationType.NONE)
                .run();
        log.info("Spring context initialized");
    }

    public static <T> T getBean(Class<T> type) {
        return context.getBean(type);
    }
}
