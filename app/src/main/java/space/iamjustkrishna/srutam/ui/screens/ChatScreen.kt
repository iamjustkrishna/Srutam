package space.iamjustkrishna.srutam.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import space.iamjustkrishna.srutam.utils.NetworkUtils
import space.iamjustkrishna.srutam.viewmodel.ChatMessage
import space.iamjustkrishna.srutam.viewmodel.DetailViewModel
import kotlinx.coroutines.launch

private val suggestedQuestions = listOf(
    "What are the main topics?",
    "What decisions were made?",
    "Summarize this in one sentence",
    "What are the action items?"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    recordingId: Long,
    onNavigateBack: () -> Unit,
    viewModel: DetailViewModel = viewModel()
) {
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isQueryLoading by viewModel.isQueryLoading.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isOnline = NetworkUtils.isInternetAvailable(context)

    fun submitQuestion(question: String) {
        val trimmedQuestion = question.trim()
        if (trimmedQuestion.isBlank() || isQueryLoading || !isOnline) return
        viewModel.askQuestion(trimmedQuestion)
        inputText = ""
    }

    LaunchedEffect(recordingId) {
        viewModel.loadRecording(recordingId)
    }

    LaunchedEffect(chatMessages.size, isQueryLoading) {
        val extraItems = if (isQueryLoading) 1 else 0
        val targetIndex = (chatMessages.size + extraItems - 1).coerceAtLeast(0)
        if (chatMessages.isNotEmpty() || isQueryLoading) {
            scope.launch {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ask Questions") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (chatMessages.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearChat() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Chat")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                inputText = inputText,
                onInputTextChanged = { inputText = it },
                onSendMessage = { submitQuestion(inputText) },
                enabled = !isQueryLoading && isOnline,
                isOnline = isOnline
            )
        }
    ) { paddingValues ->
        if (chatMessages.isEmpty()) {
            EmptyChatState(
                isOnline = isOnline,
                onSuggestionClick = { suggestion ->
                    inputText = suggestion
                    submitQuestion(suggestion)
                },
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SuggestionRow(
                        isOnline = isOnline,
                        onSuggestionClick = { suggestion ->
                            inputText = suggestion
                            submitQuestion(suggestion)
                        }
                    )
                }

                items(chatMessages) { message ->
                    ChatMessageItem(message = message)
                }

                if (isQueryLoading) {
                    item {
                        ThinkingMessage()
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyChatState(
    isOnline: Boolean,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "Ask questions about your recording",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isOnline) {
                        "Tap a suggestion to send it instantly, or type your own question below."
                    } else {
                        "You are offline. AI questions will work again when internet is available."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isOnline) {
                    SuggestionRow(
                        isOnline = true,
                        onSuggestionClick = onSuggestionClick
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    isOnline: Boolean,
    onSuggestionClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Suggestions",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            suggestedQuestions.forEach { suggestion ->
                AssistChip(
                    onClick = { if (isOnline) onSuggestionClick(suggestion) },
                    enabled = isOnline,
                    label = {
                        Text(
                            text = suggestion,
                            maxLines = 2
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ThinkingMessage() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            modifier = Modifier.padding(end = 48.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(22.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Thinking...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = if (message.isUser) {
                Modifier.padding(start = 48.dp)
            } else {
                Modifier.padding(end = 48.dp)
            },
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            shape = RoundedCornerShape(
                topStart = 22.dp,
                topEnd = 22.dp,
                bottomStart = if (message.isUser) 22.dp else 8.dp,
                bottomEnd = if (message.isUser) 8.dp else 22.dp
            )
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (message.isUser) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
fun ChatInputBar(
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    enabled: Boolean,
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isOnline) {
                Text(
                    text = "Connect to the internet to ask questions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                TextField(
                    value = inputText,
                    onValueChange = onInputTextChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask anything about this recording") },
                    enabled = enabled,
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSend = { onSendMessage() }
                    )
                )
                Spacer(modifier = Modifier.width(10.dp))
                FilledIconButton(
                    onClick = onSendMessage,
                    enabled = enabled && inputText.isNotBlank(),
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send"
                    )
                }
            }
        }
    }
}
