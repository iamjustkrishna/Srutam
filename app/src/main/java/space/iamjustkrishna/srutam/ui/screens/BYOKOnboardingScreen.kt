package space.iamjustkrishna.srutam.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.HorizontalDivider
import space.iamjustkrishna.srutam.R
import space.iamjustkrishna.srutam.ui.theme.CeramicWhite
import space.iamjustkrishna.srutam.ui.theme.CobaltBlue
import space.iamjustkrishna.srutam.ui.theme.CobaltBorder
import space.iamjustkrishna.srutam.ui.theme.CobaltContainer
import space.iamjustkrishna.srutam.ui.theme.CosmicGlowBlue
import space.iamjustkrishna.srutam.ui.theme.CosmicVoidBackground
import space.iamjustkrishna.srutam.ui.theme.CosmicVoidCard
import space.iamjustkrishna.srutam.ui.theme.CosmicVoidCardBorder
import space.iamjustkrishna.srutam.ui.theme.LocalIsCosmicDark
import space.iamjustkrishna.srutam.ui.theme.PlayfairDisplayFontFamily
import space.iamjustkrishna.srutam.ui.theme.SlateBorder
import space.iamjustkrishna.srutam.ui.theme.SlateGrouped
import space.iamjustkrishna.srutam.ui.theme.SlateSurface
import space.iamjustkrishna.srutam.ui.theme.SrutamTheme
import space.iamjustkrishna.srutam.ui.theme.StardustGold
import space.iamjustkrishna.srutam.ui.theme.TextMuted
import space.iamjustkrishna.srutam.ui.theme.TextOnDarkPrimary
import space.iamjustkrishna.srutam.ui.theme.TextOnDarkSecondary
import space.iamjustkrishna.srutam.ui.theme.TextPrimary
import space.iamjustkrishna.srutam.ui.theme.TextSecondary
import space.iamjustkrishna.srutam.utils.AppPreferences

enum class OnboardingAiChoice {
    SRUTAM_CLOUD,
    BYOK
}

@Composable
fun BYOKOnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    initialChoice: OnboardingAiChoice = OnboardingAiChoice.SRUTAM_CLOUD
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()
    val isDark = LocalIsCosmicDark.current

    var selectedChoice by rememberSaveable { mutableStateOf(initialChoice) }
    var selectedProvider by rememberSaveable { mutableStateOf(AppPreferences.PROVIDER_GEMINI) }
    var apiKeyText by rememberSaveable { mutableStateOf("") }
    var selectedModel by rememberSaveable {
        mutableStateOf(AppPreferences.getCustomModel(context, AppPreferences.PROVIDER_GEMINI))
    }
    var keyErrorText by rememberSaveable { mutableStateOf<String?>(null) }

    val isByok = selectedChoice == OnboardingAiChoice.BYOK

    val availableProviders = listOf(
        AppPreferences.PROVIDER_GEMINI to "Gemini",
        AppPreferences.PROVIDER_OPENAI to "OpenAI",
        AppPreferences.PROVIDER_ANTHROPIC to "Anthropic",
        AppPreferences.PROVIDER_GROQ to "Groq"
    )

    fun selectProvider(provider: String) {
        selectedProvider = provider
        selectedModel = AppPreferences.getCustomModel(context, provider)
        keyErrorText = null
    }

    fun handleContinue() {
        if (selectedChoice == OnboardingAiChoice.SRUTAM_CLOUD) {
            AppPreferences.setAIProvider(context, AppPreferences.PROVIDER_SRUTAM_DEFAULT)
            AppPreferences.setByokOnboardingCompleted(context, true)
            onComplete()
        } else {
            val keyTrimmed = apiKeyText.trim()
            if (keyTrimmed.isBlank()) {
                keyErrorText = "Please enter or paste your API key, or choose Srutam Cloud"
                return
            }
            keyErrorText = null
            AppPreferences.setAIProvider(context, selectedProvider)
            AppPreferences.setCustomApiKey(context, keyTrimmed)
            val modelToSave = if (selectedModel.isNotBlank()) {
                selectedModel.trim()
            } else {
                AppPreferences.getCustomModel(context, selectedProvider)
            }
            AppPreferences.setCustomModel(context, modelToSave)
            AppPreferences.setByokOnboardingCompleted(context, true)
            Toast.makeText(context, "API Key saved successfully", Toast.LENGTH_SHORT).show()
            onComplete()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (isDark) {
                    Modifier.background(CosmicVoidBackground)
                } else {
                    Modifier.background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFFFFF),
                                Color(0xFFF8FAFC),
                                Color(0xFFF1F5F9)
                            )
                        )
                    )
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Floating Branded 3D Mark with Soft Ambient Glow
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(92.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = if (isDark) {
                                    listOf(
                                        CosmicGlowBlue.copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                } else {
                                    listOf(
                                        CobaltBlue.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                }
                            ),
                            shape = CircleShape
                        )
                )
                Image(
                    painter = painterResource(id = R.drawable.srutam_final_log),
                    contentDescription = "Srutam Mark",
                    modifier = Modifier.size(72.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Choose Your AI Engine",
                fontFamily = PlayfairDisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = if (isDark) Color.White else TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Select how Srutam summarizes your thoughts, discovers recurring themes, and extracts action items.",
                fontSize = 14.sp,
                color = if (isDark) TextOnDarkSecondary else TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Card 1: Srutam Cloud (Default / Recommended)
            Card(
                onClick = {
                    selectedChoice = OnboardingAiChoice.SRUTAM_CLOUD
                    keyErrorText = null
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) CosmicVoidCard else Color.White
                ),
                border = BorderStroke(
                    width = if (!isByok) 1.5.dp else 1.dp,
                    color = when {
                        !isByok -> if (isDark) CosmicGlowBlue else CobaltBlue
                        isDark -> CosmicVoidCardBorder
                        else -> SlateBorder
                    }
                ),
                elevation = CardDefaults.cardElevation(if (!isByok) 4.dp else 1.5.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.35f) else Color(0xFFEFF6FF)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = if (isDark) CosmicGlowBlue else CobaltBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Srutam Cloud",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else TextPrimary
                            )
                            Surface(
                                color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.6f) else Color(0xFFECFDF5),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "Free",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Out-of-the-box transcription and AI summaries. Zero setup needed.",
                            fontSize = 13.sp,
                            color = if (isDark) TextOnDarkSecondary else TextSecondary,
                            lineHeight = 18.sp
                        )
                    }

                    RadioButton(
                        selected = !isByok,
                        onClick = {
                            selectedChoice = OnboardingAiChoice.SRUTAM_CLOUD
                            keyErrorText = null
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = if (isDark) CosmicGlowBlue else CobaltBlue,
                            unselectedColor = if (isDark) CosmicVoidCardBorder else SlateBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Card 2: Bring Your Own Key (BYOK)
            Card(
                onClick = {
                    selectedChoice = OnboardingAiChoice.BYOK
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) CosmicVoidCard else Color.White
                ),
                border = BorderStroke(
                    width = if (isByok) 1.5.dp else 1.dp,
                    color = when {
                        isByok -> if (isDark) CosmicGlowBlue else CobaltBlue
                        isDark -> CosmicVoidCardBorder
                        else -> SlateBorder
                    }
                ),
                elevation = CardDefaults.cardElevation(if (isByok) 4.dp else 1.5.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isDark) Color(0xFF78350F).copy(alpha = 0.35f) else Color(0xFFFEF3C7)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = if (isDark) StardustGold else Color(0xFFD97706),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Bring Your Own Key",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else TextPrimary
                                )
                                Surface(
                                    color = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFFEFF6FF),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "BYOK",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF93C5FD) else CobaltBlue,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Connect your Gemini, OpenAI, Claude, or Groq key for unlimited requests and model control.",
                                fontSize = 13.sp,
                                color = if (isDark) TextOnDarkSecondary else TextSecondary,
                                lineHeight = 18.sp
                            )
                        }

                        RadioButton(
                            selected = isByok,
                            onClick = {
                                selectedChoice = OnboardingAiChoice.BYOK
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = if (isDark) CosmicGlowBlue else CobaltBlue,
                                unselectedColor = if (isDark) CosmicVoidCardBorder else SlateBorder
                            )
                        )
                    }

                    // Expanded configuration when BYOK is selected
                    AnimatedVisibility(
                        visible = isByok,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            HorizontalDivider(
                                color = if (isDark) CosmicVoidCardBorder else SlateBorder.copy(alpha = 0.7f),
                                thickness = 1.dp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "SELECT PROVIDER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) TextOnDarkSecondary else TextSecondary,
                                letterSpacing = 0.8.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                availableProviders.forEach { (code, label) ->
                                    val isSelected = selectedProvider == code
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                selectProvider(code)
                                            },
                                        color = when {
                                            isSelected -> if (isDark) CosmicGlowBlue else CobaltBlue
                                            isDark -> CosmicVoidBackground
                                            else -> Color(0xFFF8FAFC)
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            when {
                                                isSelected -> if (isDark) CosmicGlowBlue else CobaltBlue
                                                isDark -> CosmicVoidCardBorder
                                                else -> SlateBorder
                                            }
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 9.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else if (isDark) TextOnDarkPrimary else TextPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "API KEY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) TextOnDarkSecondary else TextSecondary,
                                letterSpacing = 0.8.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = apiKeyText,
                                onValueChange = {
                                    apiKeyText = it
                                    keyErrorText = null
                                },
                                placeholder = {
                                    Text(
                                        text = "Paste your $selectedProvider API key",
                                        fontSize = 13.sp,
                                        color = if (isDark) TextOnDarkSecondary.copy(alpha = 0.7f) else TextMuted
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (isDark) TextOnDarkSecondary else TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDark) Color(0xFF1E3A8A) else CobaltContainer,
                                        modifier = Modifier
                                            .padding(end = 6.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                val clip = clipboardManager.getText()?.text.orEmpty().trim()
                                                if (clip.isNotBlank()) {
                                                    apiKeyText = clip
                                                    keyErrorText = null
                                                }
                                            }
                                    ) {
                                        Text(
                                            text = "PASTE",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF93C5FD) else CobaltBlue,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (isDark) CosmicGlowBlue else CobaltBlue,
                                    unfocusedBorderColor = if (isDark) CosmicVoidCardBorder else SlateBorder,
                                    focusedContainerColor = if (isDark) CosmicVoidBackground else Color(0xFFF8FAFC),
                                    unfocusedContainerColor = if (isDark) CosmicVoidBackground else Color(0xFFF8FAFC),
                                    focusedTextColor = if (isDark) Color.White else TextPrimary,
                                    unfocusedTextColor = if (isDark) Color.White else TextPrimary
                                )
                            )

                            if (keyErrorText != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = keyErrorText!!,
                                    fontSize = 12.sp,
                                    color = Color(0xFFEF4444)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "LATEST MODEL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) TextOnDarkSecondary else TextSecondary,
                                letterSpacing = 0.8.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            val modelPresets = when (selectedProvider) {
                                AppPreferences.PROVIDER_GEMINI -> listOf("gemini-2.0-flash", "gemini-2.0-flash-lite", "gemini-1.5-pro", "gemini-1.5-flash")
                                AppPreferences.PROVIDER_OPENAI -> listOf("gpt-4o", "gpt-4o-mini", "o3-mini")
                                AppPreferences.PROVIDER_ANTHROPIC -> listOf("claude-3-7-sonnet-20250219", "claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022")
                                AppPreferences.PROVIDER_GROQ -> listOf("llama-3.3-70b-versatile", "deepseek-r1-distill-llama-70b", "llama-3.1-8b-instant")
                                else -> listOf("gemini-2.0-flash")
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                modelPresets.forEach { preset ->
                                    val isSelected = selectedModel == preset
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                selectedModel = preset
                                            },
                                        color = when {
                                            isSelected -> if (isDark) Color(0xFF1E3A8A) else CobaltContainer
                                            isDark -> CosmicVoidBackground
                                            else -> Color(0xFFF8FAFC)
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            when {
                                                isSelected -> if (isDark) CosmicGlowBlue else CobaltBlue
                                                isDark -> CosmicVoidCardBorder
                                                else -> SlateBorder
                                            }
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = preset,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = when {
                                                isSelected -> if (isDark) Color(0xFF93C5FD) else CobaltBlue
                                                isDark -> TextOnDarkSecondary
                                                else -> TextPrimary
                                            },
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Primary CTA Button
            Button(
                onClick = { handleContinue() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) CosmicGlowBlue else CobaltBlue
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Continue to Srutam",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Skip Option
            Text(
                text = "Skip for now",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) TextOnDarkSecondary else TextSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        AppPreferences.setAIProvider(context, AppPreferences.PROVIDER_SRUTAM_DEFAULT)
                        AppPreferences.setByokOnboardingCompleted(context, true)
                        onComplete()
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BYOKOnboardingPreview() {
    SrutamTheme {
        BYOKOnboardingScreen(onComplete = {})
    }
}
