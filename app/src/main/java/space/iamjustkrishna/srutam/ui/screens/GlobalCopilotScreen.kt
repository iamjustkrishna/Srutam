package space.iamjustkrishna.srutam.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.iamjustkrishna.srutam.ai.copilot.ChatSession
import space.iamjustkrishna.srutam.ai.copilot.CopilotChatStore
import space.iamjustkrishna.srutam.ai.copilot.LiveState
import space.iamjustkrishna.srutam.ai.copilot.ProposalStatus
import space.iamjustkrishna.srutam.ai.copilot.ToolProposal
import space.iamjustkrishna.srutam.ui.theme.*
import space.iamjustkrishna.srutam.utils.NetworkUtils
import space.iamjustkrishna.srutam.viewmodel.AudioFilesViewModel

typealias GlobalChatMessage = space.iamjustkrishna.srutam.ai.copilot.GlobalChatMessage

private val GLOBAL_STARTER_PROMPTS = listOf(
    "What did I talk about recently?",
    "What's on my plate this week?",
    "Remind me tomorrow at 9 am to…",
    "Add a next step: …"
)

private val NOTE_STARTER_PROMPTS = listOf(
    "Summarize this note",
    "What are the action items?",
    "What decisions were made?",
    "Explain key points"
)

private const val WELCOME_TEXT =
    "I can search your voice notes and help you set reminders, add next steps, and save ideas. I always ask before changing anything."

/** Colours for the AI screen, resolved once per theme so light and dark stay in step. */
private class CopilotPalette(dark: Boolean) {
    val background = if (dark) CosmicVoidBackground else Color(0xFFF4F5F8)
    val card = if (dark) CosmicVoidCard else Color.White
    val border = if (dark) CosmicVoidCardBorder else Color(0xFFE5E5EA)
    val chip = if (dark) CosmicVoidCard else Color(0xFFF1F5F9)
    val chipBorder = if (dark) CosmicVoidCardBorder else Color(0xFFE2E8F0)
    val textPrimary = if (dark) TextOnDarkPrimary else Color(0xFF1C1C1E)
    val textSecondary = if (dark) TextOnDarkSecondary else Color(0xFF8E8E93)
    val textMuted = if (dark) TextOnDarkSecondary else Color(0xFF475569)
    val accent = if (dark) CosmicGlowBlue else CobaltBlue
    val accentContainer = if (dark) CobaltBlue.copy(alpha = 0.22f) else CobaltContainer.copy(alpha = 0.7f)
    val accentBorder = if (dark) CosmicGlowBlue.copy(alpha = 0.35f) else CobaltBorder.copy(alpha = 0.4f)
    val success = if (dark) CosmicAuroraGreen else EmeraldSuccess
    val error = if (dark) Color(0xFFF87171) else StudioCrimson
    val sendDisabled = if (dark) CosmicVoidCardBorder else Color(0xFFE5E5EA)
}

@Composable
private fun rememberPalette(): CopilotPalette {
    val dark = LocalIsCosmicDark.current
    return remember(dark) { CopilotPalette(dark) }
}

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
    val copilot = viewModel.copilot

    var focusedRecording by remember { mutableStateOf<space.iamjustkrishna.srutam.data.Recording?>(null) }
    var inputText by remember { mutableStateOf("") }
    var isQueryLoading by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var sessions by remember { mutableStateOf<List<ChatSession>>(emptyList()) }
    var currentId by remember { mutableStateOf(java.util.UUID.randomUUID().toString()) }
    var messages by remember { mutableStateOf(listOf(GlobalChatMessage(text = WELCOME_TEXT, isUser = false))) }
    var applyingId by remember { mutableStateOf<String?>(null) }
    var liveStates by remember { mutableStateOf<Map<String, LiveState>>(emptyMap()) }
    val latestMessages by rememberUpdatedState(messages)

    fun persist(list: List<GlobalChatMessage>) {
        if (list.none { it.isUser }) return
        val session = ChatSession(currentId, CopilotChatStore.titleFor(list), System.currentTimeMillis(), list)
        coroutineScope.launch(Dispatchers.IO) { sessions = copilot.store.save(session) }
    }

    fun setMessages(list: List<GlobalChatMessage>) {
        messages = list
        persist(list)
    }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) { copilot.store.load() }
        sessions = loaded
        if ((focusedRecordingId == null || focusedRecordingId <= 0L) && loaded.isNotEmpty()) {
            currentId = loaded.first().id
            messages = loaded.first().messages.ifEmpty { messages }
        }
    }

    // Keep Done/Undo cards honest: if the item is finished, removed or changed elsewhere, the card says so.
    LaunchedEffect(Unit) {
        suspend fun refresh() {
            liveStates = withContext(Dispatchers.IO) { copilot.liveStates(latestMessages) }
        }
        launch { copilot.changes.collect { refresh() } }
        while (true) {
            refresh()
            kotlinx.coroutines.delay(20_000)
        }
    }
    LaunchedEffect(messages) { liveStates = withContext(Dispatchers.IO) { copilot.liveStates(messages) } }

    LaunchedEffect(focusedRecordingId) {
        if (focusedRecordingId != null && focusedRecordingId > 0L) {
            val rec = viewModel.getRecordingById(focusedRecordingId)
            focusedRecording = rec
            val noteName = rec?.name?.ifBlank { "Voice Note" } ?: "Voice Note"
            currentId = java.util.UUID.randomUUID().toString()
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
            setMessages(
                messages + GlobalChatMessage(text = trimmed, isUser = true) +
                    GlobalChatMessage(text = "An internet connection is required to query your voice notes.", isUser = false)
            )
            return
        }

        val history = messages
        setMessages(messages + GlobalChatMessage(text = trimmed, isUser = true))
        inputText = ""
        isQueryLoading = true

        coroutineScope.launch {
            try {
                val reply = if (focusedRecordingId != null && focusedRecordingId > 0L) {
                    val (answer, cited) = viewModel.querySpecificRecording(focusedRecordingId, trimmed)
                    GlobalChatMessage(text = answer, isUser = false, citedNotes = cited)
                } else {
                    val result = copilot.ask(history, trimmed)
                    GlobalChatMessage(text = result.text, isUser = false, citedNotes = result.cited, proposal = result.proposal)
                }
                setMessages(messages + reply)
            } catch (e: Exception) {
                setMessages(messages + GlobalChatMessage(text = "Something went wrong. Please try again in a moment.", isUser = false))
            } finally {
                isQueryLoading = false
            }
        }
    }

    fun updateProposal(id: String, change: (ToolProposal) -> ToolProposal) {
        setMessages(messages.map { m -> if (m.proposal?.id == id) m.copy(proposal = change(m.proposal)) else m })
    }

    fun confirmProposal(id: String) {
        val proposal = messages.firstNotNullOfOrNull { it.proposal?.takeIf { p -> p.id == id } } ?: return
        if (applyingId != null || proposal.status != ProposalStatus.PENDING) return
        applyingId = id
        coroutineScope.launch {
            val applied = copilot.apply(proposal)
            updateProposal(id) { applied }
            applyingId = null
        }
    }

    fun undoProposal(id: String) {
        val proposal = messages.firstNotNullOfOrNull { it.proposal?.takeIf { p -> p.id == id } } ?: return
        if (applyingId != null) return
        applyingId = id
        coroutineScope.launch {
            val undone = copilot.undo(proposal)
            updateProposal(id) { undone }
            applyingId = null
        }
    }

    fun clearFocusedRecording() {
        focusedRecording = null
        onClearFocusedRecording()
        val systemMsg = GlobalChatMessage(
            text = "Switched to all voice notes. Now referencing all your recordings and ideas.",
            isUser = false,
            isSystem = true
        )
        setMessages(messages + systemMsg)
    }

    fun newChat() {
        currentId = java.util.UUID.randomUUID().toString()
        messages = listOf(
            if (focusedRecording != null) {
                val noteName = focusedRecording?.name?.ifBlank { "Voice Note" } ?: "Voice Note"
                GlobalChatMessage(text = "I'm focusing on your note \"$noteName\". Ask me anything about this recording.", isUser = false)
            } else {
                GlobalChatMessage(text = WELCOME_TEXT, isUser = false)
            }
        )
        showHistory = false
    }

    GlobalCopilotContent(
        messages = messages,
        inputText = inputText,
        isQueryLoading = isQueryLoading,
        focusedRecordingTitle = focusedRecording?.name?.ifBlank { "Voice Note" },
        onClearFocusedRecording = { clearFocusedRecording() },
        onInputTextChange = { inputText = it },
        onSubmitQuery = { submitQuery(it) },
        sessions = sessions,
        currentSessionId = currentId,
        showHistory = showHistory,
        onShowHistory = { showHistory = true },
        onDismissHistory = { showHistory = false },
        onNewChat = { newChat() },
        onOpenSession = { session ->
            currentId = session.id
            messages = session.messages
            showHistory = false
        },
        onDeleteSession = { session ->
            coroutineScope.launch(Dispatchers.IO) { sessions = copilot.store.delete(session.id) }
            if (session.id == currentId) newChat()
        },
        applyingProposalId = applyingId,
        liveStates = liveStates,
        onConfirmProposal = { confirmProposal(it) },
        onCancelProposal = { id -> updateProposal(id) { it.copy(status = ProposalStatus.CANCELLED) } },
        onUndoProposal = { undoProposal(it) },
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
    sessions: List<ChatSession> = emptyList(),
    currentSessionId: String? = null,
    showHistory: Boolean = false,
    onShowHistory: () -> Unit = {},
    onDismissHistory: () -> Unit = {},
    onNewChat: () -> Unit = {},
    onOpenSession: (ChatSession) -> Unit = {},
    onDeleteSession: (ChatSession) -> Unit = {},
    applyingProposalId: String? = null,
    liveStates: Map<String, LiveState> = emptyMap(),
    onConfirmProposal: (String) -> Unit = {},
    onCancelProposal: (String) -> Unit = {},
    onUndoProposal: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onRecordingClick: (Long) -> Unit = {},
    accentFontSize: androidx.compose.ui.unit.TextUnit = 24.sp,
    modifier: Modifier = Modifier
) {
    val palette = rememberPalette()
    val listState = rememberLazyListState()
    val isKeyboardOpen = WindowInsets.isImeVisible
    var inputHeightPx by remember { mutableIntStateOf(0) }
    val inputHeight = with(LocalDensity.current) { inputHeightPx.toDp() }

    // Item 0 is the suggestions row, so message n sits at index n + 1.
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = palette.background,
        topBar = {
            space.iamjustkrishna.srutam.ui.components.SrutamTopAppBar(
                title = "Srutam",
                accentText = "AI",
                accentFontSize = accentFontSize,
                actions = {
                    space.iamjustkrishna.srutam.ui.components.SquircleActionButton(
                        icon = Icons.Default.History,
                        contentDescription = "Chat history",
                        onClick = onShowHistory
                    )
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
        // The chat fills the whole area and scrolls underneath the floating input and nav bar.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Dismissable Note Context Banner
            if (focusedRecordingTitle != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = palette.accentContainer,
                    border = BorderStroke(1.dp, palette.accentBorder)
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
                                tint = palette.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Focusing on: $focusedRecordingTitle",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.accent,
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
                                tint = palette.accent,
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
                    .fillMaxWidth()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        // Content stays visible but quiet where it passes under the input and nav bar.
                        val fade = inputHeightPx.toFloat().coerceIn(0f, size.height)
                        if (fade > 0f) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.Black, Color.Black.copy(alpha = 0.12f)),
                                    startY = size.height - fade,
                                    endY = size.height
                                ),
                                topLeft = Offset(0f, size.height - fade),
                                size = Size(size.width, fade),
                                blendMode = BlendMode.DstIn
                            )
                        }
                    },
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
                            text = "Suggested",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textSecondary,
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
                                    color = palette.chip,
                                    border = BorderStroke(1.dp, palette.chipBorder),
                                    modifier = Modifier.clickable {
                                        // Prompts ending in an ellipsis are templates the user finishes.
                                        if (prompt.endsWith("…")) onInputTextChange(prompt.dropLast(1)) else onSubmitQuery(prompt)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(text = "💡", fontSize = 11.sp)
                                        Text(
                                            text = prompt,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = palette.textPrimary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Chat Messages
                itemsIndexed(messages, key = { index, m -> "$index-${m.timestamp}" }) { _, message ->
                    when {
                        message.isSystem -> SystemMessagePill(message.text, palette)
                        message.isUser -> UserMessageBubble(message.text)
                        else -> AiMessageBubble(
                            message = message,
                            palette = palette,
                            applyingProposalId = applyingProposalId,
                            liveStates = liveStates,
                            onRecordingClick = onRecordingClick,
                            onConfirmProposal = onConfirmProposal,
                            onCancelProposal = onCancelProposal,
                            onUndoProposal = onUndoProposal
                        )
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
                                color = palette.accent
                            )
                            Text(
                                text = "Thinking...",
                                fontSize = 12.sp,
                                color = palette.accent,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Spacer at bottom so content isn't covered by bottom input + floating dock
                item {
                    Spacer(modifier = Modifier.height(inputHeight + 8.dp))
                }
            }
        }

            // Bottom Input Bar: filled only inside its own pill, floats over the chat
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { inputHeightPx = it.height }
            ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                    .padding(horizontal = 16.dp)
                    .padding(bottom = if (isKeyboardOpen) 8.dp else 88.dp),
                shape = RoundedCornerShape(26.dp),
                color = palette.card,
                border = BorderStroke(1.dp, palette.border),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = onInputTextChange,
                        placeholder = {
                            Text(
                                if (focusedRecordingTitle != null) "Ask about this note..." else "Ask, or tell me what to set up...",
                                fontSize = 13.sp,
                                color = palette.textSecondary
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp, max = 120.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = palette.textPrimary,
                            unfocusedTextColor = palette.textPrimary,
                            cursorColor = palette.accent
                        ),
                        singleLine = false,
                        minLines = 1,
                        maxLines = 5
                    )

                    Surface(
                        shape = CircleShape,
                        color = if (inputText.isNotBlank() && !isQueryLoading) CobaltBlue else palette.sendDisabled,
                        modifier = Modifier
                            .padding(bottom = 6.dp)
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

    if (showHistory) {
        ChatHistorySheet(
            sessions = sessions,
            currentSessionId = currentSessionId,
            palette = palette,
            onDismiss = onDismissHistory,
            onNewChat = onNewChat,
            onOpen = onOpenSession,
            onDelete = onDeleteSession
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatHistorySheet(
    sessions: List<ChatSession>,
    currentSessionId: String?,
    palette: CopilotPalette,
    onDismiss: () -> Unit,
    onNewChat: () -> Unit,
    onOpen: (ChatSession) -> Unit,
    onDelete: (ChatSession) -> Unit
) {
    val maxSheetHeight = (LocalConfiguration.current.screenHeightDp * 0.60f).dp
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = palette.card,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.5.dp)
                    .background(palette.border, CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Chat history", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    Text("Your last 3 chats are kept on this device.", fontSize = 12.sp, color = palette.textSecondary)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = palette.textSecondary, modifier = Modifier.size(20.dp))
                }
            }
            HorizontalDivider(color = palette.border, modifier = Modifier.padding(top = 8.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, false),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Surface(
                        onClick = onNewChat,
                        shape = RoundedCornerShape(16.dp),
                        color = palette.accentContainer,
                        border = BorderStroke(1.dp, palette.accentBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
                            Text("New chat", fontWeight = FontWeight.SemiBold, color = palette.accent, fontSize = 14.sp)
                        }
                    }
                }
                if (sessions.isEmpty()) {
                    item {
                        Text("No saved chats yet.", fontSize = 13.sp, color = palette.textSecondary, modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
                items(sessions, key = { it.id }) { session ->
                    val selected = session.id == currentSessionId
                    Surface(
                        onClick = { onOpen(session) },
                        shape = RoundedCornerShape(16.dp),
                        color = palette.card,
                        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) palette.accent else palette.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    session.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    session.messages.count { it.isUser }.let { n ->
                                        "${formatHumanRelativeDate(session.updatedAt)} · $n ${if (n == 1) "message" else "messages"}"
                                    },
                                    fontSize = 11.5.sp,
                                    color = palette.textSecondary
                                )
                            }
                            IconButton(onClick = { onDelete(session) }, modifier = Modifier.size(40.dp)) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete chat",
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
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
    palette: CopilotPalette,
    applyingProposalId: String?,
    liveStates: Map<String, LiveState>,
    onRecordingClick: (Long) -> Unit,
    onConfirmProposal: (String) -> Unit,
    onCancelProposal: (String) -> Unit,
    onUndoProposal: (String) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp).copy(bottomStart = CornerSize(4.dp)),
            color = palette.card,
            border = BorderStroke(0.5.dp, palette.border),
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
                        tint = palette.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Srutam AI",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accent
                    )
                }

                Text(
                    text = remember(message.text) { markdownToAnnotated(message.text) },
                    fontSize = 14.sp,
                    color = palette.textPrimary,
                    lineHeight = 20.sp
                )

                message.proposal?.let { proposal ->
                    Spacer(modifier = Modifier.height(10.dp))
                    ProposalCard(
                        proposal = proposal,
                        palette = palette,
                        busy = applyingProposalId == proposal.id,
                        live = liveStates[proposal.id],
                        onConfirm = { onConfirmProposal(proposal.id) },
                        onCancel = { onCancelProposal(proposal.id) },
                        onUndo = { onUndoProposal(proposal.id) }
                    )
                }

                // Cited Notes Source Pills
                if (message.citedNotes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Referenced Notes:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        message.citedNotes.take(3).forEach { (id, title) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.chip,
                                border = BorderStroke(0.5.dp, palette.border),
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
                                        tint = palette.accent,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = title,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = palette.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 90.dp)
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

/** Models often reply with light markdown. Show bold text and bullets instead of raw symbols. */
private fun markdownToAnnotated(text: String): AnnotatedString = buildAnnotatedString {
    val bold = Regex("""\*\*(.+?)\*\*""")
    val bullet = Regex("""^\s*[-*]\s+""")
    val heading = Regex("""^#{1,6}\s+""")
    text.lines().forEachIndexed { i, raw ->
        if (i > 0) append("\n")
        val line = raw.trimEnd().replace(bullet, "• ").replace(heading, "")
        var last = 0
        for (m in bold.findAll(line)) {
            append(line.substring(last, m.range.first))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(m.groupValues[1]) }
            last = m.range.last + 1
        }
        append(line.substring(last))
    }
}

/** Shows what the AI wants to change and lets the user confirm, cancel, or undo it. */
@Composable
private fun ProposalCard(
    proposal: ToolProposal,
    palette: CopilotPalette,
    busy: Boolean,
    live: LiveState?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onUndo: () -> Unit
) {
    val (statusLabel, statusColor) = when (proposal.status) {
        ProposalStatus.PENDING -> "Needs your OK" to palette.accent
        ProposalStatus.APPLIED -> (live?.label ?: "Done") to (if (live == null) palette.success else palette.textSecondary)
        ProposalStatus.CANCELLED -> "Cancelled" to palette.textSecondary
        ProposalStatus.FAILED -> "Not applied" to palette.error
        ProposalStatus.UNDONE -> "Undone" to palette.textSecondary
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (proposal.status == ProposalStatus.PENDING) palette.accentContainer else palette.chip,
        border = BorderStroke(1.dp, if (proposal.status == ProposalStatus.PENDING) palette.accentBorder else palette.chipBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = when (proposal.status) {
                        ProposalStatus.APPLIED -> Icons.Default.CheckCircle
                        ProposalStatus.FAILED -> Icons.Default.ErrorOutline
                        else -> Icons.Default.Edit
                    },
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(statusLabel.uppercase(), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, color = statusColor)
            }
            Text(proposal.summary, fontSize = 13.5.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium, color = palette.textPrimary)
            proposal.resultNote?.let {
                Text(it, fontSize = 12.sp, color = if (proposal.status == ProposalStatus.FAILED) palette.error else palette.textMuted)
            }
            when (proposal.status) {
                ProposalStatus.PENDING -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCancel, enabled = !busy) {
                        Text("Cancel", color = palette.textMuted, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onConfirm,
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue, contentColor = Color.White),
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
                    ) {
                        if (busy) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text("Confirm", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                ProposalStatus.APPLIED -> if (proposal.undo != null && (live == null || live.canUndo)) {
                    TextButton(onClick = onUndo, enabled = !busy, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(15.dp), tint = palette.accent)
                        Spacer(Modifier.width(4.dp))
                        Text("Undo", color = palette.accent, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                    }
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun SystemMessagePill(text: String, palette: CopilotPalette) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = palette.chip,
            border = BorderStroke(1.dp, palette.chipBorder),
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
                    color = palette.accent,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = text,
                    fontSize = 12.sp,
                    color = palette.textMuted,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
