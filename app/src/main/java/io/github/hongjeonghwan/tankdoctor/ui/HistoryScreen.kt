@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.hongjeonghwan.tankdoctor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.hongjeonghwan.tankdoctor.AppViewModel
import io.github.hongjeonghwan.tankdoctor.UiState
import io.github.hongjeonghwan.tankdoctor.data.LogEntry
import io.github.hongjeonghwan.tankdoctor.data.daysAgo
import io.github.hongjeonghwan.tankdoctor.data.koreanLabel
import io.github.hongjeonghwan.tankdoctor.data.relativeDay
import io.github.hongjeonghwan.tankdoctor.ui.theme.color

/** Every saved AI diagnosis in one place, newest first, with how the score has moved. */
@Composable
fun HistoryScreen(state: UiState, vm: AppViewModel) {
    val diagnoses = state.diagnoses

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("진단 기록", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = vm::back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (diagnoses.isEmpty()) {
                item {
                    Text(
                        "아직 진단 기록이 없어요.\n사진으로 진단하면 결과가 여기에 쌓여요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                    )
                }
            } else {
                item(key = "trend") { TrendCard(diagnoses) }
                items(diagnoses, key = { it.id }) { entry ->
                    HistoryRow(entry) { vm.openEntry(entry) }
                }
            }
        }
    }
}

/** Score over time as a bar per diagnosis, oldest on the left. */
@Composable
private fun TrendCard(diagnoses: List<LogEntry>) {
    // Pair each entry with its parsed result so scores, colours and dates stay aligned.
    val points = diagnoses.take(12).reversed().mapNotNull { e -> e.diagnosis?.let { e to it } }
    if (points.isEmpty()) return
    val scores = points.map { it.second.score }
    val colors = points.map { it.second.level.color() }
    val track = MaterialTheme.colorScheme.surfaceContainerHigh

    SectionCard("점수 추이") {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            val gap = 8.dp.toPx()
            val barWidth = ((size.width - gap * (scores.size - 1)) / scores.size).coerceAtLeast(2f)
            val radius = CornerRadius(barWidth.coerceAtMost(12f) / 2)
            scores.forEachIndexed { i, score ->
                val x = i * (barWidth + gap)
                val height = size.height * (score / 100f).coerceIn(0.02f, 1f)
                drawRoundRect(
                    color = track,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = radius,
                )
                drawRoundRect(
                    color = colors.getOrElse(i) { Color.Gray },
                    topLeft = Offset(x, size.height - height),
                    size = Size(barWidth, height),
                    cornerRadius = radius,
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Text(
                points.first().first.date.koreanLabel(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                "최근 ${scores.last()}점",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val change = if (scores.size >= 2) scores.last() - scores.first() else 0
        Text(
            when {
                scores.size < 2 -> "진단이 쌓이면 나아지고 있는지 보여 드릴게요."
                change > 0 -> "처음 기록보다 ${change}점 좋아졌어요."
                change < 0 -> "처음 기록보다 ${-change}점 낮아졌어요."
                else -> "처음 기록과 점수가 같아요."
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun HistoryRow(entry: LogEntry, onOpen: () -> Unit) {
    val d = entry.diagnosis
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${d?.score ?: 0}",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = d?.level?.color() ?: MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("점", style = MaterialTheme.typography.labelSmall)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "${entry.date.koreanLabel()} · ${relativeDay(daysAgo(entry.date))}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    d?.headline.orEmpty().ifBlank { "결과를 읽지 못했어요" },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                d?.let {
                    Text(
                        it.level.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = it.level.color(),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
