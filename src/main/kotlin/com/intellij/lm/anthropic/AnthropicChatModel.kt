package com.intellij.lm.anthropic

import com.intellij.lm.*
import kotlinx.coroutines.flow.flow

class AnthropicChatModel(
    override val id: String,
    override val name: String,
    override val family: String,
    override val maxInputTokens: Int
) : LmChatModel {

    override val vendor = "anthropic"

    override suspend fun sendRequest(
        messages: List<LmChatMessage>,
        options: LmChatRequestOptions
    ): LmChatResponse {
        return StreamingLmChatResponse(flow {
            val response = AnthropicHttpClient.getInstance().chatCompletion(id, messages, options)
            emit(response)
        })
    }
}
