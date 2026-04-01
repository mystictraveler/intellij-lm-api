package com.intellij.lm.anthropic

import com.intellij.lm.LmChatModel
import com.intellij.lm.LmProvider

/**
 * LM provider for the Anthropic API (Claude models).
 *
 * Requires the ANTHROPIC_API_KEY environment variable or
 * the key set via IntelliJ Settings > Language Model API > Anthropic.
 */
class AnthropicLmProvider : LmProvider {
    override val id = "anthropic"
    override val displayName = "Anthropic (Claude)"

    override suspend fun getAvailableModels(): List<LmChatModel> {
        // Only return models if an API key is configured
        val apiKey = AnthropicHttpClient.resolveApiKey() ?: return emptyList()

        return listOf(
            AnthropicChatModel(
                id = "claude-opus-4-6",
                name = "Claude Opus 4.6",
                family = "claude",
                maxInputTokens = 1000000
            ),
            AnthropicChatModel(
                id = "claude-sonnet-4-6",
                name = "Claude Sonnet 4.6",
                family = "claude",
                maxInputTokens = 200000
            ),
            AnthropicChatModel(
                id = "claude-haiku-4-5-20251001",
                name = "Claude Haiku 4.5",
                family = "claude",
                maxInputTokens = 200000
            )
        )
    }
}
