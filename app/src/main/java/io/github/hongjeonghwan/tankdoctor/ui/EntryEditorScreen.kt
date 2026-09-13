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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
    val editing = state.editing
    var category by rememberSaveable { mutableStateOf(editing?.category ?: state.newCategory) }
    var note by rememberSaveable { mutableStateOf(editing?.note ?: "") }
    var epochDay by rememberSaveable { mutableLongStateOf(editing?.date?.toEpochDay() ?: LocalDate.now().toEpochDay()) }
    var showPicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val date = LocalDate.ofEpochDay(epochDay)
    val today = LocalDate.now()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editing == null) "기록 추가" else "기록 수정", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = vm::back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    if (editing != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "기록 삭제")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("무엇을 했나요?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LogCategory.userCategories.forEach { c ->
                    FilterChip(
                        selected = category == c,
                        onClick = { category = c },
                        label = { Text("${c.emoji} ${c.label}") },
                    )
                }
            }

            Text("언제 했나요?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showPicker = true }) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("${date.koreanLabel()} · ${relativeDay(daysAgo(date, today))}")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = date == today, onClick = { epochDay = today.toEpochDay() }, label = { Text("오늘") })
                FilterChip(
                    selected = date == today.minusDays(1),
                    onClick = { epochDay = today.minusDays(1).toEpochDay() },
                    label = { Text("어제") },
                )
                FilterChip(
                    selected = date == today.minusDays(2),
                    onClick = { epochDay = today.minusDays(2).toEpochDay() },
                    label = { Text("그저께") },
                )
            }

            Text("내용 (선택)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(500) },
                placeholder = { Text(category.hint) },
                minLines = 3,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
            )
            if (category.suggestions.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    category.suggestions.forEach { s ->
                        SuggestionChip(
                            onClick = { note = if (note.isBlank()) s else "${note.trimEnd()}, $s" },
                            label = { Text(s.trim()) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { vm.saveEntry(editing?.id, date, category, note) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("저장")
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showPicker) {
        val lastSelectable = (today.toEpochDay() + 1) * DAY_MILLIS - 1
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = epochDay * DAY_MILLIS,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= lastSelectable
            },
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { epochDay = Math.floorDiv(it, DAY_MILLIS) }
                    showPicker = false
                }) { Text("확인") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("취소") } },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (confirmDelete && editing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("이 기록을 삭제할까요?") },
            text = { Text("삭제하면 되돌릴 수 없어요.") },
            confirmButton = { TextButton(onClick = { vm.deleteEntry(editing.id) }) { Text("삭제") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("취소") } },
        )
    }
}
