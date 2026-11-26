package com.kyecg.materialgram

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.tdlibandroid.ktx.TdClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi

data class ChatItem(
    val id: Long,
    val title: String,
    val lastMessage: String,
    val lastMessageDate: Long,
    val unreadCount: Int,
    val isPinned: Boolean,
    val order: Long
)

class ChatListViewModel(
    private val tdClient: TdClient
) {
    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    var chats by mutableStateOf<List<ChatItem>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    var loadError by mutableStateOf<String?>(null)
        private set

    private var refreshJob: Job? = null

    init {
        observeUpdates()
        loadChats()
    }

    private fun observeUpdates() {
        scope.launch {
            tdClient.updates.collect { update ->
                when (update) {
                    is TdApi.UpdateNewChat,
                    is TdApi.UpdateChatLastMessage,
                    is TdApi.UpdateChatTitle,
                    is TdApi.UpdateChatPosition,
                    is TdApi.UpdateChatReadInbox,
                    is TdApi.UpdateChatPhoto -> {
                        scheduleRefresh()
                    }
                }
            }
        }
    }

    private fun scheduleRefresh() {
        refreshJob?.cancel()

        refreshJob = scope.launch {
            delay(300)
            refreshChatList()
        }
    }

    fun loadChats() {
        scope.launch {
            setLoading(true)
            loadError = null

            try {
                tdClient.send(
                    TdApi.LoadChats(
                        TdApi.ChatListMain(),
                        100
                    )
                )

                refreshChatList()

            } catch (e: Exception) {
                setError(
                    e.message ?: "Не удалось загрузить чаты"
                )
            }
        }
    }

    fun refresh() {
        scope.launch {
            refreshChatList()
        }
    }

    private suspend fun refreshChatList() {
        try {
            val result: TdApi.Chats = tdClient.send(
                TdApi.GetChats(
                    TdApi.ChatListMain(),
                    100
                )
            )

            val loadedChats = result.chatIds.toList().mapNotNull { chatId ->
    try {
        val chat: TdApi.Chat = tdClient.send(
            TdApi.GetChat(chatId)
        )

        val position = chat.positions.firstOrNull {
            it.list is TdApi.ChatListMain
        }

        ChatItem(
            id = chat.id,
            title = chat.title,
            lastMessage = getMessagePreview(chat.lastMessage),
            lastMessageDate = chat.lastMessage?.date?.toLong() ?: 0L,
            unreadCount = chat.unreadCount,
            isPinned = position?.isPinned ?: false,
            order = position?.order ?: 0L
        )
    } catch (_: Exception) {
        null
    }
}

            val sortedChats = loadedChats.sortedWith(
                compareByDescending<ChatItem> { it.isPinned }
                    .thenByDescending { it.order }
            )

            withContext(Dispatchers.Main) {
                chats = sortedChats
                isLoading = false
                loadError = null
            }

        } catch (e: Exception) {
            setError(
                e.message ?: "Ошибка обновления чатов"
            )
        }
    }

    private fun getMessagePreview(
        message: TdApi.Message?
    ): String {
        if (message == null) {
            return "Нет сообщений"
        }

        return when (val content = message.content) {
            is TdApi.MessageText ->
                content.text.text

            is TdApi.MessagePhoto ->
                "Фото"

            is TdApi.MessageVideo ->
                "Видео"

            is TdApi.MessageDocument ->
                "Файл"

            is TdApi.MessageSticker ->
                "Стикер"

            is TdApi.MessageVoiceNote ->
                "Голосовое сообщение"

            is TdApi.MessageAudio ->
                "Аудио"

            is TdApi.MessageAnimation ->
                "GIF"

            else ->
                "Сообщение"
        }
    }

    private suspend fun setLoading(value: Boolean) {
        withContext(Dispatchers.Main) {
            isLoading = value
        }
    }

    private suspend fun setError(value: String) {
        withContext(Dispatchers.Main) {
            loadError = value
            isLoading = false
        }
    }

    fun close() {
        scope.coroutineContext[Job]?.cancel()
    }
}