package space.iamjustkrishna.srutam.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    var showEmailLoginDialog by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isAuthLoading by remember { mutableStateOf(false) }

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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(CosmicAuroraGreen)
                            )
                            Column {
                                Text(
                                    text = "Srutam Cloud Active",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) TextOnDarkPrimary else TextPrimary
                                )
                                Text(
                                    text = userEmail ?: "Signed in",
                                    fontSize = 12.sp,
                                    color = if (isDark) TextOnDarkSecondary else TextSecondary
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    CloudSyncManager.enqueueSync(context)
                                    Toast.makeText(context, "Syncing notes to cloud...", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync", fontSize = 12.sp)
                            }

                            TextButton(
                                onClick = {
                                    authManager.signOut()
                                    isSignedIn = false
                                    userEmail = null
                                    apiKeys = emptyList()
                                }
                            ) {
                                Text("Sign Out", fontSize = 12.sp, color = Color(0xFFEF4444))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = if (isDark) CosmicVoidCardBorder else SlateBorder, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // API Keys sub-header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "MCP Agent Keys",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) TextOnDarkPrimary else TextPrimary
                            )
                            Text(
                                text = "Keys for Cursor, Antigravity, and Claude Desktop",
                                fontSize = 11.sp,
                                color = if (isDark) TextOnDarkSecondary else TextSecondary
                            )
                        }

                        Button(
                            onClick = {
                                keyNameInput = "Cursor IDE"
                                showGenerateKeyDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Key", fontSize = 12.sp)
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

                    // Setup Guide Card
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
                                        val sampleJson = """
                                        {
                                          "mcpServers": {
                                            "srutam": {
                                              "command": "npx",
                                              "args": ["-y", "@srutam/mcp-server"],
                                              "env": {
                                                "SRUTAM_API_KEY": "YOUR_GENERATED_KEY"
                                              }
                                            }
                                          }
                                        }
                                        """.trimIndent()
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("MCP Config", sampleJson))
                                        Toast.makeText(context, "Config copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Config", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(
                                text = "Add @srutam/mcp-server to your Cursor or Antigravity mcp_config.json to query your voice memos from your coding agent.",
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
                                text = "Connect Cursor, Antigravity & Claude Desktop",
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
                            showEmailLoginDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue)
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect Cloud & MCP", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                        placeholder = { Text("e.g. Work Cursor, Antigravity") },
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

    // Modal: Cloud Sign-In
    if (showEmailLoginDialog) {
        SrutamStandardDialog(
            onDismissRequest = { showEmailLoginDialog = false },
            title = "Sign In to Srutam Cloud",
            subtitle = "Enable cloud sync and API keys for AI coding agents",
            icon = Icons.Default.CloudSync,
            badgeType = DialogBadgeType.PRIMARY,
            confirmText = if (isAuthLoading) "Connecting..." else "Sign In",
            dismissText = "Cancel",
            confirmEnabled = !isAuthLoading,
            onConfirm = {
                if (emailInput.isNotBlank() && passwordInput.isNotBlank()) {
                    scope.launch {
                        isAuthLoading = true
                        val res = authManager.signInWithEmailPassword(emailInput.trim(), passwordInput.trim())
                        isAuthLoading = false
                        if (res.isSuccess) {
                            isSignedIn = true
                            userEmail = emailInput.trim()
                            showEmailLoginDialog = false
                            Toast.makeText(context, "Connected to Srutam Cloud!", Toast.LENGTH_SHORT).show()
                            CloudSyncManager.enqueueSync(context)
                        } else {
                            Toast.makeText(context, "Sign in failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                }
            },
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Address") },
                        placeholder = { Text("developer@example.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }
}
