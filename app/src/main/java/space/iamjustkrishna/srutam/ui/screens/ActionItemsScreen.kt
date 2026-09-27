package space.iamjustkrishna.srutam.ui.screens

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import space.iamjustkrishna.srutam.data.*
import space.iamjustkrishna.srutam.utils.InsightSource
import space.iamjustkrishna.srutam.utils.InsightNames
import space.iamjustkrishna.srutam.viewmodel.*

enum class InsightsTab(val label: String) { NEXT_STEPS("Next Steps"), IDEAS("Ideas"), DECISIONS("Decisions") }

data class InsightsActions(
    val toggle: (String) -> Unit = {}, val archive: () -> Unit = {},
    val restore: (String) -> Unit = {}, val deleteTask: (String) -> Unit = {},
    val reminderStatus: (String, String) -> Unit = { _, _ -> },
    val disableReminder: (String) -> Unit = {}, val deleteReminder: (String) -> Unit = {},
    val saveReminder: (ReminderEntity, Boolean) -> Unit = { _, _ -> },
    val createTask: (String, Boolean, String, ReminderEntity?) -> Unit = { _, _, _, _ -> },
    val dismissTheme: (String) -> Unit = {}, val restoreTheme: (String) -> Unit = {},
    val retry: () -> Unit = {}
)

@Composable
fun ActionItemsScreen(onRecordingClick: (Long) -> Unit, onSettingsClick: () -> Unit,
    viewModel: AudioFilesViewModel, modifier: Modifier = Modifier) {
    InsightsScreen(onRecordingClick, onSettingsClick, modifier)
}

@Composable
fun InsightsScreen(onRecordingClick: (Long) -> Unit, onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier, model: InsightsViewModel = viewModel()) {
    val state by model.state.collectAsState()
    val error by model.actionError.collectAsState()
    val created by model.createdTask.collectAsState()
    val savedReminder by model.savedReminder.collectAsState()
    val memory = remember(model) { model.readScreenMemory() }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, model) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) model.onResume() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    InsightsContent(state, onRecordingClick, onSettingsClick, modifier = modifier,
        actionError = error, clearError = model::clearError, createdTaskId = created,
        consumeCreatedTask = model::consumeCreatedTask,
        savedMemory = memory, onSaveMemory = model::saveScreenMemory,
        savedReminderId = savedReminder, consumeSavedReminder = model::consumeSavedReminder,
        actions = InsightsActions(model::toggleTask, model::archive, model::restore, model::deleteTask,
            model::reminderStatus, model::disableReminder, model::deleteReminder, model::saveReminder,
            model::createTask, model::dismissTheme, model::restoreTheme, model::retry))
}

/** Stateless adapter retained for existing previews and tablet preview fixtures. */
@Composable
fun ActionItemsContent(
    activeActions: List<InsightEntity>, allIdeas: List<InsightEntity>, allDecisions: List<InsightEntity>,
    themeClusters: List<ThemeCluster> = emptyList(), archivedActionsCount: Int = 0,
    upcomingReminders: List<ReminderEntity> = emptyList(), pastReminders: List<ReminderEntity> = emptyList(),
    initialTab: InsightsTab = InsightsTab.NEXT_STEPS, onRecordingClick: (Long) -> Unit = {},
    onSettingsClick: () -> Unit = {}, onActionToggle: (InsightEntity) -> Unit = {},
    onReminderComplete: (String) -> Unit = {}, onArchiveConfirmed: () -> Unit = {},
    onRestoreArchived: () -> Unit = {}, onDismissTheme: (String) -> Unit = {}, modifier: Modifier = Modifier
) {
    val all = activeActions + allIdeas + allDecisions
    val sources = all.associate { item ->
        val recording = Recording(id = item.recordingId, timestamp = item.createdAt, name = item.recordingName, audioFilePath = "")
        item.recordingId to InsightNames.source(item.recordingId, mapOf(item.recordingId to recording))
    } + (upcomingReminders + pastReminders).associate { it.recordingId to InsightSource(it.recordingId, it.recordingName, true) }
    InsightsContent(InsightsUiState(true, items = all, reminders = upcomingReminders + pastReminders,
        sources = sources, themes = themeClusters), onRecordingClick, onSettingsClick, initialTab, modifier,
        actions = InsightsActions(toggle = { id -> all.find { it.id == id }?.let(onActionToggle) },
            archive = onArchiveConfirmed, restore = { onRestoreArchived() },
            reminderStatus = { id, _ -> onReminderComplete(id) }, dismissTheme = onDismissTheme))
}
