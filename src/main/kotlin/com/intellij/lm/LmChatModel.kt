package com.intellij.lm

/**
 * A language model that can handle chat requests.
 * Equivalent to VS Code's LanguageModelChat.
 */
interface LmChatModel {
    /** Unique model identifier, e.g. "gpt-4o", "claude-sonnet-4-5-20250514" */
    val id: String
    /** Human-readable model name */
    val name: String
    /** Model family, e.g. "gpt-4o", "claude" */
    val family: String
    /** Provider/vendor name, e.g. "copilot", "anthropic" */
    val vendor: String
    /** Maximum input tokens the model supports */
    val maxInputTokens: Int

    /**
     * Send a chat request to this model.
     *
     * @param messages The conversation messages
     * @param options Optional request parameters
     * @return A response that supports streaming
     */
    suspend fun sendRequest(
        messages: List<LmChatMessage>,
        options: LmChatRequestOptions = LmChatRequestOptions()
    ): LmChatResponse
}
