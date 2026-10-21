package space.iamjustkrishna.srutam

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import space.iamjustkrishna.srutam.navigation.SrutamNavigation
import space.iamjustkrishna.srutam.repository.InsightsRepository
import space.iamjustkrishna.srutam.service.FloatingButtonService
import space.iamjustkrishna.srutam.service.MicPermission
import space.iamjustkrishna.srutam.service.QuickRecordNotification
import space.iamjustkrishna.srutam.service.RecordingCoordinator
import space.iamjustkrishna.srutam.ui.screens.CaptureSetupScreen
import space.iamjustkrishna.srutam.ui.screens.OnboardingIntroScreen
import space.iamjustkrishna.srutam.ui.screens.SrutamSplashScreen
import space.iamjustkrishna.srutam.ui.theme.SrutamTheme
import space.iamjustkrishna.srutam.ui.theme.ThemeMode
import space.iamjustkrishna.srutam.utils.Analytics
import space.iamjustkrishna.srutam.utils.AppPreferences

enum class AppStage {
    SPLASH,
    INTRO,
    CAPTURE_SETUP,
    MAIN
}

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_OPEN_RECORDING_ID = "space.iamjustkrishna.srutam.EXTRA_OPEN_RECORDING_ID"
        const val EXTRA_OPEN_REMINDER_ID = "extra_open_reminder_id"
        /** Set by [space.iamjustkrishna.srutam.service.MicPermission]: ask for the mic, then record from this source. */
        const val EXTRA_REQUEST_MIC_SOURCE = "extra_request_mic_source"
    }

    private var openRecordingId by mutableStateOf<Long?>(null)
    private var openReminderId by mutableStateOf<String?>(null)
    private var micRequestSource by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Srutam)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        openRecordingId = intent?.getLongExtra(EXTRA_OPEN_RECORDING_ID, -1L)?.takeIf { it > 0 }
        openReminderId = intent?.getStringExtra(EXTRA_OPEN_REMINDER_ID)
        micRequestSource = intent?.getStringExtra(EXTRA_REQUEST_MIC_SOURCE)
        cleanUpStaleRecordingNotification()

        setContent {
            val themeModeStr by AppPreferences.themeModeFlow.collectAsState(
                initial = AppPreferences.getThemeMode(this)
            )
            val themeMode = when (themeModeStr) {
                AppPreferences.THEME_LIGHT -> ThemeMode.LIGHT
                AppPreferences.THEME_COSMIC_DARK -> ThemeMode.COSMIC_DARK
                else -> ThemeMode.SYSTEM
            }
            SrutamTheme(themeMode = themeMode) {
                SrutamApp(
                    initialRecordingId = openRecordingId,
                    initialReminderId = openReminderId,
                    micRequestSource = micRequestSource,
                    onMicRequestHandled = { micRequestSource = null }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        space.iamjustkrishna.srutam.cloud.CloudSyncManager.enqueueSync(this)
        cleanUpStaleRecordingNotification()
    }

    private fun cleanUpStaleRecordingNotification() {
        if (space.iamjustkrishna.srutam.service.RecordingCoordinator.isIdle) {
            getSystemService(android.app.NotificationManager::class.java)
                ?.cancel(space.iamjustkrishna.srutam.service.RecordingForegroundService.NOTIFICATION_ID)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val id = intent.getLongExtra(EXTRA_OPEN_RECORDING_ID, -1L).takeIf { it > 0 }
        if (id != null) {
            openRecordingId = id
        }
        val remId = intent.getStringExtra(EXTRA_OPEN_REMINDER_ID)
        if (remId != null) {
            openReminderId = remId
        }
        intent.getStringExtra(EXTRA_REQUEST_MIC_SOURCE)?.let { micRequestSource = it }
    }
}

/** Storage access lets the app read recordings left in Music/Srutam by a previous install. */
private fun storagePermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

/** Android 10 also needs storage write access to save recordings into Music/Srutam. */
private fun micPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
        arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    } else {
        arrayOf(Manifest.permission.RECORD_AUDIO)
    }

@Composable
fun SrutamApp(
    initialRecordingId: Long? = null,
    initialReminderId: String? = null,
    micRequestSource: String? = null,
    onMicRequestHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    var appStage by rememberSaveable { mutableStateOf(AppStage.SPLASH) }
    var dockShownAt by remember { mutableLongStateOf(0L) }

    // Restore: storage is the only permission onboarding asks for, and only when the user picks it.
    val storageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Analytics.permissionResult(context, "storage", granted, "restore")
        appStage = AppStage.CAPTURE_SETUP
    }

    // Mic: asked on the first record tap from any entry point, then the recording starts.
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val granted = results[Manifest.permission.RECORD_AUDIO] == true
        val source = micRequestSource ?: "in_app"
        Analytics.permissionResult(context, "mic", granted, source)
        onMicRequestHandled()
        if (granted) {
            QuickRecordNotification.restoreIdle(context)
            RecordingCoordinator.requestStart(context, source)
        } else {
            Toast.makeText(context, "Srutam needs the microphone to record", Toast.LENGTH_SHORT).show()
        }
    }

    // Notifications: asked the first time a reminder couldn't notify.
    val reminderNotifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Analytics.permissionResult(context, "notifications", granted, "reminder")
        AppPreferences.setReminderNotificationAsked(context)
        if (granted) {
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { InsightsRepository.from(context).reconcile() }
            }
        }
    }

    LaunchedEffect(appStage, micRequestSource) {
        if (appStage == AppStage.MAIN && micRequestSource != null) {
            if (MicPermission.isGranted(context)) {
                val source = micRequestSource
                onMicRequestHandled()
                RecordingCoordinator.requestStart(context, source)
            } else {
                micLauncher.launch(micPermissions())
            }
        }
    }

    LaunchedEffect(appStage) {
        if (appStage == AppStage.MAIN &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            AppPreferences.isReminderNotificationAskPending(context) &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            reminderNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(appStage) {
        if (appStage == AppStage.MAIN) {
            // Earlier versions showed separate quick-record and dock notifications; now there is one.
            QuickRecordNotification.cleanUpLegacy(context)
            QuickRecordNotification.restoreIdle(context)
        }
    }

    LaunchedEffect(appStage) {
        if (appStage == AppStage.MAIN && AppPreferences.isFloatingDockEnabled(context)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || android.provider.Settings.canDrawOverlays(context)) {
                val serviceIntent = Intent(context, FloatingButtonService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }

    LaunchedEffect(appStage) {
        if (appStage == AppStage.CAPTURE_SETUP) {
            dockShownAt = SystemClock.elapsedRealtime()
            Analytics.onboardingStepViewed(context, "dock")
        }
    }

    fun finishOnboarding(dockResult: String) {
        Analytics.onboardingStepResult(context, "dock", dockResult, SystemClock.elapsedRealtime() - dockShownAt)
        Analytics.onboardingCompleted(context)
        AppPreferences.setHasCompletedCaptureSetup(context, true)
        AppPreferences.setOnboardingCompleted(context, true)
        appStage = AppStage.MAIN
    }

    AnimatedContent(
        targetState = appStage,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "AppStageTransition"
    ) { stage ->
        when (stage) {
            AppStage.SPLASH -> {
                SrutamSplashScreen(
                    onSplashFinished = {
                        appStage = if (AppPreferences.isOnboardingCompleted(context)) AppStage.MAIN else AppStage.INTRO
                    }
                )
            }
            AppStage.INTRO -> {
                OnboardingIntroScreen(
                    onFinished = { appStage = AppStage.CAPTURE_SETUP },
                    onRestoreNotes = {
                        val permission = storagePermission()
                        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                            appStage = AppStage.CAPTURE_SETUP
                        } else {
                            storageLauncher.launch(permission)
                        }
                    }
                )
            }
            AppStage.CAPTURE_SETUP -> {
                CaptureSetupScreen(
                    onComplete = { finishOnboarding(Analytics.RESULT_GRANTED) },
                    onSkip = { finishOnboarding(Analytics.RESULT_SKIPPED) }
                )
            }
            AppStage.MAIN -> {
                SrutamNavigation(initialRecordingId = initialRecordingId, initialReminderId = initialReminderId)
            }
        }
    }
}
