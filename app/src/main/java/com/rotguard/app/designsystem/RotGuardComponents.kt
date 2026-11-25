package com.rotguard.app.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val SQUARE_CORNER_DP = 6
private const val PLAQUE_CORNER_DP = 4
private const val CARD_CORNER_DP = 16
private const val CHECKBOX_OUTER_DP = 18
private const val CHECKBOX_INNER_DP = 10
private const val RESERVOIR_HEIGHT_DP = 60
private const val RESERVOIR_FILL_ALPHA = 0.28f
private const val HOLD_PRESSED_SCALE = 0.985f
private const val HOLD_SPRING_STIFFNESS = 520f
private const val NAV_SIDE_MARGIN_DP = 24
private const val NAV_BOTTOM_MARGIN_DP = 12
private const val NAV_BAR_HEIGHT_DP = 64
private const val NAV_PILL_INSET_DP = 8
private const val NAV_SHADOW_DP = 10
private const val NAV_FADE_HEIGHT_DP = 56
private const val NAV_FADE_SOLID_FRACTION = 0.6f
private const val NAV_CONTENT_GAP_DP = 20
private const val NAV_LABEL_SP = 13.5f
private const val NAV_LABEL_MIN_SP = 10f
private const val NAV_LABEL_SHRINK_STEP = 0.92f

@Composable
fun VerdantCanvasBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VerdantTheme.colors.canvas),
        content = content,
    )
}

@Composable
fun BotanicalCard(
    modifier: Modifier = Modifier,
    warm: Boolean = false,
    borderColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = VerdantTheme.colors
    val fill = if (warm) palette.warmCard else palette.card
    val stroke = borderColor ?: if (warm) palette.warmBorder else palette.cardBorder
    val shape = RoundedCornerShape(size = CARD_CORNER_DP.dp)

    if (onClick != null) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = fill,
            border = BorderStroke(width = 1.dp, color = stroke),
            onClick = onClick,
        ) {
            Column(
                modifier = Modifier.padding(all = 18.dp),
                verticalArrangement = Arrangement.spacedBy(space = 10.dp),
                content = content,
            )
        }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = fill,
            border = BorderStroke(width = 1.dp, color = stroke),
        ) {
            Column(
                modifier = Modifier.padding(all = 18.dp),
                verticalArrangement = Arrangement.spacedBy(space = 10.dp),
                content = content,
            )
        }
    }
}

@Composable
fun SquarePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    useMossAccent: Boolean = false,
    fillOverride: Color? = null,
    inkOverride: Color? = null,
) {
    val palette = VerdantTheme.colors
    val shape = RoundedCornerShape(size = SQUARE_CORNER_DP.dp)
    val fillColor = fillOverride ?: if (useMossAccent) palette.mossBtnFill else palette.primaryCtaFill
    val textColor = inkOverride ?: if (useMossAccent) palette.mossBtnInk else palette.primaryCtaInk
    val stroke = if (enabled && useMossAccent && fillOverride == null) {
        BorderStroke(width = 1.dp, color = palette.mossBtnStroke)
    } else {
        null
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height = 52.dp),
        shape = shape,
        color = if (enabled) fillColor else palette.sageTint,
        border = stroke,
        onClick = { if (enabled) onClick() },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (enabled) textColor else palette.inkMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun SquareOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color? = null,
    textColor: Color? = null,
    fillColor: Color? = null,
) {
    val palette = VerdantTheme.colors
    val resolvedBorder = borderColor ?: palette.ruleStrong
    val resolvedText = textColor ?: palette.ink
    val resolvedFill = fillColor ?: palette.card

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height = 48.dp),
        shape = RoundedCornerShape(size = SQUARE_CORNER_DP.dp),
        color = resolvedFill,
        border = BorderStroke(width = 1.2.dp, color = resolvedBorder),
        onClick = { if (enabled) onClick() },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) resolvedText else palette.inkMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun SquareSegmentButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedFill: Color? = null,
    selectedInk: Color? = null,
) {
    val palette = VerdantTheme.colors
    val fill = if (selected) (selectedFill ?: palette.mossAccent) else palette.card
    val stroke = if (selected) (selectedFill ?: palette.mossBtnStroke) else palette.cardBorder
    val ink = if (selected) (selectedInk ?: palette.mossBtnInk) else palette.inkMuted

    Surface(
        modifier = modifier.height(height = 42.dp),
        shape = RoundedCornerShape(size = SQUARE_CORNER_DP.dp),
        color = fill,
        border = BorderStroke(width = 1.dp, color = stroke),
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                ),
                color = ink,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun SquareTagBadge(
    text: String,
    accentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(size = PLAQUE_CORNER_DP.dp),
        color = containerColor,
        border = BorderStroke(width = 1.dp, color = accentColor.copy(alpha = 0.45f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = accentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun SquareCheckbox(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = VerdantTheme.colors
    val outerShape = RoundedCornerShape(size = PLAQUE_CORNER_DP.dp)
    val borderColor = if (checked) palette.mossAccent else palette.ruleStrong

    Box(
        modifier = modifier
            .size(size = CHECKBOX_OUTER_DP.dp)
            .clip(outerShape)
            .background(palette.card)
            .border(width = 1.5.dp, color = borderColor, shape = outerShape)
            .clickable(onClick = onCheckedChange),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Box(
                modifier = Modifier
                    .size(size = CHECKBOX_INNER_DP.dp)
                    .clip(RoundedCornerShape(size = 2.dp))
                    .background(palette.mossAccent),
            )
        }
    }
}

@Composable
fun SquareHoldReservoir(
    progressFraction: Float,
    isHolding: Boolean,
    idleLabel: String,
    holdingLabel: String,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCanopy: Boolean = true,
    lockMillis: Long = 0L,
) {
    val palette = VerdantTheme.colors
    val clampedProgress = progressFraction.coerceIn(0f, 1f)
    // Optional intro: locked with a countdown for lockMillis, then the padlock opens into the label.
    val unlockPhase = rememberHoldUnlockPhase(lockMillis)
    val acceptsHold = enabled && !unlockPhase.isLocked
    val latestOnHoldStart by rememberUpdatedState(onHoldStart)
    val latestOnHoldEnd by rememberUpdatedState(onHoldEnd)
    val animatedScale by animateFloatAsState(
        targetValue = if (isHolding) HOLD_PRESSED_SCALE else 1f,
        animationSpec = spring(stiffness = HOLD_SPRING_STIFFNESS),
        label = "squareHoldScale",
    )

    val outerShape = RoundedCornerShape(size = SQUARE_CORNER_DP.dp)
    val plaqueShape = RoundedCornerShape(size = PLAQUE_CORNER_DP.dp)
    val wellFill = if (onCanopy) palette.canopyCore else palette.sageTint
    val strokeColor = if (onCanopy) palette.sunGold else palette.mossAccent
    val plaqueFill = if (onCanopy) palette.canopyCard else palette.card
    val plaqueText = if (onCanopy) palette.cream else palette.ink

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height = RESERVOIR_HEIGHT_DP.dp)
            .scale(animatedScale)
            .clip(outerShape)
            .background(wellFill)
            .border(width = 1.5.dp, color = strokeColor, shape = outerShape)
            .pointerInput(acceptsHold) {
                if (!acceptsHold) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    latestOnHoldStart()
                    try {
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })
                    } finally {
                        latestOnHoldEnd()
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        if (clampedProgress > 0f) {
            ReservoirProgressFill(fraction = clampedProgress, color = strokeColor)
        }
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            shape = plaqueShape,
            color = plaqueFill,
            border = BorderStroke(width = 1.dp, color = strokeColor),
        ) {
            HoldPlaqueContent(
                label = if (isHolding) holdingLabel else idleLabel,
                phase = unlockPhase,
                textColor = plaqueText,
                lockColor = strokeColor,
                keyholeColor = plaqueFill,
            )
        }
    }
}

/** The hold button's fill: a tinted band with a solid leading edge, [fraction] of the width.
 * Laid out from the start edge by a Row, so it grows right-to-left in RTL layouts. */
@Composable
private fun ReservoirProgressFill(fraction: Float, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth(fraction)
            .fillMaxHeight(),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(color.copy(alpha = RESERVOIR_FILL_ALPHA)),
        )
        Box(
            modifier = Modifier
                .width(width = 2.dp)
                .fillMaxHeight()
                .background(color),
        )
    }
}

@Immutable
data class NavBarItems(val labels: List<String>)

/** Bottom space scrolling tab content needs so its last item can rise clear of [FloatingPillNavBar]. */
@Composable
fun floatingNavBarClearance(): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
        (NAV_BAR_HEIGHT_DP + NAV_BOTTOM_MARGIN_DP + NAV_CONTENT_GAP_DP).dp

/**
 * Option 5 "Botanical Sanctuary" nav: a floating rounded card with a sage pill behind the active tab.
 * Content scrolls underneath and fades into the canvas through the scrim above and around the pill.
 */
@Composable
fun FloatingPillNavBar(
    items: NavBarItems,
    activeIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = VerdantTheme.colors
    val pillShape = RoundedCornerShape(percent = 50)
    val fadeScrim = Brush.verticalGradient(
        0f to palette.canvas.copy(alpha = 0f),
        NAV_FADE_SOLID_FRACTION to palette.canvas,
    )
    Box(modifier = modifier.fillMaxWidth().background(fadeScrim)) {
        FloatingPillRow(items, activeIndex, onSelectIndex, pillShape)
    }
}

@Composable
private fun FloatingPillRow(
    items: NavBarItems,
    activeIndex: Int,
    onSelectIndex: (Int) -> Unit,
    pillShape: RoundedCornerShape,
) {
    val palette = VerdantTheme.colors
    Row(
        modifier = Modifier
            .padding(top = NAV_FADE_HEIGHT_DP.dp)
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = NAV_SIDE_MARGIN_DP.dp, end = NAV_SIDE_MARGIN_DP.dp, bottom = NAV_BOTTOM_MARGIN_DP.dp)
            .height(height = NAV_BAR_HEIGHT_DP.dp)
            .shadow(
                elevation = NAV_SHADOW_DP.dp,
                shape = pillShape,
                ambientColor = palette.canopy,
                spotColor = palette.canopy,
            )
            .clip(pillShape)
            .background(palette.card)
            .border(width = 1.dp, color = palette.cardBorder, shape = pillShape)
            .padding(all = NAV_PILL_INSET_DP.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.labels.forEachIndexed { index, label ->
            val isSelected = index == activeIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(pillShape)
                    .background(if (isSelected) palette.sageTint else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.Tab) { onSelectIndex(index) },
                contentAlignment = Alignment.Center,
            ) {
                NavPillLabel(label = label, isSelected = isSelected)
            }
        }
    }
}

/** One-line tab label that steps its font down until it fits (long words like "Estadísticas"). */
@Composable
private fun NavPillLabel(label: String, isSelected: Boolean) {
    val palette = VerdantTheme.colors
    var fontSize by remember(label) { mutableStateOf(NAV_LABEL_SP.sp) }
    Text(
        text = label,
        fontFamily = FontFamily.SansSerif,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
        fontSize = fontSize,
        maxLines = 1,
        softWrap = false,
        color = if (isSelected) palette.ink else palette.inkMuted,
        onTextLayout = { layout ->
            if (layout.didOverflowWidth && fontSize.value > NAV_LABEL_MIN_SP) {
                fontSize = (fontSize.value * NAV_LABEL_SHRINK_STEP).sp
            }
        },
    )
}
