package space.iamjustkrishna.srutam.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import space.iamjustkrishna.srutam.viewmodel.ThemeCluster
import space.iamjustkrishna.srutam.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

/**
 * Instrument-grade date scroller row for the Insights screen.
 *
 * Three-part layout: [Month Year (Left)] + [Horizontal Day Scroller (Center)] + [All (Right)].
 *
 * Design philosophy: Dieter Rams functional minimalism.
 * - Zero ripple / shadow / elevation on any interactive surface in this row.
 * - Single-line "Sep 2026" month-year with bounded vertical snap scroll.
 * - Smooth snap-fling day numbers with edge gradient masks.
 * - Dedicated tactile "All" squircle pill with zero elevation.
 */
@Composable
fun InsightsDateScroller(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    datesWithActivity: Set<LocalDate>,
    earliestMonth: YearMonth = YearMonth.now(),
    isSearchExpanded: Boolean = false,
    onToggleSearch: () -> Unit = {},
    modifier: Modifier = Modifier,
    // Inline theme chips
    themes: List<ThemeCluster> = emptyList(),
    selectedTheme: String? = null,
    onSelectTheme: (String?) -> Unit = {},
    onDismissTheme: (String) -> Unit = {}
) {
    val dark = LocalIsCosmicDark.current
    val today = remember { LocalDate.now() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val currentMonth = remember { YearMonth.now() }
    val boundedEarliest = remember(earliestMonth, currentMonth) {
        if (earliestMonth.isAfter(currentMonth)) currentMonth else earliestMonth
    }
    // Available months ordered from currentMonth down to boundedEarliest
    val availableMonths = remember(currentMonth, boundedEarliest) {
        val list = mutableListOf<YearMonth>()
        var m = currentMonth
        while (!m.isBefore(boundedEarliest)) {
            list.add(m)
            m = m.minusMonths(1)
        }
        if (list.isEmpty()) list.add(currentMonth)
        list
    }

    var viewingMonth by remember {
        mutableStateOf(selectedDate?.let { YearMonth.from(it) } ?: currentMonth)
    }

    LaunchedEffect(selectedDate) {
        if (selectedDate != null) {
            val m = YearMonth.from(selectedDate)
            if (m != viewingMonth) {
                viewingMonth = m
            }
        }
    }

    val year = viewingMonth.year
    val month = viewingMonth.monthValue
    val daysInMonth = remember(year, month) { viewingMonth.lengthOfMonth() }
    val days = remember(year, month, daysInMonth) {
        (1..daysInMonth).map { day -> viewingMonth.atDay(day) }
    }

    val initialIndex = remember(days, selectedDate, viewingMonth, today) {
        if (selectedDate != null) {
            days.indexOfFirst { it == selectedDate }.coerceAtLeast(0)
        } else if (viewingMonth == currentMonth) {
            days.indexOfFirst { it == today }.coerceAtLeast(0)
        } else {
            0
        }
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(listState)

    val itemWidth = 38.dp
    val cubeSize = 36.dp
    val itemWidthPx = with(density) { itemWidth.toPx() }

    // Side padding to allow first and last items to center
    var containerWidthPx by remember { mutableIntStateOf(0) }
    val sidePadding = with(density) {
        (containerWidthPx / 2f - itemWidthPx / 2f).coerceAtLeast(0f).toDp()
    }

    // Index currently nearest the visual center
    val centeredIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo
                .minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }
                ?.index ?: 0
        }
    }

    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    var hasUserScrolled by remember { mutableStateOf(false) }

    LaunchedEffect(isDragged) {
        if (isDragged) {
            hasUserScrolled = true
        }
    }

    // Emit date selection when user actively scrolls days and scroll settles
    LaunchedEffect(centeredIndex, listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && hasUserScrolled) {
            hasUserScrolled = false
            val date = days.getOrNull(centeredIndex) ?: return@LaunchedEffect
            if (date != selectedDate) {
                onDateSelected(date)
            }
        }
    }

    // Sync scroll position when selectedDate is updated externally
    LaunchedEffect(selectedDate, viewingMonth) {
        if (selectedDate != null && !listState.isScrollInProgress) {
            val targetIndex = days.indexOfFirst { it == selectedDate }
            if (targetIndex >= 0 && targetIndex != centeredIndex) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    // Month snap list state
    val initialMonthIndex = remember(availableMonths, viewingMonth) {
        availableMonths.indexOf(viewingMonth).coerceAtLeast(0)
    }
    val monthListState = rememberLazyListState(initialFirstVisibleItemIndex = initialMonthIndex)
    val monthSnapFling = rememberSnapFlingBehavior(monthListState)

    val centeredMonthIndex by remember {
        derivedStateOf {
            val info = monthListState.layoutInfo
            val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo
                .minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }
                ?.index ?: monthListState.firstVisibleItemIndex
        }
    }

    val isMonthDragged by monthListState.interactionSource.collectIsDraggedAsState()
    var monthUserScrolled by remember { mutableStateOf(false) }
    LaunchedEffect(isMonthDragged) {
        if (isMonthDragged) {
            monthUserScrolled = true
        }
    }

    // When month scroll settles, update viewingMonth
    LaunchedEffect(centeredMonthIndex, monthListState.isScrollInProgress) {
        if (!monthListState.isScrollInProgress && monthUserScrolled) {
            monthUserScrolled = false
            val newMonth = availableMonths.getOrNull(centeredMonthIndex) ?: return@LaunchedEffect
            if (newMonth != viewingMonth) {
                viewingMonth = newMonth
                if (selectedDate != null) {
                    val newDay = selectedDate.dayOfMonth.coerceAtMost(newMonth.lengthOfMonth())
                    onDateSelected(newMonth.atDay(newDay))
                }
            }
        }
    }

    // Sync monthListState when viewingMonth changes
    LaunchedEffect(viewingMonth) {
        val targetIdx = availableMonths.indexOf(viewingMonth)
        if (targetIdx >= 0 && targetIdx != centeredMonthIndex && !monthListState.isScrollInProgress) {
            monthListState.animateScrollToItem(targetIdx)
        }
    }

    val textMeasurer = rememberTextMeasurer()
    val monthTextWidth = remember(availableMonths, density) {
        val maxWidth = availableMonths.maxOfOrNull { ym ->
            val monthName = ym.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val layout = textMeasurer.measure(
                text = "$monthName ${ym.year}",
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    fontFamily = PlayfairDisplayFontFamily,
                    fontWeight = FontWeight.Bold
                )
            )
            with(density) { layout.size.width.toDp() }
        } ?: 58.dp
        maxWidth + 2.dp
    }
    val interComponentSpacing = 10.dp

    val fadeColor = if (dark) CosmicVoidBackground else Color(0xFFF4F5F8)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        // === Row 1: [Month Year (Left)] + [Horizontal Days Scroller (Center)] + ['All' Button (Right)] ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Left: Single-line "Sep 2026" — vertically scrollable, bounded, ZERO ripple
            Box(
                modifier = Modifier
                    .width(monthTextWidth)
                    .height(44.dp)
                    .testTag("date_scroller_header"),
                contentAlignment = Alignment.CenterStart
            ) {
                LazyColumn(
                    state = monthListState,
                    flingBehavior = monthSnapFling,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(availableMonths, key = { _, ym -> ym.toString() }) { index, ym ->
                        val monthName = ym.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null  // Zero ripple / shadow
                                ) {
                                    if (availableMonths.size > 1 && !monthListState.isScrollInProgress) {
                                        val nextIdx = (index + 1) % availableMonths.size
                                        scope.launch { monthListState.animateScrollToItem(nextIdx) }
                                    }
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "$monthName ${ym.year}",
                                fontSize = 14.sp,
                                fontFamily = PlayfairDisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (dark) TextOnDarkPrimary else TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(interComponentSpacing))

            // 2. Center: Centered Snap-Fling Horizontal Date Scroller with edge fade, ZERO ripple
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clipToBounds()
                    .onSizeChanged { containerWidthPx = it.width }
                    .testTag("insights_date_scroller_strip"),
                contentAlignment = Alignment.Center
            ) {
                // Background "cube" behind centered date — only shown when a date is selected
                if (selectedDate != null) {
                    Box(
                        modifier = Modifier
                            .size(cubeSize)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (dark) CosmicGlowBlue else CobaltBlue)
                    )
                }

                LazyRow(
                    state = listState,
                    flingBehavior = flingBehavior,
                    contentPadding = PaddingValues(horizontal = sidePadding),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(days, key = { _, date -> date.toString() }) { index, date ->
                        val distance = abs(index - centeredIndex)
                        val alpha = (1f - distance * 0.28f).coerceIn(0.18f, 1f)
                        val scale = (1f - distance * 0.1f).coerceIn(0.72f, 1f)
                        val isCentered = index == centeredIndex
                        val isSelected = isCentered && selectedDate != null
                        val hasActivity = date in datesWithActivity
                        val isToday = date == today

                        Box(
                            modifier = Modifier
                                .width(itemWidth)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null  // Zero ripple / shadow on date tap
                                ) {
                                    hasUserScrolled = false
                                    if (selectedDate == date) {
                                        onDateSelected(null) // Toggle off to "All"
                                    } else {
                                        onDateSelected(date)
                                        scope.launch { listState.animateScrollToItem(index) }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.graphicsLayer {
                                    this.alpha = alpha
                                    scaleX = scale
                                    scaleY = scale
                                }
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    fontSize = 17.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) {
                                        Color.White
                                    } else if (isToday) {
                                        if (dark) CosmicGlowBlue else CobaltBlue
                                    } else {
                                        if (dark) TextOnDarkPrimary else TextPrimary
                                    }
                                )

                                // Activity dot
                                if (hasActivity) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(3.5.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) Color.White
                                                else if (dark) CosmicGlowBlue.copy(alpha = 0.7f)
                                                else CobaltBlue.copy(alpha = 0.6f)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                // Left & Right subtle edge fade masks
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(16.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(fadeColor, Color.Transparent)
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(16.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, fadeColor)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.width(interComponentSpacing))

            // 3. Right: Dedicated 'All' Button — tactile squircle pill, ZERO elevation, ZERO ripple
            val isAllActive = selectedDate == null
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .widthIn(min = 44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isAllActive) {
                            if (dark) CosmicGlowBlue else CobaltBlue
                        } else {
                            if (dark) DarkSurfaceCard else Color(0xFFF1F5F9)
                        }
                    )
                    .then(
                        if (!isAllActive) {
                            Modifier.border(
                                1.dp,
                                if (dark) CosmicVoidCardBorder else Color(0xFFE2E8F0),
                                RoundedCornerShape(12.dp)
                            )
                        } else Modifier
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        hasUserScrolled = false
                        if (isAllActive) {
                            // Lead to normal where today will be selected
                            if (viewingMonth != currentMonth) {
                                viewingMonth = currentMonth
                            }
                            onDateSelected(today)
                            scope.launch {
                                val idx = (today.dayOfMonth - 1).coerceAtLeast(0)
                                listState.animateScrollToItem(idx)
                            }
                        } else {
                            onDateSelected(null)
                            // Smoothly re-center the scroller on today (if viewing current month) or day 1
                            scope.launch {
                                val targetDay = if (viewingMonth == currentMonth) today else viewingMonth.atDay(1)
                                val idx = days.indexOfFirst { it == targetDay }.coerceAtLeast(0)
                                listState.animateScrollToItem(idx)
                            }
                        }
                    }
                    .padding(horizontal = 10.dp)
                    .testTag("insights_all_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "All",
                    fontSize = 12.5.sp,
                    fontWeight = if (isAllActive) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isAllActive) Color.White else (if (dark) TextOnDarkSecondary else TextSecondary)
                )
            }
        }

    // === Row 2 (optional): Inline Theme Chips ===
        if (themes.isNotEmpty()) {
            val chipScroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(chipScroll)
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                themes.forEach { theme ->
                    val isSelected = selectedTheme == theme.key
                    Surface(
                        onClick = { onSelectTheme(if (isSelected) null else theme.key) },
                        shape = RoundedCornerShape(10.dp),
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp,
                        color = if (isSelected) {
                            if (dark) DarkSurfaceCard else CeramicWhite
                        } else {
                            if (dark) CosmicVoidCard.copy(alpha = 0.5f) else Color(0xFFF1F5F9)
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) (if (dark) CosmicGlowBlue else CobaltBlue)
                            else Color.Transparent
                        ),
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "\u2726 ${theme.title} (${theme.noteCount})",
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) (if (dark) CosmicGlowBlue else CobaltBlue)
                                else (if (dark) TextOnDarkSecondary else TextSecondary)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss theme ${theme.title}",
                                tint = (if (dark) TextOnDarkSecondary else TextMuted).copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onDismissTheme(theme.key) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactPill(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val dark = LocalIsCosmicDark.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        color = if (isActive) {
            if (dark) CosmicGlowBlue else CobaltBlue
        } else {
            Color.Transparent
        },
        interactionSource = remember { MutableInteractionSource() }
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) Color.White else if (dark) TextOnDarkSecondary else TextSecondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
