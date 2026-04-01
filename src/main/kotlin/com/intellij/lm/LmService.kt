package com.intellij.lm

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger

/**
 * Application-level service for discovering and using language models.
 * This is the IntelliJ equivalent of VS Code's `vscode.lm` API.
 *
 * Usage:
 * ```kotlin
 * val lm = LmService.getInstance()
 *
 * // Select a model by family (like vscode.lm.selectChatModels)
 * val models = lm.selectChatModels(LmModelSelector(family = "gpt-4o"))
 * val model = models.first()
 *
 * // Send a chat request
 * val response = model.sendRequest(listOf(
 *     LmChatMessage.user("Explain this code")
 * ))
 *
 * // Get the full response
 * val text = response.text()
 *
 * // Or stream it
 * response.textStream.collect { chunk -> print(chunk) }
 * ```
 */
@Service(Service.Level.APP)
class LmService {

    private val log = Logger.getInstance(LmService::class.java)

    /**
     * Select available chat models matching the given selector.
     * Equivalent to `vscode.lm.selectChatModels()`.
     *
     * @param selector Filter criteria. Pass empty selector to get all models.
     * @return List of matching models from all registered providers.
     */
    suspend fun selectChatModels(selector: LmModelSelector = LmModelSelector()): List<LmChatModel> {
        val allModels = mutableListOf<LmChatModel>()

        for (provider in LmProvider.EP_NAME.extensionList) {
            try {
                allModels.addAll(provider.getAvailableModels())
            } catch (e: Exception) {
                log.warn("Failed to get models from provider ${provider.id}: ${e.message}")
            }
        }

        return allModels.filter { model ->
            (selector.family == null || model.family == selector.family) &&
            (selector.vendor == null || model.vendor == selector.vendor) &&
            (selector.id == null || model.id == selector.id)
        }
    }

    companion object {
        fun getInstance(): LmService {
            return ApplicationManager.getApplication().getService(LmService::class.java)
        }
    }
}
