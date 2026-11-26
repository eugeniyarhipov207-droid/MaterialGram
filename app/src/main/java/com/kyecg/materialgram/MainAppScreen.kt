package com.kyecg.materialgram

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

private enum class MainTab {
    Chats, Profile, Settings
}

@Composable
fun MainAppScreen(
    viewModel: ChatListViewModel,
    onChatClick: (Long) -> Unit
) {
    var selectedTab by remember { mutableStateOf(MainTab.Chats) }
    val hazeState = rememberHazeState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
    Box(
    modifier = Modifier
        .fillMaxSize()
        .hazeSource(hazeState)
) {
            when (selectedTab) {
                MainTab.Chats -> {
                    Box(
    modifier = Modifier
        .fillMaxSize()
        .padding(bottom = 100.dp)
) {
    ChatListScreen(
        viewModel = viewModel,
        onChatClick = onChatClick
    )
}
                }

                MainTab.Profile -> {
                    PlaceholderScreen(title = "Профиль")
                }

                MainTab.Settings -> {
                    PlaceholderScreen(title = "Настройки")
                }
            }

            FloatingBottomBar(
    selectedTab = selectedTab,
    onTabSelected = { selectedTab = it },
    hazeState = hazeState,
    modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        horizontal = 20.dp,
                        vertical = 14.dp
                    )
            )
        }
    }
}

@Composable
private fun FloatingBottomBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {val glassTint = MaterialTheme.colorScheme.surface.copy(
    alpha = 0.65f
)
    Surface(
    modifier = modifier
        .fillMaxWidth()
        .hazeBlur(
            input = HazeInput.Sources(hazeState),
            style = HazeBlurStyle {
                blurRadius(24.dp)
                backgroundColor(glassTint)
                }
                )
        .shadow(
            elevation = 10.dp,
            shape = RoundedCornerShape(32.dp)
        ),
    shape = RoundedCornerShape(32.dp),
    color = Color.Transparent,
    tonalElevation = 0.dp
) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 8.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomBarItem(
                title = "Чаты",
                symbol = "▤",
                selected = selectedTab == MainTab.Chats,
                onClick = { onTabSelected(MainTab.Chats) },
                modifier = Modifier.weight(1f)
            )

            BottomBarItem(
                title = "Профиль",
                symbol = "●",
                selected = selectedTab == MainTab.Profile,
                onClick = { onTabSelected(MainTab.Profile) },
                modifier = Modifier.weight(1f)
            )

            BottomBarItem(
                title = "Настройки",
                symbol = "⚙",
                selected = selectedTab == MainTab.Settings,
                onClick = { onTabSelected(MainTab.Settings) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomBarItem(
    title: String,
    symbol: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        label = "tabBackground"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "tabContent"
    )

    Column(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = symbol,
            color = contentColor,
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = title,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            }
        )
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
