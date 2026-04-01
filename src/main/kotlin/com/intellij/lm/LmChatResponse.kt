package com.intellij.lm

import kotlinx.coroutines.flow.Flow

/**
 * Response from a language model chat request.
 * Supports both streaming and blocking access.
 */
interface LmChatResponse {
    /** Stream of text chunks as they arrive from the model. */
    val textStream: Flow<String>

    /** Collects the full response text. Suspends until complete. */
    suspend fun text(): String
}
