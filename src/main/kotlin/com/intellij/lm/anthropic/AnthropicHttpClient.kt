package com.intellij.lm.anthropic

import com.intellij.lm.LmChatMessage
import com.intellij.lm.LmChatRequestOptions
import com.intellij.lm.LmChatRole
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.net.HttpConfigurable
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets

class AnthropicHttpClient {

    private val log = Logger.getInstance(AnthropicHttpClient::class.java)
    private val gson = Gson()

    fun chatCompletion(model: String, messages: List<LmChatMessage>, options: LmChatRequestOptions): String {
        val apiKey = resolveApiKey()
            ?: throw IllegalStateException("Anthropic API key not configured. Set ANTHROPIC_API_KEY environment variable.")

        // Separate system message from conversation messages (Anthropic API uses a top-level system param)
        val systemMessage = messages.filter { it.role == LmChatRole.SYSTEM }.joinToString("\n") { it.content }
        val chatMessages = messages.filter { it.role != LmChatRole.SYSTEM }.map { msg ->
            mapOf(
                "role" to when (msg.role) {
                    LmChatRole.USER -> "user"
                    LmChatRole.ASSISTANT -> "assistant"
                    else -> "user"
                },
                "content" to msg.content
            )
        }

        val requestMap = mutableMapOf<String, Any>(
            "model" to model,
            "messages" to chatMessages,
            "max_tokens" to (options.maxTokens ?: 4096)
        )
        if (systemMessage.isNotBlank()) {
            requestMap["system"] = systemMessage
        }
        options.temperature?.let { requestMap["temperature"] = it }

        val url = URI("https://api.anthropic.com/v1/messages").toURL()
        val conn = openConnection(url)
        conn.requestMethod = "POST"
        conn.setRequestProperty("x-api-key", apiKey)
        conn.setRequestProperty("anthropic-version", "2023-06-01")
        conn.setRequestProperty("content-type", "application/json")
        conn.connectTimeout = 30000
        conn.readTimeout = 120000
        conn.doOutput = true

        conn.outputStream.use { it.write(gson.toJson(requestMap).toByteArray(StandardCharsets.UTF_8)) }

        val responseCode = conn.responseCode
        if (responseCode != 200) {
            val errorBody = try { conn.errorStream?.bufferedReader()?.readText() ?: "no error body" } catch (_: Exception) { "unreadable" }
            throw RuntimeException("Anthropic API returned $responseCode: $errorBody")
        }

        val responseBody = conn.inputStream.bufferedReader().readText()
        return extractContent(responseBody)
    }

    private fun extractContent(responseBody: String): String {
        val json = JsonParser.parseString(responseBody).asJsonObject
        val content = json.getAsJsonArray("content") ?: return ""
        val sb = StringBuilder()
        for (block in content) {
            val obj = block.asJsonObject
            if (obj.get("type")?.asString == "text") {
                sb.append(obj.get("text")?.asString ?: "")
            }
        }
        return sb.toString()
    }

    private fun openConnection(url: URL): HttpURLConnection {
        return HttpConfigurable.getInstance().openHttpConnection(url.toString()) as HttpURLConnection
    }

    companion object {
        private var instance: AnthropicHttpClient? = null

        @Synchronized
        fun getInstance(): AnthropicHttpClient {
            if (instance == null) instance = AnthropicHttpClient()
            return instance!!
        }

        /**
         * Resolve the Anthropic API key from environment or system property.
         */
        fun resolveApiKey(): String? {
            return System.getenv("ANTHROPIC_API_KEY")
                ?: System.getProperty("anthropic.api.key")
        }
    }
}
