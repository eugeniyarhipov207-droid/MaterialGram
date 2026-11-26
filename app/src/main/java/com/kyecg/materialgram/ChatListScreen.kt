
package com.kyecg.materialgram

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.statusBarsPadding

@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel,
    onChatClick: (Long) -> Unit
) {
    val chats = viewModel.chats
    val isLoading = viewModel.isLoading
    val error = viewModel.loadError

    Column(
    modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .statusBarsPadding()
) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 20.dp,
                    vertical = 16.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
    text = "MaterialGram",
    style = MaterialTheme.typography.titleLarge,
    fontWeight = FontWeight.Bold,
    modifier = Modifier.weight(1f)
)

            TextButton(
                onClick = { viewModel.refresh() }
            ) {
                Text("Обновить")
            }
        }

        when {
            isLoading && chats.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            error != null && chats.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Не удалось загрузить чаты",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = error,
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.error
                        )

                        TextButton(
                            onClick = { viewModel.loadChats() }
                        ) {
                            Text("Повторить")
                        }
                    }
                }
            }

            chats.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Пока нет чатов",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = chats,
                        key = { it.id }
                    ) { chat ->
                        ChatRow(
                            chat = chat,
                            onClick = {
                                onChatClick(chat.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatRow(
    chat: ChatItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = 16.dp,
                vertical = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChatAvatar(
            title = chat.title
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = formatChatDate(chat.lastMessageDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.lastMessage,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (chat.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))

                    Badge {
                        Text(
                            text = if (chat.unreadCount > 999) {
                                "999+"
                            } else {
                                chat.unreadCount.toString()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatAvatar(
    title: String
) {
    val initials = title
        .trim()
        .split(Regex("\\s+"))
        .take(2)
        .mapNotNull { it.firstOrNull()?.toString() }
        .joinToString("")
        .uppercase(Locale.getDefault())
        .ifEmpty { "?" }

    val colors = MaterialTheme.colorScheme
    val colorIndex = (title.hashCode() and Int.MAX_VALUE) % 5

    val avatarColor = when (colorIndex) {
        0 -> colors.primaryContainer
        1 -> colors.secondaryContainer
        2 -> colors.tertiaryContainer
        3 -> colors.surfaceVariant
        else -> colors.inversePrimary
    }

    Surface(
        modifier = Modifier.size(54.dp),
        shape = CircleShape,
        color = avatarColor
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
        }
    }
}

private fun formatChatDate(timestamp: Long): String {
    if (timestamp <= 0L) return ""

    val date = Date(timestamp * 1000L)
    val now = Date()

    val dateFormat = SimpleDateFormat(
        "yyyyMMdd",
        Locale.getDefault()
    )

    return if (dateFormat.format(date) == dateFormat.format(now)) {
        SimpleDateFormat(
            "HH:mm",
            Locale.getDefault()
        ).format(date)
    } else {
        SimpleDateFormat(
            "dd.MM",
            Locale.getDefault()
        ).format(date)
    }
}
