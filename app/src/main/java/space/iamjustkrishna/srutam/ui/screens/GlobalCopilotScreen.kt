package space.iamjustkrishna.srutam.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import space.iamjustkrishna.srutam.ui.theme.*
import space.iamjustkrishna.srutam.utils.NetworkUtils
import space.iamjustkrishna.srutam.viewmodel.AudioFilesViewModel

data class GlobalChatMessage(
    val text: String,
    val isUser: Boolean,
    val citedNotes: List<Pair<Long, String>> = emptyList(),
    val isSystem: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

private val GLOBAL_STARTER_PROMPTS = listOf(
    "What did I talk about recently?",
    "List all action items across notes",
    "Summarize my key ideas",
    "What decisions were made?"
)

private val NOTE_STARTER_PROMPTS = listOf(
    "Summarize this note",
    "What are the action items?",
    "What decisions were made?",
    "Explain key points"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GlobalCopilotScreen(
    viewModel: AudioFilesViewModel,
    onRecordingClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    focusedRecordingId: Long? = null,
    onClearFocusedRecording: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var focusedRecording by remember { mutableStateOf<space.iamjustkrishna.srutam.data.Recording?>(null) }
    var inputText by remember { mutableStateOf("") }
    var isQueryLoading by remember { mutableStateOf(false) }

    val defaultGlobalMessage = remember {
        GlobalChatMessage(
            text = "I can search across all your voice notes and answer any question about your recordings, meetings, and ideas.",
            isUser = false
        )
    }

    var messages by remember {
        mutableStateOf(listOf(defaultGlobalMessage))
    }

    LaunchedEffect(focusedRecordingId) {
        if (focusedRecordingId != null && focusedRecordingId > 0L) {
            val rec = viewModel.getRecordingById(focusedRecordingId)
            focusedRecording = rec
            val noteName = rec?.name?.ifBlank { "Voice Note" } ?: "Voice Note"
            messages = listOf(
                GlobalChatMessage(
                    text = "I'm focusing on your note \"$noteName\". You can ask me anything about this recording, or dismiss the banner to search across all notes.",
                    isUser = false
                )
            )
        } else {
            focusedRecording = null
        }
    }

    fun submitQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank() || isQueryLoading) return

        if (!NetworkUtils.isInternetAvailable(context)) {
            messages = messages + GlobalChatMessage(text = trimmed, isUser = true) +
                    GlobalChatMessage(text = "An internet connection is required to query your voice notes.", isUser = false)
            return
        }

        messages = messages + GlobalChatMessage(text = trimmed, isUser = true)
        inputText = ""
        isQueryLoading = true

        coroutineScope.launch {
            try {
                val (answer, citedNotes) = if (focusedRecordingId != null && focusedRecordingId > 0L) {
                    viewModel.querySpecificRecording(focusedRecordingId, trimmed)
                } else {
                    viewModel.queryAllVoiceNotes(trimmed)
                }
                messages = messages + GlobalChatMessage(
                    text = answer,
                    isUser = false,
                    citedNotes = citedNotes
                )
            } catch (e: Exception) {
                messages = messages + GlobalChatMessage(
                    text = "Sorry, I couldn't complete your request: ${e.message}",
                    isUser = false
                )
            } finally {
                isQueryLoading = false
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }
        }
    }

    fun clearFocusedRecording() {
        onClearFocusedRecording()
        val systemMsg = GlobalChatMessage(
            text = "Switched to all voice notes. Now referencing all your recordings and ideas.",
            isUser = false,
            isSystem = true
        )
        messages = if (messages.size <= 1) {
            listOf(defaultGlobalMessage, systemMsg)
        } else {
            messages + systemMsg
        }
        coroutineScope.launch {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    GlobalCopilotContent(
        messages = messages,
        inputText = inputText,
        isQueryLoading = isQueryLoading,
        focusedRecordingTitle = focusedRecording?.name?.ifBlank { "Voice Note" },
        onClearFocusedRecording = { clearFocusedRecording() },
        onInputTextChange = { inputText = it },
        onSubmitQuery = { submitQuery(it) },
        onNewSession = {
            if (focusedRecording != null) {
                val noteName = focusedRecording?.name?.ifBlank { "Voice Note" } ?: "Voice Note"
                messages = listOf(
                    GlobalChatMessage(
                        text = "I'm focusing on your note \"$noteName\". Ask me anything about this recording.",
                        isUser = false
                    )
                )
            } else {
                messages = listOf(defaultGlobalMessage)
            }
        },
        onSettingsClick = onSettingsClick,
        onRecordingClick = onRecordingClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GlobalCopilotContent(
    messages: List<GlobalChatMessage>,
    inputText: String,
    isQueryLoading: Boolean = false,
    focusedRecordingTitle: String? = null,
    onClearFocusedRecording: () -> Unit = {},
    onInputTextChange: (String) -> Unit = {},
    onSubmitQuery: (String) -> Unit = {},
    onNewSession: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onRecordingClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val isKeyboardOpen = WindowInsets.isImeVisible

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = CeramicWhite,
        topBar = {
            space.iamjustkrishna.srutam.ui.components.SrutamTopAppBar(
                title = "Srutam",
                accentText = "AI",
                subtitle = if (focusedRecordingTitle != null) "Focusing on note" else "Ask across all your voice notes",
                actions = {
                    if (messages.size > 1) {
                        space.iamjustkrishna.srutam.ui.components.SquircleActionButton(
                            icon = Icons.Default.AutoAwesome,
                            contentDescription = "New Session",
                            onClick = onNewSession
                        )
                    }
                    space.iamjustkrishna.srutam.ui.components.SquircleActionButton(
                        icon = Icons.Default.Settings,
                        contentDescription = "Settings",
                        onClick = onSettingsClick
                    )
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            // Dismissable Note Context Banner
            if (focusedRecordingTitle != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = CobaltContainer.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, CobaltBorder.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = CobaltBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Focusing on: $focusedRecordingTitle",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CobaltBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = onClearFocusedRecording,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear focused note",
                                tint = CobaltBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Starter Prompt Suggestions in single dynamic row
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "Suggested Questions",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF8E8E93),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            val promptList = if (focusedRecordingTitle != null) NOTE_STARTER_PROMPTS else GLOBAL_STARTER_PROMPTS
                            items(promptList) { prompt ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.clickable { onSubmitQuery(prompt) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "💡",
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = prompt,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF1E293B),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Chat Messages
                items(messages) { message ->
                    when {
                        message.isSystem -> {
                            SystemMessagePill(message.text)
                        }
                        message.isUser -> {
                            UserMessageBubble(message.text)
                        }
                        else -> {
                            AiMessageBubble(
                                message = message,
                                onRecordingClick = onRecordingClick
                            )
                        }
                    }
                }

                if (isQueryLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = CobaltBlue
                            )
                            Text(
                                text = "Searching voice notes...",
                                fontSize = 12.sp,
                                color = CobaltBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Spacer at bottom so content isn't covered by bottom input + floating dock
                item {
                    Spacer(modifier = Modifier.height(if (isKeyboardOpen) 16.dp else 110.dp))
                }
            }

            // Bottom Input Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                    .padding(horizontal = 16.dp)
                    .padding(bottom = if (isKeyboardOpen) 8.dp else 88.dp),
                shape = RoundedCornerShape(26.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE5E5EA)),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = onInputTextChange,
                        placeholder = {
                            Text(
                                if (focusedRecordingTitle != null) "Ask about this note..." else "Ask anything about your notes...",
                                fontSize = 13.sp,
                                color = Color(0xFF8E8E93)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )

                    Surface(
                        shape = CircleShape,
                        color = if (inputText.isNotBlank() && !isQueryLoading) CobaltBlue else Color(0xFFE5E5EA),
                        modifier = Modifier
                            .size(38.dp)
                            .clickable(
                                enabled = inputText.isNotBlank() && !isQueryLoading,
                                onClick = { onSubmitQuery(inputText) }
                            )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserMessageBubble(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp).copy(bottomEnd = CornerSize(4.dp)),
            color = CobaltBlue,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Text(
                text = text,
                fontSize = 14.sp,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun AiMessageBubble(
    message: GlobalChatMessage,
    onRecordingClick: (Long) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp).copy(bottomStart = CornerSize(4.dp)),
            color = Color.White,
            border = BorderStroke(0.5.dp, Color(0xFFE5E5EA)),
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 330.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CobaltBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Srutam AI",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CobaltBlue
                    )
                }

                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    color = Color(0xFF1C1C1E),
                    lineHeight = 20.sp
                )

                // Cited Notes Source Pills
                if (message.citedNotes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Referenced Notes:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF8E8E93)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        message.citedNotes.take(3).forEach { (id, title) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF2F2F7),
                                border = BorderStroke(0.5.dp, Color(0xFFE5E5EA)),
                                modifier = Modifier.clickable { onRecordingClick(id) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = CobaltBlue,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = title,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1C1C1E),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemMessagePill(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFF1F5F9),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "✦",
                    fontSize = 11.sp,
                    color = CobaltBlue,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = text,
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
