package com.example.data.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

sealed class GeminiResult<out T> {
    data class Success<out T>(val data: T) : GeminiResult<T>()
    data class Error(val message: String, val isQuotaError: Boolean = false, val isMissingKey: Boolean = false) : GeminiResult<Nothing>()
}

data class GeneratedImageResult(
    val base64Data: String,
    val mimeType: String,
    val accompanyingText: String? = null
)

class GeminiApiClient {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateChatResponse(
        apiKey: String,
        prompt: String,
        history: List<Pair<String, Boolean>>, // (text, isUser)
        languageCode: String = "en"
    ): GeminiResult<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GeminiResult.Error(
                message = "Gemini API key is not configured. Please add your key in Settings or Secrets Panel.",
                isMissingKey = true
            )
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val contentsArray = JSONArray()

            // Include system context in instruction or first exchange
            val systemInstruction = "You are MOTO AI, an intelligent, futuristic personal AI assistant. " +
                    "If asked 'Who created you?', 'Who is your owner?', 'Tumhara owner kaun hai?', or any similar question in any language, " +
                    "you MUST answer: 'I am MOTO AI, configured for Saif Ali Khan.' " +
                    "Do not claim that Saif Ali Khan created the underlying Gemini model or other third-party models. " +
                    "You are helpful, concise, sci-fi themed, and fluent in English, Hindi, Urdu, and other languages. " +
                    "When users ask you to open apps (e.g. YouTube, WhatsApp, Camera, Settings), search websites, call contacts, or send messages, " +
                    "acknowledge clearly so the assistant can trigger system controls."

            // Append history (last 8 turns for efficiency)
            val recentHistory = history.takeLast(8)
            for ((text, isUser) in recentHistory) {
                val contentObj = JSONObject()
                contentObj.put("role", if (isUser) "user" else "model")
                val parts = JSONArray()
                val partObj = JSONObject()
                partObj.put("text", text)
                parts.put(partObj)
                contentObj.put("parts", parts)
                contentsArray.put(contentObj)
            }

            // Current user turn
            val currentContent = JSONObject()
            currentContent.put("role", "user")
            val currentParts = JSONArray()
            val textPart = JSONObject()
            textPart.put("text", prompt)
            currentParts.put(textPart)
            currentContent.put("parts", currentParts)
            contentsArray.put(currentContent)

            val rootJson = JSONObject()
            rootJson.put("contents", contentsArray)

            // System instruction
            val sysInstructionObj = JSONObject()
            val sysParts = JSONArray()
            val sysPart = JSONObject()
            sysPart.put("text", systemInstruction)
            sysParts.put(sysPart)
            sysInstructionObj.put("parts", sysParts)
            rootJson.put("systemInstruction", sysInstructionObj)

            val body = rootJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errMsg = parseErrorMessage(response.code, responseBody)
                return@withContext GeminiResult.Error(
                    message = errMsg,
                    isQuotaError = response.code == 429 || errMsg.contains("quota", ignoreCase = true)
                )
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val firstPart = parts.getJSONObject(0)
                    val replyText = firstPart.optString("text", "")
                    if (replyText.isNotBlank()) {
                        return@withContext GeminiResult.Success(replyText)
                    }
                }
            }

            GeminiResult.Error("No response generated by model.")
        } catch (e: Exception) {
            GeminiResult.Error(e.localizedMessage ?: "Network error during Gemini request.")
        }
    }

    suspend fun generateImage(
        apiKey: String,
        prompt: String,
        style: String = "",
        aspectRatio: String = "1:1"
    ): GeminiResult<GeneratedImageResult> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GeminiResult.Error(
                message = "Gemini API key is required for image generation.",
                isMissingKey = true
            )
        }

        try {
            val fullPrompt = if (style.isNotBlank() && style != "None") {
                "$prompt, in $style style, ultra-detailed, 8k resolution, masterpiece"
            } else {
                prompt
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image:generateContent?key=$apiKey"

            val rootJson = JSONObject()
            val contents = JSONArray()
            val content = JSONObject()
            val parts = JSONArray()
            val textPart = JSONObject()
            textPart.put("text", fullPrompt)
            parts.put(textPart)
            content.put("parts", parts)
            contents.put(content)
            rootJson.put("contents", contents)

            val generationConfig = JSONObject()
            val imageConfig = JSONObject()
            imageConfig.put("aspectRatio", aspectRatio)
            imageConfig.put("imageSize", "1K")
            generationConfig.put("imageConfig", imageConfig)

            val modalities = JSONArray()
            modalities.put("IMAGE")
            modalities.put("TEXT")
            generationConfig.put("responseModalities", modalities)
            rootJson.put("generationConfig", generationConfig)

            val body = rootJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errMsg = parseErrorMessage(response.code, responseBody)
                return@withContext GeminiResult.Error(
                    message = errMsg,
                    isQuotaError = response.code == 429 || errMsg.contains("quota", ignoreCase = true)
                )
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val cContent = candidate.optJSONObject("content")
                val cParts = cContent?.optJSONArray("parts")
                var extractedBase64: String? = null
                var mimeType = "image/png"
                var accompanyingText: String? = null

                if (cParts != null) {
                    for (i in 0 until cParts.length()) {
                        val part = cParts.getJSONObject(i)
                        if (part.has("inlineData")) {
                            val inlineData = part.getJSONObject("inlineData")
                            extractedBase64 = inlineData.optString("data", "")
                            mimeType = inlineData.optString("mimeType", "image/png")
                        } else if (part.has("text")) {
                            accompanyingText = part.optString("text")
                        }
                    }
                }

                if (!extractedBase64.isNullOrBlank()) {
                    return@withContext GeminiResult.Success(
                        GeneratedImageResult(extractedBase64, mimeType, accompanyingText)
                    )
                }
            }

            GeminiResult.Error("Image model completed without returning an image.")
        } catch (e: Exception) {
            GeminiResult.Error(e.localizedMessage ?: "Failed to generate image.")
        }
    }

    suspend fun editPhotoWithInstruction(
        apiKey: String,
        sourceImageBase64: String,
        instructions: String
    ): GeminiResult<GeneratedImageResult> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GeminiResult.Error(
                message = "Gemini API key is required for photo editing.",
                isMissingKey = true
            )
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image:generateContent?key=$apiKey"

            val rootJson = JSONObject()
            val contents = JSONArray()
            val content = JSONObject()
            val parts = JSONArray()

            val textPart = JSONObject()
            textPart.put("text", "Perform this precise photo edit on the input image: $instructions. Output the modified edited photo.")
            parts.put(textPart)

            val imagePart = JSONObject()
            val inlineData = JSONObject()
            inlineData.put("mimeType", "image/jpeg")
            inlineData.put("data", sourceImageBase64)
            imagePart.put("inlineData", inlineData)
            parts.put(imagePart)

            content.put("parts", parts)
            contents.put(content)
            rootJson.put("contents", contents)

            val generationConfig = JSONObject()
            val modalities = JSONArray()
            modalities.put("IMAGE")
            modalities.put("TEXT")
            generationConfig.put("responseModalities", modalities)
            rootJson.put("generationConfig", generationConfig)

            val body = rootJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errMsg = parseErrorMessage(response.code, responseBody)
                return@withContext GeminiResult.Error(
                    message = errMsg,
                    isQuotaError = response.code == 429 || errMsg.contains("quota", ignoreCase = true)
                )
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val cContent = candidate.optJSONObject("content")
                val cParts = cContent?.optJSONArray("parts")
                var extractedBase64: String? = null
                var mimeType = "image/png"
                var accompanyingText: String? = null

                if (cParts != null) {
                    for (i in 0 until cParts.length()) {
                        val part = cParts.getJSONObject(i)
                        if (part.has("inlineData")) {
                            val inline = part.getJSONObject("inlineData")
                            extractedBase64 = inline.optString("data", "")
                            mimeType = inline.optString("mimeType", "image/png")
                        } else if (part.has("text")) {
                            accompanyingText = part.optString("text")
                        }
                    }
                }

                if (!extractedBase64.isNullOrBlank()) {
                    return@withContext GeminiResult.Success(
                        GeneratedImageResult(extractedBase64, mimeType, accompanyingText)
                    )
                }
            }

            GeminiResult.Error("Photo editing completed without returning an edited image.")
        } catch (e: Exception) {
            GeminiResult.Error(e.localizedMessage ?: "Failed to edit photo.")
        }
    }

    suspend fun generateVideo(
        apiKey: String,
        prompt: String,
        resolution: String = "720p",
        sourceImageBase64: String? = null
    ): GeminiResult<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GeminiResult.Error(
                message = "API key is required for video generation.",
                isMissingKey = true
            )
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/veo-3.1-fast-generate-preview:generateVideos?key=$apiKey"

            val rootJson = JSONObject()
            rootJson.put("prompt", prompt)

            val config = JSONObject()
            config.put("numberOfVideos", 1)
            config.put("resolution", resolution)
            config.put("aspectRatio", "16:9")
            rootJson.put("config", config)

            if (!sourceImageBase64.isNullOrBlank()) {
                val imageObj = JSONObject()
                imageObj.put("imageBytes", sourceImageBase64)
                rootJson.put("image", imageObj)
            }

            val body = rootJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errMsg = parseErrorMessage(response.code, responseBody)
                return@withContext GeminiResult.Error(
                    message = errMsg,
                    isQuotaError = response.code == 429 || errMsg.contains("quota", ignoreCase = true)
                )
            }

            val jsonResponse = JSONObject(responseBody)
            val opName = jsonResponse.optString("name", "")
            if (opName.isNotBlank()) {
                return@withContext GeminiResult.Success(opName)
            }

            GeminiResult.Success("Video request accepted. Processing with Veo engine.")
        } catch (e: Exception) {
            GeminiResult.Error(e.localizedMessage ?: "Video generation failed.")
        }
    }

    private fun parseErrorMessage(code: Int, body: String): String {
        return try {
            val json = JSONObject(body)
            val error = json.optJSONObject("error")
            val message = error?.optString("message")
            if (!message.isNullOrBlank()) {
                "API Error ($code): $message"
            } else {
                "HTTP $code: $body"
            }
        } catch (e: Exception) {
            "HTTP $code: $body"
        }
    }
}
