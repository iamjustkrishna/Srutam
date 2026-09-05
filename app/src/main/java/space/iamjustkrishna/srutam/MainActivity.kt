package space.iamjustkrishna.srutam

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.compose.ui.platform.LocalContext
import space.iamjustkrishna.srutam.service.FloatingButtonService
import space.iamjustkrishna.srutam.utils.AppPreferences
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import space.iamjustkrishna.srutam.navigation.SrutamNavigation
import space.iamjustkrishna.srutam.ui.theme.SrutamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Srutam)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SrutamTheme {
                SrutamApp()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SrutamApp() {
    val audioPermissionState = rememberPermissionState(
        permission = Manifest.permission.RECORD_AUDIO
    )
    val filePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    }
    val filePermissionState = rememberPermissionState(permission = filePermission)
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    LaunchedEffect(Unit) {
        if (!audioPermissionState.status.isGranted) {
            audioPermissionState.launchPermissionRequest()
        }
        if (!filePermissionState.status.isGranted) {
            filePermissionState.launchPermissionRequest()
        }
        if (notificationPermissionState != null && !notificationPermissionState.status.isGranted) {
            notificationPermissionState.launchPermissionRequest()
        }
    }

    val context = LocalContext.current
    val hasCorePermissions = audioPermissionState.status.isGranted && filePermissionState.status.isGranted

    LaunchedEffect(hasCorePermissions) {
        if (hasCorePermissions && AppPreferences.isFloatingDockEnabled(context)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || android.provider.Settings.canDrawOverlays(context)) {
                val serviceIntent = Intent(context, FloatingButtonService::class.java)
                context.startService(serviceIntent)
            }
        }
    }

    if (hasCorePermissions) {
        SrutamNavigation()
    } else {
        PermissionRequestScreen(
            audioGranted = audioPermissionState.status.isGranted,
            fileGranted = filePermissionState.status.isGranted,
            showRationale = audioPermissionState.status.shouldShowRationale || filePermissionState.status.shouldShowRationale,
            onRequestPermissions = {
                if (!audioPermissionState.status.isGranted) {
                    audioPermissionState.launchPermissionRequest()
                }
                if (!filePermissionState.status.isGranted) {
                    filePermissionState.launchPermissionRequest()
                }
            }
        )
    }
}

@Composable
fun PermissionRequestScreen(
    audioGranted: Boolean,
    fileGranted: Boolean,
    showRationale: Boolean,
    onRequestPermissions: () -> Unit
) {
    val filePermissionLabel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        "audio files permission"
    } else {
        "files permission"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Srutam needs access",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (showRationale) {
                "Allow microphone and $filePermissionLabel so Srutam can record and load saved recordings."
            } else {
                "Grant microphone and $filePermissionLabel to start using Srutam."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        PermissionStatusRow(
            label = "Microphone permission",
            granted = audioGranted
        )
        Spacer(modifier = Modifier.height(12.dp))
        PermissionStatusRow(
            label = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                "Audio files permission"
            } else {
                "Files permission"
            },
            granted = fileGranted
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(onClick = onRequestPermissions) {
            Text("Grant Permissions")
        }
    }
}

@Composable
private fun PermissionStatusRow(
    label: String,
    granted: Boolean
) {
    Text(
        text = if (granted) "$label: Granted" else "$label: Required",
        style = MaterialTheme.typography.bodyLarge,
        color = if (granted) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    )
}
