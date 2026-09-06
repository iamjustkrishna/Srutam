package space.iamjustkrishna.srutam

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import space.iamjustkrishna.srutam.navigation.SrutamNavigation
import space.iamjustkrishna.srutam.service.FloatingButtonService
import space.iamjustkrishna.srutam.ui.screens.PermissionsOnboardingScreen
import space.iamjustkrishna.srutam.ui.screens.SrutamSplashScreen
import space.iamjustkrishna.srutam.ui.theme.SrutamTheme
import space.iamjustkrishna.srutam.utils.AppPreferences

enum class AppStage {
    SPLASH,
    PERMISSIONS,
    MAIN
}

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_OPEN_RECORDING_ID = "space.iamjustkrishna.srutam.EXTRA_OPEN_RECORDING_ID"
    }

    private var openRecordingId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Srutam)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        openRecordingId = intent?.getLongExtra(EXTRA_OPEN_RECORDING_ID, -1L)?.takeIf { it > 0 }

        setContent {
            SrutamTheme {
                SrutamApp(initialRecordingId = openRecordingId)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val id = intent.getLongExtra(EXTRA_OPEN_RECORDING_ID, -1L).takeIf { it > 0 }
        if (id != null) {
            openRecordingId = id
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SrutamApp(initialRecordingId: Long? = null) {
    val context = LocalContext.current
    var appStage by rememberSaveable { mutableStateOf(AppStage.SPLASH) }

    val permissionsToRequest = remember {
        buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_AUDIO)
                add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
    }

    val multiplePermissionsState = rememberMultiplePermissionsState(permissionsToRequest)

    val checkCorePermissionsGranted = remember(context) {
        {
            val mic = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            val storage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
            mic && storage
        }
    }

    LaunchedEffect(appStage) {
        if (appStage == AppStage.MAIN && AppPreferences.isFloatingDockEnabled(context)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || android.provider.Settings.canDrawOverlays(context)) {
                val serviceIntent = Intent(context, FloatingButtonService::class.java)
                context.startService(serviceIntent)
            }
        }
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
                        if (checkCorePermissionsGranted()) {
                            appStage = AppStage.MAIN
                        } else {
                            appStage = AppStage.PERMISSIONS
                        }
                    }
                )
            }
            AppStage.PERMISSIONS -> {
                PermissionsOnboardingScreen(
                    multiplePermissionsState = multiplePermissionsState,
                    onAllPermissionsGranted = {
                        appStage = AppStage.MAIN
                    }
                )
            }
            AppStage.MAIN -> {
                SrutamNavigation(initialRecordingId = initialRecordingId)
            }
        }
    }
}
