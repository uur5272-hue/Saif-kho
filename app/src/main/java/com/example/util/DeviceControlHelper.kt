package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import com.example.data.model.AppControlAction
import java.util.Locale

object DeviceControlHelper {

    // Common app names mapped to common package names
    private val knownAppPackages = mapOf(
        "youtube" to "com.google.android.youtube",
        "whatsapp" to "com.whatsapp",
        "chrome" to "com.android.chrome",
        "maps" to "com.google.android.apps.maps",
        "camera" to "camera_intent",
        "gallery" to "gallery_intent",
        "calculator" to "calculator_intent",
        "clock" to "clock_intent",
        "play store" to "com.android.vending",
        "spotify" to "com.spotify.music",
        "instagram" to "com.instagram.android",
        "telegram" to "org.telegram.messenger",
        "twitter" to "com.twitter.android",
        "x" to "com.twitter.android",
        "gmail" to "com.google.android.gm",
        "drive" to "com.google.android.apps.docs"
    )

    fun parsePotentialAction(query: String): AppControlAction? {
        val trimmed = query.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        // 1. Phone Call commands
        val callRegex = Regex("^(?:call|dial|phone|make a call to)\\s+([a-zA-Z0-9+\\s]+)$", RegexOption.IGNORE_CASE)
        val callMatch = callRegex.find(trimmed)
        if (callMatch != null) {
            val target = callMatch.groupValues[1].trim()
            return AppControlAction.InitiateCall(contactName = target, phoneNumber = target)
        }

        // 2. Messaging commands: e.g. "message [target]: [body]" or "send message to [target] saying [body]"
        val msgRegex = Regex("^(?:send message to|message|sms|text)\\s+([a-zA-Z0-9+\\s]+?)(?:\\s*:\\s*|\\s+saying\\s+)(.+)$", RegexOption.IGNORE_CASE)
        val msgMatch = msgRegex.find(trimmed)
        if (msgMatch != null) {
            val target = msgMatch.groupValues[1].trim()
            val text = msgMatch.groupValues[2].trim()
            return AppControlAction.DraftMessage(contactName = target, phoneNumber = target, message = text)
        }

        // 3. Settings commands
        if (lower.startsWith("open wifi") || lower == "wifi settings") {
            return AppControlAction.OpenSetting(Settings.ACTION_WIFI_SETTINGS, "Wi-Fi Settings")
        }
        if (lower.startsWith("open bluetooth") || lower == "bluetooth settings") {
            return AppControlAction.OpenSetting(Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth Settings")
        }
        if (lower.startsWith("open display") || lower == "display settings") {
            return AppControlAction.OpenSetting(Settings.ACTION_DISPLAY_SETTINGS, "Display Settings")
        }
        if (lower.startsWith("open sound") || lower == "sound settings") {
            return AppControlAction.OpenSetting(Settings.ACTION_SOUND_SETTINGS, "Sound Settings")
        }
        if (lower.startsWith("open app settings") || lower == "applications settings") {
            return AppControlAction.OpenSetting(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS, "App Settings")
        }
        if (lower == "open settings" || lower == "settings") {
            return AppControlAction.OpenSetting(Settings.ACTION_SETTINGS, "System Settings")
        }

        // 4. Open Website
        if (lower.startsWith("open website") || lower.startsWith("go to http") || lower.startsWith("browse ")) {
            val urlRaw = lower.removePrefix("open website").removePrefix("browse ").trim()
            val formattedUrl = if (!urlRaw.startsWith("http://") && !urlRaw.startsWith("https://")) {
                "https://$urlRaw"
            } else urlRaw
            return AppControlAction.OpenWebsite(formattedUrl, urlRaw)
        }
        if (lower.matches(Regex("^(?:open|go to)\\s+([a-zA-Z0-9\\-]+\\.(?:com|org|net|io|edu|gov|co|in))/?.*$"))) {
            val domain = lower.substringAfter("open ").substringAfter("go to ").trim()
            val formattedUrl = if (!domain.startsWith("http://") && !domain.startsWith("https://")) {
                "https://$domain"
            } else domain
            return AppControlAction.OpenWebsite(formattedUrl, domain)
        }

        // 5. Open Installed App or Game
        val openAppRegex = Regex("^(?:open|launch|start|run)\\s+([a-zA-Z0-9\\s]+)$", RegexOption.IGNORE_CASE)
        val openMatch = openAppRegex.find(lower)
        if (openMatch != null) {
            val appName = openMatch.groupValues[1].trim()
            // Ignore generic keywords
            if (appName != "camera" && appName != "settings" && !appName.contains("website")) {
                return AppControlAction.LaunchApp(appName = appName, packageName = knownAppPackages[appName] ?: "")
            }
        }

        return null
    }

    fun executeLaunchApp(context: Context, appName: String, packageName: String): Pair<Boolean, String> {
        val pm = context.packageManager

        // Special system handlers
        when (appName.lowercase(Locale.ROOT)) {
            "camera" -> {
                val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                if (intent.resolveActivity(pm) != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return true to "Opened Camera"
                }
            }
            "calculator" -> {
                // Try standard calculator intents
                val calcIntents = listOf(
                    Intent().setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR),
                    pm.getLaunchIntentForPackage("com.google.android.calculator"),
                    pm.getLaunchIntentForPackage("com.android.calculator2")
                )
                for (ci in calcIntents) {
                    if (ci != null && ci.resolveActivity(pm) != null) {
                        ci.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(ci)
                        return true to "Opened Calculator"
                    }
                }
            }
        }

        // If package name known or provided
        if (packageName.isNotBlank()) {
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return true to "Opened $appName"
            }
        }

        // Search installed applications matching name
        try {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val match = installedApps.firstOrNull { appInfo ->
                val label = pm.getApplicationLabel(appInfo).toString().lowercase(Locale.ROOT)
                label == appName.lowercase(Locale.ROOT) || label.contains(appName.lowercase(Locale.ROOT))
            }

            if (match != null) {
                val intent = pm.getLaunchIntentForPackage(match.packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return true to "Opened ${pm.getApplicationLabel(match)}"
                }
            }
        } catch (e: Exception) {
            // fallback
        }

        return false to "App '$appName' is not installed on this device."
    }

    fun executeOpenWebsite(context: Context, url: String): Pair<Boolean, String> {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true to "Opened $url"
        } catch (e: Exception) {
            false to "Unable to open link: ${e.localizedMessage}"
        }
    }

    fun executeOpenSetting(context: Context, action: String, label: String): Pair<Boolean, String> {
        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true to "Opened $label"
        } catch (e: Exception) {
            false to "Unable to open $label"
        }
    }

    fun executeInitiateCall(context: Context, phoneNumber: String): Pair<Boolean, String> {
        return try {
            // Use ACTION_DIAL so user can review the number securely in the dialer
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${Uri.encode(phoneNumber.filter { it.isDigit() || it == '+' })}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            true to "Dialer opened for $phoneNumber"
        } catch (e: Exception) {
            false to "Failed to open dialer: ${e.localizedMessage}"
        }
    }

    fun executeDraftMessage(context: Context, phoneNumber: String, message: String): Pair<Boolean, String> {
        return try {
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${Uri.encode(phoneNumber)}")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(smsIntent)
            true to "Opened messaging app to send to $phoneNumber"
        } catch (e: Exception) {
            false to "Failed to open messaging app: ${e.localizedMessage}"
        }
    }
}
