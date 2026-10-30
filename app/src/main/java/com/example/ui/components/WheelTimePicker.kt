package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import com.example.ui.oneui.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S

/**
 * One UI-style wheel time picker. Always lays out LTR (hour : minute, then the
 * ص/م marker last), the way Samsung's clock spinner and the ar-locale "10:59 ص"
 * format read, regardless of the app's RTL direction. Follows the theme in both
 * light and dark, uses the localized ص/م markers, and switches to a 24-hour
 * wheel when the system clock is set to 24h.
 */
@Composable
fun WheelTimePicker(
    initialHour24: Int,
    initialMinute: Int,
    onTimeSelected: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val is24 = remember { android.text.format.DateFormat.is24HourFormat(context) }
    val cs = MaterialTheme.colorScheme
    val initialHour12 = if (initialHour24 == 0) 12 else if (initialHour24 > 12) initialHour24 - 12 else initialHour24
    val initialIsAm = initialHour24 < 12
    val hours = if (is24) (0..23).map { String.format("%02d", it) } else (1..12).map { it.toString() }
    val minutes = (0..59).map { String.format("%02d", it) }
    val amPmList = listOf(L10n.t(S.AmMarker), L10n.t(S.PmMarker))

    var selectedHourIndex by remember { mutableIntStateOf(if (is24) initialHour24.coerceIn(0, 23) else initialHour12 - 1) }
    var selectedMinuteIndex by remember { mutableIntStateOf(initialMinute.coerceIn(0, 59)) }
    var selectedAmPmIndex by remember { mutableIntStateOf(if (initialIsAm) 0 else 1) }

    LaunchedEffect(selectedHourIndex, selectedMinuteIndex, selectedAmPmIndex, is24) {
        val h24 = if (is24) selectedHourIndex else {
            val h12 = selectedHourIndex + 1
            val isAm = selectedAmPmIndex == 0
            if (isAm && h12 == 12) 0 else if (isAm) h12 else if (h12 == 12) 12 else h12 + 12
        }
        onTimeSelected(h24, selectedMinuteIndex)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelPickerColumn(hours, selectedHourIndex, { selectedHourIndex = it }, Modifier.width(76.dp))
            Text(
                ":",
                modifier = Modifier.padding(horizontal = 4.dp),
                color = cs.onSurface,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            WheelPickerColumn(minutes, selectedMinuteIndex, { selectedMinuteIndex = it }, Modifier.width(76.dp))
            if (!is24) {
                Spacer(modifier = Modifier.width(12.dp))
                WheelPickerColumn(amPmList, selectedAmPmIndex, { selectedAmPmIndex = it }, Modifier.width(60.dp), loop = false)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelPickerColumn(
    items: List<String>,
    initialIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleItemsCount: Int = 5,
    itemHeight: Dp = 52.dp,
    loop: Boolean = true
) {
    val cs = MaterialTheme.colorScheme
    val count = items.size
    val half = visibleItemsCount / 2
    val itemPx = with(androidx.compose.ui.platform.LocalDensity.current) { itemHeight.toPx() }
    // The band sits on the middle row, so the row that counts as selected is the one
    // `half` rows below the first visible row (a looping wheel) or the first row at the
    // snap position (a finite wheel padded with `half` empty rows above and below).
    val startIndex = remember(count, initialIndex, loop) {
        if (loop) {
            val max = (1 shl 30) - 1
            (max - (max % count)) + initialIndex
        } else initialIndex
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = if (loop) startIndex - half else startIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val selectedIndex by remember {
        derivedStateOf {
            if (loop) {
                val centerIndex = listState.firstVisibleItemIndex + half
                ((centerIndex % count) + count) % count
            } else {
                val round = if (listState.firstVisibleItemScrollOffset > itemPx / 2f) 1 else 0
                (listState.firstVisibleItemIndex + round).coerceIn(0, count - 1)
            }
        }
    }

    val wheelView = androidx.compose.ui.platform.LocalView.current
    val lastTick = remember { androidx.compose.runtime.mutableIntStateOf(selectedIndex) }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex != lastTick.intValue) { lastTick.intValue = selectedIndex; com.example.utils.AppHaptics.tick(wheelView) }
        onItemSelected(selectedIndex)
    }

    Box(
        modifier = modifier.height(itemHeight * visibleItemsCount),
        contentAlignment = Alignment.Center
    ) {
        // One UI selection band behind the center row.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(14.dp))
                .background(cs.surfaceVariant)
        )
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = if (loop) PaddingValues(0.dp) else PaddingValues(vertical = itemHeight * half),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().height(itemHeight * visibleItemsCount)
        ) {
            items(if (loop) Int.MAX_VALUE else count) { index ->
                val actualIndex = ((index % count) + count) % count
                val itemText = items[actualIndex]
                val isSelected = actualIndex == selectedIndex
                Box(
                    modifier = Modifier.height(itemHeight).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = itemText,
                        fontSize = if (isSelected) 26.sp else 20.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) cs.onSurface else cs.onSurfaceVariant.copy(alpha = 0.45f),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
