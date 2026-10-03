@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.hongjeonghwan.tankdoctor.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.hongjeonghwan.tankdoctor.AppViewModel
import io.github.hongjeonghwan.tankdoctor.Screen
import io.github.hongjeonghwan.tankdoctor.UiState
import io.github.hongjeonghwan.tankdoctor.data.Level
import io.github.hongjeonghwan.tankdoctor.data.LogCategory
import io.github.hongjeonghwan.tankdoctor.data.LogEntry
import io.github.hongjeonghwan.tankdoctor.data.WaterSchedule
import io.github.hongjeonghwan.tankdoctor.data.daysAgo
import io.github.hongjeonghwan.tankdoctor.data.isHoliday
import io.github.hongjeonghwan.tankdoctor.data.koreanLabel
import io.github.hongjeonghwan.tankdoctor.data.lastWaterChange
import io.github.hongjeonghwan.tankdoctor.data.minuteLabel
import io.github.hongjeonghwan.tankdoctor.data.relativeDay
import io.github.hongjeonghwan.tankdoctor.ui.theme.HeroMuted
import io.github.hongjeonghwan.tankdoctor.ui.theme.color
import io.github.hongjeonghwan.tankdoctor.ui.theme.heroColor
import java.io.File
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/** How many entries the home screen lists before pointing at the full log. */
private const val HOME_RECENT = 5

/** Water-test entries older than this read as due. */
private const val TEST_DUE_DAYS = 30

@Composable
fun LogScreen(state: UiState, vm: AppViewModel) {
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<LogEntry?>(null) }

    // After a save/delete: jump to 최근 기록 (after banner, hero, tiles, callout) and confirm.
    LaunchedEffect(state.toast) {
        val message = state.toast ?: return@LaunchedEffect
        listState.animateScrollToItem(if (state.update != null) 5 else 4)
        snackbar.showSnackbar(message)
        vm.consumeToast()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { HomeHeader(onSettings = { vm.open(Screen.SETTINGS) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.startNewEntry() },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("기록 추가", fontWeight = FontWeight.SemiBold) },
                shape = RoundedCornerShape(18.dp),
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.update != null) {
                item(key = "update") { UpdateCard(state, vm) }
            }
            item(key = "hero") {
                WaterHero(
                    entries = state.entries,
                    water = state.waterSchedule,
                    onDone = vm::logWaterToday,
                    onSetup = { vm.open(Screen.SETTINGS) },
                )
            }
            item(key = "tiles") { StatusTiles(state, vm) }
            item(key = "diagnose") { DiagnoseCallout(recentCount = state.recentEntries.size) { vm.open(Screen.DIAGNOSE) } }
            item(key = "recent") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    Text("최근 기록", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (state.entries.size > HOME_RECENT) {
                        TextButton(onClick = { vm.open(Screen.RECORDS) }) { Text("전체 ${state.entries.size}개 보기") }
                    }
                }
            }
            if (state.entries.isEmpty()) {
                item(key = "empty") { EmptyRecords(filtered = false) }
            }
            recordDays(state.entries.take(HOME_RECENT), vm.photoDir, onOpen = vm::openEntry, onDelete = { pendingDelete = it })
            if (state.entries.size > HOME_RECENT) {
                item(key = "more") {
                    OutlinedButton(
                        onClick = { vm.open(Screen.RECORDS) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("이전 기록 ${state.entries.size - HOME_RECENT}개 더 보기")
                    }
                }
            }
        }
    }

    pendingDelete?.let { target ->
        DeleteEntryDialog(
            target,
            onConfirm = {
                vm.deleteEntry(target.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun HomeHeader(onSettings: () -> Unit) {
    val today = LocalDate.now()
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "${today.monthValue}월 ${today.dayOfMonth}일 ${today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("어항닥터", fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp)
        }
        OutlinedIconButton(
            onClick = onSettings,
            modifier = Modifier.size(44.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = IconButtonDefaults.outlinedIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Icon(Icons.Outlined.Settings, contentDescription = "설정")
        }
    }
}

/** The 다음 환수 card: countdown, progress through the cycle and a one-tap log. */
@Composable
private fun WaterHero(entries: List<LogEntry>, water: WaterSchedule, onDone: () -> Unit, onSetup: () -> Unit) {
    val today = LocalDate.now()
    val last = lastWaterChange(entries)
    val due = water.dueDate(last, today)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = heroColor(), contentColor = Color.White),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("다음 환수", style = MaterialTheme.typography.labelLarge, color = HeroMuted, modifier = Modifier.weight(1f))
                if (due != null && water.notify) {
                    Icon(Icons.Outlined.NotificationsNone, contentDescription = null, tint = HeroMuted, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${if (isHoliday(due)) "휴일" else "평일"} ${minuteLabel(water.minuteOn(due))}",
                        style = MaterialTheme.typography.labelMedium,
                        color = HeroMuted,
                    )
                }
            }
            if (due == null) {
                Text("환수 주기를 정해 보세요", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    last?.let { "마지막 환수 ${relativeDay(daysAgo(it))} · 주기를 정하면 날짜를 세서 알려줘요" }
                        ?: "주기를 정하면 마지막 환수부터 날짜를 세서 알려줘요",
                    style = MaterialTheme.typography.bodySmall,
                    color = HeroMuted,
                )
            } else {
                val left = ChronoUnit.DAYS.between(today, due)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        when {
                            left > 0 -> "D-$left"
                            left == 0L -> "오늘"
                            else -> "${-left}일 지남"
                        },
                        fontSize = if (left >= 0) 52.sp else 40.sp,
                        lineHeight = 52.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1.5).sp,
                    )
                    Text(due.koreanLabel(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 6.dp))
                }
                val elapsed = last?.let { daysAgo(it) } ?: 0L
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(
                        progress = { (elapsed.toFloat() / water.intervalDays).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.22f),
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                    Text(
                        last?.let { "${water.intervalDays}일 주기 중 ${elapsed}일째 · 마지막 환수 ${it.monthValue}월 ${it.dayOfMonth}일" }
                            ?: "${water.intervalDays}일 주기 · 아직 환수 기록이 없어요",
                        style = MaterialTheme.typography.bodySmall,
                        color = HeroMuted,
                    )
                }
            }
            val doneToday = last == today
            Button(
                onClick = if (due == null) onSetup else onDone,
                enabled = due == null || !doneToday,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = heroColor(),
                    disabledContainerColor = Color.White.copy(alpha = 0.18f),
                    disabledContentColor = Color.White,
                ),
            ) {
                Text(
                    when {
                        due == null -> "주기 정하기"
                        doneToday -> "오늘 환수를 기록했어요"
                        else -> "오늘 환수했어요"
                    },
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/** Two-by-two status: last diagnosis, stocking, water test, livestock. */
@Composable
private fun StatusTiles(state: UiState, vm: AppViewModel) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val link = MaterialTheme.colorScheme.primary
    val lastDiagnosis = state.entries.firstOrNull { it.category == LogCategory.DIAGNOSIS }
    val d = lastDiagnosis?.diagnosis
    val stocking = state.stocking
    val testDays = state.entries.firstOrNull { it.category == LogCategory.TEST }?.let { daysAgo(it.date) }
    val fish = state.livingFish
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (lastDiagnosis != null && d != null) {
                StatusTile("최근 진단 · ${relativeDay(daysAgo(lastDiagnosis.date))}", "${d.score}", "점", d.level.label, d.level.color()) {
                    vm.open(Screen.HISTORY)
                }
            } else {
                StatusTile("최근 진단", "—", "", "사진으로 진단 ›", link) { vm.open(Screen.DIAGNOSE) }
            }
            when {
                stocking != null -> StatusTile(
                    "밀집도 · ${state.tankSize.liters}L", "${stocking.percent}", "%", stocking.level.label, stocking.level.tone.color(),
                    onClick = vm::openFishEditor,
                )
                state.tankSize.isSet ->
                    StatusTile("밀집도 · ${state.tankSize.liters}L", "—", "", "생물 등록하기 ›", link, onClick = vm::openFishEditor)
                else -> StatusTile("밀집도", "—", "", "어항 크기부터 ›", link) { vm.open(Screen.SETTINGS) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (testDays == null) {
                StatusTile("수질검사", "—", "", "기록 없음", muted) { vm.startNewEntry(LogCategory.TEST) }
            } else {
                val due = testDays > TEST_DUE_DAYS
                StatusTile(
                    "수질검사",
                    if (testDays <= 0) "오늘" else "$testDays",
                    if (testDays <= 0) "" else "일 전",
                    if (due) "할 때가 됐어요" else "잘 하고 있어요",
                    if (due) Level.CAUTION.color() else Level.GOOD.color(),
                ) { vm.startNewEntry(LogCategory.TEST) }
            }
            if (fish.isEmpty()) {
                StatusTile("사는 생물", "—", "", "등록하기 ›", link, onClick = vm::openFishEditor)
            } else {
                StatusTile("사는 생물", "${fish.sumOf { it.count }}", "마리", fish.joinToString(" · ") { it.name }, muted, onClick = vm::openFishEditor)
            }
        }
        val portraits = fish.filter { it.photo.isNotBlank() }
        if (portraits.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(portraits, key = { it.photo }) { f ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(56.dp).clickable(onClick = vm::openFishEditor),
                    ) {
                        FileThumb(File(vm.speciesDir, f.photo), 52.dp, sample = 1)
                        Text(f.name, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.StatusTile(
    label: String,
    value: String,
    unit: String,
    status: String,
    statusColor: Color,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildAnnotatedString {
                    append(value)
                    withStyle(SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)) { append(unit) }
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                status,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = statusColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DiagnoseCallout(recentCount: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("사진으로 AI 진단", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    if (recentCount > 0) "최근 기록 ${recentCount}개와 함께 분석해요" else "사진을 찍으면 어항 상태를 진단해요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
