package space.iamjustkrishna.srutam.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import space.iamjustkrishna.srutam.cloud.PairingPreview
import space.iamjustkrishna.srutam.cloud.PairingRules
import space.iamjustkrishna.srutam.ui.components.DialogBadgeType
import space.iamjustkrishna.srutam.ui.components.SrutamStandardDialog
import space.iamjustkrishna.srutam.ui.theme.*

/** Where the pairing flow currently is. Held by the caller so it survives recomposition. */
sealed interface PairingUiState {
    data object Idle : PairingUiState
    /** Manual entry, used when the scanner is unavailable or the user picks "Enter code". */
    data class EnteringCode(val code: String = "", val error: String? = null, val busy: Boolean = false) : PairingUiState
    data class Confirming(
        val code: String,
        val preview: PairingPreview,
        val name: String,
        val error: String? = null,
        val busy: Boolean = false
    ) : PairingUiState
}

/**
 * Manual code entry, for devices without Play Services and for anyone who would rather type.
 * Accepts the dashed, lowercase form the terminal prints.
 */
@Composable
fun EnterPairingCodeDialog(
    state: PairingUiState.EnteringCode,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    SrutamStandardDialog(
        onDismissRequest = { if (!state.busy) onDismiss() },
        title = "Enter the code",
        subtitle = "Type the code shown on your computer by srutam-mcp init",
        icon = Icons.Default.Keyboard,
        badgeType = DialogBadgeType.PRIMARY,
        confirmText = if (state.busy) "Checking..." else "Continue",
        dismissText = "Cancel",
        onConfirm = { if (!state.busy) onSubmit() },
        content = {
            Column {
                OutlinedTextField(
                    value = state.code,
                    onValueChange = onCodeChange,
                    label = { Text("Pairing code") },
                    placeholder = { Text("K7QF-2M9D") },
                    singleLine = true,
                    enabled = !state.busy,
                    isError = state.error != null,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp
                    ),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.error != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = state.error, fontSize = 12.sp, color = Color(0xFFEF4444))
                }
            }
        }
    )
}

/**
 * The confirmation sheet.
 *
 * It names the machine and spells out what access is being granted, because the one attack this
 * design cannot prevent server-side is a user approving a code that came from someone else's
 * screen. The countdown is driven by the server's expiry, never the phone clock.
 */
@Composable
fun ConfirmPairingDialog(
    state: PairingUiState.Confirming,
    isDark: Boolean,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var secondsLeft by remember(state.preview.expiresAt) {
        mutableStateOf(PairingRules.secondsUntil(state.preview.expiresAt, System.currentTimeMillis()))
    }
    LaunchedEffect(state.preview.expiresAt) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft = PairingRules.secondsUntil(state.preview.expiresAt, System.currentTimeMillis())
        }
    }
    val expired = secondsLeft <= 0

    SrutamStandardDialog(
        onDismissRequest = { if (!state.busy) onDismiss() },
        title = "Connect this computer?",
        subtitle = state.preview.label,
        icon = Icons.Default.Computer,
        badgeType = DialogBadgeType.PRIMARY,
        confirmText = when {
            state.busy -> "Connecting..."
            expired -> "Code expired"
            else -> "Connect"
        },
        dismissText = "Cancel",
        onConfirm = { if (!state.busy && !expired) onConfirm() },
        content = {
            Column {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0xFF0B1220) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        DetailRow("Computer", state.preview.label, isDark)
                        state.preview.clientVersion?.takeIf { it.isNotBlank() }?.let {
                            DetailRow("srutam-mcp", it, isDark)
                        }
                        DetailRow(
                            "Code expires",
                            if (expired) "expired" else formatCountdown(secondsLeft),
                            isDark,
                            valueColor = if (expired) Color(0xFFEF4444) else null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "It will be able to read notes you have not marked private, complete tasks, and add work logs.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = if (isDark) TextOnDarkSecondary else TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Only continue if you just ran srutam-mcp on your own computer.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF59E0B)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text("Key name") },
                    singleLine = true,
                    enabled = !state.busy && !expired,
                    isError = state.error != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.error != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = state.error, fontSize = 12.sp, color = Color(0xFFEF4444))
                }
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String, isDark: Boolean, valueColor: Color? = null) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            color = if (isDark) TextOnDarkSecondary else TextSecondary,
            modifier = Modifier.width(96.dp)
        )
        Text(
            text = value,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor ?: (if (isDark) TextOnDarkPrimary else TextPrimary)
        )
    }
}

private fun formatCountdown(seconds: Long): String {
    if (seconds <= 0) return "expired"
    val minutes = seconds / 60
    val rest = seconds % 60
    return if (minutes > 0) String.format("%d:%02d", minutes, rest) else "${rest}s"
}
