package com.hrbot.service;

/**
 * Abstraction for dispatching async background tasks.
 * Decouples command handlers from the concrete AWS Lambda transport.
 */
public interface AsyncTaskDispatcher {

    /**
     * Dispatch a fire-and-forget task to the named function with the given JSON payload.
     */
    void dispatch(String functionName, String payload);
}
