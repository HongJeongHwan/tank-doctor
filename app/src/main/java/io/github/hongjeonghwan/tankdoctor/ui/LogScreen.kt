@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.hongjeonghwan.tankdoctor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.hongjeonghwan.tankdoctor.AppViewModel
import io.github.hongjeonghwan.tankdoctor.Screen
import io.github.hongjeonghwan.tankdoctor.UiState
import io.github.hongjeonghwan.tankdoctor.data.Level
import io.github.hongjeonghwan.tankdoctor.data.LogCategory
import io.github.hongjeonghwan.tankdoctor.data.LogEntry
import io.github.hongjeonghwan.tankdoctor.data.TankSize
import io.github.hongjeonghwan.tankdoctor.data.FishInfo
import io.github.hongjeonghwan.tankdoctor.data.daysAgo
import io.github.hongjeonghwan.tankdoctor.data.koreanLabel
import io.github.hongjeonghwan.tankdoctor.data.relativeDay
import io.github.hongjeonghwan.tankdoctor.ui.theme.color
import java.io.File

@Composable
fun LogScreen(state: UiState, vm: AppViewModel) {
    val visible = state.entries.filter { state.filter == null || it.category == state.filter }
    val grouped = visible.groupBy { it.date }
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<LogEntry?>(null) }

    // After a save/delete: jump to the list (index 3 = first item after summary, callout, filters) and confirm.
    LaunchedEffect(state.toast) {
        val message = state.toast ?: return@LaunchedEffect
        listState.animateScrollToItem(3)
        snackbar.showSnackbar(message)
        vm.consumeToast()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("어항닥터", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { vm.open(Screen.SETTINGS) }) {
                        Icon(Icons.Filled.Settings, contentDescription = "설정")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.startNewEntry(state.filter?.takeIf { it != LogCategory.DIAGNOSIS } ?: LogCategory.WATER) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("기록 추가") },
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "summary") { SummaryCard(state.entries, state.tankSize, state.fish) { vm.open(Screen.SETTINGS) } }
            item(key = "diagnose") { DiagnoseCallout(recentCount = state.recentEntries.size) { vm.open(Screen.DIAGNOSE) } }
            item(key = "filter") { FilterRow(state.filter, vm::setFilter) }

            if (visible.isEmpty()) {
                item(key = "empty") { EmptyLog(filtered = state.filter != null) }
            }
            grouped.forEach { (date, list) ->
                item(key = "h$date") {
                    val days = daysAgo(date)
                    Text(
                        if (days <= 1) "${relativeDay(days)} · ${date.koreanLabel()}" else "${date.koreanLabel()} · ${relativeDay(days)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(list, key = { it.id }) { entry ->
                    EntryRow(
                        entry = entry,
                        photoDir = vm.photoDir,
                        onOpen = { vm.openEntry(entry) },
                        onDelete = { pendingDelete = entry },
                    )
                }
            }
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("이 기록을 삭제할까요?") },
            text = {
                Text("${target.date.koreanLabel()} · ${target.category.emoji} ${target.category.label}\n삭제하면 되돌릴 수 없어요.")
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteEntry(target.id)
                    pendingDelete = null
                }) { Text("삭제") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("취소") } },
        )
    }
}

@Composable
private fun SummaryCard(entries: List<LogEntry>, tankSize: TankSize, fish: List<FishInfo>, onEditTank: () -> Unit) {
    SectionCard("어항 한눈에 보기") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📐  어항 크기", modifier = Modifier.weight(1f))
            if (tankSize.isSet) {
                Text(tankSize.label, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onEditTank))
            } else {
                Text(
                    "등록하기 ›",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onEditTank),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🐟  물고기", modifier = Modifier.weight(1f))
            Text(
                fish.joinToString(" · ") { "${it.name} ${it.count}마리" }.ifBlank { "등록하기 ›" },
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onEditTank),
            )
        }
        SummaryRow(entries, LogCategory.WATER, "마지막 환수", warnAfterDays = 14)
        SummaryRow(entries, LogCategory.PLANT, "마지막 수초 작업", warnAfterDays = null)
        SummaryRow(entries, LogCategory.TEST, "마지막 수질검사", warnAfterDays = 30)
        val lastDiagnosis = entries.firstOrNull { it.category == LogCategory.DIAGNOSIS }
        val d = lastDiagnosis?.diagnosis
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${LogCategory.DIAGNOSIS.emoji}  최근 진단", modifier = Modifier.weight(1f))
            if (lastDiagnosis != null && d != null) {
                Text(
                    "${d.score}점 · ${d.level.label} (${relativeDay(daysAgo(lastDiagnosis.date))})",
                    color = d.level.color(),
                    fontWeight = FontWeight.SemiBold,
                )
            } else {
                Text("기록 없음", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SummaryRow(entries: List<LogEntry>, category: LogCategory, label: String, warnAfterDays: Int?) {
    val last = entries.firstOrNull { it.category == category }
    val days = last?.let { daysAgo(it.date) }
    val overdue = warnAfterDays != null && days != null && days > warnAfterDays
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("${category.emoji}  $label", modifier = Modifier.weight(1f))
        Text(
            when {
                days == null -> "기록 없음"
                overdue -> "${relativeDay(days)} · 할 때가 됐어요"
                else -> relativeDay(days)
            },
            color = when {
                days == null -> MaterialTheme.colorScheme.onSurfaceVariant
                overdue -> Level.CAUTION.color()
                else -> MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (days != null) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun DiagnoseCallout(recentCount: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("🩺", fontSize = 30.sp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("문제가 있어 보이나요?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (recentCount > 0) "사진을 찍으면 최근 기록 ${recentCount}개와 함께 AI가 진단해요"
                    else "사진을 찍으면 AI가 어항 상태를 진단해요",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun FilterRow(selected: LogCategory?, onSelect: (LogCategory?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("전체") })
        }
        items(LogCategory.entries) { c ->
            FilterChip(
                selected = selected == c,
                onClick = { onSelect(if (selected == c) null else c) },
                label = { Text("${c.emoji} ${c.label}") },
            )
        }
    }
}

@Composable
private fun EmptyLog(filtered: Boolean) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("📒", fontSize = 40.sp)
        Text(
            if (filtered) "이 카테고리의 기록이 아직 없어요." else "아직 기록이 없어요.",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "환수, 수초, 물고기 변화를 기록해 두면\nAI가 문제의 원인을 더 정확히 찾아요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EntryRow(entry: LogEntry, photoDir: File, onOpen: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(entry.category.emoji, fontSize = 20.sp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(entry.category.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                val d = entry.diagnosis
                if (entry.category == LogCategory.DIAGNOSIS && d != null) {
                    Badge("${d.score}점 · ${d.level.label}", d.level.color())
                    Text(d.headline, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (entry.note.isNotBlank()) {
                        Text(
                            "메모: ${entry.note}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    Text(
                        entry.note.ifBlank { "(내용 없음)" },
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            entry.photos.firstOrNull()?.let { name ->
                FileThumb(File(photoDir, name), 56.dp)
            }
            Box {
                var menuOpen by remember { mutableStateOf(false) }
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "수정·삭제 메뉴")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(if (entry.category == LogCategory.DIAGNOSIS) "결과 보기" else "수정") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onOpen()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("삭제") },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}
