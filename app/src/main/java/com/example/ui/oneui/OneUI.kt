package com.example.ui.oneui
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

import com.example.ui.i18n.L10n
import com.example.ui.i18n.S
import com.example.ThemeSettings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.example.utils.plainClickable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.chrisbanes.haze.hazeSource

object OneUIDimens {
    val ScreenPadding: Dp = 16.dp
    val CardRadius: Dp = 26.dp
    val HeaderCollapseDistance: Dp = 140.dp
    val BarHeight: Dp = 56.dp
    // Clears the floating pill tab bar (64dp bar + 12dp bottom margin) with an 8dp gap.
    val TabBarClearance: Dp = 84.dp
    // Title block balance (3.3.0): gap above the large title (header row to glyph top) equals
    // the gap below it (last title/subtitle line to the next element). Measured on a 420dpi
    // device: above was 13dp, below 37dp (title only) or 31dp (title + subtitle).
    val TitleTopGap: Dp = 24.dp
    val TitleSubtitleBottomExtra: Dp = 6.dp
}

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
    // Time-of-day sky (v3.2.0): the prayer screen paints its sky behind a transparent
    // container and tints only the big title for scene legibility. Defaults keep every
    // other screen byte-identical.
    containerColor: Color = MaterialTheme.colorScheme.background,
    titleColor: Color = MaterialTheme.colorScheme.onBackground,
    // v3.2.0 (his call): on the prayer screen the header button pills stay visible in
    // their frosted frames even at rest, so icons stay legible over every sky/theme
    // combo. Default false keeps every other screen's fade-in-on-scroll unchanged.
    alwaysShowHeaderTiles: Boolean = false,
    // Prayer page only: nudges the large title down away from the always-visible pills and
    // closer to the first card. Defaults keep every other page unchanged.
    titleTopExtra: Dp = 0.dp,
    titleBottomTrim: Dp = 0.dp,
    // v3.2.0 glass system: when non-null the header pills use this frost material
    // (prayer screen: matches the cards over the sky). Null = default tile, unchanged.
    frost: FrostSpec? = null,
    content: LazyListScope.() -> Unit
) {
    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // Bottom clearance must match the floating pill's real geometry plus the system nav
    // inset: a flat value under-pads 3-button navigation and over/under-pads gesture nav.
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val expandedPx = with(density) { OneUIDimens.HeaderCollapseDistance.toPx() }
    val fraction by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / expandedPx).coerceIn(0f, 1f)
        }
    }
    // One UI collapse timing (3.3.3): the large title scrolls away and fades over the first
    // third (alpha 1-3f in the header item); the collapsed bar title and the pills fade in
    // only over the last third, so the two titles never draw on top of each other.
    val barFraction = ((fraction - 0.66f) / 0.34f).coerceIn(0f, 1f)
    val barAlpha by animateFloatAsState(barFraction, tween(180), label = "barAlpha")
    val tileAlpha by animateFloatAsState(if (alwaysShowHeaderTiles) 1f else if (frost == null) barFraction else fraction, tween(180), label = "tileAlpha")
    val hazeState = LocalHazeState.current

    Box(modifier = modifier.fillMaxSize().background(containerColor)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier)
                // One UI edge fade: content dissolves smoothly under the floating pills and the nav pill.
                // No coloured strip and no blur. The top fade grows with the scroll so the resting title stays solid.
                .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val topH = (statusTop + 112.dp).toPx()
                    val f = fraction
                    if (f > 0.001f) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(0f to Color.Black.copy(alpha = 0f), 0.36f to Color.Black.copy(alpha = 0.02f * f + (1f - f)), 0.72f to Color.Black.copy(alpha = 0.5f * f + (1f - f)), 1f to Color.Black),
                                startY = 0f, endY = topH
                            ),
                            size = androidx.compose.ui.geometry.Size(size.width, topH),
                            blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                        )
                    }
                    val botH = ((bottomPadding ?: (OneUIDimens.TabBarClearance + navBottom)) + 18.dp).toPx().coerceAtMost(size.height / 2f)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(0f to Color.Black, 0.35f to Color.Black.copy(alpha = 0.7f), 0.75f to Color.Black.copy(alpha = 0.15f), 1f to Color.Black.copy(alpha = 0f)),
                            startY = size.height - botH, endY = size.height
                        ),
                        topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - botH),
                        size = androidx.compose.ui.geometry.Size(size.width, botH),
                        blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                    )
                },
            state = listState,
            contentPadding = PaddingValues(
                start = OneUIDimens.ScreenPadding,
                end = OneUIDimens.ScreenPadding,
                bottom = bottomPadding ?: (OneUIDimens.TabBarClearance + navBottom)
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = statusTop + 4.dp + OneUIDimens.TitleTopGap + titleTopExtra, bottom = 16.dp - titleBottomTrim + (if (subtitle != null) OneUIDimens.TitleSubtitleBottomExtra else 0.dp))
                        .alpha(1f - minOf(fraction * 3f, 1f))
                ) {
                    Spacer(modifier = Modifier.height(52.dp))
                    Column {
                        Text(
                            title,
                            color = titleColor,
                            fontSize = 36.sp,
                            lineHeight = 44.sp,
                            style = MaterialTheme.typography.headlineLarge
                        )
                        if (subtitle != null) {
                            Text(
                                subtitle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            content()
        }
        // Scroll-aware scrim behind the floating header (3.1.0 look, non-prayer screens
        // only): keeps scrolled content from colliding with the status bar. The prayer
        // screen (frost != null) keeps its sky clear.
        if (false) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(statusTop + 92.dp)
                    .graphicsLayer { alpha = barAlpha }
                    .background(
                        Brush.verticalGradient(
                            0f to MaterialTheme.colorScheme.background,
                            0.78f to MaterialTheme.colorScheme.background,
                            1f to MaterialTheme.colorScheme.background.copy(alpha = 0f)
                        )
                    )
            )
        }
        // Top bar: menu or back on one side, settings and extra actions on the other.
        // The row itself is always visible; only the tile behind each group and the small title fade in on scroll.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = statusTop + 10.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f, fill = false)) {
                // Glass tile fades in on scroll as a background layer; icons/labels stay visible.
                Box(Modifier.matchParentSize().alpha(tileAlpha).then(HeaderTools.glassTile(Modifier, frost)))
                Row(
                    modifier = Modifier
                        .height(52.dp)
                        .padding(start = 6.dp, end = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                val leading = onBack ?: onMenu
                if (leading != null) {
                    IconButton(onClick = leading) {
                        if (onMenu != null) {
                            Icon(Icons.Outlined.Menu, contentDescription = L10n.t(S.Menu), tint = MaterialTheme.colorScheme.onBackground)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = L10n.t(S.Back), tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                }
                Text(
                    title,
                    modifier = Modifier.alpha(if (alwaysShowHeaderTiles) tileAlpha else barAlpha),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                }
            }
            // Gear belongs to top-level pages only (3.3.3, his call): a sub-page (onBack != null)
            // shows just the back arrow, so the gear can no longer stack Settings over sub-pages.
            // On the Settings screen itself it is also suppressed (no empty ghost tile).
            val showSettingsButton = onBack == null && !HeaderTools.isOnSettings()
            if (showSettingsButton || actions != null) {
                Box {
                    Box(Modifier.matchParentSize().alpha(tileAlpha).then(HeaderTools.glassTile(Modifier, frost)))
                    Row(
                        modifier = Modifier.height(52.dp).padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Action icons sit nearest the menu (start in RTL), the gear is the
                        // outermost icon - per user request, matching One UI ordering.
                        actions?.invoke(this)
                        if (showSettingsButton) HeaderTools.SettingsButton()
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsGlyph() {
    Icon(Icons.Outlined.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
}

@Composable
fun OneUIIconAction(icon: ImageVector, contentDescription: String?, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    Text(
        text,
        modifier = modifier.padding(start = 8.dp, top = 12.dp, end = 8.dp, bottom = 0.dp),
        color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.titleSmall
    )
}

@Composable
fun OneUICard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(0.dp),
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(OneUIDimens.CardRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content
    )
}

@Composable
fun OneUIDivider(inset: Dp = 16.dp, color: Color = MaterialTheme.colorScheme.outline) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = inset)
            .height(0.5.dp)
            .background(color)
    )
}

@Composable
fun OneUIIconBadge(
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 40.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun OneUIRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    titleColor: Color = Color.Unspecified
) {
    val resolvedTitleColor = if (titleColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else titleColor
    val tapRow = onClick
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (tapRow != null) Modifier.clickable(onClick = tapRow) else Modifier)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = if (summary != null) 14.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(modifier = Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = resolvedTitleColor, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(summary,
                    modifier = Modifier.padding(top = 2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, lineHeight = 20.sp))
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun OneUIButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    icon: ImageVector? = null,
    destructive: Boolean = false,
    accentOverride: Color? = null
) {
    val cs = MaterialTheme.colorScheme
    val accent = accentOverride ?: if (destructive) cs.error else cs.primary
    val container = if (filled) accent else accent.copy(alpha = 0.14f)
    val content = if (filled) Color.White else accent
    val tapBtn = onClick
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .background(container)
            .clickable(onClick = tapBtn)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(text, color = content, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 1, softWrap = false, modifier = Modifier.weight(1f, fill = false))
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
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .background(cs.surface)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(placeholder, color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = cs.onSurface),
                cursorBrush = SolidColor(cs.primary),
                modifier = Modifier.fillMaxWidth().then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
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
    maxLines: Int = 1,
    rawLabels: Boolean = false
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
                    // rawLabels: render without the app-wide digit conversion (DigitText). Only the
                    // digits-style selector itself uses this - its two example labels must stay
                    // literal under both numeral settings (his spec: that selector is the ONLY
                    // place exempt from the global digits conversion).
                    if (rawLabels) {
                        androidx.compose.material3.Text(
                            label,
                            color = textColor,
                            fontSize = 14.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = maxLines
                        )
                    } else {
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
    val bg by animateColorAsState(if (selected) cs.primary.copy(alpha = 0.16f) else cs.surface, label = "chipBg")
    val tapChip = com.example.utils.AppHaptics.tap(onClick)
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = tapChip)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dot != null) {
            Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(dot))
            Spacer(modifier = Modifier.width(7.dp))
        }
        Text(
            label,
            color = if (selected) cs.primary else cs.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun OneUISwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val dark = cs.background == Color.Black
    val trackOff = if (dark) Color(0xFF39393C) else Color(0xFFB7B7B7)
    val track by animateColorAsState(if (checked) cs.primary else trackOff, label = "swTrack")
    val thumb by animateDpAsState(if (checked) 22.dp else 0.dp, spring(0.75f, 1500f), label = "swThumb")
    val switchView = androidx.compose.ui.platform.LocalView.current
    Box(
        modifier = modifier
            .width(52.dp)
            .height(30.dp)
            .clip(CircleShape)
            .background(track)
            .toggleable(
                value = checked,
                role = Role.Switch,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = onCheckedChange != null
            ) {
                // 3.3.0: the platform's own toggle haptic (API 34+), a soft click on older Android.
                // Respects the system touch-feedback setting.
                val turningOn = !checked
                com.example.utils.AppHaptics.load(switchView.context)
                if (com.example.utils.AppHaptics.enabled) switchView.performHapticFeedback(
                    if (android.os.Build.VERSION.SDK_INT >= 34)
                        (if (turningOn) android.view.HapticFeedbackConstants.TOGGLE_ON else android.view.HapticFeedbackConstants.TOGGLE_OFF)
                    else android.view.HapticFeedbackConstants.CLOCK_TICK
                )
                onCheckedChange?.invoke(turningOn)
            }
            .padding(3.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumb)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
fun OneUIEmptyState(text: String, icon: ImageVector, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = cs.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(56.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(text, color = cs.onSurfaceVariant, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
    }
}

fun parseColorSafe(hex: String, fallback: Color = Color(0xFF008080)): Color =
    try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }

fun azkarCount(n: Int): String = L10n.azkarCount(n)

fun readsCount(n: Int): String = L10n.readsCount(n)

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
        offset = androidx.compose.ui.unit.DpOffset(14.dp, (-44).dp),
        shape = RoundedCornerShape(24.dp),
        containerColor = oneUiMenuColor(),
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
        border = null,
        content = { com.example.ui.theme.AppFontScaled { Column { content() } } }
    )
}

@Composable
fun OneUIMenuItem(text: String, onClick: () -> Unit, icon: ImageVector? = null, destructive: Boolean = false) {
    DropdownMenuItem(
        text = { Text(text, color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, maxLines = 1) },
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
    // Gradient runs from the fill's anchor edge to its leading edge, mirrored in RTL
    // (matches the One UI battery/browser bars and the home widgets).
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(CircleShape)
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(CircleShape)
                .background(
                    if (gradient != null) Brush.horizontalGradient(if (rtl) gradient.reversed() else gradient)
                    else SolidColor(color)
                )
        )
    }
}

class BottomTab(
    val route: String,
    val label: String,
    val icon: @Composable (Color) -> Unit
)

@Composable
fun OneUIBottomBar(
    tabs: List<BottomTab>,
    selectedRoute: String,
    onSelect: (BottomTab) -> Unit,
    modifier: Modifier = Modifier,
    frost: FrostSpec? = null
) {
    val cs = MaterialTheme.colorScheme
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // v3.2.0 (his call): the open tab reads at a glance - the exact sky blue he
    // circled in Samsung Reminder's Scheduled tile (#78B8F8). Light theme uses a
    // darker shade of the same hue so the label stays legible on light glass.
    val tabBlue = if (ThemeSettings.isDark()) Color(0xFF78B8F8) else Color(0xFF2E6DB4)
    val tabView = androidx.compose.ui.platform.LocalView.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = navInset + 12.dp)
            .then(if (frost != null) Modifier.oneUIFrosted(RoundedCornerShape(32.dp), frost, 28) else Modifier.oneUIGlass(RoundedCornerShape(32.dp), cs.surfaceContainerHigh, 0.85f, 28))
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val selected = tab.route == selectedRoute
            val tint by animateColorAsState(
                if (selected) tabBlue else cs.onSurface.copy(alpha = 0.78f),
                label = "tabTint"
            )
            Column(
                modifier = Modifier
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
                    modifier = Modifier
                        .height(30.dp)
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    style = MaterialTheme.typography.labelSmall
                )
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
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (singleLine) 48.dp else 120.dp)
                .padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(16.dp))
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                }
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
        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(cs.outline))
    }
}

class OneUIDrawerItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val tint: Color
)

@Composable
fun DrawerHeader() {
    Text(
        "وَذكّر",
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 48.dp),
        style = MaterialTheme.typography.headlineMedium
    )
    Spacer(modifier = Modifier.height(32.dp))
}

@Composable
fun DottedDivider() {
    val c = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp).height(2.dp)) {
        drawLine(
            c, androidx.compose.ui.geometry.Offset(0f, 1f), androidx.compose.ui.geometry.Offset(size.width, 1f),
            strokeWidth = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(0.1f, 6.dp.toPx()), 0f)
        )
    }
}

val DrawerMaxWidth = 360.dp

@Composable
fun OneUIDrawer(
    open: Boolean,
    currentRoute: String,
    items: List<OneUIDrawerItem>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    footer: (@Composable () -> Unit)? = null,
    frost: FrostSpec? = null
) {
    val cs = MaterialTheme.colorScheme
    val drawerDark = com.example.ThemeSettings.isDark()
    val drawerFrost = if (drawerDark) FrostSpec(Color(0xFF2E2E2E), 0.90f, Color.White.copy(alpha = 0.06f))
    else FrostSpec(Color(0xFFF3F3F4), 0.90f, Color.Black.copy(alpha = 0.05f))
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = open, enter = fadeIn(tween(220)), exit = fadeOut(tween(220))) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val drawerProgress by animateFloatAsState(if (open) 1f else 0f, tween(280), label = "drawerSlide")
        if (open || drawerProgress > 0f) {
            val drawerSlidePx = with(LocalDensity.current) { 324.dp.toPx() }
            val slideSign = if (isRtl) 1f else -1f
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset((slideSign * (1f - drawerProgress) * drawerSlidePx).toInt(), 0) }
                    .alpha(drawerProgress)
                    .padding(start = 11.dp, top = statusTop + 14.dp, bottom = 40.dp)
                    .layout { measurable, constraints ->
                        val w = minOf((constraints.maxWidth * 0.78f).toInt(), DrawerMaxWidth.roundToPx())
                        val p = measurable.measure(constraints.copy(minWidth = w, maxWidth = w))
                        layout(p.width, p.height) { p.place(0, 0) }
                    }
                    .fillMaxHeight()
                    .then(Modifier.oneUIFrosted(RoundedCornerShape(26.dp), drawerFrost, 48))
                    .then(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()))
                    .padding(vertical = 18.dp)
            ) {
                DrawerHeader()
                items.forEach { item ->
                    if (item.route == "settings") DottedDivider()
                    val selected = item.route == currentRoute
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) (if (drawerDark) Color(0xFF424242) else Color(0xFFE2E2E5)) else Color.Transparent)
                            .clickable { onSelect(item.route) }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(item.icon, contentDescription = null, tint = item.tint, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(item.label, color = cs.onSurface, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(modifier = Modifier.height(56.dp))
                if (footer != null) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) { footer() }
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


/** One chip entry in a OneUIChipGroup: label plus optional colored dot (tag chips). */
class OneUIChipItem(val label: String, val dot: Color? = null)

/**
 * One UI chip row with a sliding selection pill (3.3.3, his spec: "انميشن الانتقال السلس بين هذه
 * الازرار" - animation ONLY, no joined container; the chips keep the exact OneUIChip look, size
 * and spacing). Instead of each chip's fill cross-fading, one blue pill slides from the old chip
 * to the new one with the One UI basic path interpolator cubic-bezier(0.22, 0.25, 0.00, 1.00)
 * over 280ms (developer.samsung.com/one-ui/motion/basic.html: 100-500ms by object size).
 * Implementation: the pill is drawn in drawBehind on the scrolling Row, so it rides the same
 * scrolled content as the chips with no viewport/scroll math. Chip positions come from
 * onGloballyPositioned boundsInParent() against that same Row - one raw coordinate space, no
 * manual RTL mirroring (Modifier.offset would mirror twice; drawBehind never mirrors). x and
 * width are two Animatables so the pill stretches in flight like the platform indicators.
 * A selected index outside the list (stored value not among the options) draws no pill.
 */
@Composable
fun OneUIChipGroup(
    items: List<OneUIChipItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val pillColor = cs.primary.copy(alpha = 0.16f)
    // compose-ui 1.9 removed LayoutCoordinates.positionInParent/boundsInParent (compile break
    // caught on CI): positions are now taken relative to the group Row via localBoundingBoxOf,
    // computed lazily from the stored coordinates so first-layout ordering cannot drop them.
    val chipCoords = remember { mutableStateMapOf<Int, androidx.compose.ui.layout.LayoutCoordinates>() }
    var rowCoords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    val animX = remember { androidx.compose.animation.core.Animatable(0f) }
    val animW = remember { androidx.compose.animation.core.Animatable(0f) }
    var pillReady by remember { mutableStateOf(false) }
    val targetChip = chipCoords[selected]
    val target = remember(targetChip, rowCoords) {
        val rc = rowCoords
        if (targetChip == null || rc == null || !targetChip.isAttached || !rc.isAttached) null
        else try { rc.localBoundingBoxOf(targetChip) } catch (_: Exception) { null }
    }
    androidx.compose.runtime.LaunchedEffect(selected, target) {
        val b = target
        if (selected < 0 || b == null) { pillReady = false; return@LaunchedEffect }
        val easing = androidx.compose.animation.core.CubicBezierEasing(0.22f, 0.25f, 0f, 1f)
        if (!pillReady) {
            animX.snapTo(b.left); animW.snapTo(b.width); pillReady = true
        } else {
            launch { animX.animateTo(b.left, tween(280, easing = easing)) }
            launch { animW.animateTo(b.width, tween(280, easing = easing)) }
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(androidx.compose.foundation.rememberScrollState())
            .onGloballyPositioned { rowCoords = it }
            .drawBehind {
                if (pillReady) {
                    drawRoundRect(
                        color = pillColor,
                        topLeft = androidx.compose.ui.geometry.Offset(animX.value, 0f),
                        size = androidx.compose.ui.geometry.Size(animW.value, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
                    )
                }
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEachIndexed { i, chip ->
            val active = i == selected
            val textColor by animateColorAsState(if (active) cs.primary else cs.onSurface, label = "grpText")
            val chipBg by animateColorAsState(if (active) Color.Transparent else cs.surface, label = "grpBg")
            val tapChip = com.example.utils.AppHaptics.tap { onSelect(i) }
            Row(
                modifier = Modifier
                    .onGloballyPositioned { chipCoords[i] = it }
                    .height(38.dp)
                    .clip(CircleShape)
                    .background(chipBg)
                    .clickable(onClick = tapChip)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (chip.dot != null) {
                    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(chip.dot))
                    Spacer(modifier = Modifier.width(7.dp))
                }
                Text(
                    chip.label,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
