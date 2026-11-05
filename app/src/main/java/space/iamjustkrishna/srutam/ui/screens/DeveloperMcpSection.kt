package space.iamjustkrishna.srutam.ui.screens

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.togetherWith
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.exceptions.GetCredentialCancellationException
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import space.iamjustkrishna.srutam.cloud.PairingError
import space.iamjustkrishna.srutam.cloud.PairingException
import space.iamjustkrishna.srutam.cloud.PairingRules
import space.iamjustkrishna.srutam.cloud.ApiKeyItem
import space.iamjustkrishna.srutam.cloud.ApiKeyLimitException
import space.iamjustkrishna.srutam.cloud.ApiKeyNameException
import space.iamjustkrishna.srutam.cloud.ApiKeyRules
import space.iamjustkrishna.srutam.cloud.CloudSyncManager
import space.iamjustkrishna.srutam.cloud.SupabaseAuthManager
import space.iamjustkrishna.srutam.cloud.SupabaseCloudClient
import space.iamjustkrishna.srutam.ui.components.DialogBadgeType
import space.iamjustkrishna.srutam.ui.components.PillStyle
import space.iamjustkrishna.srutam.ui.components.PillButton
import space.iamjustkrishna.srutam.ui.components.SrutamStandardDialog
import space.iamjustkrishna.srutam.ui.theme.*
import space.iamjustkrishna.srutam.utils.AppPreferences

/** Whether someone is signed in to Srutam Cloud. Kept in step with what is stored, and changes the moment they sign in or out. */
@Composable
internal fun rememberCloudSignedIn(): Boolean {
    val context = LocalContext.current
    remember { AppPreferences.isCloudSignedIn(context) }
    val signedIn by AppPreferences.cloudAuthStateFlow.collectAsState()
    return signedIn
}

/**
 * Where an account-deletion request goes. Satisfies Google Play's requirement for an
 * in-app deletion path (the in-app part may simply link out to the handling channel -
 * it does not need to self-execute). Swap to the published web deletion page's URL via
 * [Intent.ACTION_VIEW] once that page exists; a mailto is an explicitly accepted
 * mechanism in the meantime per Google's own account-deletion FAQ.
 */
private const val ACCOUNT_DELETION_EMAIL = "hlo.krsna@gmail.com"

internal fun requestAccountDeletion(context: Context, accountEmail: String?) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(ACCOUNT_DELETION_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, "Delete my Srutam account")
        putExtra(
            Intent.EXTRA_TEXT,
            "Please delete my Srutam account and all associated cloud data." +
                (accountEmail?.let { "\n\nAccount email: $it" } ?: "")
        )
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // No email app resolves ACTION_SENDTO (rare, but real on some custom ROMs/emulators).
        Toast.makeText(
            context,
            "No email app found. Email $ACCOUNT_DELETION_EMAIL to request deletion.",
            Toast.LENGTH_LONG
        ).show()
    }
}

@Composable
fun DeveloperMcpSection(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authManager = remember { SupabaseAuthManager(context) }
    val cloudClient = remember { SupabaseCloudClient(context) }

    val isSignedIn = rememberCloudSignedIn()
    var userEmail by remember {
        mutableStateOf(AppPreferences.getCloudUserEmail(context))
    }

    var apiKeys by remember { mutableStateOf<List<ApiKeyItem>>(emptyList()) }
    var isLoadingKeys by remember { mutableStateOf(false) }

    var showGenerateKeyDialog by remember { mutableStateOf(false) }
    var keyNameInput by remember { mutableStateOf("Cursor IDE") }
    var generatedKeyResult by remember { mutableStateOf<Pair<String, String>?>(null) } // (plainKey, prefix)
    // Single-flight guard for key creation: set synchronously on the first tap, before any coroutine starts.
    var isCreatingKey by remember { mutableStateOf(false) }
    var keyDialogError by remember { mutableStateOf<String?>(null) }

    var isAuthLoading by remember { mutableStateOf(false) }
    // Why the last sign-in attempt failed, kept on screen (a toast is gone before it can be read or reported).
    var signInError by remember { mutableStateOf<String?>(null) }
    var isAutoSyncEnabled by remember {
        mutableStateOf(AppPreferences.isAutoCloudSyncEnabled(context))
    }

    fun refreshKeys() {
        if (isSignedIn) {
            scope.launch {
                isLoadingKeys = true
                val res = cloudClient.listApiKeys()
                if (res.isSuccess) {
                    apiKeys = res.getOrDefault(emptyList())
                }
                isLoadingKeys = false
            }
        }
    }

    // ---- QR pairing state ----------------------------------------------------------------
    // Survives rotation and backgrounding, so a half-finished pairing is not silently lost.
    var pairingState by rememberSaveable(
        stateSaver = androidx.compose.runtime.saveable.Saver(
            save = { state -> if (state is PairingUiState.EnteringCode) state.code else "" },
            restore = { code -> if ((code as String).isEmpty()) PairingUiState.Idle else PairingUiState.EnteringCode(code) }
        )
    ) { mutableStateOf<PairingUiState>(PairingUiState.Idle) }
    // Single-flight guard, same reasoning as key creation: set before any coroutine starts.
    var isPairing by remember { mutableStateOf(false) }

    /** Looks a scanned or typed code up, then shows the confirmation sheet. */
    fun previewCode(code: String) {
        if (isPairing) return
        isPairing = true
        pairingState = PairingUiState.EnteringCode(code = code, busy = true)
        scope.launch {
            val result = cloudClient.pairPreview(code)
            isPairing = false
            result.fold(
                onSuccess = { preview ->
                    pairingState = PairingUiState.Confirming(
                        code = code,
                        preview = preview,
                        name = preview.label
                    )
                },
                onFailure = { error ->
                    val message = error.message ?: "Could not read that code."
                    // A bad code keeps the entry sheet open so the user can correct it; anything
                    // else is a dead end, so it closes with a toast.
                    val pairingError = (error as? PairingException)?.error
                    if (pairingError == PairingError.INVALID_CODE || pairingError == PairingError.NOT_A_SRUTAM_CODE) {
                        pairingState = PairingUiState.EnteringCode(code = code, error = message)
                    } else {
                        pairingState = PairingUiState.Idle
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }

    fun startScan() {
        if (isPairing) return
        CodeScannerLauncher.scan(
            context = context,
            onResult = { raw ->
                try {
                    previewCode(PairingRules.parseScanned(raw))
                } catch (e: PairingException) {
                    Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                }
            },
            onUnavailable = {
                // No Play Services (or the scanner failed): typing the code always works.
                Toast.makeText(context, "Scanner unavailable - enter the code instead.", Toast.LENGTH_SHORT).show()
                pairingState = PairingUiState.EnteringCode()
            },
            onCancelled = { }
        )
    }

    /** Approves the pairing after the device-credential check. */
    fun approvePairing(state: PairingUiState.Confirming) {
        if (isPairing) return
        val nameError = ApiKeyRules.validateName(state.name, apiKeys.map { it.name })
        if (nameError != null) {
            pairingState = state.copy(error = nameError)
            return
        }
        PairingConfirmGate.confirm(
            context = context,
            onConfirmed = {
                if (isPairing) return@confirm
                isPairing = true
                pairingState = state.copy(busy = true, error = null)
                scope.launch {
                    val result = cloudClient.pairApprove(state.code, state.name)
                    isPairing = false
                    result.fold(
                        onSuccess = { keyName ->
                            pairingState = PairingUiState.Idle
                            Toast.makeText(context, "Connected \"$keyName\"", Toast.LENGTH_SHORT).show()
                            space.iamjustkrishna.srutam.utils.Analytics.mcpConnectResult(context, true)
                            refreshKeys()
                        },
                        onFailure = { error ->
                            space.iamjustkrishna.srutam.utils.Analytics.mcpConnectResult(
                                context, false, (error as? PairingException)?.error?.name ?: error.javaClass.simpleName
                            )
                            pairingState = state.copy(busy = false, error = error.message ?: "Could not connect.")
                        }
                    )
                }
            },
            onDenied = { message ->
                if (message.isNotBlank()) Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        )
    }



    LaunchedEffect(isSignedIn) {
        if (isSignedIn) {
            userEmail = AppPreferences.getCloudUserEmail(context)
            refreshKeys()
        } else {
            // Signed out, here or from Settings: the next thing seen is the plain "connect" card.
            userEmail = null
            apiKeys = emptyList()
        }
    }

    AnimatedContent(
        targetState = isSignedIn,
        modifier = modifier.fillMaxWidth(),
        transitionSpec = {
            (fadeIn(tween(320, delayMillis = 90)) + expandVertically(tween(320), expandFrom = Alignment.Top)) togetherWith
                (fadeOut(tween(180)) + shrinkVertically(tween(320), shrinkTowards = Alignment.Top))
        },
        label = "cloudState"
    ) { signedIn ->
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        if (signedIn) {
            val keysFull = apiKeys.size >= ApiKeyRules.MAX_ACTIVE_KEYS
            val secondary = if (isDark) TextOnDarkSecondary else TextSecondary

            // The account, and the one switch that keeps notes in step with it.
            SettingsSection(title = "") {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RowTile(Icons.Default.Cloud)
                    Column(Modifier.weight(1f)) {
                        Text("Srutam Cloud", fontSize = 15.5.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, color = if (isDark) TextOnDarkPrimary else TextPrimary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(CosmicAuroraGreen))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = userEmail ?: "Signed in",
                                fontSize = 12.5.sp,
                                lineHeight = 17.sp,
                                color = secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    PillButton(
                        "Sync now",
                        onClick = {
                            CloudSyncManager.enqueueSync(context, forceAll = true)
                            Toast.makeText(context, "Syncing all notes to cloud...", Toast.LENGTH_SHORT).show()
                        },
                        style = PillStyle.Secondary,
                        icon = Icons.Default.Sync,
                        compact = true
                    )
                }
                InsetDivider()
                SettingsToggleRow(
                    icon = Icons.Default.CloudUpload,
                    title = "Auto-sync",
                    subtitle = "Upload new notes and insights automatically",
                    checked = isAutoSyncEnabled,
                    onCheckedChange = { enabled ->
                        isAutoSyncEnabled = enabled
                        AppPreferences.setAutoCloudSyncEnabled(context, enabled)
                    }
                )
            }

            // Coding agents: connect a computer, see what is connected, or make a key by hand.
            SettingsSection(title = "CODING AGENTS (MCP)") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RowTile(Icons.Default.QrCodeScanner)
                    RowText("Connect a computer", if (keysFull) "Max ${ApiKeyRules.MAX_ACTIVE_KEYS} keys reached. Revoke one to add another." else "Run srutam-mcp init, then scan the code it shows", Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 58.dp, end = 14.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val canConnect = !keysFull && !isPairing
                    PillButton("Scan QR", onClick = { startScan() }, icon = Icons.Default.QrCodeScanner, compact = true, enabled = canConnect)
                    PillButton("Enter code", onClick = { pairingState = PairingUiState.EnteringCode() }, style = PillStyle.Secondary, compact = true, enabled = canConnect)
                }
                InsetDivider()
                SettingsValueRow(
                    icon = Icons.Default.Computer,
                    title = "Connected computers",
                    subtitle = if (!isLoadingKeys && apiKeys.isEmpty()) "None yet" else "",
                    value = if (isLoadingKeys) "..." else "${apiKeys.size} of ${ApiKeyRules.MAX_ACTIVE_KEYS}"
                )
                apiKeys.forEach { key ->
                    InsetDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 58.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                            Text(key.name, fontSize = 14.5.sp, fontWeight = FontWeight.Medium, color = if (isDark) TextOnDarkPrimary else TextPrimary)
                            Text(key.keyPrefix, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = if (isDark) CosmicGlowBlue else CobaltBlue)
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val res = cloudClient.revokeApiKey(key.id)
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "API Key revoked", Toast.LENGTH_SHORT).show()
                                        refreshKeys()
                                    }
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Revoke Key", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        }
                    }
                }
                InsetDivider()
                // Manual key creation stays available but secondary: it is the only route for
                // WSL, dev containers and SSH boxes, which have no screen to scan from.
                Box(Modifier.alpha(if (keysFull) 0.5f else 1f)) {
                    SettingsLinkRow(
                        icon = Icons.Default.VpnKey,
                        title = "Create key manually",
                        subtitle = "For servers and dev containers with no screen",
                        onClick = {
                            if (!keysFull && !isCreatingKey) {
                                keyNameInput = ""
                                keyDialogError = null
                                showGenerateKeyDialog = true
                            }
                        }
                    )
                }
                InsetDivider()
                // srutam-mcp init configures every supported client itself, so one command is all anyone needs.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Srutam MCP setup", "npx -y srutam-mcp init"))
                            Toast.makeText(context, "Command copied", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RowTile(Icons.Default.Terminal)
                    Column(Modifier.weight(1f)) {
                        Text("Setup command", fontSize = 15.5.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, color = if (isDark) TextOnDarkPrimary else TextPrimary)
                        Text("npx -y srutam-mcp init", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy command", tint = secondary.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
            }

        } else {
            // Opt-in Sign-in Card
            // The group around it is already the card, so this is just a row and a button inside it.
            SettingsSection(title = "") {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isDark) Color(0xFF1A2A55) else Color(0xFFEAF1FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = if (isDark) CosmicGlowBlue else CobaltBlue, modifier = Modifier.size(18.dp))
                        }

                        Column {
                            Text(
                                text = "Srutam Cloud",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) TextOnDarkPrimary else TextPrimary
                            )
                            Text(
                                text = "Sync notes and connect coding agents",
                                fontSize = 12.5.sp,
                                color = if (isDark) TextOnDarkSecondary else TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Back up your notes to your account, and let coding agents like Cursor, Windsurf and Claude read them through MCP.",
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                        color = if (isDark) TextOnDarkSecondary else TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (!isAuthLoading) {
                                scope.launch {
                                    isAuthLoading = true
                                    signInError = null
                                    val startedAt = android.os.SystemClock.elapsedRealtime()
                                    space.iamjustkrishna.srutam.utils.Analytics.cloudSignInStarted(context)
                                    val res = authManager.signInWithGoogle(context)
                                    val elapsed = android.os.SystemClock.elapsedRealtime() - startedAt
                                    isAuthLoading = false
                                    if (res.isSuccess) {
                                        space.iamjustkrishna.srutam.utils.Analytics.cloudSignInSucceeded(context, elapsed)
                                        userEmail = AppPreferences.getCloudUserEmail(context)
                                        Toast.makeText(context, "Connected to Srutam Cloud!", Toast.LENGTH_SHORT).show()
                                        CloudSyncManager.enqueueSync(context, forceAll = true)
                                        refreshKeys()
                                    } else {
                                        val err = res.exceptionOrNull()
                                        val why = space.iamjustkrishna.srutam.cloud.SignInDiagnosis.of(err, elapsed)
                                        space.iamjustkrishna.srutam.utils.Analytics.cloudSignInFailed(context, why.code, elapsed)
                                        android.util.Log.w("CloudSignIn", "Google sign-in failed (${why.code}) after ${elapsed} ms", err)
                                        if (why.show) signInError = "${why.message} (code: ${why.code})"
                                    }
                                }
                            }
                        },
                        enabled = !isAuthLoading,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue)
                    ) {
                        if (isAuthLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connecting...", fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                        } else {
                            Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connect Cloud & MCP", fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                        }
                    }
                    signInError?.let { message ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = message,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }
            }
        }
    }
    }

    // ---- Pairing dialogs -----------------------------------------------------------------
    when (val state = pairingState) {
        is PairingUiState.EnteringCode -> EnterPairingCodeDialog(
            state = state,
            onCodeChange = { typed ->
                // Clear the previous error as soon as the user edits, so it does not look stuck.
                pairingState = state.copy(code = typed.uppercase(), error = null)
            },
            onSubmit = {
                try {
                    previewCode(PairingRules.normalizeCode(state.code))
                } catch (e: PairingException) {
                    pairingState = state.copy(error = e.message)
                }
            },
            onDismiss = { pairingState = PairingUiState.Idle }
        )

        is PairingUiState.Confirming -> ConfirmPairingDialog(
            state = state,
            isDark = isDark,
            onNameChange = { pairingState = state.copy(name = it, error = null) },
            onConfirm = { approvePairing(state) },
            onDismiss = { pairingState = PairingUiState.Idle }
        )

        PairingUiState.Idle -> Unit
    }


    // Modal: Generate New API Key
    if (showGenerateKeyDialog) {
        if (generatedKeyResult == null) {
            val nameError = ApiKeyRules.validateName(keyNameInput, apiKeys.map { it.name })
            SrutamStandardDialog(
                onDismissRequest = { showGenerateKeyDialog = false },
                title = "Create Agent API Key",
                subtitle = "Label this key to recognize your IDE or laptop",
                icon = Icons.Default.VpnKey,
                badgeType = DialogBadgeType.PRIMARY,
                confirmText = if (isCreatingKey) "Creating key..." else "Generate",
                dismissText = "Cancel",
                confirmEnabled = nameError == null,
                isLoading = isCreatingKey,
                onConfirm = {
                    // Ignore every tap after the first until the request finishes.
                    if (isCreatingKey) return@SrutamStandardDialog
                    if (nameError != null) {
                        keyDialogError = nameError
                        return@SrutamStandardDialog
                    }
                    isCreatingKey = true
                    keyDialogError = null
                    scope.launch {
                        try {
                            val res = cloudClient.createApiKey(keyNameInput)
                            if (res.isSuccess) {
                                generatedKeyResult = res.getOrThrow()
                            } else {
                                keyDialogError = when (val err = res.exceptionOrNull()) {
                                    is ApiKeyLimitException, is ApiKeyNameException -> err.message
                                    else -> "Could not create the key. Check your connection and try again."
                                }
                            }
                        } finally {
                            isCreatingKey = false
                            refreshKeys()
                        }
                    }
                },
                content = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = keyNameInput,
                            onValueChange = {
                                keyNameInput = it.take(ApiKeyRules.MAX_NAME_LENGTH)
                                keyDialogError = null
                            },
                            label = { Text("Key Label") },
                            placeholder = { Text("e.g. OpenCode, Cursor, Windsurf") },
                            singleLine = true,
                            readOnly = isCreatingKey,
                            isError = keyNameInput.isNotBlank() && nameError != null || keyDialogError != null,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        val hint = keyDialogError ?: if (keyNameInput.isNotBlank()) nameError else null
                        if (hint != null) {
                            Text(text = hint, fontSize = 12.sp, color = Color(0xFFEF4444))
                        } else if (isCreatingKey) {
                            Text(
                                text = "Creating your key. This can take a few seconds.",
                                fontSize = 12.sp,
                                color = if (isDark) TextOnDarkSecondary else TextSecondary
                            )
                        }
                    }
                }
            )
        } else {
            val (plainKey, _) = generatedKeyResult!!
            SrutamStandardDialog(
                onDismissRequest = {
                    showGenerateKeyDialog = false
                    generatedKeyResult = null
                },
                title = "Your API Key is Ready",
                subtitle = "Copy this key now. It will not be shown again.",
                icon = Icons.Default.VpnKey,
                badgeType = DialogBadgeType.PRIMARY,
                confirmText = "Copy & Done",
                dismissText = null,
                onConfirm = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Srutam API Key", plainKey))
                    Toast.makeText(context, "API Key copied to clipboard!", Toast.LENGTH_SHORT).show()
                    showGenerateKeyDialog = false
                    generatedKeyResult = null
                },
                content = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF070B18) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else SlateBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = plainKey,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CobaltBlue,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            )
        }
    }
}

