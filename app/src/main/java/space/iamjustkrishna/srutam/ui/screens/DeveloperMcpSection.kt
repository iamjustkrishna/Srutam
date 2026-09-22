package space.iamjustkrishna.srutam.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.exceptions.GetCredentialCancellationException
import kotlinx.coroutines.launch
import space.iamjustkrishna.srutam.cloud.ApiKeyItem
import space.iamjustkrishna.srutam.cloud.CloudSyncManager
import space.iamjustkrishna.srutam.cloud.SupabaseAuthManager
import space.iamjustkrishna.srutam.cloud.SupabaseCloudClient
import space.iamjustkrishna.srutam.ui.components.DialogBadgeType
import space.iamjustkrishna.srutam.ui.components.SrutamStandardDialog
import space.iamjustkrishna.srutam.ui.theme.*
import space.iamjustkrishna.srutam.utils.AppPreferences

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

    var isAuthLoading by remember { mutableStateOf(false) }
    var isAutoSyncEnabled by remember {
        mutableStateOf(AppPreferences.isAutoCloudSyncEnabled(context))
    }
    var selectedMcpClient by remember { mutableStateOf("OpenCode") }

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
                                text = if (apiKeys.size >= 3) "Max 3 keys reached. Revoke one to create new." else "Keys for OpenCode, Cursor, Windsurf, Zed & Claude",
                                fontSize = 11.sp,
                                color = if (apiKeys.size >= 3) Color(0xFFEF4444) else (if (isDark) TextOnDarkSecondary else TextSecondary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = {
                                if (apiKeys.size >= 3) {
                                    Toast.makeText(context, "Maximum 3 keys allowed. Please revoke an old key first.", Toast.LENGTH_SHORT).show()
                                } else {
                                    keyNameInput = "Cursor IDE"
                                    showGenerateKeyDialog = true
                                }
                            },
                            enabled = apiKeys.size < 3,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CobaltBlue,
                                disabledContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                                disabledContentColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Key", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isLoadingKeys) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = CobaltBlue)
                        }
                    } else if (apiKeys.isEmpty()) {
                        Text(
                            text = "No active API keys. Generate one to connect your IDE coding agents.",
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Setup Guide Card with Client Selector
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF070B18) else Color(0xFF1E293B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Agent MCP Config Snippet",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                IconButton(
                                    onClick = {
                                        val sampleJson = when (selectedMcpClient) {
                                            "OpenCode" -> """
                                            {
                                              "${'$'}schema": "https://opencode.ai/config.json",
                                              "mcp": {
                                                "srutam": {
                                                  "type": "local",
                                                  "command": ["npx", "-y", "srutam-mcp"],
                                                  "environment": {
                                                    "SRUTAM_API_KEY": "YOUR_GENERATED_KEY"
                                                  },
                                                  "enabled": true
                                                }
                                              }
                                            }
                                            """.trimIndent()
                                            "Zed" -> """
                                            {
                                              "context_servers": {
                                                "srutam": {
                                                  "command": {
                                                    "path": "npx",
                                                    "args": ["-y", "srutam-mcp"],
                                                    "env": {
                                                      "SRUTAM_API_KEY": "YOUR_GENERATED_KEY"
                                                    }
                                                  }
                                                }
                                              }
                                            }
                                            """.trimIndent()
                                            else -> """
                                            {
                                              "mcpServers": {
                                                "srutam": {
                                                  "command": "npx",
                                                  "args": ["-y", "srutam-mcp"],
                                                  "env": {
                                                    "SRUTAM_API_KEY": "YOUR_GENERATED_KEY"
                                                  }
                                                }
                                              }
                                            }
                                            """.trimIndent()
                                        }
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("$selectedMcpClient MCP Config", sampleJson))
                                        Toast.makeText(context, "$selectedMcpClient config copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Config", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Client Switcher Chips Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("OpenCode", "Cursor", "Windsurf", "Zed", "Claude").forEach { client ->
                                    val isSelected = selectedMcpClient == client
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) CobaltBlue else (if (isDark) Color(0xFF131B2E) else Color(0xFF334155)),
                                        modifier = Modifier.clickable { selectedMcpClient = client }
                                    ) {
                                        Text(
                                            text = client,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val configPathHint = when (selectedMcpClient) {
                                "OpenCode" -> "opencode.json (project root or ~/.config/opencode/)"
                                "Cursor" -> ".cursor/mcp.json or Settings > Features > MCP"
                                "Windsurf" -> "~/.codeium/windsurf/mcp_config.json"
                                "Zed" -> "settings.json (under context_servers)"
                                "Claude" -> "claude_desktop_config.json"
                                else -> "mcp_config.json"
                            }

                            Text(
                                text = "Path: $configPathHint",
                                fontSize = 10.5.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            val snippetDisplay = when (selectedMcpClient) {
                                "OpenCode" -> """
                                {
                                  "${'$'}schema": "https://opencode.ai/config.json",
                                  "mcp": {
                                    "srutam": {
                                      "type": "local",
                                      "command": ["npx", "-y", "srutam-mcp"],
                                      "environment": {
                                        "SRUTAM_API_KEY": "YOUR_GENERATED_KEY"
                                      },
                                      "enabled": true
                                    }
                                  }
                                }
                                """.trimIndent()
                                "Zed" -> """
                                {
                                  "context_servers": {
                                    "srutam": {
                                      "command": {
                                        "path": "npx",
                                        "args": ["-y", "srutam-mcp"],
                                        "env": {
                                          "SRUTAM_API_KEY": "YOUR_GENERATED_KEY"
                                        }
                                      }
                                    }
                                  }
                                }
                                """.trimIndent()
                                else -> """
                                {
                                  "mcpServers": {
                                    "srutam": {
                                      "command": "npx",
                                      "args": ["-y", "srutam-mcp"],
                                      "env": {
                                        "SRUTAM_API_KEY": "YOUR_GENERATED_KEY"
                                      }
                                    }
                                  }
                                }
                                """.trimIndent()
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color(0xFF03050B) else Color(0xFF0F172A),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = snippetDisplay,
                                    fontSize = 10.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF38BDF8),
                                    lineHeight = 15.sp,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .horizontalScroll(rememberScrollState())
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Run 'srutam-mcp init' in terminal for auto configuration wizard.",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
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

    // Modal: Generate New API Key
    if (showGenerateKeyDialog) {
        if (generatedKeyResult == null) {
            SrutamStandardDialog(
                onDismissRequest = { showGenerateKeyDialog = false },
                title = "Create Agent API Key",
                subtitle = "Label this key to recognize your IDE or laptop",
                icon = Icons.Default.VpnKey,
                badgeType = DialogBadgeType.PRIMARY,
                confirmText = "Generate",
                dismissText = "Cancel",
                onConfirm = {
                    scope.launch {
                        val res = cloudClient.createApiKey(keyNameInput)
                        if (res.isSuccess) {
                            generatedKeyResult = res.getOrThrow()
                            refreshKeys()
                        } else {
                            Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                content = {
                    OutlinedTextField(
                        value = keyNameInput,
                        onValueChange = { keyNameInput = it },
                        label = { Text("Key Label") },
                        placeholder = { Text("e.g. OpenCode, Cursor, Windsurf") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
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
