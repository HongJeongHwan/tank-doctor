@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.hongjeonghwan.tankdoctor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import io.github.hongjeonghwan.tankdoctor.data.FishChange
import io.github.hongjeonghwan.tankdoctor.data.FishEvent
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.hongjeonghwan.tankdoctor.AppViewModel
import io.github.hongjeonghwan.tankdoctor.UiState
import io.github.hongjeonghwan.tankdoctor.data.LogCategory
import io.github.hongjeonghwan.tankdoctor.data.daysAgo
import io.github.hongjeonghwan.tankdoctor.data.koreanLabel
import io.github.hongjeonghwan.tankdoctor.data.relativeDay
import java.time.LocalDate

private const val DAY_MILLIS = 86_400_000L

@Composable
fun EntryEditorScreen(state: UiState, vm: AppViewModel) {
    val draft = state.draft
    val isEdit = draft.editingId != null
    val count = draft.selected.size
    val canSave = count > 0
    var showPicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val today = LocalDate.now()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "기록 수정" else "기록 추가", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = vm::back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    TextButton(onClick = vm::saveDraft, enabled = canSave) { Text("저장", fontWeight = FontWeight.Bold) }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (isEdit) {
                        OutlinedButton(
                            onClick = { confirmDelete = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.height(52.dp),
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("삭제")
                        }
                    }
                    Button(onClick = vm::saveDraft, enabled = canSave, modifier = Modifier.weight(1f).height(52.dp)) {
                        Text(
                            when {
                                isEdit -> "수정 내용 저장"
                                count == 0 -> "무엇을 했는지 골라 주세요"
                                count == 1 -> "저장"
                                else -> "기록 ${count}개 저장"
                            }
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("언제 했나요?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = { showPicker = true }) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("${draft.date.koreanLabel()} · ${relativeDay(daysAgo(draft.date, today))}")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("오늘" to 0L, "어제" to 1L, "그저께" to 2L).forEach { (label, back) ->
                    val d = today.minusDays(back)
                    FilterChip(selected = draft.date == d, onClick = { vm.setDraftDate(d) }, label = { Text(label) })
                }
            }

            Text(
                if (isEdit) "무엇을 했나요?" else "무엇을 했나요? (여러 개 고를 수 있어요)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LogCategory.userCategories.forEach { c ->
                    FilterChip(
                        selected = c in draft.selected,
                        onClick = { vm.toggleDraftCategory(c) },
                        label = { Text("${c.emoji} ${c.label}") },
                    )
                }
            }

            if (draft.selected.isEmpty()) {
                Text(
                    "위에서 한 가지 이상 골라 주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            draft.selected.forEach { c ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("${c.emoji} ${c.label}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (c == LogCategory.FISH) {
                            Text(
                                "들어오거나 나간 생물을 적어 주세요. 환경설정에 등록한 마릿수에 이 기록이 더해져요.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            draft.changes.forEachIndexed { index, change ->
                                ChangeRow(
                                    change = change,
                                    onChange = { vm.setDraftChange(index, it) },
                                    onRemove = { vm.removeDraftChange(index) },
                                )
                            }
                            TextButton(onClick = vm::addDraftChange) { Text("+ 생물 추가·폐사 줄 넣기") }
                        }
                        OutlinedTextField(
                            value = draft.notes[c].orEmpty(),
                            onValueChange = { vm.setDraftNote(c, it) },
                            placeholder = { Text("${c.hint} (선택)") },
                            minLines = 2,
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (c.suggestions.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                c.suggestions.forEach { s ->
                                    SuggestionChip(onClick = { vm.appendDraftSuggestion(c, s) }, label = { Text(s.trim()) })
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showPicker) {
        val lastSelectable = (today.toEpochDay() + 1) * DAY_MILLIS - 1
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = draft.date.toEpochDay() * DAY_MILLIS,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= lastSelectable
            },
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { vm.setDraftDate(LocalDate.ofEpochDay(Math.floorDiv(it, DAY_MILLIS))) }
                    showPicker = false
                }) { Text("확인") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("취소") } },
        ) {
            DatePicker(state = pickerState)
        }
    }

    val editingId = draft.editingId
    if (confirmDelete && editingId != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("이 기록을 삭제할까요?") },
            text = { Text("삭제하면 되돌릴 수 없어요.") },
            confirmButton = { TextButton(onClick = { vm.deleteEntry(editingId) }) { Text("삭제") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("취소") } },
        )
    }
}

/** One stocking line: which species, how many, and whether it came in or went out. */
@Composable
private fun ChangeRow(change: FishChange, onChange: (FishChange) -> Unit, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = change.name,
            onValueChange = { onChange(change.copy(name = it.take(30))) },
            label = { Text("종류") },
            placeholder = { Text("구피") },
            singleLine = true,
            modifier = Modifier.weight(1.2f),
        )
        OutlinedTextField(
            value = if (change.count > 0) change.count.toString() else "",
            onValueChange = { v -> onChange(change.copy(count = v.filter(Char::isDigit).take(3).toIntOrNull() ?: 0)) },
            label = { Text("마릿수") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(0.9f),
        )
        EventPicker(change.event) { onChange(change.copy(event = it)) }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, contentDescription = "이 줄 지우기")
        }
    }
}

@Composable
private fun EventPicker(event: FishEvent, onPick: (FishEvent) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) { Text(event.label) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            FishEvent.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text("${option.label} (${if (option.sign > 0) "+" else "-"})") },
                    onClick = {
                        onPick(option)
                        open = false
                    },
                )
            }
        }
    }
}
