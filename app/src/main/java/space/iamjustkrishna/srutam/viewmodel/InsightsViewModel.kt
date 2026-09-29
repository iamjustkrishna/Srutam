package space.iamjustkrishna.srutam.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import space.iamjustkrishna.srutam.data.*
import space.iamjustkrishna.srutam.repository.InsightsRepository
import space.iamjustkrishna.srutam.utils.AppPreferences
import space.iamjustkrishna.srutam.utils.InsightNames
import space.iamjustkrishna.srutam.utils.InsightSource
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class InsightsUiState(
    val loaded: Boolean = false,
    val error: String? = null,
    val items: List<InsightEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val sources: Map<Long, InsightSource> = emptyMap(),
    val themes: List<ThemeCluster> = emptyList(),
    val now: Long = System.currentTimeMillis(),
    val earliestMonth: YearMonth = YearMonth.now()
) {
    val openTasks get() = items.filter { it.kind == InsightKind.ACTION && it.status == InsightStatus.OPEN }
    val completedTasks get() = items.filter { it.kind == InsightKind.ACTION && it.status == InsightStatus.COMPLETED }
    val archivedTasks get() = items.filter { it.kind == InsightKind.ACTION && it.status == InsightStatus.ARCHIVED }
    val ideas get() = items.filter { it.kind == InsightKind.IDEA && it.status != InsightStatus.ARCHIVED }
    val decisions get() = items.filter { it.kind == InsightKind.DECISION && it.status != InsightStatus.ARCHIVED }
    val activeReminders get() = reminders.filterNot { inHistory(it) }.sortedWith(
        compareBy<ReminderEntity> { it.eventTimeMs ?: Long.MAX_VALUE }.thenBy { it.id }
    )
    private val THREE_DAYS_MS = 3L * 24 * 60 * 60 * 1000

    val history get() = reminders
        .filter { inHistory(it) }
        .filter { item ->
            val resolvedAt = item.confirmedAt.takeIf { item.status == ReminderStatus.COMPLETED } ?: 0L
            val time = maxOf(item.eventTimeMs ?: item.createdAt, resolvedAt)
            (now - time) <= THREE_DAYS_MS
        }
        .sortedByDescending { it.eventTimeMs ?: it.createdAt }

    fun inHistory(item: ReminderEntity): Boolean {
        if (item.status != ReminderStatus.ACTIVE) return true
        if (item.needsReview || item.type == ReminderType.MILESTONE) return false
        if (item.timePrecision == "EXACT") return (item.eventTimeMs ?: Long.MAX_VALUE) < now
        val date = runCatching { LocalDate.parse(item.localDate) }.getOrNull() ?: return false
        val zone = runCatching { ZoneId.of(item.zoneId) }.getOrDefault(ZoneId.systemDefault())
        return date.isBefore(java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate())
    }

    fun initialTab(): String = when {
        openTasks.isNotEmpty() -> "NEXT_STEPS"
        ideas.isNotEmpty() -> "IDEAS"
        decisions.isNotEmpty() -> "DECISIONS"
        else -> "NEXT_STEPS"
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class InsightsViewModel(application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    private val repository = InsightsRepository.from(application)
    private val refresh = MutableStateFlow(0)
    private val dismissed = MutableStateFlow(AppPreferences.getDismissedThemes(application).toSet())
    private val _actionError = MutableStateFlow<String?>(null)
    val actionError = _actionError.asStateFlow()
    private val _createdTask = MutableStateFlow<String?>(null)
    val createdTask = _createdTask.asStateFlow()
    private val _savedReminder = MutableStateFlow<String?>(null)
    val savedReminder = _savedReminder.asStateFlow()
    fun readScreenMemory() = InsightsScreenMemory.from(savedStateHandle["insightsUi"])
    fun saveScreenMemory(memory: InsightsScreenMemory) { savedStateHandle["insightsUi"] = memory.toBundle() }
    private val clock = flow {
        while (true) { emit(System.currentTimeMillis()); delay(30_000) }
    }
    val state = refresh.flatMapLatest {
        combine(repository.insights, repository.reminders, repository.recordings, dismissed, clock) { items, reminders, recordings, hidden, now ->
            val byId = recordings.associateBy { it.id }
            val earliestMs = listOfNotNull(
                recordings.minOfOrNull { it.timestamp },
                items.minOfOrNull { it.createdAt },
                reminders.minOfOrNull { it.createdAt }
            ).minOrNull()
            val earliestMonth = earliestMs?.let {
                Instant.ofEpochMilli(it)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .let { d -> YearMonth.from(d) }
            } ?: YearMonth.now()
            InsightsUiState(
                loaded = true,
                items = items.sortedWith(compareByDescending<InsightEntity> { it.createdAt }.thenBy { it.id }),
                reminders = reminders,
                sources = recordings.associate { it.id to InsightNames.source(it.id, byId) } +
                    (SourceIds.CHAT to InsightNames.source(SourceIds.CHAT, byId)),
                themes = ThemeClusterEngine.build(recordings, hidden), now = now,
                earliestMonth = earliestMonth
            )
        }.catch { e ->
            if (e is CancellationException) throw e
            emit(InsightsUiState(error = "Could not load Insights. Please retry."))
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())

    init { action { repository.importLegacy() } }

    fun retry() { refresh.value += 1 }
    fun clearError() { _actionError.value = null }
    fun consumeCreatedTask() { _createdTask.value = null }
    fun consumeSavedReminder() { _savedReminder.value = null }
    fun onResume() { refresh.value += 1; action { repository.reconcile() } }
    fun toggleTask(id: String) = action { repository.toggleTask(id) }
    fun archive() = action { repository.archiveCompleted() }
    fun restore(id: String) = action { repository.restoreTask(id) }
    fun deleteTask(id: String) = action { repository.deleteTask(id) }
    fun reminderStatus(id: String, status: String) = action { repository.setReminderStatus(id, status) }
    fun undoReminder(id: String) = action { repository.undoReminderStatus(id) }
    fun disableReminder(id: String) = action { repository.disableReminder(id) }
    fun deleteReminder(id: String) = action { repository.deleteReminder(id) }
    fun clearReminderHistory() = action { repository.clearReminderHistory() }
    fun saveReminder(item: ReminderEntity, notificationsAvailable: Boolean) =
        action { repository.saveReminder(item, notificationsAvailable); _savedReminder.value = item.id }

    fun createTask(sourceId: String, fromReminder: Boolean, text: String, reminder: ReminderEntity?) = action {
        _createdTask.value = repository.createTask(sourceId, fromReminder, text, reminder)
    }

    fun dismissTheme(key: String) {
        AppPreferences.dismissTheme(getApplication(), key)
        dismissed.value = AppPreferences.getDismissedThemes(getApplication()).toSet()
    }

    fun restoreTheme(key: String) {
        AppPreferences.restoreTheme(getApplication(), key)
        dismissed.value = AppPreferences.getDismissedThemes(getApplication()).toSet()
    }

    private fun action(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try { block() } catch (e: CancellationException) { throw e } catch (e: Exception) {
                _actionError.value = e.message ?: "Could not save this change. Please retry."
            }
        }
    }
}
