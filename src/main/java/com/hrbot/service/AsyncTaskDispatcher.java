package com.hrbot.service;

/**
 * Abstraction for invoking background Lambda functions.
 * Decouples callers from the concrete AWS Lambda transport.
 */
public interface AsyncTaskDispatcher {

    /**
     * Fire-and-forget: invoke the function asynchronously, do not wait for a result.
     * Use for background tasks (e.g. triggering a scan).
     */
    void dispatch(String functionName, String payload);

    /**
     * Synchronous invoke: call the function and block until it returns.
     * Use when the caller depends on side-effects produced by the function
     * (e.g. cookie-refresher writing to SSM before the next SSM read).
     */
    void invokeSync(String functionName, String payload);
}
