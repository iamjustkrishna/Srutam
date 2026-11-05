package space.iamjustkrishna.srutam.ui.components

import space.iamjustkrishna.srutam.ui.screens.InsetDivider
import space.iamjustkrishna.srutam.ui.screens.SettingsValueRow
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import space.iamjustkrishna.srutam.ui.theme.PlayfairDisplayFontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import space.iamjustkrishna.srutam.data.signature.BlockWriteAccess
import space.iamjustkrishna.srutam.ui.theme.Sem
import space.iamjustkrishna.srutam.utils.Analytics
import space.iamjustkrishna.srutam.utils.PriorInstall
import space.iamjustkrishna.srutam.utils.AudioFileReader
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.iamjustkrishna.srutam.utils.StoragePermission
import space.iamjustkrishna.srutam.viewmodel.AudioFilesViewModel

/**
 * Home's cards for recordings a previous install left behind. New users see none of this:
 *  - Access: when an earlier install's Music/Srutam folder was found ([PriorInstall]), Srutam asks for
 *    access to Music and audio once. If recordings turn up, a "Welcome back" sheet says so and their
 *    notes come back; if the folder was empty, nothing more is said. Settings has the same access
 *    for anyone who said no.
 *  - "Sign in to restore, or process with AI?" for ones with no note saved inside.
 *  - "Save your notes into their recordings" when notes can't be written into those files yet.
 */
@Composable
fun RecordingAccessBanners(
    viewModel: AudioFilesViewModel?,
    hasRealNotes: Boolean,
    isCloudSignedIn: Boolean,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // "Music and audio" access
    var storageGranted by remember { mutableStateOf(StoragePermission.isGranted(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = StoragePermission.isGranted(context)
                if (granted && !storageGranted) viewModel?.loadAudioFiles()
                storageGranted = granted
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // How many earlier recordings turned up once access was given; the Welcome back sheet says so.
    var welcomeBackCount by remember { mutableStateOf(0) }
    var askedAutomatically by remember { mutableStateOf(false) }
    val storageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Analytics.permissionResult(context, "storage", granted, if (askedAutomatically) "earlier_folder" else "home_card")
        storageGranted = granted
        if (granted) {
            viewModel?.loadAudioFiles()
            viewModel?.processPendingOfflineRecordings()
            // The folder alone can't say whether it holds recordings; now it can be looked into.
            if (PriorInstall.recordingsFromBefore(context)) {
                scope.launch {
                    val found = withContext(Dispatchers.IO) { AudioFileReader.getAudioFiles().size }
                    if (found > 0) {
                        Analytics.log(context, EVENT_WELCOME_BACK, mapOf(Analytics.PARAM_RESULT to "found", "count" to found))
                        welcomeBackCount = found
                    }
                }
            }
        } else if (!askedAutomatically) {
            val activity = context as? Activity
            val blocked = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, StoragePermission.permission)
            if (blocked) openAppSettings(context)
        }
    }

    // Writing notes into a previous install's recordings
    val pendingBlocks by BlockWriteAccess.pending(context).collectAsState()
    val pendingCount = pendingBlocks?.size ?: 0
    var accessGrantedThisSession by remember { mutableStateOf(false) }
    val writeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val granted = result.resultCode == Activity.RESULT_OK
        Analytics.permissionResult(context, "modify_recordings", granted, "notes_into_recordings")
        if (granted) {
            accessGrantedThisSession = true
            BlockWriteAccess.onAccessGranted(context)
        }
    }
    fun askToModifyRecordings() {
        scope.launch {
            BlockWriteAccess.createRequest(context)?.let {
                writeLauncher.launch(IntentSenderRequest.Builder(it).build())
            }
        }
    }
    // Notes that were listed only because their file couldn't be refreshed don't need the access.
    LaunchedEffect(Unit) { BlockWriteAccess.pruneSatisfied(context) }

    // Recordings found from before, waiting for a choice
    val found by (viewModel?.foundRecordings ?: remember { kotlinx.coroutines.flow.MutableStateFlow(0) }).collectAsState()
    var foundDialogDismissed by rememberSaveable { mutableStateOf(false) }
    val showFound = found > 0 && !isCloudSignedIn

    // An earlier install left its folder: ask for access to Music and audio, once, so its recordings can be
    // looked for. A new install has no such folder and is never asked.
    val shouldAsk = PriorInstall.shouldAskForAccess(context)
    LaunchedEffect(shouldAsk) {
        if (shouldAsk) {
            PriorInstall.markPrompted(context)
            askedAutomatically = true
            storageLauncher.launch(StoragePermission.permission)
        }
    }
    if (welcomeBackCount > 0) {
        WelcomeBackSheet(count = welcomeBackCount, onDone = { welcomeBackCount = 0 })
    }

    if (showFound && !foundDialogDismissed) {
        FoundRecordingsDialog(
            count = found,
            onSignIn = {
                Analytics.log(context, EVENT_FOUND_RECORDINGS, mapOf(Analytics.PARAM_RESULT to "sign_in"))
                foundDialogDismissed = true
                onOpenSettings()
            },
            onProcess = {
                Analytics.log(context, EVENT_FOUND_RECORDINGS, mapOf(Analytics.PARAM_RESULT to "ai"))
                foundDialogDismissed = true
                viewModel?.processFoundRecordingsWithAi()
            },
            onLater = {
                Analytics.log(context, EVENT_FOUND_RECORDINGS, mapOf(Analytics.PARAM_RESULT to "later"))
                foundDialogDismissed = true
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        if (showFound && foundDialogDismissed) {
            AccessCard(
                icon = Icons.Default.CloudDownload,
                title = "$found recording${if (found == 1) "" else "s"} from before",
                body = "Sign in to restore their notes from Srutam Cloud, or process them with AI.",
                action = "Sign in",
                onAction = onOpenSettings,
                secondaryAction = "Process with AI",
                onSecondaryAction = { viewModel?.processFoundRecordingsWithAi() }
            )
        }
        if (pendingCount > 0 && BlockWriteAccess.isSupported && !accessGrantedThisSession) {
            AccessCard(
                icon = Icons.Default.Save,
                title = "Keep $pendingCount note${if (pendingCount == 1) "" else "s"} safe in ${if (pendingCount == 1) "its recording" else "their recordings"}",
                body = "These recordings are from an earlier install. Allow Srutam to modify them so their notes come back after a reinstall, without AI.",
                action = "Allow",
                onAction = { askToModifyRecordings() }
            )
        }
    }
}

/** Settings rows for the same two accesses, for when the Home cards were skipped. */
@Composable
fun RecordingAccessSettings() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var storageGranted by remember { mutableStateOf(StoragePermission.isGranted(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) storageGranted = StoragePermission.isGranted(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val storageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Analytics.permissionResult(context, "storage", granted, "settings")
        storageGranted = granted
        if (!granted) {
            val activity = context as? Activity
            if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, StoragePermission.permission)) {
                openAppSettings(context)
            }
        }
    }
    val pendingBlocks by BlockWriteAccess.pending(context).collectAsState()
    val pendingCount = pendingBlocks?.size ?: 0
    val writeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val granted = result.resultCode == Activity.RESULT_OK
        Analytics.permissionResult(context, "modify_recordings", granted, "settings")
        if (granted) BlockWriteAccess.onAccessGranted(context)
    }

    Column {
        SettingsValueRow(
            icon = Icons.Default.FolderOpen,
            title = "Find old recordings",
            subtitle = "Bring back notes from an earlier install",
            value = if (storageGranted) "Allowed" else "Allow",
            valueColor = if (storageGranted) null else Sem.accent,
            onClick = if (storageGranted) null else ({ storageLauncher.launch(StoragePermission.permission) })
        )
        InsetDivider()
        val canAllow = pendingCount > 0 && BlockWriteAccess.isSupported
        SettingsValueRow(
            icon = Icons.Default.Save,
            title = "Save notes inside recordings",
            subtitle = "A reinstall brings them back without AI",
            value = when {
                canAllow -> "Allow"
                pendingCount == 0 -> "All saved"
                else -> "$pendingCount not saved"
            },
            valueColor = if (canAllow) Sem.accent else null,
            onClick = if (canAllow) ({
                scope.launch {
                    BlockWriteAccess.createRequest(context)?.let {
                        writeLauncher.launch(IntentSenderRequest.Builder(it).build())
                    }
                }
            }) else null
        )
    }
}

private const val EVENT_FOUND_RECORDINGS = "found_recordings_choice"
private const val EVENT_WELCOME_BACK = "welcome_back_choice"

/** Shown once access was given and recordings from an earlier install turned up. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WelcomeBackSheet(count: Int, onDone: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDone,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Sem.scaffold
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .background(Sem.accentContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Sem.accent, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Welcome back",
                fontSize = 24.sp,
                fontFamily = PlayfairDisplayFontFamily,
                fontWeight = FontWeight.Bold,
                color = Sem.text
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Found $count recording${if (count == 1) "" else "s"} from an earlier install. Their notes are coming back as they were, even offline.",
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Sem.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = Sem.accent),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text("Great", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun openAppSettings(context: android.content.Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

@Composable
private fun FoundRecordingsDialog(
    count: Int,
    onSignIn: () -> Unit,
    onProcess: () -> Unit,
    onLater: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onLater,
        containerColor = Sem.card,
        icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Sem.accent) },
        title = {
            Text(
                "Found $count recording${if (count == 1) "" else "s"} from before",
                fontWeight = FontWeight.Bold,
                color = Sem.text
            )
        },
        text = {
            Text(
                "If you used Srutam Cloud, sign in to bring back their notes as they were. " +
                    "Otherwise Srutam can process them with AI again.",
                color = Sem.textSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(onClick = onSignIn, colors = ButtonDefaults.buttonColors(containerColor = Sem.accent)) {
                Text("Sign in to restore", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onLater) { Text("Later", color = Sem.textSecondary) }
                TextButton(onClick = onProcess) { Text("Process with AI", color = Sem.accent, fontWeight = FontWeight.SemiBold) }
            }
        }
    )
}

@Composable
private fun AccessCard(
    icon: ImageVector,
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
    secondaryAction: String? = null,
    onSecondaryAction: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Sem.accentContainer,
        border = BorderStroke(1.dp, Sem.accentBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 6.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(icon, contentDescription = null, tint = Sem.accent, modifier = Modifier.size(20.dp).padding(top = 1.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Sem.text)
                    Spacer(Modifier.height(2.dp))
                    Text(body, fontSize = 12.5.sp, lineHeight = 17.sp, color = Sem.textSecondary)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (secondaryAction != null) {
                    TextButton(onClick = onSecondaryAction) {
                        Text(secondaryAction, fontSize = 13.sp, color = Sem.textSecondary, fontWeight = FontWeight.SemiBold)
                    }
                }
                TextButton(onClick = onAction) {
                    Text(action, fontSize = 13.sp, color = Sem.accent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
