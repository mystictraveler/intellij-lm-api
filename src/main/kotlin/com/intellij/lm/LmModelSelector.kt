package com.intellij.lm

/**
 * Selector for filtering available models.
 * Equivalent to VS Code's LanguageModelChatSelector.
 * All fields are optional — omitted fields match any value.
 */
data class LmModelSelector(
    val family: String? = null,
    val vendor: String? = null,
    val id: String? = null
)
