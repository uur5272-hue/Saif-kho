package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SupportedLanguages
import com.example.data.model.VideoResolution
import com.example.service.MotoAiWakeWordService
import com.example.ui.components.MotoLogo
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SpaceDark
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MotoAiViewModel

@Composable
fun SettingsScreen(
    viewModel: MotoAiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = viewModel.preferences

    var customApiKeyInput by remember { mutableStateOf(prefs.customApiKey) }
    var selectedLanguage by remember { mutableStateOf(prefs.selectedLanguage) }
    var speechRate by remember { mutableFloatStateOf(prefs.speechRate) }
    var speechPitch by remember { mutableFloatStateOf(prefs.speechPitch) }
    var wakeWordEnabled by remember { mutableStateOf(prefs.isWakeWordEnabled) }
    var floatingButtonEnabled by remember { mutableStateOf(prefs.isFloatingButtonEnabled) }
    var selectedVideoRes by remember { mutableStateOf(prefs.defaultVideoResolution) }

    val hasActiveApiKey = prefs.getEffectiveApiKey().isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepVoid)
    ) {
        // Header
        Surface(
            color = SpaceDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "MOTO AI SYSTEM CONFIGURATION",
                        color = NeonCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Fine-tune models, voice engine, permissions & hardware services",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. API Security & Key Configuration
            item {
                SettingsSectionCard(
                    title = "Google Gemini API Connection",
                    icon = Icons.Default.Key
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (hasActiveApiKey) SuccessEmerald.copy(alpha = 0.15f) else ErrorCrimson.copy(alpha = 0.15f))
                            .border(1.dp, if (hasActiveApiKey) SuccessEmerald else ErrorCrimson, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (hasActiveApiKey) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (hasActiveApiKey) SuccessEmerald else ErrorCrimson,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (hasActiveApiKey) "Status: SECURE & CONNECTED" else "Status: NO API KEY DETECTED",
                                color = if (hasActiveApiKey) SuccessEmerald else ErrorCrimson,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (hasActiveApiKey) "Ready for Gemini 3.5 Flash chat & 2.5 Flash image" else "Add your free Google Gemini API key below to unlock full AI capabilities",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Custom / Override Gemini API Key:",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customApiKeyInput,
                        onValueChange = { customApiKeyInput = it },
                        placeholder = { Text("AIzaSy...", color = TextSecondary, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = SurfaceCardBorder,
                            focusedContainerColor = SurfaceCardElevated,
                            unfocusedContainerColor = SurfaceCardElevated
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_api_key_field")
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ElevatedButton(
                            onClick = {
                                prefs.customApiKey = customApiKeyInput
                            },
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = NeonCyan,
                                contentColor = DeepVoid
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save Key", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        if (customApiKeyInput.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    customApiKeyInput = ""
                                    prefs.customApiKey = ""
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Clear Override", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "ℹ️ How to get a free Gemini API Key:\n1. Visit aistudio.google.com\n2. Click 'Get API key' and create a free project key\n3. Paste above or enter in Secrets Panel (GEMINI_API_KEY).",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // 2. Language & Multilingual Settings
            item {
                SettingsSectionCard(
                    title = "Assistant Language (Hindi, English, Urdu...)",
                    icon = Icons.Default.Translate
                ) {
                    Text(
                        text = "Select spoken & recognized response language:",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SupportedLanguages.forEach { lang ->
                            val isSelected = selectedLanguage == lang.code
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else SurfaceCardElevated)
                                    .border(1.dp, if (isSelected) NeonCyan else SurfaceCardBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedLanguage = lang.code
                                        prefs.selectedLanguage = lang.code
                                        viewModel.updateTtsLanguage()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = lang.displayName,
                                    color = if (isSelected) NeonCyan else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // 3. Voice Engine & TTS Speed/Pitch
            item {
                SettingsSectionCard(
                    title = "Voice Synthesis & Speech Speed",
                    icon = Icons.Default.RecordVoiceOver
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Voice Audio Responses", color = TextPrimary, fontSize = 13.sp)
                        Switch(
                            checked = prefs.isVoiceEnabled,
                            onCheckedChange = { prefs.isVoiceEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Speech Rate: ${String.format("%.1fx", speechRate)}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Slider(
                        value = speechRate,
                        onValueChange = {
                            speechRate = it
                            prefs.speechRate = it
                            viewModel.updateTtsLanguage()
                        },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Voice Pitch: ${String.format("%.1fx", speechPitch)}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Slider(
                        value = speechPitch,
                        onValueChange = {
                            speechPitch = it
                            prefs.speechPitch = it
                            viewModel.updateTtsLanguage()
                        },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = ElectricViolet, activeTrackColor = ElectricViolet)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.speak("MOTO AI voice engine test. Saif Ali Khan, system operational.") },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Test Voice Synthesis", fontSize = 12.sp)
                    }
                }
            }

            // 4. Wake-Word Detection ("Hey MOTO" / "MOTO")
            item {
                SettingsSectionCard(
                    title = "Wake-Word Engine ('Hey MOTO')",
                    icon = Icons.Default.Mic
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Continuous Wake Word Service", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "Monitors microphone for 'Hey MOTO' or 'MOTO' triggers using persistent foreground service.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = wakeWordEnabled,
                            onCheckedChange = {
                                wakeWordEnabled = it
                                viewModel.setWakeWordEnabled(it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceCardElevated)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.BatteryAlert, contentDescription = null, tint = CyberMagenta, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Android System Notice: In accordance with Android security, background voice triggers require an ongoing notification. Background listening ceases if battery saver aggressively stops services.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // 5. Floating Assistant Button (Overlay)
            item {
                SettingsSectionCard(
                    title = "Floating Assistant Button",
                    icon = Icons.Default.Layers
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Floating MOTO AI Orb", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "Displays a draggable holographic MOTO AI orb over other applications for 1-tap assistant launch.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = floatingButtonEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                    // Open overlay permission settings
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                } else {
                                    floatingButtonEnabled = enabled
                                    viewModel.setFloatingButtonEnabled(enabled)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = ElectricViolet)
                        )
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Grant 'Display over other apps' Permission", fontSize = 11.sp)
                        }
                    }
                }
            }

            // 6. Default Video Resolution
            item {
                SettingsSectionCard(
                    title = "Video Resolution Preset",
                    icon = Icons.Default.Videocam
                ) {
                    Text(
                        text = "Veo video generation resolution (Default 720p for smooth Android playback):",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        VideoResolution.values().forEach { res ->
                            val code = res.name.substringAfter("RES_").lowercase()
                            val isSelected = selectedVideoRes == code
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) ElectricViolet.copy(alpha = 0.25f) else SurfaceCardElevated,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) ElectricViolet else SurfaceCardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedVideoRes = code
                                        prefs.defaultVideoResolution = code
                                    }
                            ) {
                                Text(
                                    text = code,
                                    color = if (isSelected) ElectricViolet else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // 7. Identity & About MOTO AI
            item {
                SettingsSectionCard(
                    title = "Assistant Identity & Owner",
                    icon = Icons.Default.Security
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DeepVoid)
                            .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MotoLogo(size = 32.dp, showSubtext = false)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "MOTO AI CORE",
                                    color = NeonCyan,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "I am MOTO AI, configured for Saif Ali Khan.",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Model: Google Gemini 3.5 Flash & Gemini 2.5 Flash Image. Owner identity is configured for Saif Ali Khan. No personal telemetry collected.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}
