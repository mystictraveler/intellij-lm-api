# intellij-lm-api

Platform Language Model API for IntelliJ -- the equivalent of VS Code's `vscode.lm` namespace.

## Why this exists

VS Code ships a built-in Language Model API (`vscode.lm`) that lets any extension discover, select, and call LLMs without knowing which provider is installed. IntelliJ has no such platform API. Every plugin that wants to talk to an LLM must hard-code its own HTTP client, auth flow, and model catalog.

This plugin fills that gap. It defines a single extension point (`com.intellij.lm.provider`) that LLM providers register with, and an application service (`LmService`) that consumer plugins use to discover and call models. Providers and consumers never depend on each other directly -- they depend on this shared API.

## API surface

### Types at a glance

| Type | Role |
|---|---|
| `LmService` | Application service. Entry point for consumers -- discovers models from all registered providers. |
| `LmProvider` | Extension point interface. Implemented by provider plugins (Copilot, Anthropic, OpenAI, ...). |
| `LmChatModel` | A single model exposed by a provider. Handles `sendRequest`. |
| `LmChatMessage` | A message in a conversation. Has a `role` (`USER`, `ASSISTANT`, `SYSTEM`) and `content`. |
| `LmChatResponse` | Response interface. Exposes a `Flow<String>` stream and a blocking `text()` suspend function. |
| `LmChatRequestOptions` | Optional request parameters: `temperature`, `maxTokens`, `metadata`. |
| `LmModelSelector` | Filter for selecting models by `family`, `vendor`, or `id`. |
| `DefaultLmChatResponse` | Response implementation for providers that return the full text at once. |
| `StreamingLmChatResponse` | Response implementation for providers that stream chunks via a `Flow<String>`. |
| `LmChatRole` | Enum: `USER`, `ASSISTANT`, `SYSTEM`. |

---

### For providers: registering a language model

1. Depend on this plugin (`com.intellij.lm`) in your `plugin.xml`.
2. Implement `LmProvider`.
3. Register via the extension point.

**plugin.xml:**

```xml
<idea-plugin>
    <depends>com.intellij.lm</depends>

    <extensions defaultExtensionNs="com.intellij.lm">
        <provider implementation="com.example.MyLmProvider"/>
    </extensions>
</idea-plugin>
```

**Provider implementation:**

```kotlin
class MyLmProvider : LmProvider {
    override val id = "my-provider"
    override val displayName = "My LLM Service"

    override suspend fun getAvailableModels(): List<LmChatModel> {
        return listOf(MyChatModel())
    }
}

class MyChatModel : LmChatModel {
    override val id = "my-model-v1"
    override val name = "My Model v1"
    override val family = "my-model"
    override val vendor = "my-provider"
    override val maxInputTokens = 128_000

    override suspend fun sendRequest(
        messages: List<LmChatMessage>,
        options: LmChatRequestOptions
    ): LmChatResponse {
        // For a non-streaming provider, return the full text at once:
        return DefaultLmChatResponse {
            callMyApi(messages, options)  // returns String
        }

        // For a streaming provider, emit chunks through a Flow:
        // return StreamingLmChatResponse(flow {
        //     myStreamingApi(messages).forEach { chunk -> emit(chunk) }
        // })
    }
}
```

The extension point is declared `dynamic="true"`, so providers can be loaded and unloaded without restarting the IDE.

---

### For consumers: discovering and calling models

Get the service, select models by family/vendor/id, then send a request.

```kotlin
val lm = LmService.getInstance()

// Select models matching a family
val models = lm.selectChatModels(LmModelSelector(family = "gpt-4o"))
val model = models.firstOrNull() ?: error("No matching model found")

// Build messages
val messages = listOf(
    LmChatMessage.system("You are a helpful coding assistant."),
    LmChatMessage.user("Explain what this function does."),
)

// Send the request
val response = model.sendRequest(messages, LmChatRequestOptions(
    temperature = 0.3,
    maxTokens = 1024,
))
```

**Selecting models:**

`LmModelSelector` fields are all optional. Omitted fields match any value.

```kotlin
// All models from all providers
lm.selectChatModels()

// All models from a specific vendor
lm.selectChatModels(LmModelSelector(vendor = "copilot"))

// A specific model by id
lm.selectChatModels(LmModelSelector(id = "claude-sonnet-4-5-20250514"))
```

---

### Streaming vs blocking response access

Every `LmChatResponse` offers two ways to read the result:

**Blocking** -- suspends until the full response is available:

```kotlin
val fullText: String = response.text()
```

**Streaming** -- collects text chunks as they arrive from the model:

```kotlin
response.textStream.collect { chunk ->
    // Process each chunk incrementally (e.g. append to editor)
    print(chunk)
}
```

Both methods work regardless of whether the provider uses `DefaultLmChatResponse` (non-streaming) or `StreamingLmChatResponse`. The difference is only in latency: streaming lets consumers display partial results before the model finishes.

---

### Response implementations for providers

**`DefaultLmChatResponse`** -- wraps a suspend lambda that returns the complete text. The `textStream` emits a single chunk. Use this when your backend does not support streaming.

```kotlin
DefaultLmChatResponse { myApi.complete(prompt) }
```

**`StreamingLmChatResponse`** -- wraps a `Flow<String>`. The `text()` function collects the entire flow into a single string. Use this when your backend streams chunks.

```kotlin
StreamingLmChatResponse(flow {
    myApi.stream(prompt).forEach { chunk -> emit(chunk) }
})
```

## Build instructions

**Requirements:** JDK 17+, Gradle 8.10+

```bash
# Clone and build
git clone <repo-url>
cd intellij-lm-api
./gradlew build

# Run a sandboxed IDE with the plugin loaded
./gradlew runIde
```

The plugin targets IntelliJ platform 2024.3+ (build range `243` to `253.*`).

## Project structure

```
src/main/kotlin/com/intellij/lm/
    LmService.kt              # Application service (entry point for consumers)
    LmProvider.kt              # Extension point interface (implemented by providers)
    LmChatModel.kt            # Chat model interface
    LmChatMessage.kt          # Message data class + LmChatRole enum
    LmChatResponse.kt         # Response interface (Flow + text())
    LmChatRequestOptions.kt   # Optional request parameters
    LmModelSelector.kt        # Model filter/selector
    DefaultLmChatResponse.kt  # Non-streaming and streaming response implementations

src/main/resources/META-INF/
    plugin.xml                 # Plugin descriptor, service + extension point registration
```

## Known Issues

- **`DefaultLmChatResponse.text()` re-invokes producer** -- Each call to `text()` on a `DefaultLmChatResponse` re-executes the producer lambda, which can result in duplicate API calls. Consumers should call `text()` once and cache the result, or use `StreamingLmChatResponse` with `textStream.collect()`.
- **Anthropic provider connection leak** -- `HttpURLConnection` is not explicitly disconnected in a `finally` block in `AnthropicHttpClient`, which may leak connections on errors in some JVM implementations.

## License

This project is licensed under the [MIT License](LICENSE).
