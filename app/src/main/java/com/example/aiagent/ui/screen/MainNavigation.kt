package com.example.aiagent.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.aiagent.ui.screen.components.McpToolsScreen

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Chat : BottomNavItem("chat", Icons.Default.Chat, "Chat")
    object Settings : BottomNavItem("settings", Icons.Default.Settings, "Settings")
    object Profile : BottomNavItem("profile", Icons.Default.Person, "Profile")
    object Tools : BottomNavItem("tools", Icons.Default.Build, "Tools")
    object Support : BottomNavItem("support", Icons.Default.HeadsetMic, "Поддержка")
}

@Composable
fun MainNavigation(
    chatScreen: @Composable () -> Unit,
    settingsScreen: @Composable () -> Unit,
    profileScreen: @Composable () -> Unit,
    toolsScreen: @Composable () -> Unit,
    supportScreen: @Composable () -> Unit
) {
    var selectedItem by remember { mutableStateOf(0) }
    val items = listOf(
        BottomNavItem.Chat,
        BottomNavItem.Settings,
        BottomNavItem.Profile,
        BottomNavItem.Tools,
        BottomNavItem.Support
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = selectedItem,
                transitionSpec = {
                    // Slide animation for navigation transitions
                    slideInHorizontally(
                        initialOffsetX = { if (targetState > initialState) it else -it },
                        animationSpec = tween(300)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { if (targetState > initialState) -it else it },
                        animationSpec = tween(300)
                    )
                },
                label = "navigation_transition"
            ) { targetIndex ->
                when (targetIndex) {
                    0 -> chatScreen()
                    1 -> settingsScreen()
                    2 -> profileScreen()
                    3 -> toolsScreen()
                    4 -> supportScreen()
                }
            }
        }
    }
}
