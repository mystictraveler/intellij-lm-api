package com.intellij.lm

import com.intellij.openapi.extensions.ExtensionPointName

/**
 * Extension point for language model providers.
 *
 * Providers (e.g. GitHub Copilot, Anthropic, OpenAI) implement this interface
 * and register via the `com.intellij.lm.provider` extension point:
 *
 * ```xml
 * <extensions defaultExtensionNs="com.intellij.lm">
 *     <provider implementation="com.example.MyCopilotLmProvider"/>
 * </extensions>
 * ```
 */
interface LmProvider {
    /** Unique identifier for this provider, e.g. "copilot", "anthropic" */
    val id: String

    /** Human-readable provider name */
    val displayName: String

    /** Returns the chat models available from this provider. */
    suspend fun getAvailableModels(): List<LmChatModel>

    companion object {
        val EP_NAME = ExtensionPointName.create<LmProvider>("com.intellij.lm.provider")
    }
}
