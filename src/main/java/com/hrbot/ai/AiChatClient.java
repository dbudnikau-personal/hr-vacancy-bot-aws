package com.hrbot.ai;

/**
 * Abstraction over any LLM provider that supports a simple chat completion call.
 * Keeps high-level services independent of the concrete provider (DeepSeek, OpenAI, etc.).
 */
public interface AiChatClient {

    /**
     * Send a single-turn chat request and return the model's text response,
     * or {@code null} if the request fails.
     */
    String chat(String systemPrompt, String userMessage);
}
