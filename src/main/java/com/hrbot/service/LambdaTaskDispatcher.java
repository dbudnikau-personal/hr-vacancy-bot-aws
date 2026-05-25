package com.hrbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class LambdaTaskDispatcher implements AsyncTaskDispatcher {

    private final LambdaClient lambdaClient;

    @Override
    public void dispatch(String functionName, String payload) {
        log.debug("Dispatching async task to Lambda '{}'", functionName);
        lambdaClient.invoke(InvokeRequest.builder()
                .functionName(functionName)
                .invocationType(InvocationType.EVENT)
                .payload(SdkBytes.fromUtf8String(payload))
                .build());
    }

    @Override
    public void invokeSync(String functionName, String payload) {
        log.debug("Invoking Lambda '{}' synchronously", functionName);
        lambdaClient.invoke(InvokeRequest.builder()
                .functionName(functionName)
                .invocationType(InvocationType.REQUEST_RESPONSE)
                .payload(SdkBytes.fromUtf8String(payload))
                .build());
    }
}
