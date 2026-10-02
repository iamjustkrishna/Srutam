package space.iamjustkrishna.srutam.ui.screens

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import space.iamjustkrishna.srutam.ui.components.SrutamStandardDialog
import space.iamjustkrishna.srutam.ui.theme.*
import space.iamjustkrishna.srutam.utils.AppPreferences

/**
 * Where an account-deletion request goes. Satisfies Google Play's requirement for an
 * in-app deletion path (the in-app part may simply link out to the handling channel -
 * it does not need to self-execute). Swap to the published web deletion page's URL via
 * [Intent.ACTION_VIEW] once that page exists; a mailto is an explicitly accepted
 * mechanism in the meantime per Google's own account-deletion FAQ.
 */
private const val ACCOUNT_DELETION_EMAIL = "hlo.krsna@gmail.com"

private fun requestAccountDeletion(context: Context, accountEmail: String?) {
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

    var isSignedIn by remember {
        mutableStateOf(AppPreferences.isCloudSignedIn(context))
    }
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
                            refreshKeys()
                        },
                        onFailure = { error ->
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
        refreshKeys()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (isSignedIn) {
            // Signed-in State Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) CosmicVoidCard else SlateGrouped,
                border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Top Row: Status Indicator and Sign Out button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CosmicAuroraGreen)
                            )
                            Text(
                                text = "Srutam Cloud Active",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) TextOnDarkPrimary else TextPrimary
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                authManager.signOut()
                                isSignedIn = false
                                userEmail = null
                                apiKeys = emptyList()
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.45f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Sign Out",
                                modifier = Modifier.size(13.dp),
                                tint = Color(0xFFEF4444)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sign Out",
                                fontSize = 11.sp,
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Second Row: Email & Sync Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isDark) TextOnDarkSecondary else TextSecondary
                            )
                            Text(
                                text = userEmail ?: "Signed in",
                                fontSize = 12.sp,
                                color = if (isDark) TextOnDarkSecondary else TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                CloudSyncManager.enqueueSync(context, forceAll = true)
                                Toast.makeText(context, "Syncing all notes to cloud...", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Auto-Sync Toggle Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Auto-Sync to Cloud",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) TextOnDarkPrimary else TextPrimary
                            )
                            Text(
                                text = "Upload new notes and insights automatically",
                                fontSize = 11.sp,
                                color = if (isDark) TextOnDarkSecondary else TextSecondary
                            )
                        }
                        Switch(
                            checked = isAutoSyncEnabled,
                            onCheckedChange = { enabled ->
                                isAutoSyncEnabled = enabled
                                AppPreferences.setAutoCloudSyncEnabled(context, enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CobaltBlue
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = if (isDark) CosmicVoidCardBorder else SlateBorder, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // API Keys sub-header with 3-key limit enforcement
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "MCP Agent Keys",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) TextOnDarkPrimary else TextPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (apiKeys.size >= 3) Color(0xFFEF4444).copy(alpha = 0.15f) else (if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                                ) {
                                    Text(
                                        text = "${apiKeys.size}/3",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (apiKeys.size >= 3) Color(0xFFEF4444) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (apiKeys.size >= 3) "Max 3 keys reached. Revoke one to create new." else "Keys for Claude Code, Codex, Cursor, Gemini CLI & more",
                                fontSize = 11.sp,
                                color = if (apiKeys.size >= 3) Color(0xFFEF4444) else (if (isDark) TextOnDarkSecondary else TextSecondary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    ConnectComputerCard(
                        isDark = isDark,
                        enabled = apiKeys.size < ApiKeyRules.MAX_ACTIVE_KEYS && !isPairing,
                        onScanRequested = { startScan() },
                        onEnterCodeRequested = { pairingState = PairingUiState.EnteringCode() }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Manual key creation stays available but secondary: it is the only route for
                    // WSL, dev containers and SSH boxes, which have no screen to scan from.
                    Text(
                        text = "Create key manually",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (apiKeys.size >= ApiKeyRules.MAX_ACTIVE_KEYS) (if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)) else CobaltBlue,
                        modifier = Modifier.clickable(enabled = apiKeys.size < ApiKeyRules.MAX_ACTIVE_KEYS && !isCreatingKey) {
                            keyNameInput = ""
                            keyDialogError = null
                            showGenerateKeyDialog = true
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isLoadingKeys) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = CobaltBlue)
                        }
                    } else if (apiKeys.isEmpty()) {
                        Text(
                            text = "No computers connected yet. Run srutam-mcp init on your computer and scan the code.",
                            fontSize = 12.sp,
                            color = if (isDark) TextOnDarkSecondary else TextSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            apiKeys.forEach { key ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDark) Color(0xFF14192D) else Color.White,
                                    border = BorderStroke(0.8.dp, if (isDark) CosmicVoidCardBorder else SlateBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = key.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) TextOnDarkPrimary else TextPrimary
                                            )
                                            Text(
                                                text = key.keyPrefix,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (isDark) CosmicGlowBlue else CobaltBlue
                                            )
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
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Revoke Key", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // srutam-mcp init configures every supported client itself, so the long
                    // per-client snippet card this screen used to carry is gone. One command is
                    // all a user needs, and it keeps anyone whose CLI predates pairing unstuck.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Srutam MCP setup", "npx -y srutam-mcp init"))
                            Toast.makeText(context, "Command copied", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isDark) TextOnDarkSecondary else TextSecondary
                        )
                        Text(
                            text = "npx -y srutam-mcp init",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isDark) TextOnDarkSecondary else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy command",
                            modifier = Modifier.size(13.dp),
                            tint = if (isDark) TextOnDarkSecondary else TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = if (isDark) CosmicVoidCardBorder else SlateBorder, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Last item in the card, centered: discoverable but deliberately unhurried,
                    // matching Google's own account-deletion UX guidance to keep this reachable
                    // without making it compete with the actions someone actually uses daily.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { requestAccountDeletion(context, userEmail) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFFEF4444).copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Request account & data deletion",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = Color(0xFFEF4444).copy(alpha = 0.85f)
                        )
                    }
                }
            }
        } else {
            // Opt-in Sign-in Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isDark) CosmicVoidCard else SlateGrouped,
                border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CobaltContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = CobaltBlue, modifier = Modifier.size(22.dp))
                        }

                        Column {
                            Text(
                                text = "Developer Brain & MCP Sync",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) TextOnDarkPrimary else TextPrimary
                            )
                            Text(
                                text = "Connect OpenCode, Cursor, Windsurf, Zed & Claude",
                                fontSize = 12.sp,
                                color = if (isDark) TextOnDarkSecondary else TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Capture spoken architectural thoughts on your phone. Query, search, and implement them instantly inside your coding agents with our local Model Context Protocol (MCP) server.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = if (isDark) TextOnDarkSecondary else TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (!isAuthLoading) {
                                scope.launch {
                                    isAuthLoading = true
                                    val res = authManager.signInWithGoogle(context)
                                    isAuthLoading = false
                                    if (res.isSuccess) {
                                        isSignedIn = true
                                        userEmail = AppPreferences.getCloudUserEmail(context)
                                        Toast.makeText(context, "Connected to Srutam Cloud!", Toast.LENGTH_SHORT).show()
                                        CloudSyncManager.enqueueSync(context, forceAll = true)
                                        refreshKeys()
                                    } else {
                                        val err = res.exceptionOrNull()
                                        if (err !is GetCredentialCancellationException) {
                                            Toast.makeText(context, "Sign-in error: ${err?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isAuthLoading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue)
                    ) {
                        if (isAuthLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connecting...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        } else {
                            Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connect Cloud & MCP", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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

