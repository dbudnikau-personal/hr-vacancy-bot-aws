package com.hrbot.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrbot.bot.DeploymentNotifier;
import com.hrbot.bot.callback.CallbackRouter;
import com.hrbot.bot.command.CommandRouter;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.function.BooleanSupplier;

@Slf4j
public class BotHandlerLambda implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private final CommandRouter commandRouter;
    private final CallbackRouter callbackRouter;
    private final ObjectMapper objectMapper;
    private final DeploymentNotifier deploymentNotifier;
    private final BooleanSupplier pendingNotification;

    public BotHandlerLambda() {
        this(LambdaContextHolder.commandRouter, LambdaContextHolder.callbackRouter,
                LambdaContextHolder.objectMapper, LambdaContextHolder.deploymentNotifier,
                LambdaContextHolder::consumePendingNotification);
    }

    public BotHandlerLambda(CommandRouter commandRouter, CallbackRouter callbackRouter,
                             ObjectMapper objectMapper, DeploymentNotifier deploymentNotifier) {
        this(commandRouter, callbackRouter, objectMapper, deploymentNotifier, () -> false);
    }

    BotHandlerLambda(CommandRouter commandRouter, CallbackRouter callbackRouter,
                     ObjectMapper objectMapper, DeploymentNotifier deploymentNotifier,
                     BooleanSupplier pendingNotification) {
        this.commandRouter = commandRouter;
        this.callbackRouter = callbackRouter;
        this.objectMapper = objectMapper;
        this.deploymentNotifier = deploymentNotifier;
        this.pendingNotification = pendingNotification;
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        if (pendingNotification.getAsBoolean()) {
            deploymentNotifier.notifyDeployment();
        }
        try {
            Update update = objectMapper.readValue(event.getBody(), Update.class);
            if (update.hasMessage() && update.getMessage().hasText()) {
                String text = update.getMessage().getText().trim();
                if (text.startsWith("/")) {
                    commandRouter.route(update.getMessage());
                }
            } else if (update.hasCallbackQuery()) {
                callbackRouter.route(update.getCallbackQuery());
            }
        } catch (Exception e) {
            log.error("Error processing Telegram update: {}", e.getMessage(), e);
        }
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(200)
                .build();
    }
}
