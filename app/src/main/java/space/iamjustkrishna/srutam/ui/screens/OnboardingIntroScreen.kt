package space.iamjustkrishna.srutam.ui.screens

import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import space.iamjustkrishna.srutam.ui.theme.CobaltBlue
import space.iamjustkrishna.srutam.ui.theme.CobaltBorder
import space.iamjustkrishna.srutam.ui.theme.CobaltContainer
import space.iamjustkrishna.srutam.ui.theme.CosmicVoidBackground
import space.iamjustkrishna.srutam.ui.theme.LocalIsCosmicDark
import space.iamjustkrishna.srutam.ui.theme.PlayfairDisplayFontFamily
import space.iamjustkrishna.srutam.ui.theme.SlateGrouped
import space.iamjustkrishna.srutam.ui.theme.TextOnDarkPrimary
import space.iamjustkrishna.srutam.ui.theme.TextOnDarkSecondary
import space.iamjustkrishna.srutam.ui.theme.TextPrimary
import space.iamjustkrishna.srutam.ui.theme.TextSecondary
import space.iamjustkrishna.srutam.utils.Analytics

private data class IntroPage(
    val step: String,
    val icon: ImageVector,
    val tag: String,
    val title: String,
    val body: String
)

private val introPages = listOf(
    IntroPage(
        step = "intro_1",
        icon = Icons.Default.Mic,
        tag = "ON YOUR PHONE",
        title = "Speak, and it's transcribed on your phone",
        body = "Record a lecture, a meeting or a passing thought. Srutam turns your voice into text right on the device."
    ),
    IntroPage(
        step = "intro_2",
        icon = Icons.Default.AutoAwesome,
        tag = "INSIGHTS",
        title = "Get a summary, tasks, ideas, decisions",
        body = "Every note comes back organised: what was said, what to do next, and what was decided."
    ),
    IntroPage(
        step = "intro_3",
        icon = Icons.Default.NotificationsActive,
        tag = "REMINDERS",
        title = "Reminders from what you said",
        body = "Say \"remind me to call the landlord on Friday\" and Srutam schedules it for you."
    )
)

/**
 * First-run intro: three value pages, no permissions. The last page offers a way back in
 * for people who used Srutam before. Permissions are asked later, when a feature needs them.
 */
@Composable
fun OnboardingIntroScreen(
    onFinished: () -> Unit,
    onRestoreNotes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = LocalIsCosmicDark.current
    val pagerState = rememberPagerState(pageCount = { introPages.size })
    val scope = rememberCoroutineScope()
    var pageShownAt by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    fun timeOnPage() = SystemClock.elapsedRealtime() - pageShownAt

    LaunchedEffect(pagerState.currentPage) {
        pageShownAt = SystemClock.elapsedRealtime()
        Analytics.onboardingStepViewed(context, introPages[pagerState.currentPage].step)
    }

    val isLastPage = pagerState.currentPage == introPages.lastIndex

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) CosmicVoidBackground else SlateGrouped)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (!isLastPage) {
                    TextButton(onClick = {
                        val page = introPages[pagerState.currentPage]
                        Analytics.onboardingStepResult(context, page.step, Analytics.RESULT_SKIPPED, timeOnPage())
                        onFinished()
                    }) {
                        Text("Skip", color = if (isDark) TextOnDarkSecondary else TextSecondary)
                    }
                } else {
                    Spacer(Modifier.height(48.dp))
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { index ->
                IntroPageContent(introPages[index], isDark)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                introPages.indices.forEach { i ->
                    val selected = i == pagerState.currentPage
                    val width by animateDpAsState(if (selected) 22.dp else 8.dp, label = "dotWidth")
                    val color by animateColorAsState(
                        if (selected) CobaltBlue else CobaltBorder.copy(alpha = 0.5f),
                        label = "dotColor"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(width = width, height = 8.dp)
                            .background(color, CircleShape)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        val page = introPages[pagerState.currentPage]
                        Analytics.onboardingStepResult(context, page.step, Analytics.RESULT_NEXT, timeOnPage())
                        if (isLastPage) {
                            onFinished()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = if (isLastPage) "Get started" else "Next",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                if (isLastPage) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        Analytics.onboardingStepResult(context, "intro_3", "restore", timeOnPage())
                        onRestoreNotes()
                    }) {
                        Text(
                            text = "Used Srutam before? Restore my notes",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CobaltBlue
                        )
                    }
                } else {
                    Spacer(Modifier.height(56.dp))
                }
            }
        }
    }
}

@Composable
private fun IntroPageContent(page: IntroPage, isDark: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = if (isDark) CobaltContainer.copy(alpha = 0.35f) else CobaltContainer,
            border = BorderStroke(1.dp, CobaltBorder.copy(alpha = 0.4f)),
            modifier = Modifier.size(112.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    tint = CobaltBlue,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = page.tag,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = CobaltBlue
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = page.title,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontFamily = PlayfairDisplayFontFamily,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = if (isDark) TextOnDarkPrimary else TextPrimary
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = page.body,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            color = if (isDark) TextOnDarkSecondary else TextSecondary,
            modifier = Modifier.widthIn(max = 320.dp)
        )
    }
}
