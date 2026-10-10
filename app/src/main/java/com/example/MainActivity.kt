package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.service.MotoAiWakeWordService
import com.example.ui.components.ActionConfirmationDialog
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ImageStudioScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideoStudioScreen
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.MotoAiTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SpaceDark
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MotoAiViewModel

enum class MainTab(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    CHAT("Assistant", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
    IMAGES("Image Studio", Icons.Filled.Image, Icons.Outlined.Image),
    VIDEOS("Video Studio", Icons.Filled.Videocam, Icons.Outlined.Videocam),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MotoAiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestInitialPermissions()
        handleWakeIntent(intent)

        setContent {
            MotoAiTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleWakeIntent(intent)
    }

    private fun handleWakeIntent(intent: Intent?) {
        val query = intent?.getStringExtra(MotoAiWakeWordService.EXTRA_TRIGGER_QUERY)
        if (!query.isNullOrBlank()) {
            viewModel.sendMessage(query)
        }
    }

    private fun requestInitialPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissions(permissionsToRequest.toTypedArray(), 101)
        }
    }
}

@Composable
fun MainContent(viewModel: MotoAiViewModel) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val pendingAction by viewModel.pendingAction.collectAsState()

    // Handle system back button
    BackHandler(enabled = selectedTabIndex != 0) {
        selectedTabIndex = 0
    }

    // Action Confirmation Dialog (Call / Message)
    ActionConfirmationDialog(
        action = pendingAction,
        onConfirm = { viewModel.confirmPendingAction() },
        onDismiss = { viewModel.dismissPendingAction() }
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepVoid),
        containerColor = DeepVoid,
        bottomBar = {
            NavigationBar(
                containerColor = SpaceDark,
                tonalElevation = 8.dp,
                modifier = Modifier.border(
                    width = 0.5.dp,
                    color = SurfaceCardBorder
                )
            ) {
                MainTab.values().forEachIndexed { index, tab ->
                    val isSelected = selectedTabIndex == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            indicatorColor = NeonCyan.copy(alpha = 0.15f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTabIndex) {
                0 -> ChatScreen(viewModel = viewModel)
                1 -> ImageStudioScreen(viewModel = viewModel)
                2 -> VideoStudioScreen(viewModel = viewModel)
                3 -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
