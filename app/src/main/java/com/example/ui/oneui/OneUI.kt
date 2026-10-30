package com.example.ui.oneui

import androidx.compose.foundation.Canvas
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.example.utils.plainClickable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.graphics.luminance
import com.example.ThemeSettings
import androidx.compose.ui.focus.focusRequester as focusReq
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.hazeSource
import androidx.compose.ui.graphics.Shape

object OneUIDimens {
    val ScreenPadding = 16.dp
    val CardRadius = 26.dp
    val HeaderCollapseDistance = 140.dp
    val BarHeight = 56.dp
    val TitleTopGap = 24.dp
    val TitleSubtitleBottomExtra = 6.dp
}

/** Large collapsing One UI header + scrolling content. Content starts in the lower 2/3 of the screen. */
@Composable
fun OneUIScreen(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    onMenu: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    listState: LazyListState = rememberLazyListState(),
    bottomPadding: Dp? = null,
    background: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit
) {
    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val titleTopExtra = 0.dp
    val titleBottomTrim = 0.dp
    val expandedPx = with(density) { OneUIDimens.HeaderCollapseDistance.toPx() }
    val fraction by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / expandedPx).coerceIn(0f, 1f)
        }
    }
    // Original APK b51.q: titles appear only in the final third of header collapse.
    val titleAlpha = ((fraction - 0.66f) / 0.34f).coerceIn(0f, 1f)
    val barAlpha by animateFloatAsState(titleAlpha, tween(180), label = "barAlpha")
    val tileAlpha by animateFloatAsState(
        if (background != null) 1f else titleAlpha,
        tween(180), label = "tileAlpha"
    )
    val bg = MaterialTheme.colorScheme.background
    val hazeState = LocalHazeState.current
    Box(modifier.fillMaxSize().background(bg)) {
        if (background != null) {
            Box(Modifier.fillMaxSize()) { background() }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .then(if (hazeState != null && background == null) Modifier.hazeSource(hazeState) else Modifier)
                // Original b51.q masks the scrolling layer, not the floating bottom bar.
                // Without this, the prayer card stays visible under the bar and looks like
                // one tall glass shape instead of a separate 64dp pill.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    if (fraction > 0.001f) {
                        val h = (statusTop + 112.dp).toPx()
                        val rest = 1f - fraction
                        drawRect(Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0f),
                            .36f to Color.Black.copy(alpha = rest + .02f * fraction),
                            .72f to Color.Black.copy(alpha = rest + .5f * fraction),
                            1f to Color.Black, endY = h), size = Size(size.width, h), blendMode = BlendMode.DstIn)
                    }
                    val h = (84.dp + navigationBottom + 18.dp).toPx().coerceAtMost(size.height / 2)
                    val y = size.height - h
                    drawRect(Brush.verticalGradient(0f to Color.Black, .35f to Color.Black.copy(alpha = .7f),
                        .75f to Color.Black.copy(alpha = .15f), 1f to Color.Transparent, startY = y, endY = size.height),
                        topLeft = Offset(0f, y), size = Size(size.width, h), blendMode = BlendMode.DstIn)
                },
            contentPadding = PaddingValues(
                start = OneUIDimens.ScreenPadding,
                end = OneUIDimens.ScreenPadding,
                bottom = bottomPadding ?: (84.dp + navigationBottom)
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "oneui-header") {
                Column(
                    Modifier.fillMaxWidth()
                        .padding(top = statusTop + 4.dp + OneUIDimens.TitleTopGap + titleTopExtra,
                            bottom = 16.dp - titleBottomTrim + (if (subtitle != null) OneUIDimens.TitleSubtitleBottomExtra else 0.dp))
                        .alpha(1f - (fraction * 3f).coerceAtMost(1f))
                ) {
                    Spacer(Modifier.height(52.dp))
                    Text(title, style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground, fontSize = 36.sp, lineHeight = 44.sp)
                    if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            content()
        }
        // Header controls stay in one fixed row. Only pill backgrounds and the small title fade.
        val cs = MaterialTheme.colorScheme
        Row(
            Modifier.fillMaxWidth().padding(top = statusTop + 10.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(Modifier.weight(1f, fill = false)) {
                Box(Modifier.matchParentSize().alpha(tileAlpha)
                    .oneUIGlass(RoundedCornerShape(26.dp), cs.surfaceContainerHigh, 0.85f, 28))
                Row(Modifier.height(52.dp).padding(start = 6.dp, end = 18.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    val leading = onBack ?: onMenu
                    if (leading != null) {
                        IconButton(onClick = leading) {
                            Icon(if (onBack != null) Icons.AutoMirrored.Filled.ArrowBack else Icons.Outlined.Menu,
                                com.example.ui.i18n.L10n.t(if (onBack != null) com.example.ui.i18n.S.Back else com.example.ui.i18n.S.Menu),
                                tint = cs.onSurface)
                        }
                        Spacer(Modifier.width(14.dp))
                    }
                    Text(title, modifier = Modifier.alpha(if (background != null) tileAlpha else barAlpha),
                        style = MaterialTheme.typography.titleMedium, color = cs.onSurface,
                        fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (actions != null) Box {
                Box(Modifier.matchParentSize().alpha(tileAlpha)
                    .oneUIGlass(RoundedCornerShape(26.dp), cs.surfaceContainerHigh, 0.85f, 28))
                Row(Modifier.height(52.dp).padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) { actions() }
            }
        }
    }
}

@Composable
fun OneUIIconAction(icon: ImageVector, contentDescription: String?, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription, tint = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    Text(
        text,
        modifier = modifier.padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 0.dp),
        style = MaterialTheme.typography.titleSmall,
        color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun OneUICard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(0.dp),
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(OneUIDimens.CardRadius)
    // Mirror Surface semantics: children default to the theme content color for this surface,
    // so uncolored Text stays readable in dark mode.
    CompositionLocalProvider(LocalContentColor provides contentColorFor(color)) {
        Column(
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(color)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(padding),
            content = content
        )
    }
}

@Composable
fun OneUIDivider(inset: Dp = 16.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = inset)
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outline)
    )
}

@Composable
fun OneUIIconBadge(icon: ImageVector, tint: Color = MaterialTheme.colorScheme.primary, size: Dp = 40.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

/** Standard settings-style row: leading icon, title, summary, trailing content. */
@Composable
fun OneUIRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = if (summary != null) 14.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
            if (summary != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun OneUIButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    icon: ImageVector? = null,
    destructive: Boolean = false,
    accentOverride: Color? = null
) {
    val cs = MaterialTheme.colorScheme
    val accent = accentOverride ?: if (destructive) cs.error else cs.primary
    val container = if (filled) accent else accent.copy(alpha = 0.14f)
    val content = if (filled) Color.White else accent
    Row(
        modifier
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, null, tint = content, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = content)
    }
}

@Composable
fun OneUISearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .background(cs.surface)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = cs.onSurface),
                cursorBrush = SolidColor(cs.primary),
                modifier = Modifier.fillMaxWidth().then(if (focusRequester != null) Modifier.focusReq(focusRequester) else Modifier)
            )
        }
        if (value.isNotEmpty()) {
            Text(
                "✕",
                color = cs.onSurfaceVariant,
                modifier = Modifier.clickable { onValueChange("") }.padding(4.dp)
            )
        }
    }
}

/**
 * One UI two-option segmented control (3.3.3, his spec): one joined container; the selected
 * segment gets the existing Hisn chip look (blue-tinted fill primary 16%, blue text); the
 * unselected segment stays plain. The blue indicator slides between segments with the One UI
 * basic path interpolator cubic-bezier(0.22, 0.25, 0.00, 1.00) over 280ms (Samsung motion
 * guidance: 100-500ms depending on object size - developer.samsung.com/one-ui/motion/basic.html),
 * with a subtle in-flight stretch like the platform indicators.
 */
@Composable
fun OneUISegmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxLines: Int = 1
) {
    val cs = MaterialTheme.colorScheme
    val pos = remember { androidx.compose.animation.core.Animatable(selected.toFloat()) }
    androidx.compose.runtime.LaunchedEffect(selected) {
        pos.animateTo(selected.toFloat(), tween(280, easing = androidx.compose.animation.core.CubicBezierEasing(0.22f, 0.25f, 0f, 1f)))
    }
    val density = LocalDensity.current
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(cs.surfaceVariant.copy(alpha = 0.55f))
            .padding(4.dp)
    ) {
        val segW = maxWidth / options.size
        val p = pos.value
        val frac = p - kotlin.math.floor(p)
        val stretch = 1f + 0.14f * kotlin.math.sin(Math.PI * frac).toFloat()
        val segWpx = with(density) { segW.toPx() }
        // Modifier.offset{} already mirrors its offset in RTL - computing an RTL start here
        // too would mirror the pill twice and land it on the wrong segment (caught on the
        // emulator: pill rendered under the unselected option).
        val startPx = segWpx * p
        val centerPx = startPx + segWpx / 2f
        // sliding blue indicator (behind the labels)
        Box(
            Modifier
                .fillMaxHeight()
                .width(segW * stretch)
                .offset { androidx.compose.ui.unit.IntOffset((centerPx - segWpx * stretch / 2f).toInt(), 0) }
                .clip(RoundedCornerShape(20.dp))
                .background(cs.primary.copy(alpha = 0.16f))
        )
        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, label ->
                val active = i == selected
                val textColor by animateColorAsState(if (active) cs.primary else cs.onSurfaceVariant, label = "segText")
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .plainClickable { onSelect(i) }
                        .padding(vertical = 9.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = maxLines
                    )
                }
            }
        }
    }
}

@Composable
fun OneUIChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dot: Color? = null
) {
    val cs = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (selected) cs.primary.copy(alpha = 0.16f) else cs.surface)
    Row(
        modifier
            .height(38.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dot != null) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(7.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) cs.primary else cs.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun OneUISwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val dark = cs.background.luminance() < 0.5f
    val track by animateColorAsState(
        if (checked) cs.primary else if (dark) Color(0xFF39393C) else Color(0xFFB7B7B7)
    )
    val thumb by animateDpAsState(
        if (checked) 22.dp else 0.dp,
        spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium)
    )
    val switchView = androidx.compose.ui.platform.LocalView.current
    Box(
        modifier
            .width(52.dp)
            .height(30.dp)
            .clip(CircleShape)
            .background(track)
            .toggleable(
                value = checked,
                role = Role.Switch,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = onCheckedChange != null,
                onValueChange = {
                    com.example.utils.AppHaptics.load(switchView.context)
                    if (com.example.utils.AppHaptics.enabled) switchView.performHapticFeedback(
                        if (android.os.Build.VERSION.SDK_INT >= 34)
                            (if (!checked) android.view.HapticFeedbackConstants.TOGGLE_ON else android.view.HapticFeedbackConstants.TOGGLE_OFF)
                        else android.view.HapticFeedbackConstants.CLOCK_TICK
                    )
                    onCheckedChange?.invoke(!checked)
                }
            )
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .offset(x = thumb)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
fun OneUIEmptyState(text: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Parse "#RRGGBB" safely. */
fun parseColorSafe(hex: String, fallback: Color = Color(0xFF3E91FF)): Color =
    try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { fallback }

/** Pluralised Arabic counts for azkar. */
fun azkarCount(n: Int): String = com.example.ui.i18n.L10n.azkarCount(n)

fun readsCount(n: Int): String = com.example.ui.i18n.L10n.readsCount(n)

@Composable
fun oneUiMenuColor(): Color {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (dark) Color(0xFF252525) else Color(0xFFFCFCFC)
}

/** Samsung One UI floating popup: rounded plain panel, no border, soft shadow, text-only rows. */
@Composable
fun OneUIMenu(expanded: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = 190.dp),
        offset = androidx.compose.ui.unit.DpOffset(14.dp, (-44.2).dp),
        shape = RoundedCornerShape(24.dp),
        containerColor = oneUiMenuColor(),
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
        border = null,
        content = content
    )
}

@Composable
fun OneUIMenuItem(text: String, onClick: () -> Unit, icon: ImageVector? = null, destructive: Boolean = false) {
    DropdownMenuItem(
        text = { Text(text, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.5.sp, maxLines = 2) },
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 4.dp)
    )
}

@Composable
fun OneUIProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    gradient: List<Color>? = null
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(CircleShape)
            .background(trackColor)
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(CircleShape)
                .background(if (gradient != null) androidx.compose.ui.graphics.Brush.horizontalGradient(gradient) else androidx.compose.ui.graphics.SolidColor(color))
        )
    }
}

/** Palette of the sky scene behind the Prayer tab (0 none, 1 dark sky: light-on-dark, 2 bright sky: dark ink). The floating bar follows it on that tab. */
object PrayerSky { var mode by androidx.compose.runtime.mutableIntStateOf(0) }

/** Frosted One UI bottom bar. */
@Composable
fun OneUIBottomBar(
    tabs: List<BottomTab>,
    selectedRoute: String?,
    onSelect: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // Original b51.c/xd1/cb0.a uses ml.G (surfaceContainer), not ml.p (surface).
    // Radius is 28dp; the parent provides bottom alignment.
    val skyMode = if (selectedRoute == "prayer") PrayerSky.mode else 0
    val barShape = RoundedCornerShape(32.dp)
    val dark = ThemeSettings.isDark()
    val selectedTint = if (dark) Color(0xFF78B8F8) else Color(0xFF2E6DB4)
    Box(modifier.fillMaxWidth()) {
    Row(
        Modifier.align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = navInset + 12.dp)
            .then(Modifier.oneUIGlass(barShape, cs.surfaceContainer,
                alpha = if (skyMode == 1) 0.46f else if (skyMode == 2) 0.55f else 0.85f,
                blurRadius = 28))
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
            tabs.forEach { tab ->
                val selected = tab.route == selectedRoute
                val tint by animateColorAsState(
                    if (selected) selectedTint else cs.onSurface.copy(alpha = 0.78f), label = "tabTint")
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        Modifier
                            .height(30.dp)
                            .width(56.dp)
                            .clip(CircleShape)
                            .background(if (selected) selectedTint.copy(alpha = 0.16f) else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) { tab.icon(tint) }
                    Text(
                        tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = tint,
                        fontSize = 11.sp,
                        maxLines = 1,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OneUITextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    val cs = MaterialTheme.colorScheme
    // One UI form row: flat, thin bottom divider, optional leading icon
    Column(modifier.fillMaxWidth()) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = if (singleLine) 48.dp else 121.dp)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(16.dp))
        }
        Box(
            Modifier.weight(1f),
            contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart
        ) {
        if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface),
            cursorBrush = SolidColor(cs.primary),
            modifier = Modifier.fillMaxWidth()
        )
        }
    }
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(cs.outline))
    }
}


// ---- One UI side drawer: floating rounded panel ----
class OneUIDrawerItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val tint: Color
)

/** Material 3 DrawerDefaults.MaximumDrawerWidth (360dp), the platform default for a navigation drawer. */
val DrawerMaxWidth = 360.dp

@Composable
fun OneUIDrawer(
    open: Boolean,
    currentRoute: String?,
    items: List<OneUIDrawerItem>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    footer: (@Composable () -> Unit)? = null
) {
    val cs = MaterialTheme.colorScheme
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // Panel is CenterStart: left edge in LTR, right edge in RTL. Slide from its own edge.
    val fromEdge: (Int) -> Int = { full -> if (isRtl) full else -full }
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = open,
            enter = fadeIn(tween(240)),
            exit = fadeOut(tween(200))
        ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
        )
        }
        AnimatedVisibility(
            visible = open,
            modifier = Modifier.align(Alignment.CenterStart),
            enter = slideInHorizontally(tween(280), fromEdge) + fadeIn(tween(220)),
            exit = slideOutHorizontally(tween(240), fromEdge) + fadeOut(tween(180))
        ) {
        // Calibrated to the original: panel 312dp wide at x=29px, top 212px, bottom margin 152px;
        // wordmark text, 48dp rows with 16dp side inset and 136px pitch, dotted divider before Settings.
        val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val light = cs.background.luminance() > 0.5f
        val panelColor = if (light) Color(0xFFDDDEE0) else cs.surface
        Column(
            Modifier
                .padding(start = 11.dp, top = statusTop + 32.04.dp, bottom = navBottom + 33.9.dp)
                .width(312.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(28.dp))
                .background(panelColor)
        ) {
            Text(
                "وذكّر",
                color = cs.onSurface,
                fontSize = 28.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 48.dp)
            )
            Spacer(Modifier.height(32.dp))
            items.forEachIndexed { i, item ->
                val selected = item.route == currentRoute
                if (item.route == "settings") {
                    Canvas(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).height(2.dp)) {
                        drawLine(
                            color = cs.onSurface.copy(alpha = 0.35f), start = androidx.compose.ui.geometry.Offset(0f, 1f),
                            end = androidx.compose.ui.geometry.Offset(size.width, 1f), strokeWidth = 2f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(2f, 10f), 0f),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 1.9.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) cs.onSurface.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { onSelect(item.route) }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(item.icon, null, tint = item.tint, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(18.dp))
                    Text(item.label, style = MaterialTheme.typography.bodyLarge, color = cs.onSurface)
                }
            }
            if (footer != null) {
                Spacer(Modifier.weight(1f))
                Box(Modifier.fillMaxWidth().padding(bottom = 7.8.dp), contentAlignment = Alignment.Center) {
                    footer()
                }
            }
        }
        }
    }
}

/**
 * 3.4 large-screen mode: true when the window is at least 600dp wide (Android's Medium/Expanded
 * width classes: tablets, unfolded foldables, large split-screen windows). It follows the live
 * window width, so a rotated or resized window switches layout by itself.
 */
@Composable
fun isWideScreen(): Boolean = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600

/**
 * Two side-by-side panes (list + detail) only from 840dp window width, Android's Expanded width class and
 * the width where each pane still gets about 400dp after the 108dp rail. Between 600 and 840dp (portrait
 * tablets, narrow split-screen) the rail stays and each page keeps a single pane.
 */
@Composable
fun isTwoPane(): Boolean = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 840

/** Content column used on wide screens for single-column pages so lines do not stretch edge to edge. */
@Composable
fun WideContentFrame(content: @Composable () -> Unit) {
    if (!isWideScreen()) { content(); return }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.fillMaxHeight().widthIn(max = 760.dp).fillMaxWidth()) { content() }
    }
}

/** Width the floating rail takes at the start edge (84dp pill + 12dp padding each side). */
val RailInset = 108.dp
val LocalRailInset = androidx.compose.runtime.compositionLocalOf { 0.dp }

/** 3.4: side navigation rail that replaces the floating bottom bar on wide screens (same tabs, same tint rules). */
@Composable
fun OneUINavRail(
    tabs: List<BottomTab>,
    selectedRoute: String,
    onSelect: (BottomTab) -> Unit,
    modifier: Modifier = Modifier,
    solid: Boolean = true
) {
    val cs = MaterialTheme.colorScheme
    val tabBlue = if (ThemeSettings.isDark()) Color(0xFF78B8F8) else Color(0xFF2E6DB4)
    val railView = androidx.compose.ui.platform.LocalView.current
    Box(
        modifier = modifier
            .fillMaxHeight()
            .then(if (solid) Modifier.background(cs.background) else Modifier)
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(WindowInsets.navigationBars.asPaddingValues())
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(84.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(cs.surfaceContainerHigh.copy(alpha = 0.9f))
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            tabs.forEach { tab ->
                val selected = tab.route == selectedRoute
                val tint by animateColorAsState(
                    if (selected) tabBlue else cs.onSurface.copy(alpha = 0.78f),
                    label = "railTint"
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(tab) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .width(56.dp)
                            .clip(CircleShape)
                            .background(if (selected) tabBlue.copy(alpha = 0.16f) else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        tab.icon(tint)
                    }
                    Text(
                        text = tab.label,
                        color = tint,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

/** A bottom bar / rail destination: route id, label and icon drawn with the given tint. */
class BottomTab(val route: String, val label: String, val icon: @androidx.compose.runtime.Composable (androidx.compose.ui.graphics.Color) -> Unit)

/** Holds the app NavController so drawer/header actions can navigate. */
object HeaderTools {
    @JvmField var nav: androidx.navigation.NavController? = null
    fun setNav(n: androidx.navigation.NavController) { nav = n }
}
