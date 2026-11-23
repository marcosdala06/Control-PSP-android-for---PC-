package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PadProtocol
import com.example.model.ConnectionState
import com.example.model.LrMode
import com.example.model.PadButton
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.DeckBezel
import com.example.ui.theme.DeckObsidian
import com.example.ui.theme.DeckSurface
import com.example.ui.theme.DeckSurfaceElevated
import com.example.ui.theme.DeckWell
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.PsCircleRed
import com.example.ui.theme.PsCrossBlue
import com.example.ui.theme.PsSquarePink
import com.example.ui.theme.PsTriangleGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Multi-touch safe hold modifier: tracks all active pointer IDs that touch this control
 * and only releases when every finger on this control lifts.
 */
@Composable
fun Modifier.multiTouchHold(
    key: Any,
    onPressChanged: (Boolean) -> Unit,
): Modifier {
    val currentCallback by rememberUpdatedState(onPressChanged)
    DisposableEffect(key) {
        onDispose { currentCallback(false) }
    }
    return this.pointerInput(key) {
        awaitPointerEventScope {
            val activeIds = mutableSetOf<PointerId>()
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                var changed = false
                for (change in event.changes) {
                    if (change.changedToDownIgnoreConsumed()) {
                        if (activeIds.add(change.id)) changed = true
                        change.consume()
                    } else if (change.changedToUpIgnoreConsumed() || !change.pressed) {
                        if (activeIds.remove(change.id)) changed = true
                        change.consume()
                    }
                }
                if (changed) {
                    currentCallback(activeIds.isNotEmpty())
                }
            }
        }
    }
}

@Composable
fun ShoulderTriggerButton(
    button: PadButton,
    subtitle: String,
    isPressed: Boolean,
    isLocked: Boolean = false,
    accentColor: Color = CyanPulse,
    contentDesc: String,
    testTag: String,
    onPressChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = 700f),
        label = "shoulder_scale",
    )
    val shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = 28.dp,
        bottomEnd = 28.dp,
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isLocked -> EmeraldLive
            isPressed -> accentColor
            else -> DeckBezel
        },
        label = "shoulder_border",
    )
    val bgBrush = if (isPressed || isLocked) {
        Brush.verticalGradient(
            listOf(
                (if (isLocked) EmeraldLive else accentColor).copy(alpha = 0.34f),
                DeckSurfaceElevated,
            ),
        )
    } else {
        Brush.verticalGradient(
            listOf(
                DeckSurfaceElevated,
                DeckSurface,
            ),
        )
    }

    Box(
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minWidth = 110.dp, minHeight = 56.dp)
            .clip(shape)
            .background(bgBrush, shape)
            .border(width = if (isPressed || isLocked) 2.dp else 1.2.dp, color = borderColor, shape = shape)
            .testTag(testTag)
            .semantics {
                contentDescription = contentDesc
                role = Role.Button
            }
            .multiTouchHold(button, onPressChanged)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = button.label,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isPressed || isLocked) TextPrimary else TextSecondary,
                ),
            )
            Column {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isLocked) EmeraldLive else if (isPressed) accentColor else TextMuted,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                )
                if (isLocked) {
                    Text(
                        text = "TRAVADO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = EmeraldLive,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
fun ConsolePillButton(
    button: PadButton,
    isPressed: Boolean,
    contentDesc: String,
    testTag: String,
    onPressChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(stiffness = 700f),
        label = "pill_scale",
    )
    val shape = RoundedCornerShape(50)
    val bgColor by animateColorAsState(
        targetValue = if (isPressed) CyanPulse.copy(alpha = 0.25f) else DeckSurfaceElevated,
        label = "pill_bg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isPressed) CyanPulse else DeckBezel,
        label = "pill_border",
    )

    Box(
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minWidth = 68.dp, minHeight = 48.dp)
            .clip(shape)
            .background(bgColor, shape)
            .border(1.2.dp, borderColor, shape)
            .testTag(testTag)
            .semantics {
                contentDescription = contentDesc
                role = Role.Button
            }
            .multiTouchHold(button, onPressChanged)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = button.label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = if (isPressed) TextPrimary else TextSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
            ),
            maxLines = 1,
        )
    }
}

@Composable
fun DpadControl(
    dpadX: Int,
    dpadY: Int,
    onDpadChanged: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 152.dp,
) {
    val currentCallback by rememberUpdatedState(onDpadChanged)
    DisposableEffect(Unit) {
        onDispose { currentCallback(0, 0) }
    }

    val dpadDesc = stringResource(R.string.cd_dpad)

    Box(
        modifier = modifier
            .size(size)
            .testTag("dpad_control")
            .semantics { contentDescription = dpadDesc }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    var activeId: PointerId? = null
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        for (change in event.changes) {
                            if (activeId == null && change.changedToDownIgnoreConsumed()) {
                                activeId = change.id
                                change.consume()
                                val halfW = this.size.width / 2f
                                val halfH = this.size.height / 2f
                                val (nx, ny) = PadProtocol.computeDpadAxes(
                                    change.position.x - halfW,
                                    change.position.y - halfH,
                                    halfW,
                                )
                                currentCallback(nx, ny)
                            } else if (change.id == activeId) {
                                change.consume()
                                if (change.changedToUpIgnoreConsumed() || !change.pressed) {
                                    activeId = null
                                    currentCallback(0, 0)
                                } else {
                                    val halfW = this.size.width / 2f
                                    val halfH = this.size.height / 2f
                                    val (nx, ny) = PadProtocol.computeDpadAxes(
                                        change.position.x - halfW,
                                        change.position.y - halfH,
                                        halfW,
                                    )
                                    currentCallback(nx, ny)
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val armThickness = w * 0.34f
            val armPad = (w - armThickness) / 2f
            val r = CornerRadius(w * 0.08f, w * 0.08f)

            // Subtle outer recessed disc
            drawCircle(
                color = DeckWell.copy(alpha = 0.7f),
                radius = w * 0.49f,
            )
            drawCircle(
                color = DeckBezel.copy(alpha = 0.55f),
                radius = w * 0.49f,
                style = Stroke(width = 2.dp.toPx()),
            )

            // Cross base
            drawRoundRect(
                color = DeckSurfaceElevated,
                topLeft = Offset(armPad, 4.dp.toPx()),
                size = Size(armThickness, h - 8.dp.toPx()),
                cornerRadius = r,
            )
            drawRoundRect(
                color = DeckSurfaceElevated,
                topLeft = Offset(4.dp.toPx(), armPad),
                size = Size(w - 8.dp.toPx(), armThickness),
                cornerRadius = r,
            )

            // Active arm highlights
            val highlight = CyanPulse.copy(alpha = 0.38f)
            if (dpadY < 0) {
                drawRoundRect(
                    color = highlight,
                    topLeft = Offset(armPad, 4.dp.toPx()),
                    size = Size(armThickness, armPad + armThickness * 0.15f),
                    cornerRadius = r,
                )
            }
            if (dpadY > 0) {
                drawRoundRect(
                    color = highlight,
                    topLeft = Offset(armPad, armPad + armThickness * 0.85f),
                    size = Size(armThickness, armPad - 4.dp.toPx() + armThickness * 0.15f),
                    cornerRadius = r,
                )
            }
            if (dpadX < 0) {
                drawRoundRect(
                    color = highlight,
                    topLeft = Offset(4.dp.toPx(), armPad),
                    size = Size(armPad + armThickness * 0.15f, armThickness),
                    cornerRadius = r,
                )
            }
            if (dpadX > 0) {
                drawRoundRect(
                    color = highlight,
                    topLeft = Offset(armPad + armThickness * 0.85f, armPad),
                    size = Size(armPad - 4.dp.toPx() + armThickness * 0.15f, armThickness),
                    cornerRadius = r,
                )
            }

            // Directional chevrons
            fun drawChevron(cx: Float, cy: Float, dx: Float, dy: Float, active: Boolean) {
                val color = if (active) CyanPulse else TextMuted
                val s = w * 0.065f
                val path = Path().apply {
                    when {
                        dy < 0 -> {
                            moveTo(cx, cy - s)
                            lineTo(cx - s, cy + s * 0.6f)
                            lineTo(cx + s, cy + s * 0.6f)
                            close()
                        }
                        dy > 0 -> {
                            moveTo(cx, cy + s)
                            lineTo(cx - s, cy - s * 0.6f)
                            lineTo(cx + s, cy - s * 0.6f)
                            close()
                        }
                        dx < 0 -> {
                            moveTo(cx - s, cy)
                            lineTo(cx + s * 0.6f, cy - s)
                            lineTo(cx + s * 0.6f, cy + s)
                            close()
                        }
                        else -> {
                            moveTo(cx + s, cy)
                            lineTo(cx - s * 0.6f, cy - s)
                            lineTo(cx - s * 0.6f, cy + s)
                            close()
                        }
                    }
                }
                drawPath(path, color = color)
            }

            drawChevron(w / 2f, w * 0.16f, 0f, -1f, dpadY < 0)
            drawChevron(w / 2f, h - w * 0.16f, 0f, 1f, dpadY > 0)
            drawChevron(w * 0.16f, h / 2f, -1f, 0f, dpadX < 0)
            drawChevron(w - w * 0.16f, h / 2f, 1f, 0f, dpadX > 0)

            // Center pivot dimple
            drawCircle(
                color = DeckWell,
                radius = w * 0.075f,
            )
        }
    }
}

@Composable
fun AnalogStickControl(
    stickX: Float,
    stickY: Float,
    deadzone: Float,
    onStickChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 132.dp,
) {
    val currentCallback by rememberUpdatedState(onStickChanged)
    val currentDeadzone by rememberUpdatedState(deadzone)
    DisposableEffect(Unit) {
        onDispose { currentCallback(0f, 0f) }
    }

    val analogDesc = stringResource(R.string.cd_analog)
    val density = LocalDensity.current
    val maxTravelPx = with(density) { (size * 0.27f).toPx() }
    val active = stickX != 0f || stickY != 0f

    Box(
        modifier = modifier
            .size(size)
            .testTag("analog_stick")
            .semantics { contentDescription = analogDesc }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    var activeId: PointerId? = null
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        for (change in event.changes) {
                            if (activeId == null && change.changedToDownIgnoreConsumed()) {
                                activeId = change.id
                                change.consume()
                                val cx = this.size.width / 2f
                                val cy = this.size.height / 2f
                                val (nx, ny) = PadProtocol.normalizeStick(
                                    change.position.x - cx,
                                    change.position.y - cy,
                                    maxTravelPx,
                                    currentDeadzone,
                                )
                                currentCallback(nx, ny)
                            } else if (change.id == activeId) {
                                change.consume()
                                if (change.changedToUpIgnoreConsumed() || !change.pressed) {
                                    activeId = null
                                    currentCallback(0f, 0f)
                                } else {
                                    val cx = this.size.width / 2f
                                    val cy = this.size.height / 2f
                                    val (nx, ny) = PadProtocol.normalizeStick(
                                        change.position.x - cx,
                                        change.position.y - cy,
                                        maxTravelPx,
                                        currentDeadzone,
                                    )
                                    currentCallback(nx, ny)
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Outer recessed well
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(DeckWell, Color(0xFF0E131D)),
                ),
                radius = r,
            )
            drawCircle(
                color = if (active) CyanPulse.copy(alpha = 0.7f) else DeckBezel,
                radius = r - 1.5.dp.toPx(),
                style = Stroke(width = if (active) 2.dp.toPx() else 1.5.dp.toPx()),
            )
            // Subtle directional gate ticks
            drawCircle(
                color = DeckBezel.copy(alpha = 0.35f),
                radius = r * 0.56f,
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        // Floating thumb nub
        val nubSize = size * 0.52f
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (stickX * maxTravelPx).roundToInt(),
                        y = (stickY * maxTravelPx).roundToInt(),
                    )
                }
                .size(nubSize)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (active) Color(0xFF475569) else Color(0xFF334155),
                            DeckSurface,
                        ),
                    ),
                )
                .border(
                    width = 1.5.dp,
                    color = if (active) CyanPulse else DeckBezel,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            // PSP micro-grip concentric ring
            Box(
                modifier = Modifier
                    .size(nubSize * 0.56f)
                    .border(1.dp, TextMuted.copy(alpha = 0.45f), CircleShape),
            )
        }
    }
}

@Composable
fun FaceButtonsDiamond(
    pressedButtons: Set<PadButton>,
    onButtonChanged: (PadButton, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 188.dp,
) {
    val currentCallback by rememberUpdatedState(onButtonChanged)
    DisposableEffect(Unit) {
        onDispose {
            currentCallback(PadButton.TRIANGLE, false)
            currentCallback(PadButton.CIRCLE, false)
            currentCallback(PadButton.CROSS, false)
            currentCallback(PadButton.SQUARE, false)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .size(size)
            .testTag("face_buttons_diamond"),
    ) {
        val density = LocalDensity.current
        val boxPx = with(density) { maxWidth.toPx() }
        val btnSizeDp = (maxWidth * 0.34f).coerceAtLeast(52.dp)
        val btnRadiusPx = with(density) { (btnSizeDp / 2f).toPx() }

        val centers = listOf(
            PadButton.TRIANGLE to Offset(boxPx * 0.5f, btnRadiusPx + 2f),
            PadButton.CROSS to Offset(boxPx * 0.5f, boxPx - btnRadiusPx - 2f),
            PadButton.SQUARE to Offset(btnRadiusPx + 2f, boxPx * 0.5f),
            PadButton.CIRCLE to Offset(boxPx - btnRadiusPx - 2f, boxPx * 0.5f),
        )

        fun resolveButton(pos: Offset): PadButton? {
            var bestBtn: PadButton? = null
            var bestDist = Float.MAX_VALUE
            val maxHitRadius = btnRadiusPx * 1.42f
            for ((btn, center) in centers) {
                val dist = hypot(pos.x - center.x, pos.y - center.y)
                if (dist <= maxHitRadius && dist < bestDist) {
                    bestDist = dist
                    bestBtn = btn
                }
            }
            return bestBtn
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(boxPx) {
                    awaitPointerEventScope {
                        val pointerMap = mutableMapOf<PointerId, PadButton>()
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val beforePressed = pointerMap.values.toSet()

                            for (change in event.changes) {
                                if (change.changedToDownIgnoreConsumed()) {
                                    change.consume()
                                    val hit = resolveButton(change.position)
                                    if (hit != null) {
                                        pointerMap[change.id] = hit
                                    }
                                } else if (change.changedToUpIgnoreConsumed() || !change.pressed) {
                                    change.consume()
                                    pointerMap.remove(change.id)
                                } else if (change.id in pointerMap) {
                                    change.consume()
                                    val hit = resolveButton(change.position)
                                    if (hit != null) {
                                        pointerMap[change.id] = hit
                                    } else {
                                        pointerMap.remove(change.id)
                                    }
                                }
                            }

                            val afterPressed = pointerMap.values.toSet()
                            for (btn in listOf(
                                PadButton.TRIANGLE,
                                PadButton.CIRCLE,
                                PadButton.CROSS,
                                PadButton.SQUARE,
                            )) {
                                val was = btn in beforePressed
                                val now = btn in afterPressed
                                if (was != now) {
                                    currentCallback(btn, now)
                                }
                            }
                        }
                    }
                },
        ) {
            FaceButtonCap(
                button = PadButton.TRIANGLE,
                accentColor = PsTriangleGreen,
                isPressed = PadButton.TRIANGLE in pressedButtons,
                contentDesc = stringResource(R.string.cd_btn_triangle),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(btnSizeDp)
                    .testTag("btn_triangle"),
            )
            FaceButtonCap(
                button = PadButton.CROSS,
                accentColor = PsCrossBlue,
                isPressed = PadButton.CROSS in pressedButtons,
                contentDesc = stringResource(R.string.cd_btn_cross),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(btnSizeDp)
                    .testTag("btn_cross"),
            )
            FaceButtonCap(
                button = PadButton.SQUARE,
                accentColor = PsSquarePink,
                isPressed = PadButton.SQUARE in pressedButtons,
                contentDesc = stringResource(R.string.cd_btn_square),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(btnSizeDp)
                    .testTag("btn_square"),
            )
            FaceButtonCap(
                button = PadButton.CIRCLE,
                accentColor = PsCircleRed,
                isPressed = PadButton.CIRCLE in pressedButtons,
                contentDesc = stringResource(R.string.cd_btn_circle),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(btnSizeDp)
                    .testTag("btn_circle"),
            )
        }
    }
}

@Composable
private fun FaceButtonCap(
    button: PadButton,
    accentColor: Color,
    isPressed: Boolean,
    contentDesc: String,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(stiffness = 750f),
        label = "face_scale",
    )
    Box(
        modifier = modifier
            .scale(scale)
            .semantics {
                contentDescription = contentDesc
                role = Role.Button
            }
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = if (isPressed) {
                        listOf(accentColor.copy(alpha = 0.45f), DeckSurfaceElevated)
                    } else {
                        listOf(DeckSurfaceElevated, DeckSurface)
                    },
                ),
            )
            .border(
                width = if (isPressed) 2.2.dp else 1.4.dp,
                color = if (isPressed) accentColor else DeckBezel,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize(0.46f)) {
            val stroke = Stroke(
                width = 2.6.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )
            val w = size.width
            val h = size.height
            when (button) {
                PadButton.TRIANGLE -> {
                    val path = Path().apply {
                        moveTo(w / 2f, h * 0.08f)
                        lineTo(w * 0.92f, h * 0.88f)
                        lineTo(w * 0.08f, h * 0.88f)
                        close()
                    }
                    drawPath(path, color = accentColor, style = stroke)
                }
                PadButton.CIRCLE -> {
                    drawCircle(
                        color = accentColor,
                        radius = w * 0.44f,
                        style = stroke,
                    )
                }
                PadButton.CROSS -> {
                    drawLine(
                        color = accentColor,
                        start = Offset(w * 0.14f, h * 0.14f),
                        end = Offset(w * 0.86f, h * 0.86f),
                        strokeWidth = stroke.width,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = accentColor,
                        start = Offset(w * 0.86f, h * 0.14f),
                        end = Offset(w * 0.14f, h * 0.86f),
                        strokeWidth = stroke.width,
                        cap = StrokeCap.Round,
                    )
                }
                PadButton.SQUARE -> {
                    drawRect(
                        color = accentColor,
                        topLeft = Offset(w * 0.12f, h * 0.12f),
                        size = Size(w * 0.76f, h * 0.76f),
                        style = stroke,
                    )
                }
                else -> Unit
            }
        }
    }
}

@Composable
fun ThumbAssistPill(
    button: PadButton,
    subtitle: String,
    isPressed: Boolean,
    accentColor: Color,
    onPressChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(stiffness = 700f),
        label = "assist_scale",
    )
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minWidth = 84.dp, minHeight = 48.dp)
            .clip(shape)
            .background(
                if (isPressed) accentColor.copy(alpha = 0.28f) else DeckSurfaceElevated.copy(alpha = 0.92f),
                shape,
            )
            .border(
                width = if (isPressed) 1.8.dp else 1.dp,
                color = if (isPressed) accentColor else DeckBezel,
                shape = shape,
            )
            .multiTouchHold("assist_${button.wireKey}", onPressChanged)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = button.label,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = if (isPressed) accentColor else TextPrimary,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isPressed) TextPrimary else TextSecondary,
                ),
            )
        }
    }
}

@Composable
fun CenterHudCluster(
    connectionState: ConnectionState,
    latencyMs: Long?,
    lastAck: String,
    lrMode: LrMode,
    sprintLocked: Boolean,
    showSprintLock: Boolean,
    landscapeLocked: Boolean,
    onOpenConnection: () -> Unit,
    onCycleLrMode: () -> Unit,
    onToggleSprintLock: () -> Unit,
    onToggleLandscapeLock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (statusDotColor, statusText) = when (connectionState) {
        is ConnectionState.Connected -> {
            val ms = latencyMs?.let { " • ${it}ms" }.orEmpty()
            EmeraldLive to "Ligado$ms"
        }
        is ConnectionState.Connecting -> AmberWarn to "A ligar…"
        is ConnectionState.Reconnecting -> AmberWarn to "A religar (${connectionState.attempt})…"
        is ConnectionState.Error -> CrimsonAlert to "Erro"
        ConnectionState.Disconnected -> TextMuted to "Desligado"
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Top telemetry pill + quick action buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                onClick = onOpenConnection,
                shape = RoundedCornerShape(50),
                color = DeckSurfaceElevated,
                border = BorderStroke(1.dp, statusDotColor.copy(alpha = 0.6f)),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("hud_connection_pill"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = if (connectionState is ConnectionState.Connected) {
                            Icons.Rounded.Wifi
                        } else {
                            Icons.Rounded.WifiOff
                        },
                        contentDescription = null,
                        tint = statusDotColor,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelLarge.copy(color = TextPrimary),
                        maxLines = 1,
                    )
                }
            }

            Surface(
                onClick = onToggleLandscapeLock,
                shape = CircleShape,
                color = DeckSurfaceElevated,
                border = BorderStroke(1.dp, DeckBezel),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(48.dp)
                    .testTag("hud_fullscreen_button"),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (landscapeLocked) {
                            Icons.Rounded.FullscreenExit
                        } else {
                            Icons.Rounded.Fullscreen
                        },
                        contentDescription = stringResource(R.string.cd_fullscreen),
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Surface(
                onClick = onOpenConnection,
                shape = CircleShape,
                color = DeckSurfaceElevated,
                border = BorderStroke(1.dp, DeckBezel),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(48.dp)
                    .testTag("hud_settings_button"),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.cd_settings),
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // L/R Mode Cycler + Sprint-Lock row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                onClick = onCycleLrMode,
                shape = RoundedCornerShape(14.dp),
                color = DeckSurfaceElevated,
                border = BorderStroke(1.dp, CyanPulse.copy(alpha = 0.45f)),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("hud_lr_mode_chip"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SwapHoriz,
                        contentDescription = null,
                        tint = CyanPulse,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "L/R: ${lrMode.shortLabel}",
                        style = MaterialTheme.typography.labelMedium.copy(color = CyanPulse),
                        maxLines = 1,
                    )
                }
            }

            if (showSprintLock) {
                Surface(
                    onClick = onToggleSprintLock,
                    shape = RoundedCornerShape(14.dp),
                    color = if (sprintLocked) EmeraldLive.copy(alpha = 0.22f) else DeckSurfaceElevated,
                    border = BorderStroke(
                        1.dp,
                        if (sprintLocked) EmeraldLive else DeckBezel,
                    ),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("hud_sprint_lock_chip"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = if (sprintLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                            contentDescription = null,
                            tint = if (sprintLocked) EmeraldLive else TextSecondary,
                            modifier = Modifier.size(15.dp),
                        )
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = if (sprintLocked) EmeraldLive else TextSecondary,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = if (sprintLocked) "Correr: ON" else "Travar R",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (sprintLocked) EmeraldLive else TextSecondary,
                            ),
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        // Live PC Ack Readout Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DeckWell.copy(alpha = 0.85f))
                .border(1.dp, DeckBezel.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.SportsEsports,
                    contentDescription = null,
                    tint = CyanPulse.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = lastAck,
                    style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
