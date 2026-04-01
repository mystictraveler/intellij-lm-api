package com.intellij.lm

enum class LmChatRole {
    USER, ASSISTANT, SYSTEM
}

data class LmChatMessage(
    val role: LmChatRole,
    val content: String
) {
    companion object {
        fun user(content: String) = LmChatMessage(LmChatRole.USER, content)
        fun assistant(content: String) = LmChatMessage(LmChatRole.ASSISTANT, content)
        fun system(content: String) = LmChatMessage(LmChatRole.SYSTEM, content)
    }
}
