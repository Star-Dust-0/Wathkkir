package com.example.ui.screens
import com.example.ui.i18n.S
import com.example.ui.i18n.L10n

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.MaterialTheme
import com.example.ui.oneui.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DhikrHistory
import com.example.ui.oneui.OneUICard
import com.example.ui.oneui.OneUIDivider
import com.example.ui.oneui.OneUIIconBadge
import com.example.ui.oneui.OneUIRow
import com.example.ui.oneui.OneUIScreen
import com.example.ui.oneui.SectionLabel
import com.example.ui.oneui.readsCount
import com.example.ui.theme.OneUICategoryViolet
import com.example.ui.viewmodel.DhikrStats
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatisticsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val allDhikr by viewModel.allDhikr.collectAsStateWithLifecycle()
    val allHistory by viewModel.allHistory.collectAsStateWithLifecycle()
    val titles = remember(allDhikr) { allDhikr.associate { it.id to it.title } }
    StatsContent(stats, titles, allHistory, onBack)
}

@Composable
fun StatsContent(
    stats: DhikrStats,
    titles: Map<Int, String>,
    history: List<DhikrHistory>,
    onBack: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    OneUIScreen(
        title = L10n.t(S.Stats),
        subtitle = readsCount(stats.totalCount) + L10n.t(S.TotalSuffix),
        onBack = onBack
    ) {
        item(key = "donut") {
            OneUICard(padding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                        Donut(stats.topReadDhikr.map { it.second })
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stats.todayCount.toString(),
                                color = cs.onSurface,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(L10n.t(S.Today), color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.width(20.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (stats.topReadDhikr.isEmpty()) {
                            Text(
                                L10n.t(S.NoReadsYet),
                                color = cs.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        stats.topReadDhikr.take(3).forEachIndexed { i, (id, count) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(9.dp).clip(CircleShape).background(donutColor(i)))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    titles[id] ?: L10n.t(S.DhikrIdFmt).format(id),
                                    modifier = Modifier.weight(1f),
                                    color = cs.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    count.toString(),
                                    color = cs.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
        item(key = "periods-l") { SectionLabel(L10n.t(S.Periods)) }
        item(key = "periods") {
            OneUICard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    PeriodCell(L10n.t(S.Today), stats.todayCount, Modifier.weight(1f))
                    Box(Modifier.width(0.5.dp).height(44.dp).background(cs.outline).align(Alignment.CenterVertically))
                    PeriodCell(L10n.t(S.Week), stats.weekCount, Modifier.weight(1f))
                    Box(Modifier.width(0.5.dp).height(44.dp).background(cs.outline).align(Alignment.CenterVertically))
                    PeriodCell(L10n.t(S.MonthLabel), stats.monthCount, Modifier.weight(1f))
                }
            }
        }
        item(key = "streak") {
            OneUICard {
                OneUIRow(
                    title = L10n.t(S.DayStreak),
                    summary = L10n.t(S.KeepGoing),
                    leading = { OneUIIconBadge(Icons.Outlined.LocalFireDepartment) },
                    trailing = {
                        Text(
                            dayCount(stats.streakDays),
                            color = OneUICategoryViolet,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                )
                OneUIDivider(70.dp)
                OneUIRow(
                    title = L10n.t(S.TotalReads),
                    summary = L10n.t(S.SinceStart),
                    leading = { OneUIIconBadge(Icons.Outlined.EmojiEvents) },
                    trailing = {
                        Text(
                            stats.totalCount.toString(),
                            color = OneUICategoryViolet,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                )
            }
        }
        if (history.isNotEmpty()) {
            item(key = "hist-l") { SectionLabel(L10n.t(S.RecentReads)) }
            item(key = "hist") {
                OneUICard {
                    history.take(10).forEachIndexed { i, h ->
                        if (i > 0) OneUIDivider(70.dp)
                        val date = SimpleDateFormat("d MMM yyyy  hh:mm a", Locale("ar")).format(Date(h.timestamp))
                        OneUIRow(
                            title = titles[h.dhikrId] ?: L10n.t(S.DhikrIdFmt).format(h.dhikrId),
                            summary = date,
                            leading = { OneUIIconBadge(Icons.Outlined.History) }
                        )
                    }
                }
            }
        }
    }
}

private fun dayCount(n: Int): String = L10n.daysCount(n)

@Composable
private fun PeriodCell(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineSmall)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

private fun donutColor(i: Int): Color {
    val p = OneUICategoryViolet
    return when (i) {
        0 -> p
        1 -> p.copy(alpha = 0.6f)
        else -> p.copy(alpha = 0.32f)
    }
}

@Composable
private fun Donut(values: List<Int>) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val colors = listOf(donutColor(0), donutColor(1), donutColor(2))
    Canvas(Modifier.fillMaxSize()) {
        val stroke = 12.dp.toPx()
        val d = minOf(size.width, size.height) - stroke
        val tl = Offset((size.width - d) / 2f, (size.height - d) / 2f)
        val sz = Size(d, d)
        drawArc(track, 0f, 360f, false, tl, sz, style = Stroke(stroke))
        val total = values.sum().toFloat()
        if (total > 0f) {
            var start = -90f
            values.take(3).forEachIndexed { i, v ->
                val sweep = v / total * 360f
                drawArc(
                    colors[i], start, (sweep - 3f).coerceAtLeast(1f), false, tl, sz,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
                start += sweep
            }
        }
    }
}
