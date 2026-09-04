package space.iamjustkrishna.srutam.ui.mesh

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import space.iamjustkrishna.srutam.data.InsightEntity
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.ui.components.SquircleActionButton
import space.iamjustkrishna.srutam.ui.theme.*
import space.iamjustkrishna.srutam.viewmodel.ThemeCluster
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

@Composable
fun ConceptMeshCanvas(
    recordings: List<Recording>,
    insights: List<InsightEntity>,
    themes: List<ThemeCluster>,
    onRecordingClick: (Long) -> Unit,
    onActionToggle: (InsightEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // Build Deterministic Spatial Graph Data
    val graphData = remember(recordings, insights, themes) {
        ConceptMeshBuilder.buildGraph(recordings, insights, themes)
    }

    var zoom by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    var activeFilter by remember { mutableStateOf<MeshNodeKind?>(null) }

    // Physical spring-mass simulation state
    val physics = remember(graphData) { MeshPhysicsEngine(graphData) }
    var isPhysicsActive by remember { mutableStateOf(false) }
    var draggedNodeId by remember { mutableStateOf<String?>(null) }

    // Real-time animation loop for elastic physics relaxation
    LaunchedEffect(isPhysicsActive, graphData) {
        if (isPhysicsActive) {
            var lastFrameNanos = 0L
            while (isActive) {
                withFrameNanos { frameTimeNanos ->
                    if (lastFrameNanos == 0L) {
                        lastFrameNanos = frameTimeNanos
                        return@withFrameNanos
                    }
                    val dt = ((frameTimeNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0.005f, 0.033f)
                    lastFrameNanos = frameTimeNanos

                    val hasEnergy = physics.step(draggedNodeId, dt)
                    if (!hasEnergy && draggedNodeId == null) {
                        isPhysicsActive = false
                    }
                }
            }
        }
    }

    // Pulsing halo animation for selected node
    val infiniteTransition = rememberInfiniteTransition(label = "meshHaloPulse")
    val pulseFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseFraction"
    )

    val selectedNode = remember(selectedNodeId, graphData) {
        if (selectedNodeId != null) graphData.nodeMap[selectedNodeId] else null
    }

    val highlightedNodeIds = remember(selectedNodeId, graphData) {
        if (selectedNodeId != null) {
            graphData.getConnectedNodeIds(selectedNodeId!!)
        } else {
            emptySet()
        }
    }

    val connectedNodesList = remember(highlightedNodeIds, graphData) {
        highlightedNodeIds.mapNotNull { graphData.nodeMap[it] }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SlateSurface)
    ) {
        if (graphData.nodes.isEmpty()) {
            EmptyMeshPlaceholder(modifier = Modifier.align(Alignment.Center))
        } else {
            // Interactive Canvas for 2D Node Mesh with Drag Physics & Multi-Touch Gestures
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(graphData) {
                        awaitEachGesture {
                            val firstDown = awaitFirstDown(requireUnconsumed = false)
                            var hasMoved = false
                            var isMultiTouch = false
                            val touchSlop = viewConfiguration.touchSlop
                            val initialDownPosition = firstDown.position

                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val safeZ = if (zoom.isNaN() || zoom <= 0f) 1f else zoom.coerceIn(0.35f, 3.0f)
                            val safePan = if (panOffset.x.isNaN() || panOffset.y.isNaN()) Offset.Zero else panOffset

                            // Hit-test if a node was touched on pointer down
                            val touchedNode = graphData.nodes.minByOrNull { node ->
                                val nodePos = physics.positions[node.id] ?: Offset(node.x, node.y)
                                val screenX = nodePos.x * safeZ + safePan.x + cx
                                val screenY = nodePos.y * safeZ + safePan.y + cy
                                hypot(screenX - firstDown.position.x, screenY - firstDown.position.y)
                            }
                            val isNodeHit = if (touchedNode != null) {
                                val nodePos = physics.positions[touchedNode.id] ?: Offset(touchedNode.x, touchedNode.y)
                                val screenX = nodePos.x * safeZ + safePan.x + cx
                                val screenY = nodePos.y * safeZ + safePan.y + cy
                                val dist = hypot(screenX - firstDown.position.x, screenY - firstDown.position.y)
                                dist <= (touchedNode.radius * safeZ) + 36.dp.toPx()
                            } else false

                            var currentDraggedNodeId: String? = if (isNodeHit) touchedNode?.id else null
                            var isDraggingNode = isNodeHit

                            if (isDraggingNode) {
                                draggedNodeId = currentDraggedNodeId
                                isPhysicsActive = true
                            }

                            do {
                                val event = awaitPointerEvent()
                                val pressedChanges = event.changes.filter { it.pressed }
                                val pointerCount = pressedChanges.size

                                if (pointerCount >= 2) {
                                    isMultiTouch = true
                                    hasMoved = true
                                    isDraggingNode = false
                                    draggedNodeId = null
                                    currentDraggedNodeId = null

                                    val gestureZoom = event.calculateZoom()
                                    val gesturePan = event.calculatePan()

                                    if (!gestureZoom.isNaN() && !gestureZoom.isInfinite() && gestureZoom > 0.05f) {
                                        val newZoom = (zoom * gestureZoom).coerceIn(0.35f, 3.0f)
                                        if (!newZoom.isNaN()) {
                                            zoom = newZoom
                                        }
                                    }

                                    if (!gesturePan.x.isNaN() && !gesturePan.y.isNaN() &&
                                        !gesturePan.x.isInfinite() && !gesturePan.y.isInfinite()
                                    ) {
                                        panOffset += gesturePan
                                    }

                                    event.changes.forEach { it.consume() }
                                } else if (pointerCount == 1 && !isMultiTouch) {
                                    val change = pressedChanges.firstOrNull() ?: event.changes.first()
                                    val totalDrag = change.position - initialDownPosition

                                    if (!hasMoved && totalDrag.getDistance() > touchSlop) {
                                        hasMoved = true
                                    }

                                    if (hasMoved) {
                                        if (isDraggingNode && currentDraggedNodeId != null) {
                                            val worldX = (change.position.x - safePan.x - cx) / safeZ
                                            val worldY = (change.position.y - safePan.y - cy) / safeZ
                                            physics.updateDraggedNode(currentDraggedNodeId, Offset(worldX, worldY))
                                            isPhysicsActive = true
                                            change.consume()
                                        } else {
                                            val panDelta = change.position - change.previousPosition
                                            if (!panDelta.x.isNaN() && !panDelta.y.isNaN() &&
                                                !panDelta.x.isInfinite() && !panDelta.y.isInfinite()
                                            ) {
                                                panOffset += panDelta
                                            }
                                            change.consume()
                                        }
                                    }
                                }
                            } while (event.changes.any { it.pressed })

                            // On touch release
                            if (!hasMoved && !isMultiTouch) {
                                if (isNodeHit && touchedNode != null) {
                                    selectedNodeId = touchedNode.id
                                } else {
                                    selectedNodeId = null
                                }
                            }

                            if (draggedNodeId != null || currentDraggedNodeId != null) {
                                draggedNodeId = null
                                isDraggingNode = false
                                isPhysicsActive = true
                            }
                        }
                    }
            ) {
                try {
                    val safeZ = if (zoom.isNaN() || zoom <= 0f) 1f else zoom.coerceIn(0.35f, 3.0f)
                    val safePan = if (panOffset.x.isNaN() || panOffset.y.isNaN()) Offset.Zero else panOffset
                    val cx = size.width / 2f
                    val cy = size.height / 2f

                    // 1. Subtle Dot Matrix Grid Backdrop
                    drawDotMatrix(safePan, safeZ)

                    // 2. Render Flexible Wire Connections with Dynamic Spring Positions
                    graphData.edges.forEach { edge ->
                        val from = graphData.nodeMap[edge.fromId]
                        val to = graphData.nodeMap[edge.toId]
                        if (from != null && to != null) {
                            val pA = physics.positions[from.id] ?: Offset(from.x, from.y)
                            val pB = physics.positions[to.id] ?: Offset(to.x, to.y)
                            val p1 = Offset(pA.x * safeZ + safePan.x + cx, pA.y * safeZ + safePan.y + cy)
                            val p2 = Offset(pB.x * safeZ + safePan.x + cx, pB.y * safeZ + safePan.y + cy)

                            val isEdgeHighlighted = selectedNodeId != null &&
                                    (edge.fromId in highlightedNodeIds && edge.toId in highlightedNodeIds)
                            val isEdgeDimmed = selectedNodeId != null && !isEdgeHighlighted

                            val wireColor = when {
                                edge.relation == "SIMILAR_IDEA" -> Color(0xFFF59E0B) // Radiant Amber
                                edge.isSimilar -> Color(0xFF8B5CF6) // Royal Violet
                                else -> CobaltBlue
                            }

                            drawFlexibleWire(
                                p1 = p1,
                                p2 = p2,
                                isHighlighted = isEdgeHighlighted,
                                isDimmed = isEdgeDimmed,
                                isSimilar = edge.isSimilar,
                                edgeColor = wireColor,
                                zoom = safeZ
                            )
                        }
                    }

                    // 3. Render Nodes without Text Labels (Clean Jewels with Centered Glyphs)
                    graphData.nodes.forEach { node ->
                        val isFiltered = activeFilter != null && node.kind != activeFilter
                        val isSelected = node.id == selectedNodeId
                        val isConnected = selectedNodeId != null && node.id in highlightedNodeIds
                        val isDimmed = (selectedNodeId != null && !isConnected) || isFiltered

                        val pos = physics.positions[node.id] ?: Offset(node.x, node.y)
                        val p = Offset(pos.x * safeZ + safePan.x + cx, pos.y * safeZ + safePan.y + cy)
                        val r = (node.radius * safeZ).coerceIn(4f, 160f)

                        drawMeshNode(
                            center = p,
                            radius = r,
                            node = node,
                            isSelected = isSelected,
                            isConnected = isConnected,
                            isDimmed = isDimmed,
                            pulseFraction = pulseFraction,
                            zoom = safeZ
                        )
                    }
                } catch (e: Throwable) {
                    // Safe guard against unexpected coordinate bounds
                }
            }

            // Top Filter Chips Bar
            MeshFilterBar(
                activeFilter = activeFilter,
                onFilterSelected = { activeFilter = it },
                nodeCount = graphData.nodes.size,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )

            // Floating Navigation & Recenter Controls
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 64.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SquircleActionButton(
                    icon = Icons.Default.CenterFocusStrong,
                    contentDescription = "Recenter canvas",
                    onClick = {
                        zoom = 1.0f
                        panOffset = Offset.Zero
                        physics.reset()
                        isPhysicsActive = true
                    }
                )

                SquircleActionButton(
                    icon = Icons.Default.Add,
                    contentDescription = "Zoom In",
                    onClick = {
                        zoom = (zoom * 1.25f).coerceAtMost(2.6f)
                    }
                )

                SquircleActionButton(
                    icon = Icons.Default.Remove,
                    contentDescription = "Zoom Out",
                    onClick = {
                        zoom = (zoom / 1.25f).coerceAtLeast(0.4f)
                    }
                )
            }

            // Sleek Legend Pill (Floating at Bottom-Left)
            MeshLegendPill(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = if (selectedNode != null) 380.dp else 100.dp)
            )

            // Floating Frosted Glass Detail Card
            MeshNodeDetailCard(
                node = selectedNode,
                connectedNodes = connectedNodesList,
                onSelectNode = { nextNode ->
                    selectedNodeId = nextNode.id
                    val nextPos = physics.positions[nextNode.id] ?: Offset(nextNode.x, nextNode.y)
                    panOffset = Offset(-nextPos.x * zoom, -nextPos.y * zoom)
                },
                onRecordingClick = onRecordingClick,
                onActionToggle = onActionToggle,
                onDismiss = { selectedNodeId = null },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 98.dp)
            )
        }
    }
}

private fun DrawScope.drawDotMatrix(pan: Offset, zoom: Float) {
    val spacing = 36.dp.toPx()
    val dotRadius = 1.2.dp.toPx()
    val startX = (pan.x % spacing) - spacing
    val startY = (pan.y % spacing) - spacing

    var x = startX
    while (x < size.width + spacing) {
        var y = startY
        while (y < size.height + spacing) {
            drawCircle(
                color = SlateBorder.copy(alpha = 0.45f),
                radius = dotRadius,
                center = Offset(x, y)
            )
            y += spacing
        }
        x += spacing
    }
}

private fun DrawScope.drawFlexibleWire(
    p1: Offset,
    p2: Offset,
    isHighlighted: Boolean,
    isDimmed: Boolean,
    isSimilar: Boolean,
    edgeColor: Color,
    zoom: Float
) {
    if (p1.x.isNaN() || p1.y.isNaN() || p2.x.isNaN() || p2.y.isNaN()) return

    val dx = p2.x - p1.x
    val dy = p2.y - p1.y
    val dist = hypot(dx, dy)
    if (dist < 1f) return

    // Physics catenary gravity sag: longer wires have deeper organic drape
    val sagFactor = if (isSimilar) 0.25f else 0.18f
    val sagDepth = (dist * sagFactor).coerceIn(12f, 130f) * zoom.coerceIn(0.6f, 1.25f)

    // Cubic Bezier with natural physics sag
    val cp1 = Offset(p1.x + dx * 0.28f, p1.y + dy * 0.28f + sagDepth)
    val cp2 = Offset(p1.x + dx * 0.72f, p1.y + dy * 0.72f + sagDepth)

    val path = Path().apply {
        moveTo(p1.x, p1.y)
        cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p2.x, p2.y)
    }

    val widthScale = zoom.coerceIn(0.65f, 1.25f)
    val beadOuterRadius = (3.4.dp.toPx() * widthScale).coerceIn(2f, 6.5f)
    val beadInnerRadius = (1.5.dp.toPx() * widthScale).coerceIn(1f, 3.2f)

    when {
        isHighlighted -> {
            // Radiant Aura Halo (Outer Energy Glow)
            val auraColor = if (isSimilar) Color(0xFFF59E0B).copy(alpha = 0.38f) else edgeColor.copy(alpha = 0.35f)
            drawPath(
                path = path,
                color = auraColor,
                style = Stroke(width = 7.5.dp.toPx() * widthScale, cap = StrokeCap.Round)
            )

            // Flexible Cable Jacket / Sleeve
            val jacketColor = if (isSimilar) Color(0xFF8B5CF6) else CobaltBlue
            drawPath(
                path = path,
                color = jacketColor,
                style = Stroke(width = 3.8.dp.toPx() * widthScale, cap = StrokeCap.Round)
            )

            // Inner Conducting Core Beam
            val coreColor = if (isSimilar) Color(0xFFFEF3C7) else CeramicWhite
            drawPath(
                path = path,
                color = coreColor,
                style = Stroke(width = 1.6.dp.toPx() * widthScale, cap = StrokeCap.Round)
            )

            // Terminal Strain-Relief Connector Plug Beads at both ends
            drawCircle(color = jacketColor, radius = beadOuterRadius + 1.5f, center = p1)
            drawCircle(color = coreColor, radius = beadInnerRadius, center = p1)
            drawCircle(color = jacketColor, radius = beadOuterRadius + 1.5f, center = p2)
            drawCircle(color = coreColor, radius = beadInnerRadius, center = p2)
        }
        isDimmed -> {
            // Subtle, non-intrusive background cable
            drawPath(
                path = path,
                color = Color(0xFFE2E8F0).copy(alpha = 0.12f),
                style = Stroke(width = 1.2.dp.toPx() * widthScale, cap = StrokeCap.Round)
            )
        }
        else -> {
            // Standard Physical Flexible Wire
            val jacketColor = if (isSimilar) Color(0xFFD97706).copy(alpha = 0.8f) else Color(0xFF94A3B8).copy(alpha = 0.65f)
            val coreColor = if (isSimilar) Color(0xFFFDE68A).copy(alpha = 0.9f) else CeramicWhite.copy(alpha = 0.75f)

            // Outer Rubber/Braided Insulation Jacket
            drawPath(
                path = path,
                color = jacketColor,
                style = Stroke(width = (if (isSimilar) 3.4.dp else 2.6.dp).toPx() * widthScale, cap = StrokeCap.Round)
            )

            // Inner Wire Core
            drawPath(
                path = path,
                color = coreColor,
                style = Stroke(width = 1.1.dp.toPx() * widthScale, cap = StrokeCap.Round)
            )

            // Terminal Plug Beads
            drawCircle(color = jacketColor, radius = beadOuterRadius, center = p1)
            drawCircle(color = coreColor, radius = beadInnerRadius, center = p1)
            drawCircle(color = jacketColor, radius = beadOuterRadius, center = p2)
            drawCircle(color = coreColor, radius = beadInnerRadius, center = p2)
        }
    }
}

private fun DrawScope.drawMeshNode(
    center: Offset,
    radius: Float,
    node: MeshNode,
    isSelected: Boolean,
    isConnected: Boolean,
    isDimmed: Boolean,
    pulseFraction: Float,
    zoom: Float
) {
    if (center.x.isNaN() || center.y.isNaN() || radius.isNaN() || radius <= 0f) return

    val safeBorder = min(3.dp.toPx(), (radius * 0.32f).coerceAtLeast(1f))

    when {
        isDimmed -> {
            drawCircle(
                color = node.color.copy(alpha = 0.12f),
                radius = radius,
                center = center
            )
            drawCircle(
                color = SlateBorder.copy(alpha = 0.25f),
                radius = radius,
                center = center,
                style = Stroke(width = min(1.dp.toPx(), safeBorder))
            )
        }
        isSelected -> {
            // Animated concentric pulse halo
            val haloRadius = (radius * (1f + 0.65f * pulseFraction)).coerceAtLeast(radius + 1f)
            val haloAlpha = (1f - pulseFraction) * 0.45f
            drawCircle(
                color = node.color.copy(alpha = haloAlpha),
                radius = haloRadius,
                center = center
            )

            // Bright secondary aura
            drawCircle(
                color = node.color.copy(alpha = 0.3f),
                radius = radius + (8.dp.toPx() * zoom.coerceIn(0.6f, 1.2f)),
                center = center
            )

            // Solid node circle
            drawCircle(
                color = node.color,
                radius = radius,
                center = center
            )
            // Crisp high-contrast white border
            drawCircle(
                color = CeramicWhite,
                radius = radius,
                center = center,
                style = Stroke(width = safeBorder)
            )

            // Center node glyph
            drawNodeGlyph(center, radius, node.kind, Color.White)
        }
        isConnected -> {
            // Illuminated halo
            drawCircle(
                color = node.color.copy(alpha = 0.32f),
                radius = radius + (6.dp.toPx() * zoom.coerceIn(0.6f, 1.2f)),
                center = center
            )
            drawCircle(
                color = node.color,
                radius = radius,
                center = center
            )
            drawCircle(
                color = CeramicWhite.copy(alpha = 0.95f),
                radius = radius,
                center = center,
                style = Stroke(width = min(2.dp.toPx(), safeBorder))
            )

            drawNodeGlyph(center, radius, node.kind, Color.White)
        }
        else -> {
            // Normal state
            drawCircle(
                color = node.color.copy(alpha = 0.18f),
                radius = radius + (3.dp.toPx() * zoom.coerceIn(0.6f, 1.2f)),
                center = center
            )
            drawCircle(
                color = node.color,
                radius = radius,
                center = center
            )
            drawCircle(
                color = CeramicWhite,
                radius = radius,
                center = center,
                style = Stroke(width = min(1.5.dp.toPx(), safeBorder))
            )

            drawNodeGlyph(center, radius, node.kind, Color.White)
        }
    }
}

private fun DrawScope.drawNodeGlyph(
    center: Offset,
    radius: Float,
    kind: MeshNodeKind,
    color: Color
) {
    if (radius < 8.dp.toPx()) return // Avoid drawing micro glyphs on tiny nodes to preserve Skia stability
    val s = radius * 0.48f
    val strokeW = min(2.dp.toPx(), (s * 0.32f).coerceAtLeast(1f))

    when (kind) {
        MeshNodeKind.ACTION -> {
            // Crisp Checkmark
            val path = Path().apply {
                moveTo(center.x - s * 0.7f, center.y)
                lineTo(center.x - s * 0.1f, center.y + s * 0.6f)
                lineTo(center.x + s * 0.8f, center.y - s * 0.5f)
            }
            drawPath(path, color = color, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        MeshNodeKind.IDEA -> {
            // Lightbulb Glyph
            val bulbRadius = (s * 0.65f).coerceAtLeast(2f)
            drawCircle(color = color, radius = bulbRadius, center = Offset(center.x, center.y - s * 0.2f), style = Stroke(strokeW))
            drawLine(color = color, start = Offset(center.x - s * 0.35f, center.y + s * 0.5f), end = Offset(center.x + s * 0.35f, center.y + s * 0.5f), strokeWidth = strokeW)
        }
        MeshNodeKind.DECISION -> {
            // Verified Seal Check
            val sealRadius = (s * 0.85f).coerceAtLeast(2f)
            drawCircle(color = color, radius = sealRadius, center = center, style = Stroke(strokeW))
            val path = Path().apply {
                moveTo(center.x - s * 0.45f, center.y)
                lineTo(center.x - s * 0.05f, center.y + s * 0.4f)
                lineTo(center.x + s * 0.55f, center.y - s * 0.3f)
            }
            drawPath(path, color = color, style = Stroke(width = strokeW, cap = StrokeCap.Round))
        }
        MeshNodeKind.RECORDING -> {
            // Audio Waveform Glyph
            drawLine(color = color, start = Offset(center.x - s * 0.7f, center.y - s * 0.4f), end = Offset(center.x - s * 0.7f, center.y + s * 0.4f), strokeWidth = strokeW, cap = StrokeCap.Round)
            drawLine(color = color, start = Offset(center.x - s * 0.22f, center.y - s * 0.85f), end = Offset(center.x - s * 0.22f, center.y + s * 0.85f), strokeWidth = strokeW, cap = StrokeCap.Round)
            drawLine(color = color, start = Offset(center.x + s * 0.22f, center.y - s * 0.65f), end = Offset(center.x + s * 0.22f, center.y + s * 0.65f), strokeWidth = strokeW, cap = StrokeCap.Round)
            drawLine(color = color, start = Offset(center.x + s * 0.7f, center.y - s * 0.3f), end = Offset(center.x + s * 0.7f, center.y + s * 0.3f), strokeWidth = strokeW, cap = StrokeCap.Round)
        }
        MeshNodeKind.THEME -> {
            // Constellation / Hub Star
            drawCircle(color = color, radius = (s * 0.35f).coerceAtLeast(2f), center = center)
            drawLine(color = color, start = Offset(center.x - s * 0.8f, center.y), end = Offset(center.x + s * 0.8f, center.y), strokeWidth = strokeW, cap = StrokeCap.Round)
            drawLine(color = color, start = Offset(center.x, center.y - s * 0.8f), end = Offset(center.x, center.y + s * 0.8f), strokeWidth = strokeW, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun MeshFilterBar(
    activeFilter: MeshNodeKind?,
    onFilterSelected: (MeshNodeKind?) -> Unit,
    nodeCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CeramicWhite.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, SlateBorder.copy(alpha = 0.8f)),
        shadowElevation = 6.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChipItem(
                label = "All ($nodeCount)",
                isSelected = activeFilter == null,
                color = CobaltBlue,
                onClick = { onFilterSelected(null) }
            )
            FilterChipItem(
                label = "Next Steps",
                isSelected = activeFilter == MeshNodeKind.ACTION,
                color = CobaltBlue,
                onClick = { onFilterSelected(if (activeFilter == MeshNodeKind.ACTION) null else MeshNodeKind.ACTION) }
            )
            FilterChipItem(
                label = "Ideas",
                isSelected = activeFilter == MeshNodeKind.IDEA,
                color = Color(0xFFF59E0B),
                onClick = { onFilterSelected(if (activeFilter == MeshNodeKind.IDEA) null else MeshNodeKind.IDEA) }
            )
            FilterChipItem(
                label = "Decisions",
                isSelected = activeFilter == MeshNodeKind.DECISION,
                color = Color(0xFF10B981),
                onClick = { onFilterSelected(if (activeFilter == MeshNodeKind.DECISION) null else MeshNodeKind.DECISION) }
            )
            FilterChipItem(
                label = "Notes",
                isSelected = activeFilter == MeshNodeKind.RECORDING,
                color = Color(0xFF334155),
                onClick = { onFilterSelected(if (activeFilter == MeshNodeKind.RECORDING) null else MeshNodeKind.RECORDING) }
            )
            FilterChipItem(
                label = "Themes",
                isSelected = activeFilter == MeshNodeKind.THEME,
                color = Color(0xFF8B5CF6),
                onClick = { onFilterSelected(if (activeFilter == MeshNodeKind.THEME) null else MeshNodeKind.THEME) }
            )
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.12f) else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, color.copy(alpha = 0.4f)) else null,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) color else color.copy(alpha = 0.5f))
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) TextPrimary else TextSecondary
            )
        }
    }
}

@Composable
private fun MeshLegendPill(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CeramicWhite.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, SlateBorder.copy(alpha = 0.7f)),
        shadowElevation = 4.dp,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            LegendItem(color = CobaltBlue, label = "Action")
            LegendItem(color = Color(0xFFF59E0B), label = "Idea")
            LegendItem(color = Color(0xFF10B981), label = "Decision")
            LegendItem(color = Color(0xFF8B5CF6), label = "Theme")
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun EmptyMeshPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(CobaltContainer)
        ) {
            Icon(
                imageVector = Icons.Default.Hub,
                contentDescription = null,
                tint = CobaltBlue,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Concept Nodes Yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Record voice notes to see your thoughts, decisions, and tasks connect into an interactive concept mesh.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            lineHeight = 18.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
