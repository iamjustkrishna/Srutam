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
import space.iamjustkrishna.srutam.R
import space.iamjustkrishna.srutam.ui.theme.CeramicWhite
import space.iamjustkrishna.srutam.ui.theme.CobaltBlue
import space.iamjustkrishna.srutam.ui.theme.CobaltBorder
import space.iamjustkrishna.srutam.ui.theme.CobaltContainer
import space.iamjustkrishna.srutam.ui.theme.PlayfairDisplayFontFamily
import space.iamjustkrishna.srutam.ui.theme.SlateBorder
import space.iamjustkrishna.srutam.ui.theme.SlateGrouped
import space.iamjustkrishna.srutam.ui.theme.SlateSurface
import space.iamjustkrishna.srutam.ui.theme.SrutamTheme
import space.iamjustkrishna.srutam.ui.theme.TextMuted
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

    var selectedChoice by rememberSaveable { mutableStateOf(initialChoice) }
    var selectedProvider by rememberSaveable { mutableStateOf(AppPreferences.PROVIDER_GEMINI) }
    var apiKeyText by rememberSaveable { mutableStateOf("") }
    var selectedModel by rememberSaveable { mutableStateOf("") }
    var keyErrorText by rememberSaveable { mutableStateOf<String?>(null) }

    val isByok = selectedChoice == OnboardingAiChoice.BYOK

    val availableProviders = listOf(
        AppPreferences.PROVIDER_GEMINI to "Gemini",
        AppPreferences.PROVIDER_OPENAI to "OpenAI",
        AppPreferences.PROVIDER_ANTHROPIC to "Anthropic",
        AppPreferences.PROVIDER_GROQ to "Groq"
    )

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
            if (selectedModel.isNotBlank()) {
                AppPreferences.setCustomModel(context, selectedModel.trim())
            }
            AppPreferences.setByokOnboardingCompleted(context, true)
            Toast.makeText(context, "API Key saved successfully", Toast.LENGTH_SHORT).show()
            onComplete()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SlateSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Branded Jewel Header Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFEFF6FF),
                                Color(0xFFDBEAFE)
                            )
                        )
                    )
                    .border(
                        BorderStroke(1.5.dp, Color(0xFFBFDBFE)),
                        RoundedCornerShape(26.dp)
                    )
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(26.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.srutam_final_log),
                    contentDescription = "Srutam Mark",
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Choose Your AI Engine",
                fontFamily = PlayfairDisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Select how Srutam summarizes your thoughts, discovers recurring themes, and extracts action items.",
                fontSize = 14.sp,
                color = TextSecondary,
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
                    containerColor = if (!isByok) CeramicWhite else CeramicWhite.copy(alpha = 0.7f)
                ),
                border = BorderStroke(
                    width = if (!isByok) 2.dp else 1.dp,
                    color = if (!isByok) CobaltBlue else SlateBorder
                ),
                elevation = CardDefaults.cardElevation(if (!isByok) 4.dp else 1.dp)
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
                            .background(Color(0xFFEFF6FF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = CobaltBlue,
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
                                color = TextPrimary
                            )
                            Surface(
                                color = Color(0xFFECFDF5),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "Free",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Out-of-the-box transcription and AI summaries. Zero setup needed.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }

                    RadioButton(
                        selected = !isByok,
                        onClick = {
                            selectedChoice = OnboardingAiChoice.SRUTAM_CLOUD
                            keyErrorText = null
                        },
                        colors = RadioButtonDefaults.colors(selectedColor = CobaltBlue)
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
                    containerColor = if (isByok) CeramicWhite else CeramicWhite.copy(alpha = 0.7f)
                ),
                border = BorderStroke(
                    width = if (isByok) 2.dp else 1.dp,
                    color = if (isByok) CobaltBlue else SlateBorder
                ),
                elevation = CardDefaults.cardElevation(if (isByok) 4.dp else 1.dp)
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
                                .background(Color(0xFFFEF3C7))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
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
                                    color = TextPrimary
                                )
                                Surface(
                                    color = Color(0xFFEFF6FF),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "BYOK",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CobaltBlue,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Connect your Gemini, OpenAI, Claude, or Groq key for unlimited requests and model control.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )
                        }

                        RadioButton(
                            selected = isByok,
                            onClick = {
                                selectedChoice = OnboardingAiChoice.BYOK
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = CobaltBlue)
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
                            Text(
                                text = "SELECT PROVIDER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 0.8.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

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
                                                selectedProvider = code
                                            },
                                        color = if (isSelected) CobaltBlue else SlateGrouped,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) CobaltBlue else SlateBorder
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else TextPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "API KEY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 0.8.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

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
                                        color = TextMuted
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = CobaltContainer,
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
                                            color = CobaltBlue,
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
                                    focusedBorderColor = CobaltBlue,
                                    unfocusedBorderColor = SlateBorder,
                                    focusedContainerColor = CeramicWhite,
                                    unfocusedContainerColor = CeramicWhite
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
                    containerColor = CobaltBlue
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
                color = TextSecondary,
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
