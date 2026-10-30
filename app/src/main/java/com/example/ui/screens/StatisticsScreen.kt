package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.example.ui.oneui.*
import androidx.compose.ui.unit.sp
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
fun StatsContent(stats: DhikrStats, titles: Map<Int, String>, history: List<DhikrHistory>, onBack: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    OneUIScreen(title = com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Stats), subtitle = "${com.example.ui.i18n.L10n.readsCount(stats.totalCount)}${com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.TotalSuffix)}", onBack = onBack) {
        item(key = "donut") {
            OneUICard(padding = PaddingValues(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                        Donut(stats.topReadDhikr.map { it.second })
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${stats.todayCount}", style = MaterialTheme.typography.headlineMedium, color = cs.onSurface)
                            Text(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Today), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.width(20.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (stats.topReadDhikr.isEmpty()) {
                            Text(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.NoReadsYet), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                        }
                        stats.topReadDhikr.take(3).forEachIndexed { i, (id, count) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(9.dp).clip(CircleShape).background(donutColor(i)))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    titles[id] ?: com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.DhikrIdFmt).format(id),
                                    style = MaterialTheme.typography.bodyMedium, color = cs.onSurface,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                                )
                                Text("$count", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        item(key = "periods-l") { SectionLabel(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Periods)) }
        item(key = "periods") {
            OneUICard {
                Row(Modifier.fillMaxWidth().padding(vertical = 18.dp)) {
                    PeriodCell(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Today), stats.todayCount, Modifier.weight(1f))
                    Box(Modifier.width(0.5.dp).height(44.dp).background(cs.outline).align(Alignment.CenterVertically))
                    PeriodCell(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Week), stats.weekCount, Modifier.weight(1f))
                    Box(Modifier.width(0.5.dp).height(44.dp).background(cs.outline).align(Alignment.CenterVertically))
                    PeriodCell(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Month), stats.monthCount, Modifier.weight(1f))
                }
            }
        }
        item(key = "streak") {
            OneUICard {
                OneUIRow(
                    com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.DayStreak), summary = com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.KeepGoing),
                    leading = { OneUIIconBadge(Icons.Default.LocalFireDepartment) },
                    trailing = { Text(dayCount(stats.streakDays), style = MaterialTheme.typography.titleSmall, color = Color(0xFFBD9FEA)) }
                )
                OneUIDivider(inset = 70.dp)
                OneUIRow(
                    com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.TotalReads), summary = com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.SinceStart),
                    leading = { OneUIIconBadge(Icons.Default.EmojiEvents) },
                    trailing = { Text("${stats.totalCount}", style = MaterialTheme.typography.titleSmall, color = Color(0xFFBD9FEA)) }
                )
            }
        }
        if (history.isNotEmpty()) {
            item(key = "hist-l") { SectionLabel(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.RecentReads)) }
            item(key = "hist") {
                OneUICard {
                    history.take(10).forEachIndexed { i, h ->
                        if (i > 0) OneUIDivider(inset = 70.dp)
                        val time = remember(h.timestamp) {
                            SimpleDateFormat("d MMM yyyy  hh:mm a", Locale("ar")).format(Date(h.timestamp))
                        }
                        OneUIRow(
                            titles[h.dhikrId] ?: com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.DhikrIdFmt).format(h.dhikrId), summary = time,
                            leading = { OneUIIconBadge(Icons.Default.History) }
                        )
                    }
                }
            }
        }
    }
}

private fun dayCount(n: Int) = com.example.ui.i18n.L10n.daysCount(n)

@Composable
private fun PeriodCell(label: String, value: Int, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp, lineHeight = 26.sp), color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun donutColor(i: Int): Color {
    val p = MaterialTheme.colorScheme.primary
    return when (i) { 0 -> p; 1 -> p.copy(alpha = 0.6f); else -> p.copy(alpha = 0.32f) }
}

@Composable
private fun Donut(values: List<Int>) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val colors = listOf(donutColor(0), donutColor(1), donutColor(2))
    Canvas(Modifier.fillMaxSize()) {
        val stroke = 12.dp.toPx()
        val d = size.minDimension - stroke
        val tl = Offset((size.width - d) / 2f, (size.height - d) / 2f)
        val sz = Size(d, d)
        drawArc(track, 0f, 360f, false, tl, sz, style = Stroke(stroke))
        val total = values.sum().toFloat()
        if (total > 0f) {
            var start = -90f
            values.take(3).forEachIndexed { i, v ->
                val sweep = v / total * 360f
                drawArc(colors[i], start, (sweep - 3f).coerceAtLeast(1f), false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round))
                start += sweep
            }
        }
    }
}
