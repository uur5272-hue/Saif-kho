package com.example.ui.screens

import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import com.example.util.MediaStoreHelper
import com.example.viewmodel.MotoAiViewModel
import kotlinx.coroutines.launch

@Composable
fun ImageStudioScreen(
    viewModel: MotoAiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Text-to-Image, 1 = Photo Edit

    val studioItems by viewModel.studioItems.collectAsState()
    val isGenerating by viewModel.isStudioGenerating.collectAsState()
    val statusMessage by viewModel.studioStatusMessage.collectAsState()

    // Text to image state
    var promptText by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("Cyberpunk") }
    var selectedAspect by remember { mutableStateOf("1:1") }

    // Photo Edit state
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPhotoBase64 by remember { mutableStateOf<String?>(null) }
    var editInstructions by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri
            coroutineScope.launch {
                selectedPhotoBase64 = MediaStoreHelper.uriToBase64(context, uri)
            }
        }
    }

    val styleOptions = listOf("Cyberpunk", "Hyper-Realistic", "Anime Studio", "Fantasy Art", "3D Cinematic", "None")
    val aspectOptions = listOf("1:1", "16:9", "9:16")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepVoid)
    ) {
        // Studio Header
        Surface(
            color = SpaceDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MOTO NEURAL VISUAL STUDIO",
                        color = NeonCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "Powered by Gemini 2.5 Flash Image synthesis matrix",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // Mode Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SpaceDark,
            contentColor = NeonCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = NeonCyan,
                    height = 2.dp
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Generate Image", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Upload & Edit Photo", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (selectedTab == 0) {
                // Text-to-Image Form
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
                            text = "Describe your vision:",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = promptText,
                            onValueChange = { promptText = it },
                            placeholder = {
                                Text(
                                    "e.g. A cybernetic dragon over a neon holographic skyscraper at midnight",
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = SurfaceCardBorder,
                                focusedContainerColor = SurfaceCardElevated,
                                unfocusedContainerColor = SurfaceCardElevated
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("image_prompt_input"),
                            minLines = 2,
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Artistic Style:",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(styleOptions) { style ->
                                val isSelected = selectedStyle == style
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else SurfaceCardElevated,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) NeonCyan else SurfaceCardBorder
                                    ),
                                    modifier = Modifier.clickable { selectedStyle = style }
                                ) {
                                    Text(
                                        text = style,
                                        color = if (isSelected) NeonCyan else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Aspect Ratio:",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            aspectOptions.forEach { aspect ->
                                val isSelected = selectedAspect == aspect
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) ElectricViolet.copy(alpha = 0.25f) else SurfaceCardElevated,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) ElectricViolet else SurfaceCardBorder
                                    ),
                                    modifier = Modifier.clickable { selectedAspect = aspect }
                                ) {
                                    Text(
                                        text = aspect,
                                        color = if (isSelected) ElectricViolet else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        ElevatedButton(
                            onClick = {
                                if (promptText.isNotBlank()) {
                                    viewModel.generateImage(promptText, selectedStyle, selectedAspect)
                                }
                            },
                            enabled = promptText.isNotBlank() && !isGenerating,
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = NeonCyan,
                                contentColor = DeepVoid
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("generate_image_button")
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    color = DeepVoid,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Synthesizing Visuals...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Image", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Photo Edit Form
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
                            text = "Select Photo to Edit:",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (selectedPhotoUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DeepVoid)
                                    .border(1.dp, NeonCyan, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = selectedPhotoUri,
                                    contentDescription = "Selected Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Change Photo", fontSize = 12.sp)
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceCardElevated)
                                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Tap to select photo from Gallery", color = TextSecondary, fontSize = 13.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Edit Instructions:",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = editInstructions,
                            onValueChange = { editInstructions = it },
                            placeholder = {
                                Text(
                                    "e.g. Replace the background with a futuristic cyberpunk street, add neon sunglasses, and adjust lighting",
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
                                .testTag("photo_edit_instruction_input"),
                            minLines = 2,
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        ElevatedButton(
                            onClick = {
                                val b64 = selectedPhotoBase64
                                if (b64 != null && editInstructions.isNotBlank()) {
                                    viewModel.editPhoto(b64, editInstructions, selectedPhotoUri?.toString())
                                }
                            },
                            enabled = selectedPhotoBase64 != null && editInstructions.isNotBlank() && !isGenerating,
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = ElectricViolet,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("apply_photo_edit_button")
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Applying Neural Edits...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Apply Neural Photo Edit", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Status message
            if (statusMessage != null) {
                item {
                    Text(
                        text = statusMessage ?: "",
                        color = if (statusMessage?.startsWith("Failed") == true) ErrorCrimson else NeonCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // Results Section
            val imageItems = studioItems.filter { it.type == MediaType.IMAGE }
            if (imageItems.isNotEmpty()) {
                item {
                    Text(
                        text = "STUDIO OUTPUTS (${imageItems.size})",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                items(imageItems, key = { it.id }) { item ->
                    GeneratedImageCard(
                        item = item,
                        onSave = { viewModel.saveMediaItemToGallery(item) },
                        onShare = {
                            val uri = item.mediaUri
                            if (uri != null) {
                                MediaStoreHelper.shareImage(context, Uri.parse(uri))
                            } else {
                                viewModel.saveMediaItemToGallery(item)
                            }
                        },
                        onRegenerate = {
                            viewModel.generateImage(item.prompt, item.style, item.resolution)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GeneratedImageCard(
    item: GeneratedMediaItem,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onRegenerate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        // Prompt
        Text(
            text = item.prompt,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        if (item.style.isNotBlank()) {
            Text(
                text = "Style: ${item.style} • Aspect: ${item.resolution}",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        if (item.status == MediaStatus.COMPLETED && item.base64Data != null) {
            val bitmap = remember(item.base64Data) {
                try {
                    val bytes = Base64.decode(item.base64Data, Base64.DEFAULT)
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } catch (e: Exception) {
                    null
                }
            }

            if (bitmap != null) {
                // If original image is available, show comparison view
                if (item.originalImageUri != null) {
                    Text(
                        text = "Original Photo vs Neural Edit:",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(10.dp))
                        ) {
                            AsyncImage(
                                model = item.originalImageUri,
                                contentDescription = "Original",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                "ORIGINAL",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .background(DeepVoid.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, NeonCyan, RoundedCornerShape(10.dp))
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Neural Edit",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                "EDITED",
                                color = NeonCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .background(DeepVoid.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = item.prompt,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ElevatedButton(
                            onClick = onSave,
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = if (item.mediaUri != null) SuccessEmerald else NeonCyan,
                                contentColor = DeepVoid
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                if (item.mediaUri != null) Icons.Default.Check else Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (item.mediaUri != null) "In Gallery" else "Save to Gallery", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onShare,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", fontSize = 12.sp)
                        }
                    }

                    IconButton(onClick = onRegenerate) {
                        Icon(Icons.Default.Refresh, contentDescription = "Regenerate", tint = TextSecondary)
                    }
                }
            }
        } else if (item.status == MediaStatus.FAILED) {
            Text(
                text = "Error: ${item.errorMessage ?: "Generation failed"}",
                color = ErrorCrimson,
                fontSize = 12.sp
            )
        }
    }
}
