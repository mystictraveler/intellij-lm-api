package com.intellij.lm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Simple LmChatResponse for providers that return the full text at once.
 */
class DefaultLmChatResponse(private val producer: suspend () -> String) : LmChatResponse {
    override val textStream: Flow<String> = flow { emit(producer()) }
    override suspend fun text(): String = producer()
}

/**
 * Streaming LmChatResponse for providers that support chunk-by-chunk delivery.
 */
class StreamingLmChatResponse(override val textStream: Flow<String>) : LmChatResponse {
    override suspend fun text(): String {
        val sb = StringBuilder()
        textStream.collect { sb.append(it) }
        return sb.toString()
    }
}
