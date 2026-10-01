package space.iamjustkrishna.srutam.ui.screens

import android.app.Activity
import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.ConnectionResult
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch
import space.iamjustkrishna.srutam.cloud.PairingError
import space.iamjustkrishna.srutam.cloud.PairingException
import space.iamjustkrishna.srutam.cloud.PairingPreview
import space.iamjustkrishna.srutam.cloud.PairingRules
import space.iamjustkrishna.srutam.cloud.SupabaseCloudClient
import space.iamjustkrishna.srutam.ui.theme.*

/**
 * "Connect a computer": the primary way to give a coding agent access.
 *
 * The computer generates its own key and shows a short-lived code; this screen only approves it.
 * No key is ever displayed here, so nothing secret can be photographed off the phone.
 */
@Composable
fun ConnectComputerCard(
    isDark: Boolean,
    enabled: Boolean,
    onScanRequested: () -> Unit,
    onEnterCodeRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF101A33) else Color(0xFFEFF4FF),
        border = BorderStroke(1.dp, if (isDark) CosmicGlowBlue.copy(alpha = 0.35f) else CobaltBlue.copy(alpha = 0.25f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) CobaltBlue.copy(alpha = 0.22f) else CobaltContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = CobaltBlue, modifier = Modifier.size(19.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Connect a computer",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) TextOnDarkPrimary else TextPrimary
                    )
                    Text(
                        text = "Run srutam-mcp init and scan the code it shows",
                        fontSize = 11.5.sp,
                        color = if (isDark) TextOnDarkSecondary else TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onScanRequested,
                    enabled = enabled,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CobaltBlue,
                        disabledContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                        disabledContentColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(38.dp).weight(1f)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan QR", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onEnterCodeRequested,
                    enabled = enabled,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isDark) CosmicVoidCardBorder else SlateBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isDark) TextOnDarkPrimary else TextPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text("Enter code", fontSize = 12.5.sp)
                }
            }

            if (!enabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Revoke a key below to connect another computer.",
                    fontSize = 11.sp,
                    color = Color(0xFFEF4444)
                )
            }
        }
    }
}

/**
 * Launches Google's code scanner, which runs in Play Services and renders its own camera UI, so
 * the app needs no CAMERA permission. Falls back to manual entry wherever it is unavailable
 * (no Play Services, emulators, some custom ROMs).
 */
object CodeScannerLauncher {

    fun isAvailable(context: Context): Boolean =
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

    fun scan(
        context: Context,
        onResult: (String) -> Unit,
        onUnavailable: () -> Unit,
        onCancelled: () -> Unit
    ) {
        if (!isAvailable(context)) {
            onUnavailable()
            return
        }
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
        try {
            GmsBarcodeScanning.getClient(context, options)
                .startScan()
                .addOnSuccessListener { barcode -> onResult(barcode.rawValue.orEmpty()) }
                .addOnCanceledListener { onCancelled() }
                .addOnFailureListener { onUnavailable() }
        } catch (_: Exception) {
            onUnavailable()
        }
    }
}

/**
 * Device-credential gate before a pairing is approved.
 *
 * Approving hands a computer long-lived read access to the user's notes, so it should not be
 * possible from a phone someone else is holding. Falls back to PIN/pattern when no biometric is
 * enrolled, and proceeds without a prompt only when the device has no secure lock at all.
 */
object PairingConfirmGate {

    fun confirm(context: Context, onConfirmed: () -> Unit, onDenied: (String) -> Unit) {
        val activity = context.findFragmentActivity()
        val manager = BiometricManager.from(context)
        val allowed = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

        if (activity == null || manager.canAuthenticate(allowed) != BiometricManager.BIOMETRIC_SUCCESS) {
            // No secure lock (or not hosted in a FragmentActivity): the in-app confirmation is the gate.
            onConfirmed()
            return
        }

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onConfirmed()
                override fun onAuthenticationError(code: Int, message: CharSequence) {
                    if (code == BiometricPrompt.ERROR_USER_CANCELED || code == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        onDenied("")
                    } else {
                        onDenied(message.toString())
                    }
                }
            }
        )

        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Confirm it's you")
                .setSubtitle("Connecting a computer gives it access to your notes")
                .setAllowedAuthenticators(allowed)
                .build()
        )
    }

    private fun Context.findFragmentActivity(): FragmentActivity? {
        var current: Context? = this
        while (current is android.content.ContextWrapper) {
            if (current is FragmentActivity) return current
            current = current.baseContext
        }
        return null
    }
}
