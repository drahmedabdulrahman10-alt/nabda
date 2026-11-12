package io.github.drahmedabdulrahman10_alt.nabda.data.ai

import io.github.drahmedabdulrahman10_alt.nabda.data.model.TaskPriority
import io.github.drahmedabdulrahman10_alt.nabda.data.model.UserSettings
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ParsedTaskDto(
    val title: String,
    val priority: TaskPriority
)

/**
 * AI Assistant service supporting both Google AI Studio (Gemini) and OpenRouter
 * to parse natural language or voice-dictated Arabic text into prioritized daily tasks.
 */
class AiTaskAssistantService {

    companion object {
        const val PROVIDER_GEMINI = "GEMINI"
        const val PROVIDER_OPENROUTER = "OPENROUTER"

        const val DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"
        const val DEFAULT_OPENROUTER_MODEL = "google/gemini-2.5-flash"

        private const val SYSTEM_PROMPT =
            "أنت مساعد ذكي داخل تطبيق 'نبضة' لإدارة المهام. قم بتحليل النص التالي الذي أملاه المستخدم واستخرج منه قائمة المهام اليومية مع تحديد أولوية كل مهمة (HIGH أو MEDIUM أو LOW) بناءً على أهميتها وسياق الكلام. أخرج النتيجة بصيغة JSON فقط بدون أي نص إضافي أو علامات Markdown، بهذا الشكل:\n" +
                "[{\"title\": \"عنوان المهمة\", \"priority\": \"HIGH\"}]"
    }

    /**
     * Parses free-form Arabic text or speech transcript into a list of [ParsedTaskDto].
     */
    suspend fun parseNaturalLanguageTasks(
        userInput: String,
        settings: UserSettings
    ): Result<List<ParsedTaskDto>> = withContext(Dispatchers.IO) {
        val cleanInput = userInput.trim()
        if (cleanInput.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("الرجاء إدخال نص أو التحدث بالمهام أولاً."))
        }

        val provider = settings.normalizedProvider()
        val apiKey = if (provider == PROVIDER_OPENROUTER) {
            settings.openRouterApiKey.trim()
        } else {
            settings.geminiApiKey.trim()
        }

        if (apiKey.isEmpty()) {
            val providerName = if (provider == PROVIDER_OPENROUTER) "OpenRouter" else "Google AI Studio (Gemini)"
            return@withContext Result.failure(
                IllegalStateException("مفتاح الـ API لـ $providerName غير مُدخل. يرجى إضافته من شاشة الإعدادات أولاً.")
            )
        }

        val model = settings.aiModelName.trim().ifBlank {
            if (provider == PROVIDER_OPENROUTER) DEFAULT_OPENROUTER_MODEL else DEFAULT_GEMINI_MODEL
        }

        try {
            val rawResponseText = if (provider == PROVIDER_OPENROUTER) {
                callOpenRouter(apiKey, model, SYSTEM_PROMPT, cleanInput)
            } else {
                callGemini(apiKey, model, SYSTEM_PROMPT, cleanInput)
            }

            val parsedTasks = parseTasksJsonArray(rawResponseText)
            if (parsedTasks.isEmpty()) {
                Result.failure(IllegalStateException("لم يتم العثور على مهام واضحة في النص المدخل."))
            } else {
                Result.success(parsedTasks)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sends a lightweight test ping to verify the selected AI provider, API key, and model name.
     */
    suspend fun testConnection(settings: UserSettings): Result<String> = withContext(Dispatchers.IO) {
        val testSample = "مراجعة ورد القرآن الكريم صباحاً ضروري"
        val res = parseNaturalLanguageTasks(testSample, settings)
        res.fold(
            onSuccess = { tasks ->
                val providerLabel = if (settings.normalizedProvider() == PROVIDER_OPENROUTER) {
                    "OpenRouter"
                } else {
                    "Google Gemini"
                }
                Result.success(
                    "تم الاتصال بنجاح عبر $providerLabel (${settings.aiModelName.ifBlank { DEFAULT_GEMINI_MODEL }}) • استجابة تجريبية: ${tasks.firstOrNull()?.title ?: "جاهز"} "
                )
            },
            onFailure = { err ->
                Result.failure(err)
            }
        )
    }

    private fun callGemini(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userText: String
    ): String {
        val cleanModel = model.removePrefix("models/").trim()
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent"
        val payload = JSONObject().apply {
            put(
                "systemInstruction",
                JSONObject().apply {
                    put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", systemPrompt))
                    )
                }
            )
            put(
                "contents",
                JSONArray().put(
                    JSONObject().apply {
                        put("role", "user")
                        put(
                            "parts",
                            JSONArray().put(JSONObject().put("text", userText))
                        )
                    }
                )
            )
            put(
                "generationConfig",
                JSONObject().apply {
                    put("temperature", 0.2)
                }
            )
        }

        val responseStr = executeJsonPost(
            urlString = endpoint,
            headers = mapOf(
                // The key goes in a header so it never appears in URLs or logs.
                "x-goog-api-key" to apiKey,
                "Content-Type" to "application/json; charset=utf-8"
            ),
            bodyJson = payload.toString()
        )

        val root = JSONObject(responseStr)
        val candidates = root.optJSONArray("candidates")
            ?: throw IllegalStateException("لم يُرجع Gemini أي استجابة صالحة.")
        if (candidates.length() == 0) {
            throw IllegalStateException("قائمة الاستجابات من Gemini فارغة.")
        }
        val content = candidates.getJSONObject(0).optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val textBuilder = StringBuilder()
        if (parts != null) {
            for (i in 0 until parts.length()) {
                textBuilder.append(parts.getJSONObject(i).optString("text", ""))
            }
        }
        return textBuilder.toString()
    }

    private fun callOpenRouter(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userText: String
    ): String {
        val endpoint = "https://openrouter.ai/api/v1/chat/completions"
        val messages = JSONArray().apply {
            put(
                JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                }
            )
            put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", userText)
                }
            )
        }
        val payload = JSONObject().apply {
            put("model", model.trim())
            put("messages", messages)
            put("temperature", 0.2)
        }

        val responseStr = executeJsonPost(
            urlString = endpoint,
            headers = mapOf(
                "Authorization" to "Bearer $apiKey",
                "Content-Type" to "application/json; charset=utf-8",
                "HTTP-Referer" to "https://nabda.app",
                "X-Title" to "Nabda App"
            ),
            bodyJson = payload.toString()
        )

        val root = JSONObject(responseStr)
        val choices = root.optJSONArray("choices")
            ?: throw IllegalStateException("لم يُرجع OpenRouter أي استجابة صالحة.")
        if (choices.length() == 0) {
            throw IllegalStateException("قائمة الخيارات من OpenRouter فارغة.")
        }
        val messageObj = choices.getJSONObject(0).optJSONObject("message")
        return messageObj?.optString("content", "") ?: ""
    }

    private fun executeJsonPost(
        urlString: String,
        headers: Map<String, String>,
        bodyJson: String
    ): String {
        val url = URL(urlString)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 25000
            doInput = true
            doOutput = true
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }

        try {
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(bodyJson)
                writer.flush()
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseBody = if (stream != null) {
                BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } else {
                ""
            }

            if (status !in 200..299) {
                val friendlyError = try {
                    val errJson = JSONObject(responseBody)
                    errJson.optJSONObject("error")?.optString("message")
                        ?.takeIf { it.isNotBlank() }
                        ?: responseBody.take(200)
                } catch (ignored: Exception) {
                    responseBody.take(200)
                }
                throw IllegalStateException("خطأ من الخادم ($status): $friendlyError")
            }

            return responseBody
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Strips Markdown code fences (` ```json ` / ` ``` `) and extracts the JSON array of tasks.
     */
    internal fun parseTasksJsonArray(rawText: String): List<ParsedTaskDto> {
        var cleaned = rawText.trim()
            .replace(Regex("```json", RegexOption.IGNORE_CASE), "")
            .replace("```", "")
            .trim()

        val startBracket = cleaned.indexOf('[')
        val endBracket = cleaned.lastIndexOf(']')
        if (startBracket != -1 && endBracket != -1 && endBracket > startBracket) {
            cleaned = cleaned.substring(startBracket, endBracket + 1)
        }

        val jsonArray = JSONArray(cleaned)
        val results = mutableListOf<ParsedTaskDto>()
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.optJSONObject(i) ?: continue
            val title = item.optString("title", "").trim()
            val priorityStr = item.optString("priority", "MEDIUM").trim()
            if (title.isNotBlank()) {
                results.add(
                    ParsedTaskDto(
                        title = title.take(300),
                        priority = TaskPriority.fromString(priorityStr)
                    )
                )
            }
        }
        return results
    }
}
