@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package space.iamjustkrishna.srutam.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import space.iamjustkrishna.srutam.data.*
import space.iamjustkrishna.srutam.ui.components.SquircleActionButton
import space.iamjustkrishna.srutam.ui.components.SrutamTopAppBar
import space.iamjustkrishna.srutam.ui.theme.*
import space.iamjustkrishna.srutam.viewmodel.InsightsUiState
import space.iamjustkrishna.srutam.viewmodel.InsightsScreenMemory

@Composable
fun InsightsContent(
    state: InsightsUiState,
    onRecordingClick: (Long) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    initialTab: InsightsTab? = null,
    modifier: Modifier = Modifier,
    actions: InsightsActions = InsightsActions(),
    actionError: String? = null,
    clearError: () -> Unit = {},
    createdTaskId: String? = null,
    consumeCreatedTask: () -> Unit = {},
    savedMemory: InsightsScreenMemory = InsightsScreenMemory(),
    onSaveMemory: (InsightsScreenMemory) -> Unit = {},
    savedReminderId: String? = null,
    consumeSavedReminder: () -> Unit = {}
) {
    var selection by rememberSaveable { mutableStateOf(initialTab?.name ?: savedMemory.selection) }
    var datesExpanded by rememberSaveable { mutableStateOf(savedMemory.datesExpanded) }
    var themesExpanded by rememberSaveable { mutableStateOf(savedMemory.themesExpanded) }
    var completedExpanded by rememberSaveable { mutableStateOf(savedMemory.completedExpanded) }
    var ideaSearch by rememberSaveable { mutableStateOf(savedMemory.ideaSearch) }
    var decisionSearch by rememberSaveable { mutableStateOf(savedMemory.decisionSearch) }
    var selectedThemeKey by rememberSaveable { mutableStateOf<String?>(null) }
    var historyView by rememberSaveable { mutableStateOf<String?>(null) }
    var editingReminder by rememberSaveable { mutableStateOf<String?>(null) }
    var creatingFrom by rememberSaveable { mutableStateOf<String?>(null) }
    var fromReminder by rememberSaveable { mutableStateOf(false) }
    var viewingTask by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteIsReminder by rememberSaveable { mutableStateOf(false) }
    var legacyNoticeDismissed by rememberSaveable { mutableStateOf(false) }

    val taskScroll = rememberLazyListState(savedMemory.taskIndex, savedMemory.taskOffset)
    val ideaScroll = rememberLazyListState(savedMemory.ideaIndex, savedMemory.ideaOffset)
    val decisionScroll = rememberLazyListState(savedMemory.decisionIndex, savedMemory.decisionOffset)
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val dark = LocalIsCosmicDark.current
    val active = state.activeReminders

    LaunchedEffect(Unit) {
        snapshotFlow {
            InsightsScreenMemory(
                selection, ideaSearch, decisionSearch, datesExpanded, themesExpanded, completedExpanded,
                taskScroll.firstVisibleItemIndex, taskScroll.firstVisibleItemScrollOffset,
                ideaScroll.firstVisibleItemIndex, ideaScroll.firstVisibleItemScrollOffset,
                decisionScroll.firstVisibleItemIndex, decisionScroll.firstVisibleItemScrollOffset
            )
        }.collect { onSaveMemory(it) }
    }

    LaunchedEffect(savedReminderId) {
        if (savedReminderId != null) {
            editingReminder = null
            consumeSavedReminder()
        }
    }

    LaunchedEffect(state.loaded) {
        if (state.loaded && selection == null) {
            selection = state.initialTab()
        }
    }

    LaunchedEffect(actionError) {
        actionError?.let {
            snack.showSnackbar(it)
            clearError()
        }
    }

    LaunchedEffect(createdTaskId) {
        createdTaskId?.let {
            creatingFrom = null
            if (snack.showSnackbar("Next step created", "View") == SnackbarResult.ActionPerformed) {
                viewingTask = it
            }
            consumeCreatedTask()
        }
    }

    CompositionLocalProvider(LocalContentColor provides if (dark) TextOnDarkPrimary else TextPrimary) {
        Scaffold(
            modifier = modifier,
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snack) },
            topBar = {
                SrutamTopAppBar(
                    title = "Srutam",
                    accentText = "Insights",
                    actions = {
                        SquircleActionButton(
                            icon = Icons.Default.Archive,
                            contentDescription = "Open archive",
                            onClick = { historyView = "Archive" }
                        )
                        SquircleActionButton(
                            icon = Icons.Default.History,
                            contentDescription = "Open reminder history",
                            onClick = { historyView = "Reminder history" }
                        )
                        SquircleActionButton(
                            icon = Icons.Default.Settings,
                            contentDescription = "Settings",
                            onClick = onSettingsClick
                        )
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val selected = selection ?: state.initialTab()

                // Top Segmented Capsule matching the rhythm of the Notes screen
                SingleRowInsightsCapsule(
                    selectedTab = selected,
                    onTabSelected = { selection = it },
                    ideasCount = state.ideas.size,
                    nextStepsCount = state.openTasks.size,
                    decisionsCount = state.decisions.size,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Thematic Filter Chips Row
                if (state.themes.isNotEmpty()) {
                    val activeCount = when (selected) {
                        "IDEAS" -> state.ideas.size
                        "DECISIONS" -> state.decisions.size
                        else -> state.openTasks.size
                    }
                    ThematicFilterChipsRow(
                        themes = state.themes,
                        selectedTheme = selectedThemeKey,
                        onSelectTheme = { selectedThemeKey = it },
                        onDismissTheme = { key ->
                            actions.dismissTheme(key)
                            scope.launch {
                                if (snack.showSnackbar("Theme dismissed", "Undo") == SnackbarResult.ActionPerformed) {
                                    actions.restoreTheme(key)
                                }
                            }
                        },
                        totalCount = activeCount,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                }

                when {
                    state.error != null -> {
                        Column(Modifier.padding(24.dp)) {
                            Text(state.error)
                            TextButton(onClick = actions.retry) { Text("Retry") }
                        }
                    }
                    !state.loaded -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.testTag("insights_loading"))
                        }
                    }
                    else -> {
                        val listState = when (selected) {
                            "IDEAS" -> ideaScroll
                            "DECISIONS" -> decisionScroll
                            else -> taskScroll
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("insights_list_$selected"),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Active Reminders & Target Dates Section
                            if (active.isNotEmpty()) {
                                item(key = "dates_header") {
                                    val scheduled = active.count { it.notificationEnabled }
                                    val review = active.count { it.needsReview }
                                    val targets = active.size - scheduled - review
                                    val nearest = active.firstOrNull { it.notificationEnabled && (it.eventTimeMs ?: 0) > state.now }
                                        ?: active.firstOrNull { !it.needsReview }
                                    ExpandInsightsRow(
                                        title = "Dates & reminders",
                                        detail = "$scheduled scheduled · $review to review · ${targets.coerceAtLeast(0)} targets",
                                        expanded = datesExpanded,
                                        onClick = { datesExpanded = !datesExpanded },
                                        subtitle = nearest?.let { "${it.title} · ${reminderDate(it)}" }
                                    )
                                }
                                if (active.any { it.legacyReview } && !legacyNoticeDismissed) {
                                    item(key = "legacy_notice") {
                                        InsightSurface {
                                            Text(
                                                text = "Review your reminders. Existing notifications are paused until you confirm them.",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            TextButton(onClick = { legacyNoticeDismissed = true }) {
                                                Text("Got it")
                                            }
                                        }
                                    }
                                }
                                if (datesExpanded) {
                                    val groups = listOf(
                                        "Scheduled" to active.filter { it.notificationEnabled },
                                        "To review" to active.filter { !it.notificationEnabled && it.needsReview },
                                        "Target dates" to active.filter { !it.notificationEnabled && !it.needsReview }
                                    )
                                    groups.forEach { (title, reminders) ->
                                        if (reminders.isNotEmpty()) {
                                            item(key = "reminder_group_$title") {
                                                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            }
                                            items(reminders, key = { "reminder_${it.id}" }) { reminder ->
                                                ReminderSummary(
                                                    item = reminder,
                                                    state = state,
                                                    open = onRecordingClick,
                                                    onReview = { editingReminder = reminder.id },
                                                    onDone = { actions.reminderStatus(reminder.id, ReminderStatus.COMPLETED) },
                                                    onDismiss = { actions.reminderStatus(reminder.id, ReminderStatus.DISMISSED) },
                                                    onDisable = { actions.disableReminder(reminder.id) },
                                                    onConvert = { creatingFrom = reminder.id; fromReminder = true }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Themes Accordion (Preserved for compatibility and deep review)
                            if (state.themes.isNotEmpty()) {
                                item(key = "themes_header") {
                                    ExpandInsightsRow(
                                        title = "Recurring themes",
                                        detail = "${state.themes.size} ${if (state.themes.size == 1) "theme" else "themes"}",
                                        expanded = themesExpanded,
                                        onClick = { themesExpanded = !themesExpanded }
                                    )
                                }
                                if (themesExpanded) {
                                    items(state.themes, key = { "theme_${it.key}" }) { theme ->
                                        InsightSurface {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.Top,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(Modifier.weight(1f)) {
                                                    Text(theme.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                    Text("Surfaced across ${theme.noteCount} notes", style = MaterialTheme.typography.bodySmall, color = if (dark) TextOnDarkSecondary else TextSecondary)
                                                }
                                                IconButton(onClick = {
                                                    actions.dismissTheme(theme.key)
                                                    scope.launch {
                                                        if (snack.showSnackbar("Theme dismissed", "Undo") == SnackbarResult.ActionPerformed) {
                                                            actions.restoreTheme(theme.key)
                                                        }
                                                    }
                                                }) {
                                                    Icon(Icons.Default.Close, contentDescription = "Dismiss this theme")
                                                }
                                            }
                                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                theme.noteIds.take(3).forEach { noteId ->
                                                    InsightSourceChip(noteId, state, onRecordingClick)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Content Feed based on selected tab
                            if (selected == "NEXT_STEPS") {
                                if (state.openTasks.isNotEmpty()) {
                                    item(key = "progress") {
                                        Text(
                                            text = "${state.openTasks.size} open · ${state.completedTasks.size} completed",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (dark) TextOnDarkSecondary else TextSecondary,
                                            modifier = Modifier.testTag("action_progress")
                                        )
                                    }
                                } else if (state.completedTasks.isNotEmpty()) {
                                    item(key = "caught_up") {
                                        InsightSurface {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = if (dark) CosmicAuroraGreen else EmeraldSuccess,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "All caught up · ${state.completedTasks.size} completed",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                                TextButton(onClick = actions.archive) {
                                                    Text("Archive", fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    item(key = "empty_tasks") {
                                        EmptyInsights(
                                            title = "No next steps",
                                            text = "Create a next step from an idea, or capture a clear commitment in a note."
                                        )
                                    }
                                }

                                items(state.openTasks, key = { it.id }) { task ->
                                    InsightTaskCard(task, state, actions, onRecordingClick)
                                }

                                if (state.completedTasks.isNotEmpty()) {
                                    item(key = "completed_header") {
                                        ExpandInsightsRow(
                                            title = "Completed",
                                            detail = "${state.completedTasks.size} tasks",
                                            expanded = completedExpanded,
                                            onClick = { completedExpanded = !completedExpanded }
                                        )
                                        if (completedExpanded && state.openTasks.isNotEmpty()) {
                                            TextButton(onClick = actions.archive) {
                                                Text("Archive completed")
                                            }
                                        }
                                    }
                                    if (completedExpanded) {
                                        items(state.completedTasks, key = { it.id }) { task ->
                                            InsightTaskCard(task, state, actions, onRecordingClick)
                                        }
                                    }
                                }
                            } else {
                                val isIdeas = selected == "IDEAS"
                                val query = if (isIdeas) ideaSearch else decisionSearch
                                val data = if (isIdeas) state.ideas else state.decisions

                                // Filter by search and optionally by selected theme
                                val themeFiltered = if (selectedThemeKey != null) {
                                    val theme = state.themes.find { it.key == selectedThemeKey }
                                    if (theme != null) data.filter { it.recordingId in theme.noteIds } else data
                                } else {
                                    data
                                }

                                val filtered = themeFiltered.filter { item ->
                                    listOf(item.text, item.rationale.orEmpty(), state.sources[item.recordingId]?.label.orEmpty())
                                        .any { it.contains(query, ignoreCase = true) }
                                }

                                if (data.isNotEmpty()) {
                                    item(key = "search_$selected") {
                                        OutlinedTextField(
                                            value = query,
                                            onValueChange = { if (isIdeas) ideaSearch = it else decisionSearch = it },
                                            label = { Text(if (isIdeas) "Search ideas" else "Search decisions") },
                                            singleLine = true,
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("insights_search"),
                                            trailingIcon = {
                                                if (query.isNotEmpty()) {
                                                    IconButton(onClick = { if (isIdeas) ideaSearch = "" else decisionSearch = "" }) {
                                                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                                                    }
                                                }
                                            }
                                        )
                                        if (query.isNotEmpty()) {
                                            Text(
                                                text = "${filtered.size} results",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (dark) TextOnDarkSecondary else TextSecondary
                                            )
                                        }
                                    }
                                }

                                if (data.isEmpty()) {
                                    item {
                                        EmptyInsights(
                                            title = if (isIdeas) "No ideas yet" else "No decisions yet",
                                            text = "Insights from your notes will appear here."
                                        )
                                    }
                                } else if (filtered.isEmpty()) {
                                    item {
                                        EmptyInsights(
                                            title = "No matching ${if (isIdeas) "ideas" else "decisions"}",
                                            text = "Try another search or clear the search field."
                                        )
                                    }
                                }

                                items(filtered, key = { it.id }) { insight ->
                                    InsightSurface {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            InsightPillBadge(
                                                text = if (isIdeas) "💡 Idea" else "⚖️ Decision",
                                                containerColor = if (isIdeas) {
                                                    if (dark) Color(0xFF4C1D95).copy(alpha = 0.35f) else Color(0xFFEDE9FE)
                                                } else {
                                                    if (dark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                                                },
                                                contentColor = if (isIdeas) {
                                                    if (dark) CosmicGlowPurple else Color(0xFF7C3AED)
                                                } else {
                                                    if (dark) TextOnDarkSecondary else Color(0xFF475569)
                                                }
                                            )
                                            Text(
                                                text = formatHumanRelativeDate(insight.createdAt),
                                                fontSize = 11.5.sp,
                                                color = if (dark) TextOnDarkSecondary.copy(alpha = 0.6f) else TextMuted
                                            )
                                        }

                                        Text(
                                            text = insight.text,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (dark) TextOnDarkPrimary else TextPrimary
                                        )

                                        if (!isIdeas && !insight.rationale.isNullOrBlank()) {
                                            Text(
                                                text = insight.rationale,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (dark) TextOnDarkSecondary else TextSecondary
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            InsightSourceChip(insight.recordingId, state, onRecordingClick)
                                            if (isIdeas) {
                                                val task = state.items.firstOrNull { it.sourceInsightId == insight.id }
                                                TextButton(
                                                    onClick = {
                                                        if (task != null) viewingTask = task.id else {
                                                            creatingFrom = insight.id
                                                            fromReminder = false
                                                        }
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text(if (task != null) "View next step" else "Create next step", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheets and Dialogs
    historyView?.let { title ->
        var count by rememberSaveable(title) { mutableIntStateOf(50) }
        ModalBottomSheet(onDismissRequest = { historyView = null }) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, false),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Text(title, style = MaterialTheme.typography.headlineSmall) }
                if (title == "Archive") {
                    if (state.archivedTasks.isEmpty()) item { Text("No archived next steps.") }
                    items(state.archivedTasks.take(count), key = { it.id }) { task ->
                        InsightSurface {
                            Text(task.text)
                            InsightSourceChip(task.recordingId, state, onRecordingClick)
                            FlowRow {
                                TextButton(onClick = { actions.restore(task.id) }) { Text("Restore") }
                                TextButton(onClick = { deleteId = task.id; deleteIsReminder = false }) { Text("Delete permanently") }
                            }
                        }
                    }
                    if (state.archivedTasks.size > count) {
                        item { TextButton(onClick = { count += 50 }) { Text("Load more") } }
                    }
                } else {
                    if (state.history.isEmpty()) item { Text("No reminder history.") }
                    items(state.history.take(count), key = { it.id }) { reminder ->
                        InsightSurface {
                            Text(reminder.title, fontWeight = FontWeight.SemiBold)
                            Text("${if (reminder.status == ReminderStatus.ACTIVE) "Past" else reminder.status.lowercase().replaceFirstChar { it.titlecase() }} · ${reminderDate(reminder)}")
                            InsightSourceChip(reminder.recordingId, state, onRecordingClick)
                            FlowRow {
                                TextButton(onClick = { editingReminder = reminder.id }) { Text("Review") }
                                TextButton(onClick = { deleteId = reminder.id; deleteIsReminder = true }) { Text("Delete permanently") }
                            }
                        }
                    }
                    if (state.history.size > count) {
                        item { TextButton(onClick = { count += 50 }) { Text("Load more") } }
                    }
                }
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    deleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text("Delete permanently?") },
            text = { Text("This removes the saved item and cancels any linked reminder. It cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    if (deleteIsReminder) actions.deleteReminder(id) else actions.deleteTask(id)
                    deleteId = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteId = null }) { Text("Cancel") }
            }
        )
    }

    editingReminder?.let { id ->
        state.reminders.find { it.id == id }?.let { item ->
            ReminderEditor(
                item = item,
                actionError = actionError,
                onDismiss = { editingReminder = null },
                onSave = { updated, allowed -> actions.saveReminder(updated, allowed) }
            )
        }
    }

    creatingFrom?.let { id ->
        val text = if (fromReminder) state.reminders.find { it.id == id }?.title else state.items.find { it.id == id }?.text
        if (text != null) {
            InsightTaskEditor(
                sourceId = id,
                initialText = text,
                actionError = actionError,
                onDismiss = { creatingFrom = null },
                onSave = { task, reminder -> actions.createTask(id, fromReminder, task, reminder) }
            )
        }
    }

    viewingTask?.let { id ->
        state.items.find { it.id == id }?.let { task ->
            AlertDialog(
                onDismissRequest = { viewingTask = null },
                title = { Text("Next step") },
                text = {
                    Column {
                        Text(task.text)
                        Text(task.status.lowercase().replaceFirstChar { it.titlecase() })
                        InsightSourceChip(task.recordingId, state, onRecordingClick)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (task.status == InsightStatus.ARCHIVED) actions.restore(task.id) else actions.toggle(task.id)
                        viewingTask = null
                    }) {
                        Text(if (task.status == InsightStatus.ARCHIVED) "Restore" else if (task.status == InsightStatus.COMPLETED) "Reopen" else "Mark done")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewingTask = null }) { Text("Close") }
                }
            )
        }
    }
}
