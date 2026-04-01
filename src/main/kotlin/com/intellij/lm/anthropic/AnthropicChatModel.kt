package com.intellij.lm.anthropic

import com.intellij.lm.*
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.flow.flow

class AnthropicChatModel(
    override val id: String,
    override val name: String,
    override val family: String,
    override val maxInputTokens: Int
) : LmChatModel {

    private val log = Logger.getInstance(AnthropicChatModel::class.java)

    override val vendor = "anthropic"

    override suspend fun sendRequest(
        messages: List<LmChatMessage>,
        options: LmChatRequestOptions
    ): LmChatResponse {
        log.info("[LmAnthropic] sendRequest called: model=$id, messageCount=${messages.size}, maxTokens=${options.maxTokens}, temperature=${options.temperature}")
        return StreamingLmChatResponse(flow {
            val response = AnthropicHttpClient.getInstance().chatCompletion(id, messages, options)
            emit(response)
        })
    }
}
