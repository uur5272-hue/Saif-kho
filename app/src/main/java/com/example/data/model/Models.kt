package com.example.data.model

enum class MediaType {
    IMAGE,
    VIDEO
}

enum class MediaStatus {
    PENDING,
    COMPLETED,
    FAILED
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val action: AppControlAction? = null,
    val imageBase64: String? = null,
    val isError: Boolean = false
)

sealed class AppControlAction {
    data class LaunchApp(val appName: String, val packageName: String) : AppControlAction()
    data class OpenWebsite(val url: String, val title: String) : AppControlAction()
    data class OpenSetting(val action: String, val label: String) : AppControlAction()
    data class InitiateCall(val contactName: String, val phoneNumber: String) : AppControlAction()
    data class DraftMessage(val contactName: String, val phoneNumber: String, val message: String) : AppControlAction()
    data class SearchWeb(val query: String) : AppControlAction()
}

data class GeneratedMediaItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: MediaType,
    val prompt: String,
    val style: String = "",
    val resolution: String = "720p",
    val base64Data: String? = null,
    val mediaUri: String? = null,
    val originalImageUri: String? = null,
    val status: MediaStatus = MediaStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)

data class LanguageOption(
    val code: String,
    val displayName: String,
    val localeTag: String
)

val SupportedLanguages = listOf(
    LanguageOption("en", "English", "en-US"),
    LanguageOption("hi", "Hindi (हिन्दी)", "hi-IN"),
    LanguageOption("ur", "Urdu (اردو)", "ur-PK"),
    LanguageOption("es", "Spanish (Español)", "es-ES"),
    LanguageOption("ar", "Arabic (العربية)", "ar-SA"),
    LanguageOption("fr", "French (Français)", "fr-FR")
)

enum class VideoResolution(val label: String, val width: Int, val height: Int) {
    RES_360P("360p (Fastest)", 640, 360),
    RES_480P("480p (Balanced)", 854, 480),
    RES_720P("720p (Default HD)", 1280, 720),
    RES_1080P("1080p (Full HD)", 1920, 1080)
}
