@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package space.iamjustkrishna.srutam.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    consumeSavedReminder: () -> Unit = {},
    accentFontSize: androidx.compose.ui.unit.TextUnit = 20.sp,
    externalSelectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate?) -> Unit)? = null
) {
    var selection by rememberSaveable { mutableStateOf(initialTab?.name ?: savedMemory.selection) }
    var remindersExpanded by rememberSaveable { mutableStateOf(savedMemory.datesExpanded) }
    var themesExpanded by rememberSaveable { mutableStateOf(savedMemory.themesExpanded) }
    var completedExpanded by rememberSaveable { mutableStateOf(savedMemory.completedExpanded) }
    var ideaSearch by rememberSaveable { mutableStateOf(savedMemory.ideaSearch) }
    var decisionSearch by rememberSaveable { mutableStateOf(savedMemory.decisionSearch) }
    var selectedThemeKey by rememberSaveable { mutableStateOf<String?>(null) }
    var historyView by rememberSaveable { mutableStateOf<String?>(null) }
    var editingReminder by rememberSaveable { mutableStateOf<String?>(null) }
    var creatingFrom by rememberSaveable { mutableStateOf<String?>(null) }
    var creatingSession by rememberSaveable { mutableIntStateOf(0) }
    var fromReminder by rememberSaveable { mutableStateOf(false) }
    var viewingTask by rememberSaveable { mutableStateOf<String?>(null) }
    var viewingIdeaId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteIsReminder by rememberSaveable { mutableStateOf(false) }
    var localSelectedDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    val selectedDate = if (onDateSelected != null) externalSelectedDate else localSelectedDate
    val updateDate: (LocalDate?) -> Unit = { date ->
        if (onDateSelected != null) {
            onDateSelected(date)
        } else {
            localSelectedDate = date
        }
    }

    val datesWithActivity = remember(state.ideas, state.decisions, state.openTasks, state.completedTasks, state.activeReminders) {
        val zone = ZoneId.systemDefault()
        val set = mutableSetOf<LocalDate>()
        state.ideas.forEach { runCatching { set.add(Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()) } }
        state.decisions.forEach { runCatching { set.add(Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()) } }
        state.openTasks.forEach { runCatching { set.add(Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()) } }
        state.completedTasks.forEach { task ->
            runCatching { set.add(Instant.ofEpochMilli(task.createdAt).atZone(zone).toLocalDate()) }
            task.completedAt?.let { runCatching { set.add(Instant.ofEpochMilli(it).atZone(zone).toLocalDate()) } }
        }
        state.activeReminders.forEach { reminder ->
            reminder.localDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }?.let { set.add(it) }
                ?: reminder.eventTimeMs?.let { runCatching { set.add(Instant.ofEpochMilli(it).atZone(zone).toLocalDate()) } }
        }
        set
    }

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
                selection, ideaSearch, decisionSearch, remindersExpanded, themesExpanded, completedExpanded,
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
        createdTaskId?.let { taskId ->
            creatingFrom = null
            if (snack.showSnackbar("Next step created", "View") == SnackbarResult.ActionPerformed) {
                val createdTask = state.items.find { it.id == taskId }
                val parentIdea = createdTask?.sourceInsightId?.let { sId ->
                    state.ideas.find { it.id == sId } ?: state.items.find { it.id == sId }
                }
                if (parentIdea != null && parentIdea.kind == InsightKind.IDEA) {
                    viewingIdeaId = parentIdea.id
                } else {
                    viewingTask = taskId
                }
            }
            consumeCreatedTask()
        }
    }

    CompositionLocalProvider(LocalContentColor provides if (dark) TextOnDarkPrimary else TextPrimary) {
        Scaffold(
            modifier = modifier,
            containerColor = if (dark) CosmicVoidBackground else Color(0xFFF4F5F8),
            snackbarHost = {
                SnackbarHost(
                    hostState = snack,
                    modifier = Modifier.padding(bottom = 90.dp, start = 16.dp, end = 16.dp)
                ) { data ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (dark) CosmicVoidCard else Color(0xFF1E293B),
                        border = BorderStroke(1.dp, if (dark) CosmicGlowBlue.copy(alpha = 0.5f) else Color(0xFF334155)),
                        shadowElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (dark) CosmicAuroraGreen else EmeraldSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = data.visuals.message,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                            data.visuals.actionLabel?.let { label ->
                                Surface(
                                    onClick = { data.performAction() },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (dark) CobaltBlue.copy(alpha = 0.35f) else Color(0xFF334155),
                                    border = BorderStroke(1.dp, if (dark) CosmicGlowBlue.copy(alpha = 0.5f) else Color(0xFF475569))
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (dark) CosmicGlowBlue else Color(0xFF93C5FD),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            topBar = {
                SrutamTopAppBar(
                    title = "Srutam",
                    accentText = "Insights",
                    accentFontSize = accentFontSize,
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

                // Compact Date Scroller with inline theme chips (Month/Year next to scroller)
                InsightsDateScroller(
                    selectedDate = selectedDate,
                    onDateSelected = updateDate,
                    datesWithActivity = datesWithActivity,
                    earliestMonth = state.earliestMonth,
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
                    }
                )

                val remindersToReview = remember(active) { active.filter { it.needsReview } }

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
                            // Dates & Reminders to Review Horizontal Carousel (confined to Next Steps)
                            if (selected == "NEXT_STEPS" && remindersToReview.isNotEmpty()) {
                                item(key = "review_carousel") {
                                    ReminderReviewCarousel(
                                        remindersToReview = remindersToReview,
                                        state = state,
                                        onRecordingClick = onRecordingClick,
                                        onReview = { reminder -> editingReminder = reminder.id },
                                        onDismiss = { reminder -> actions.reminderStatus(reminder.id, ReminderStatus.DISMISSED) },
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }
                            // Dates & reminders: Compact glance bar with inline horizontal LazyRow expansion
                            val displayReminders = if (selectedDate != null) {
                                active.filter { reminder ->
                                    val rDate = reminder.localDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                                        ?: reminder.eventTimeMs?.let { runCatching { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }.getOrNull() }
                                    rDate == selectedDate && reminder.notificationEnabled
                                }
                            } else {
                                active.filter { !it.needsReview }
                            }
                            if (displayReminders.isNotEmpty()) {
                                item(key = "reminders_glance_bar") {
                                    CompactRemindersGlanceBar(
                                        reminders = displayReminders,
                                        expanded = remindersExpanded,
                                        onClick = { remindersExpanded = !remindersExpanded },
                                        nowMs = state.now,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("reminders_glance_bar")
                                    )
                                }
                                if (remindersExpanded) {
                                    item(key = "reminders_lazy_row") {
                                        val rowState = rememberLazyListState()
                                        val cardWidth = ((LocalConfiguration.current.screenWidthDp - 32) * 0.82f).dp
                                        val flingBehavior = rememberSnapFlingBehavior(rowState)
                                        LazyRow(
                                            state = rowState,
                                            flingBehavior = flingBehavior,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .testTag("reminders_lazy_row")
                                        ) {
                                            items(displayReminders, key = { it.id }) { reminder ->
                                                CompactReminderCard(
                                                    item = reminder,
                                                    state = state,
                                                    open = onRecordingClick,
                                                    onReview = { editingReminder = reminder.id },
                                                    onDone = { actions.reminderStatus(reminder.id, ReminderStatus.COMPLETED) },
                                                    onDismiss = { actions.reminderStatus(reminder.id, ReminderStatus.DISMISSED) },
                                                    nowMs = state.now,
                                                    modifier = Modifier.width(cardWidth)
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
                                val openTasks = if (selectedDate != null) {
                                    state.openTasks.filter { task ->
                                        runCatching {
                                            Instant.ofEpochMilli(task.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
                                        }.getOrNull() == selectedDate
                                    }
                                } else {
                                    state.openTasks
                                }

                                val completedTasks = if (selectedDate != null) {
                                    state.completedTasks.filter { task ->
                                        val zone = ZoneId.systemDefault()
                                        val createdDate = runCatching {
                                            Instant.ofEpochMilli(task.createdAt).atZone(zone).toLocalDate()
                                        }.getOrNull()
                                        val completedDate = task.completedAt?.let {
                                            runCatching {
                                                Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
                                            }.getOrNull()
                                        }
                                        createdDate == selectedDate || completedDate == selectedDate
                                    }
                                } else {
                                    state.completedTasks
                                }

                                if (openTasks.isNotEmpty()) {
                                    item(key = "progress") {
                                        Text(
                                            text = if (selectedDate != null) {
                                                if (completedTasks.isNotEmpty()) {
                                                    "${openTasks.size} open · ${completedTasks.size} completed on ${selectedDate!!.format(DateTimeFormatter.ofPattern("MMM d"))}"
                                                } else {
                                                    "${openTasks.size} open on ${selectedDate!!.format(DateTimeFormatter.ofPattern("MMM d"))}"
                                                }
                                            } else {
                                                "${openTasks.size} open · ${completedTasks.size} completed"
                                            },
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (dark) TextOnDarkSecondary else TextSecondary,
                                            modifier = Modifier.testTag("action_progress")
                                        )
                                    }
                                } else if (completedTasks.isNotEmpty()) {
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
                                                        text = if (selectedDate != null) {
                                                            "All caught up · ${completedTasks.size} completed on ${selectedDate!!.format(DateTimeFormatter.ofPattern("MMM d"))}"
                                                        } else {
                                                            "All caught up · ${completedTasks.size} completed"
                                                        },
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
                                } else if (selectedDate != null) {
                                    item(key = "empty_tasks_date") {
                                        EmptyInsights(
                                            title = "No next steps on ${selectedDate!!.format(DateTimeFormatter.ofPattern("MMM d"))}",
                                            text = "Next steps created on this date will appear here."
                                        )
                                    }
                                    item(key = "clear_date_tasks") {
                                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            TextButton(onClick = { updateDate(null) }) { Text("Show all dates") }
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

                                items(openTasks, key = { it.id }) { task ->
                                    InsightTaskCard(task, state, actions, onRecordingClick)
                                }

                                if (completedTasks.isNotEmpty()) {
                                    item(key = "completed_header") {
                                        ExpandInsightsRow(
                                            title = "Completed",
                                            detail = "${completedTasks.size} task${if (completedTasks.size == 1) "" else "s"}",
                                            expanded = completedExpanded,
                                            onClick = { completedExpanded = !completedExpanded }
                                        )
                                        if (completedExpanded && (openTasks.isNotEmpty() || selectedDate == null)) {
                                            TextButton(onClick = actions.archive) {
                                                Text("Archive completed")
                                            }
                                        }
                                    }
                                    if (completedExpanded) {
                                        items(completedTasks, key = { it.id }) { task ->
                                            InsightTaskCard(task, state, actions, onRecordingClick)
                                        }
                                    }
                                }
                            } else {
                                val isIdeas = selected == "IDEAS"
                                val baseData = if (isIdeas) state.ideas else state.decisions

                                // Filter by selected calendar date
                                val dateFiltered = if (selectedDate != null) {
                                    baseData.filter { item ->
                                        runCatching {
                                            Instant.ofEpochMilli(item.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
                                        }.getOrNull() == selectedDate
                                    }
                                } else {
                                    baseData
                                }

                                // Filter optionally by selected theme
                                val filtered = if (selectedThemeKey != null) {
                                    val theme = state.themes.find { it.key == selectedThemeKey }
                                    if (theme != null) dateFiltered.filter { it.recordingId in theme.noteIds } else dateFiltered
                                } else {
                                    dateFiltered
                                }

                                if (baseData.isEmpty()) {
                                    item {
                                        EmptyInsights(
                                            title = if (isIdeas) "No ideas yet" else "No decisions yet",
                                            text = "Insights from your notes will appear here."
                                        )
                                    }
                                } else if (selectedDate != null && dateFiltered.isEmpty()) {
                                    item {
                                        EmptyInsights(
                                            title = "No ${if (isIdeas) "ideas" else "decisions"} on ${selectedDate!!.format(DateTimeFormatter.ofPattern("MMM d"))}",
                                            text = "Notes or insights captured on this date will appear here."
                                        )
                                    }
                                    item {
                                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            TextButton(onClick = { updateDate(null) }) {
                                                Text("Show all dates")
                                            }
                                        }
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
                                    val steps = if (isIdeas) {
                                        state.items.filter { it.sourceInsightId == insight.id && it.status != InsightStatus.ARCHIVED }
                                    } else emptyList()
                                    InsightAccentCard(
                                        insight = insight,
                                        isIdea = isIdeas,
                                        state = state,
                                        open = onRecordingClick,
                                        steps = steps,
                                        hasNextStep = steps.isNotEmpty(),
                                        onNextStep = {
                                            if (steps.isNotEmpty()) {
                                                viewingIdeaId = insight.id
                                            } else {
                                                creatingSession++
                                                creatingFrom = insight.id
                                                fromReminder = false
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    historyView?.let { title ->
        var count by rememberSaveable(title) { mutableIntStateOf(50) }
        val maxSheetHeight = (LocalConfiguration.current.screenHeightDp * 0.60f).dp
        ModalBottomSheet(
            onDismissRequest = { historyView = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = if (dark) CosmicVoidCard else CeramicWhite,
            scrimColor = Color.Black.copy(alpha = 0.45f),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 40.dp, height = 4.5.dp)
                        .background(if (dark) CosmicVoidCardBorder else Color(0xFFD1D1D6), CircleShape)
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
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (title == "Archive") Icons.Default.Archive else Icons.Default.History,
                            contentDescription = null,
                            tint = if (dark) CosmicGlowBlue else CobaltBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (dark) TextOnDarkPrimary else TextPrimary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (title == "Reminder history" && state.history.isNotEmpty()) {
                            TextButton(
                                onClick = { actions.clearReminderHistory() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Clear history",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        IconButton(
                            onClick = { historyView = null },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = if (dark) TextOnDarkSecondary else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                if (title == "Reminder history") {
                    Text(
                        text = "Keeps history of past & completed reminders for the last 3 days.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (dark) TextOnDarkSecondary else TextMuted,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 6.dp)
                    )
                }
                HorizontalDivider(
                    color = if (dark) CosmicVoidCardBorder else SlateBorder,
                    thickness = 1.dp
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, false),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (title == "Archive") {
                        if (state.archivedTasks.isEmpty()) item {
                            Text(
                                "No archived next steps.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (dark) TextOnDarkSecondary else TextSecondary,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                        items(state.archivedTasks.take(count), key = { it.id }) { task ->
                            InsightSurface {
                                Text(
                                    task.text,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (dark) TextOnDarkPrimary else TextPrimary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    InsightSourceChip(
                                        id = task.recordingId,
                                        state = state,
                                        open = onRecordingClick,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        TextButton(
                                            onClick = { actions.restore(task.id) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Restore", fontSize = 12.sp)
                                        }
                                        TextButton(
                                            onClick = { deleteId = task.id; deleteIsReminder = false },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                "Delete",
                                                color = MaterialTheme.colorScheme.error,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (state.archivedTasks.size > count) {
                            item { TextButton(onClick = { count += 50 }) { Text("Load more") } }
                        }
                    } else {
                        if (state.history.isEmpty()) item {
                            Text(
                                "No reminder history.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (dark) TextOnDarkSecondary else TextSecondary,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                        items(state.history.take(count), key = { it.id }) { reminder ->
                            InsightSurface {
                                Text(
                                    reminder.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (dark) TextOnDarkPrimary else TextPrimary
                                )
                                Text(
                                    "${if (reminder.status == ReminderStatus.ACTIVE) "Past" else reminder.status.lowercase().replaceFirstChar { it.titlecase() }} · ${reminderDate(reminder)}",
                                    fontSize = 12.sp,
                                    color = if (dark) TextOnDarkSecondary else TextMuted
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    InsightSourceChip(
                                        id = reminder.recordingId,
                                        state = state,
                                        open = onRecordingClick,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    val resolvedAt = reminder.confirmedAt
                                    val canUndo = reminder.status == ReminderStatus.COMPLETED &&
                                        resolvedAt != null && state.now - resolvedAt <= 24L * 60 * 60 * 1000
                                    if (canUndo) {
                                        TextButton(
                                            onClick = { actions.undoReminder(reminder.id) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text("Undo done", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    IconButton(
                                        onClick = { actions.deleteReminder(reminder.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove from history",
                                            tint = if (dark) TextOnDarkSecondary else TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (state.history.size > count) {
                            item { TextButton(onClick = { count += 50 }) { Text("Load more") } }
                        }
                    }
                }
            }
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
                onSave = { updated, allowed -> actions.saveReminder(updated, allowed) },
                onConvertToTask = {
                    creatingFrom = item.id
                    fromReminder = true
                    editingReminder = null
                },
                onMarkDone = {
                    actions.reminderStatus(item.id, ReminderStatus.COMPLETED)
                    editingReminder = null
                }
            )
        }
    }

    creatingFrom?.let { id ->
        val text = if (fromReminder) {
            state.reminders.find { it.id == id }?.title
        } else {
            val hasExistingSteps = state.items.any { it.sourceInsightId == id && it.status != InsightStatus.ARCHIVED }
            if (hasExistingSteps) "" else state.items.find { it.id == id }?.text
        }
        if (text != null) {
            key(id, creatingSession) {
                InsightTaskEditor(
                    sourceId = "${id}_$creatingSession",
                    initialText = text,
                    actionError = actionError,
                    onDismiss = { creatingFrom = null },
                    onSave = { task, reminder -> actions.createTask(id, fromReminder, task, reminder) }
                )
            }
        }
    }

    viewingIdeaId?.let { ideaId ->
        val idea = state.ideas.find { it.id == ideaId } ?: state.items.find { it.id == ideaId }
        val steps = state.items.filter { it.sourceInsightId == ideaId && it.status != InsightStatus.ARCHIVED }
            .sortedBy { step ->
                step.status == InsightStatus.COMPLETED ||
                    state.reminders.any { it.linkedTaskId == step.id && it.status == ReminderStatus.COMPLETED }
            }
        if (idea != null) {
            IdeaStepsDialog(
                idea = idea,
                steps = steps,
                state = state,
                onRecordingClick = onRecordingClick,
                onToggleStep = { stepId -> actions.toggle(stepId) },
                onDeleteStep = { stepId ->
                    deleteId = stepId
                    deleteIsReminder = false
                },
                onAddStep = {
                    val currentIdeaId = ideaId
                    viewingIdeaId = null
                    creatingSession++
                    creatingFrom = currentIdeaId
                    fromReminder = false
                },
                onDismiss = { viewingIdeaId = null }
            )
        }
    }

    viewingTask?.let { id ->
        state.items.find { it.id == id }?.let { task ->
            val parentIdea = task.sourceInsightId?.let { sourceId ->
                state.ideas.find { it.id == sourceId } ?: state.items.find { it.id == sourceId }
            }
            if (parentIdea != null && parentIdea.kind == InsightKind.IDEA) {
                LaunchedEffect(id) {
                    viewingTask = null
                    viewingIdeaId = parentIdea.id
                }
                return@let
            }

            val isCompleted = task.status == InsightStatus.COMPLETED ||
                state.reminders.any { it.linkedTaskId == task.id && it.status == ReminderStatus.COMPLETED }
            val isArchived = task.status == InsightStatus.ARCHIVED
            AlertDialog(
                onDismissRequest = { viewingTask = null },
                shape = RoundedCornerShape(24.dp),
                containerColor = if (dark) CosmicVoidCard else CeramicWhite,
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (dark) CobaltBlue.copy(alpha = 0.25f) else CobaltContainer,
                                border = BorderStroke(1.dp, if (dark) CosmicGlowBlue.copy(alpha = 0.4f) else CobaltBorder),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.TaskAlt,
                                        contentDescription = null,
                                        tint = if (dark) CosmicGlowBlue else CobaltBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Next step",
                                fontFamily = PlayfairDisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = if (dark) TextOnDarkPrimary else TextPrimary
                            )
                        }

                        IconButton(
                            onClick = { viewingTask = null },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = if (dark) TextOnDarkSecondary else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = task.text,
                            fontSize = 15.sp,
                            fontWeight = if (isCompleted) FontWeight.Normal else FontWeight.Medium,
                            lineHeight = 22.sp,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (isCompleted) {
                                if (dark) TextOnDarkSecondary.copy(alpha = 0.6f) else TextMuted
                            } else {
                                if (dark) TextOnDarkPrimary else TextPrimary
                            }
                        )

                        HorizontalDivider(
                            color = if (dark) CosmicVoidCardBorder else SlateBorder.copy(alpha = 0.7f),
                            thickness = 0.8.dp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Source Note",
                                fontSize = 12.sp,
                                color = if (dark) TextOnDarkSecondary else TextMuted
                            )
                            InsightSourceChip(task.recordingId, state, onRecordingClick, compact = false)
                        }
                    }
                },
                confirmButton = {
                    if (!isCompleted || isArchived) {
                        Button(
                            onClick = {
                                if (task.status == InsightStatus.ARCHIVED) actions.restore(task.id) else actions.toggle(task.id)
                                viewingTask = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    isArchived -> if (dark) CosmicGlowBlue else CobaltBlue
                                    else -> if (dark) CosmicAuroraGreen else EmeraldSuccess
                                },
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = if (task.status == InsightStatus.ARCHIVED) "Restore task" else "Mark as done",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                actions.toggle(task.id)
                                viewingTask = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (dark) CosmicVoidCardBorder else SlateGrouped,
                                contentColor = if (dark) TextOnDarkPrimary else TextPrimary
                            )
                        ) {
                            Text(
                                text = "Reopen task",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun IdeaStepsDialog(
    idea: InsightEntity,
    steps: List<InsightEntity>,
    state: InsightsUiState,
    onRecordingClick: (Long) -> Unit,
    onToggleStep: (String) -> Unit,
    onDeleteStep: (String) -> Unit,
    onAddStep: () -> Unit,
    onDismiss: () -> Unit
) {
    val dark = LocalIsCosmicDark.current
    val isStepDone = { step: InsightEntity ->
        step.status == InsightStatus.COMPLETED ||
            state.reminders.any { it.linkedTaskId == step.id && it.status == ReminderStatus.COMPLETED }
    }
    val completedCount = steps.count { isStepDone(it) }
    val allCompleted = steps.isNotEmpty() && completedCount == steps.size

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = if (dark) CosmicVoidCard else CeramicWhite,
            border = BorderStroke(1.dp, if (dark) CosmicVoidCardBorder else SlateBorder),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Playfair Display title, counter, and single 'X' close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (dark) CobaltBlue.copy(alpha = 0.25f) else CobaltContainer,
                            border = BorderStroke(1.dp, if (dark) CosmicGlowBlue.copy(alpha = 0.4f) else CobaltBorder),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.TaskAlt,
                                    contentDescription = null,
                                    tint = if (dark) CosmicGlowBlue else CobaltBlue,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Next steps",
                                fontFamily = PlayfairDisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = if (dark) TextOnDarkPrimary else TextPrimary
                            )
                            if (steps.isNotEmpty()) {
                                Text(
                                    text = "$completedCount of ${steps.size} completed",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (allCompleted) {
                                        if (dark) CosmicAuroraGreen else EmeraldSuccess
                                    } else {
                                        if (dark) TextOnDarkSecondary else TextMuted
                                    }
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (dark) TextOnDarkSecondary else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Idea reference card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (dark) CosmicVoidCard.copy(alpha = 0.7f) else SlateGrouped.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, if (dark) CosmicVoidCardBorder else SlateBorder.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (dark) Color(0xFF4C1D95).copy(alpha = 0.4f) else Color(0xFFEDE9FE)
                            ) {
                                Text(
                                    text = "IDEA",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (dark) CosmicGlowPurple else Color(0xFF7C3AED),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            InsightSourceChip(
                                id = idea.recordingId,
                                state = state,
                                open = onRecordingClick,
                                compact = true
                            )
                        }
                        Text(
                            text = idea.text,
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.Normal,
                            color = if (dark) TextOnDarkPrimary else TextPrimary,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Steps list (scrollable if many)
                if (steps.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        steps.forEach { step ->
                            val linkedReminder = state.reminders.find { it.linkedTaskId == step.id }
                                ?: state.activeReminders.find { it.linkedTaskId == step.id }
                            val isDone = isStepDone(step)

                            Surface(
                                onClick = { onToggleStep(step.id) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (dark) {
                                    if (isDone) CosmicVoidCard.copy(alpha = 0.35f) else CosmicVoidCard
                                } else {
                                    if (isDone) SlateSurface.copy(alpha = 0.5f) else CeramicWhite
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isDone) {
                                        if (dark) CosmicVoidCardBorder.copy(alpha = 0.5f) else SlateBorder.copy(alpha = 0.5f)
                                    } else {
                                        if (dark) CosmicVoidCardBorder else SlateBorder
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 6.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { onToggleStep(step.id) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = if (isDone) "Mark not done" else "Mark done",
                                            tint = if (isDone) {
                                                if (dark) CosmicAuroraGreen else EmeraldSuccess
                                            } else {
                                                if (dark) TextOnDarkSecondary.copy(alpha = 0.6f) else TextMuted
                                            },
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(vertical = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = step.text,
                                            fontSize = 14.sp,
                                            lineHeight = 19.sp,
                                            fontWeight = if (isDone) FontWeight.Normal else FontWeight.Medium,
                                            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                                            color = if (isDone) {
                                                if (dark) TextOnDarkSecondary.copy(alpha = 0.5f) else TextMuted
                                            } else {
                                                if (dark) TextOnDarkPrimary else TextPrimary
                                            }
                                        )

                                        if (linkedReminder != null) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Alarm,
                                                    contentDescription = null,
                                                    tint = if (dark) CosmicGlowBlue else CobaltBlue,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = reminderDate(linkedReminder),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (dark) CosmicGlowBlue else CobaltBlue
                                                )
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = { onDeleteStep(step.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete step",
                                            tint = if (dark) TextOnDarkSecondary.copy(alpha = 0.5f) else TextMuted.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // "+ Add another step" button
                Surface(
                    onClick = onAddStep,
                    shape = RoundedCornerShape(12.dp),
                    color = if (dark) CobaltBlue.copy(alpha = 0.15f) else CobaltContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, if (dark) CosmicGlowBlue.copy(alpha = 0.35f) else CobaltBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = if (dark) CosmicGlowBlue else CobaltBlue,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (steps.isEmpty()) "Add next step" else "Add another step",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (dark) CosmicGlowBlue else CobaltBlue
                        )
                    }
                }
            }
        }
    }
}
