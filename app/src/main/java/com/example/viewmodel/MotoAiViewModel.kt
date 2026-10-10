package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiApiClient
import com.example.data.api.GeminiResult
import com.example.data.model.AppControlAction
import com.example.data.model.ChatMessage
import com.example.data.model.GeneratedMediaItem
import com.example.data.model.MediaStatus
import com.example.data.model.MediaType
import com.example.data.preferences.MotoPreferences
import com.example.service.MotoAiOverlayService
import com.example.service.MotoAiWakeWordService
import com.example.util.DeviceControlHelper
import com.example.util.MediaStoreHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class MotoAiViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    val preferences = MotoPreferences(application)
    private val geminiClient = GeminiApiClient()

    // Chat state
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _liveVoiceInput = MutableStateFlow("")
    val liveVoiceInput: StateFlow<String> = _liveVoiceInput.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _pendingAction = MutableStateFlow<AppControlAction?>(null)
    val pendingAction: StateFlow<AppControlAction?> = _pendingAction.asStateFlow()

    // Image & Video Studio state
    private val _studioItems = MutableStateFlow<List<GeneratedMediaItem>>(emptyList())
    val studioItems: StateFlow<List<GeneratedMediaItem>> = _studioItems.asStateFlow()

    private val _isStudioGenerating = MutableStateFlow(false)
    val isStudioGenerating: StateFlow<Boolean> = _isStudioGenerating.asStateFlow()

    private val _studioStatusMessage = MutableStateFlow<String?>(null)
    val studioStatusMessage: StateFlow<String?> = _studioStatusMessage.asStateFlow()

    // Native Speech Recognizer & TTS
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    init {
        // Welcome message from MOTO AI
        val welcomeMsg = ChatMessage(
            text = "Greetings! I am MOTO AI, configured for Saif Ali Khan. " +
                    "I can chat in multiple languages, launch your apps, dial contacts, draft messages, or generate stunning images and videos. How can I assist you?",
            isUser = false
        )
        _messages.value = listOf(welcomeMsg)

        // Init TextToSpeech
        try {
            textToSpeech = TextToSpeech(application, this)
        } catch (e: Exception) {}

        // Apply wake-word or floating button service if previously active
        if (preferences.isWakeWordEnabled) {
            startWakeWordService()
        }
        if (preferences.isFloatingButtonEnabled) {
            startOverlayService()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            updateTtsLanguage()
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }
                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
        }
    }

    fun updateTtsLanguage() {
        if (!isTtsInitialized || textToSpeech == null) return
        val langCode = preferences.selectedLanguage
        val locale = when (langCode) {
            "hi" -> Locale("hi", "IN")
            "ur" -> Locale("ur", "PK")
            "es" -> Locale("es", "ES")
            "ar" -> Locale("ar", "SA")
            "fr" -> Locale("fr", "FR")
            else -> Locale.US
        }
        textToSpeech?.language = locale
        textToSpeech?.setPitch(preferences.speechPitch)
        textToSpeech?.setSpeechRate(preferences.speechRate)
    }

    fun speak(text: String) {
        if (!preferences.isVoiceEnabled || !isTtsInitialized || textToSpeech == null) return
        updateTtsLanguage()
        val utteranceId = "moto_tts_${System.currentTimeMillis()}"
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        _isSpeaking.value = false
    }

    // Owner check rule:
    // "If asked “Who created you?”, “Who is your owner?”, or “Tumhara owner kaun hai?”, reply:
    // “I am MOTO AI, configured for Saif Ali Khan.” Do not claim that Saif Ali Khan created the underlying Gemini model or other third-party AI models."
    private fun isOwnerQuestion(query: String): Boolean {
        val lower = query.lowercase(Locale.ROOT).trim()
        val ownerTriggers = listOf(
            "who created you",
            "who is your owner",
            "who is owner",
            "who made you",
            "tumhara owner kaun hai",
            "tumhara malik kaun hai",
            "aapka owner kaun hai",
            "apka owner kaun hai",
            "who owns you",
            "creator kaun hai",
            "owner kaun hai",
            "who is your creator"
        )
        return ownerTriggers.any { lower.contains(it) }
    }

    fun sendMessage(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        stopSpeaking()

        // Append user turn
        val userMsg = ChatMessage(text = trimmed, isUser = true)
        _messages.value = _messages.value + userMsg

        // 1. Direct Owner Question Check
        if (isOwnerQuestion(trimmed)) {
            val ownerReply = "I am MOTO AI, configured for Saif Ali Khan."
            val replyMsg = ChatMessage(text = ownerReply, isUser = false)
            _messages.value = _messages.value + replyMsg
            speak(ownerReply)
            return
        }

        // 2. Check Device Control Commands (Open apps, call, message, settings)
        val action = DeviceControlHelper.parsePotentialAction(trimmed)
        if (action != null) {
            handleDeviceAction(action)
            return
        }

        // 3. Conversational / Gemini AI request
        processWithGemini(trimmed)
    }

    private fun handleDeviceAction(action: AppControlAction) {
        when (action) {
            is AppControlAction.InitiateCall,
            is AppControlAction.DraftMessage -> {
                // Requires explicit user confirmation dialog
                _pendingAction.value = action
            }
            is AppControlAction.LaunchApp -> {
                val (success, message) = DeviceControlHelper.executeLaunchApp(
                    getApplication(),
                    action.appName,
                    action.packageName
                )
                val reply = ChatMessage(text = message, isUser = false, action = action)
                _messages.value = _messages.value + reply
                speak(message)
            }
            is AppControlAction.OpenWebsite -> {
                val (success, message) = DeviceControlHelper.executeOpenWebsite(getApplication(), action.url)
                val reply = ChatMessage(text = message, isUser = false, action = action)
                _messages.value = _messages.value + reply
                speak(message)
            }
            is AppControlAction.OpenSetting -> {
                val (success, message) = DeviceControlHelper.executeOpenSetting(getApplication(), action.action, action.label)
                val reply = ChatMessage(text = message, isUser = false, action = action)
                _messages.value = _messages.value + reply
                speak(message)
            }
            is AppControlAction.SearchWeb -> {
                val searchUrl = "https://www.google.com/search?q=${android.net.Uri.encode(action.query)}"
                DeviceControlHelper.executeOpenWebsite(getApplication(), searchUrl)
                val reply = ChatMessage(text = "Searching web for: ${action.query}", isUser = false)
                _messages.value = _messages.value + reply
            }
        }
    }

    fun confirmPendingAction() {
        val action = _pendingAction.value ?: return
        _pendingAction.value = null

        when (action) {
            is AppControlAction.InitiateCall -> {
                val (success, message) = DeviceControlHelper.executeInitiateCall(getApplication(), action.phoneNumber)
                val reply = ChatMessage(text = message, isUser = false)
                _messages.value = _messages.value + reply
                speak(message)
            }
            is AppControlAction.DraftMessage -> {
                val (success, message) = DeviceControlHelper.executeDraftMessage(
                    getApplication(),
                    action.phoneNumber,
                    action.message
                )
                val reply = ChatMessage(text = message, isUser = false)
                _messages.value = _messages.value + reply
                speak(message)
            }
            else -> {}
        }
    }

    fun dismissPendingAction() {
        _pendingAction.value = null
        val cancelMsg = ChatMessage(text = "Action cancelled.", isUser = false)
        _messages.value = _messages.value + cancelMsg
    }

    private fun processWithGemini(query: String) {
        viewModelScope.launch {
            _isGenerating.value = true
            val apiKey = preferences.getEffectiveApiKey()

            val history = _messages.value.map { it.text to it.isUser }
            val result = geminiClient.generateChatResponse(
                apiKey = apiKey,
                prompt = query,
                history = history,
                languageCode = preferences.selectedLanguage
            )

            _isGenerating.value = false

            when (result) {
                is GeminiResult.Success -> {
                    val reply = ChatMessage(text = result.data, isUser = false)
                    _messages.value = _messages.value + reply
                    speak(result.data)
                }
                is GeminiResult.Error -> {
                    val errorText = if (result.isMissingKey) {
                        "⚠️ ${result.message}\nPlease open Settings to configure your API key."
                    } else if (result.isQuotaError) {
                        "⚠️ Gemini API Quota limit reached. Please wait a moment or verify your quota."
                    } else {
                        "⚠️ Unable to reach MOTO AI core: ${result.message}"
                    }
                    val errorMsg = ChatMessage(text = errorText, isUser = false, isError = true)
                    _messages.value = _messages.value + errorMsg
                    speak("I encountered an issue connecting to the AI core. Please check your connection or API key.")
                }
            }
        }
    }

    // Voice recognition handling
    fun startVoiceRecognition(context: Context) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            val err = ChatMessage(text = "Speech recognition is not available on this device.", isUser = false, isError = true)
            _messages.value = _messages.value + err
            return
        }

        stopSpeaking()
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
                _liveVoiceInput.value = "Listening..."
            }
            override fun onBeginningOfSpeech() {
                _liveVoiceInput.value = "Detecting voice..."
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                _isListening.value = false
            }
            override fun onError(error: Int) {
                _isListening.value = false
                _liveVoiceInput.value = ""
            }
            override fun onResults(results: Bundle?) {
                _isListening.value = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognized = matches?.firstOrNull() ?: ""
                _liveVoiceInput.value = ""
                if (recognized.isNotBlank()) {
                    sendMessage(recognized)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull()
                if (!partial.isNullOrBlank()) {
                    _liveVoiceInput.value = partial
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            val langTag = when (preferences.selectedLanguage) {
                "hi" -> "hi-IN"
                "ur" -> "ur-PK"
                "es" -> "es-ES"
                else -> "en-US"
            }
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
        }
    }

    fun stopVoiceRecognition() {
        speechRecognizer?.stopListening()
        _isListening.value = false
        _liveVoiceInput.value = ""
    }

    // Image Studio: Generation
    fun generateImage(prompt: String, style: String, aspectRatio: String) {
        viewModelScope.launch {
            _isStudioGenerating.value = true
            _studioStatusMessage.value = "Synthesizing visual matrix with Gemini 2.5 Flash Image..."

            val apiKey = preferences.getEffectiveApiKey()
            val result = geminiClient.generateImage(apiKey, prompt, style, aspectRatio)

            _isStudioGenerating.value = false

            when (result) {
                is GeminiResult.Success -> {
                    _studioStatusMessage.value = "Image generated successfully!"
                    val item = GeneratedMediaItem(
                        type = MediaType.IMAGE,
                        prompt = prompt,
                        style = style,
                        resolution = aspectRatio,
                        base64Data = result.data.base64Data,
                        status = MediaStatus.COMPLETED
                    )
                    _studioItems.value = listOf(item) + _studioItems.value

                    // Auto-save to gallery if enabled
                    if (preferences.autoSaveToGallery) {
                        saveMediaItemToGallery(item)
                    }
                }
                is GeminiResult.Error -> {
                    _studioStatusMessage.value = "Failed: ${result.message}"
                    val failedItem = GeneratedMediaItem(
                        type = MediaType.IMAGE,
                        prompt = prompt,
                        style = style,
                        status = MediaStatus.FAILED,
                        errorMessage = result.message
                    )
                    _studioItems.value = listOf(failedItem) + _studioItems.value
                }
            }
        }
    }

    // Photo Editing with Instruction
    fun editPhoto(sourceBase64: String, instructions: String, originalUri: String?) {
        viewModelScope.launch {
            _isStudioGenerating.value = true
            _studioStatusMessage.value = "Applying neural photo edits: '$instructions'..."

            val apiKey = preferences.getEffectiveApiKey()
            val result = geminiClient.editPhotoWithInstruction(apiKey, sourceBase64, instructions)

            _isStudioGenerating.value = false

            when (result) {
                is GeminiResult.Success -> {
                    _studioStatusMessage.value = "Photo edit applied successfully!"
                    val item = GeneratedMediaItem(
                        type = MediaType.IMAGE,
                        prompt = instructions,
                        base64Data = result.data.base64Data,
                        originalImageUri = originalUri,
                        status = MediaStatus.COMPLETED
                    )
                    _studioItems.value = listOf(item) + _studioItems.value

                    if (preferences.autoSaveToGallery) {
                        saveMediaItemToGallery(item)
                    }
                }
                is GeminiResult.Error -> {
                    _studioStatusMessage.value = "Edit Failed: ${result.message}"
                    val failedItem = GeneratedMediaItem(
                        type = MediaType.IMAGE,
                        prompt = instructions,
                        originalImageUri = originalUri,
                        status = MediaStatus.FAILED,
                        errorMessage = result.message
                    )
                    _studioItems.value = listOf(failedItem) + _studioItems.value
                }
            }
        }
    }

    // Video Studio: Text / Image to Video
    fun generateVideo(prompt: String, resolution: String, sourceImageBase64: String? = null) {
        viewModelScope.launch {
            _isStudioGenerating.value = true
            _studioStatusMessage.value = "Initializing Veo video engine ($resolution)..."

            val apiKey = preferences.getEffectiveApiKey()
            val result = geminiClient.generateVideo(apiKey, prompt, resolution, sourceImageBase64)

            _isStudioGenerating.value = false

            when (result) {
                is GeminiResult.Success -> {
                    _studioStatusMessage.value = "Video request submitted (${result.data})"
                    val item = GeneratedMediaItem(
                        type = MediaType.VIDEO,
                        prompt = prompt,
                        resolution = resolution,
                        status = MediaStatus.COMPLETED
                    )
                    _studioItems.value = listOf(item) + _studioItems.value
                }
                is GeminiResult.Error -> {
                    _studioStatusMessage.value = "Video Error: ${result.message}"
                    val failedItem = GeneratedMediaItem(
                        type = MediaType.VIDEO,
                        prompt = prompt,
                        resolution = resolution,
                        status = MediaStatus.FAILED,
                        errorMessage = result.message
                    )
                    _studioItems.value = listOf(failedItem) + _studioItems.value
                }
            }
        }
    }

    fun saveMediaItemToGallery(item: GeneratedMediaItem) {
        val base64 = item.base64Data ?: return
        viewModelScope.launch {
            val result = MediaStoreHelper.saveBase64ImageToGallery(
                getApplication(),
                base64,
                "MOTO_AI_${System.currentTimeMillis()}"
            )
            result.onSuccess { uri ->
                _studioStatusMessage.value = "Saved to Gallery: Pictures/MOTO_AI"
                // Update item mediaUri
                _studioItems.value = _studioItems.value.map {
                    if (it.id == item.id) it.copy(mediaUri = uri.toString()) else it
                }
            }.onFailure {
                _studioStatusMessage.value = "Failed to save: ${it.localizedMessage}"
            }
        }
    }

    // Wake-word service management
    fun setWakeWordEnabled(enabled: Boolean) {
        preferences.isWakeWordEnabled = enabled
        if (enabled) {
            startWakeWordService()
        } else {
            stopWakeWordService()
        }
    }

    private fun startWakeWordService() {
        try {
            val intent = Intent(getApplication(), MotoAiWakeWordService::class.java).apply {
                action = MotoAiWakeWordService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getApplication<Application>().startForegroundService(intent)
            } else {
                getApplication<Application>().startService(intent)
            }
        } catch (e: Exception) {}
    }

    private fun stopWakeWordService() {
        try {
            val intent = Intent(getApplication(), MotoAiWakeWordService::class.java).apply {
                action = MotoAiWakeWordService.ACTION_STOP
            }
            getApplication<Application>().startService(intent)
        } catch (e: Exception) {}
    }

    // Floating Assistant Overlay management
    fun setFloatingButtonEnabled(enabled: Boolean) {
        preferences.isFloatingButtonEnabled = enabled
        if (enabled) {
            startOverlayService()
        } else {
            stopOverlayService()
        }
    }

    private fun startOverlayService() {
        try {
            val intent = Intent(getApplication(), MotoAiOverlayService::class.java).apply {
                action = MotoAiOverlayService.ACTION_START
            }
            getApplication<Application>().startService(intent)
        } catch (e: Exception) {}
    }

    private fun stopOverlayService() {
        try {
            val intent = Intent(getApplication(), MotoAiOverlayService::class.java).apply {
                action = MotoAiOverlayService.ACTION_STOP
            }
            getApplication<Application>().startService(intent)
        } catch (e: Exception) {}
    }

    fun clearChat() {
        _messages.value = emptyList()
    }

    override fun onCleared() {
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onCleared()
    }
}
