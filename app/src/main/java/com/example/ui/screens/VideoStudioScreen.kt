package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.GeneratedMediaItem
import com.example.data.model.MediaStatus
import com.example.data.model.MediaType
import com.example.data.model.VideoResolution
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SpaceDark
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.MediaStoreHelper
import com.example.viewmodel.MotoAiViewModel
import kotlinx.coroutines.launch

@Composable
fun VideoStudioScreen(
    viewModel: MotoAiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val studioItems by viewModel.studioItems.collectAsState()
    val isGenerating by viewModel.isStudioGenerating.collectAsState()
    val statusMessage by viewModel.studioStatusMessage.collectAsState()

    var videoPrompt by remember { mutableStateOf("") }
    var selectedResolution by remember { mutableStateOf(viewModel.preferences.defaultVideoResolution) }
    var sourceImageUri by remember { mutableStateOf<Uri?>(null) }
    var sourceImageBase64 by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            sourceImageUri = uri
            coroutineScope.launch {
                sourceImageBase64 = MediaStoreHelper.uriToBase64(context, uri)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepVoid)
    ) {
        // Video Header
        Surface(
            color = SpaceDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = ElectricViolet,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VEO AI VIDEO SYNTHESIS",
                        color = ElectricViolet,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "High-efficiency cinematic video generation (Default 720p HD)",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Form Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceCard)
                        .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Video Prompt:",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = videoPrompt,
                        onValueChange = { videoPrompt = it },
                        placeholder = {
                            Text(
                                "e.g. A futuristic neon cyber vehicle cruising through rain-slicked highway at dusk",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = ElectricViolet,
                            unfocusedBorderColor = SurfaceCardBorder,
                            focusedContainerColor = SurfaceCardElevated,
                            unfocusedContainerColor = SurfaceCardElevated
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("video_prompt_input"),
                        minLines = 2,
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Output Resolution (Optimized for Android):",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        VideoResolution.values().forEach { res ->
                            val isSelected = selectedResolution == res.name.substringAfter("RES_").lowercase() ||
                                    selectedResolution.contains(res.name.substringAfter("RES_").lowercase())
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) ElectricViolet.copy(alpha = 0.25f) else SurfaceCardElevated,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) ElectricViolet else SurfaceCardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedResolution = res.name.substringAfter("RES_").lowercase()
                                        viewModel.preferences.defaultVideoResolution = selectedResolution
                                    }
                            ) {
                                Text(
                                    text = res.name.substringAfter("RES_").lowercase(),
                                    color = if (isSelected) ElectricViolet else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Image-to-Video Reference (Optional):",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (sourceImageUri != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceCardElevated)
                                .padding(8.dp)
                        ) {
                            AsyncImage(
                                model = sourceImageUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Reference image attached",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = {
                                    sourceImageUri = null
                                    sourceImageBase64 = null
                                }
                            ) {
                                Text("Remove", fontSize = 11.sp)
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Attach Starting Photo", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    ElevatedButton(
                        onClick = {
                            if (videoPrompt.isNotBlank()) {
                                viewModel.generateVideo(
                                    prompt = videoPrompt,
                                    resolution = selectedResolution,
                                    sourceImageBase64 = sourceImageBase64
                                )
                            }
                        },
                        enabled = videoPrompt.isNotBlank() && !isGenerating,
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = ElectricViolet,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_video_button")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Queueing Veo Video Engine...")
                        } else {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Video ($selectedResolution)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Compatibility notice banner
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceCardElevated)
                        .border(1.dp, SurfaceCardBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Standard Android phones smoothly stream 720p resolution without overheating. If high bitrate is returned, format is automatically verified.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            // Status message
            if (statusMessage != null) {
                item {
                    Text(
                        text = statusMessage ?: "",
                        color = if (statusMessage?.startsWith("Video Error") == true) ErrorCrimson else NeonCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // Video items
            val videoItems = studioItems.filter { it.type == MediaType.VIDEO }
            if (videoItems.isNotEmpty()) {
                item {
                    Text(
                        text = "VIDEO QUEUE & EXPORTS (${videoItems.size})",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                items(videoItems, key = { it.id }) { item ->
                    VideoItemCard(item = item)
                }
            }
        }
    }
}

@Composable
fun VideoItemCard(item: GeneratedMediaItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.prompt,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = ElectricViolet.copy(alpha = 0.2f)
            ) {
                Text(
                    text = item.resolution.uppercase(),
                    color = ElectricViolet,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Video Player Placeholder / Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(listOf(DeepVoid, SpaceDark))
                )
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Video",
                    tint = NeonCyan,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = if (item.status == MediaStatus.COMPLETED) "Video Render Complete • ${item.resolution}" else "Processing...",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (item.status == MediaStatus.FAILED) {
            Text(
                text = "Error: ${item.errorMessage ?: "Video generation failed"}",
                color = ErrorCrimson,
                fontSize = 12.sp
            )
        }
    }
}
