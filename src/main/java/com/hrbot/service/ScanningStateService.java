package com.hrbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;
import software.amazon.awssdk.services.ssm.model.ParameterType;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScanningStateService {

    static final String PAUSED_PARAM = "/hrbot/scanning/paused";

    private final SsmClient ssmClient;

    public boolean isPaused() {
        try {
            String value = ssmClient.getParameter(GetParameterRequest.builder()
                            .name(PAUSED_PARAM)
                            .build())
                    .parameter().value();
            return "true".equals(value);
        } catch (ParameterNotFoundException e) {
            return false;
        }
    }

    public void setPaused(boolean paused) {
        ssmClient.putParameter(PutParameterRequest.builder()
                .name(PAUSED_PARAM)
                .value(String.valueOf(paused))
                .type(ParameterType.STRING)
                .overwrite(true)
                .build());
        log.info("Scanning paused={}", paused);
    }
}
