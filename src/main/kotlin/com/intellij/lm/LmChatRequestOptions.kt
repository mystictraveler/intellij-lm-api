package com.intellij.lm

data class LmChatRequestOptions(
    /** Temperature for response generation (0.0 - 2.0) */
    val temperature: Double? = null,
    /** Maximum tokens in the response */
    val maxTokens: Int? = null,
    /** Additional model-specific parameters */
    val metadata: Map<String, Any> = emptyMap()
)
