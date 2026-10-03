@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.hongjeonghwan.tankdoctor.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.hongjeonghwan.tankdoctor.AppViewModel
import io.github.hongjeonghwan.tankdoctor.BuildConfig
import io.github.hongjeonghwan.tankdoctor.Screen
import io.github.hongjeonghwan.tankdoctor.UiState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import io.github.hongjeonghwan.tankdoctor.data.MODELS
import io.github.hongjeonghwan.tankdoctor.data.TankSize
import io.github.hongjeonghwan.tankdoctor.data.WaterSchedule
import io.github.hongjeonghwan.tankdoctor.data.dueLabel
import io.github.hongjeonghwan.tankdoctor.data.koreanLabel
import io.github.hongjeonghwan.tankdoctor.data.lastWaterChange
import io.github.hongjeonghwan.tankdoctor.data.minuteLabel
import io.github.hongjeonghwan.tankdoctor.ui.theme.color

@Composable
fun SettingsScreen(state: UiState, vm: AppViewModel) {
    var key by rememberSaveable { mutableStateOf(state.apiKey) }
    var model by rememberSaveable { mutableStateOf(state.model) }
    var showKey by rememberSaveable { mutableStateOf(false) }
    fun cm(v: Int) = if (v > 0) v.toString() else ""
    var width by rememberSaveable { mutableStateOf(cm(state.tankSize.width)) }
    var depth by rememberSaveable { mutableStateOf(cm(state.tankSize.depth)) }
    var height by rememberSaveable { mutableStateOf(cm(state.tankSize.height)) }
    val size = TankSize(width.toIntOrNull() ?: 0, depth.toIntOrNull() ?: 0, height.toIntOrNull() ?: 0)
    var waterDays by rememberSaveable { mutableStateOf(state.waterSchedule.intervalDays.takeIf { it > 0 }?.toString().orEmpty()) }
    var waterNotify by rememberSaveable { mutableStateOf(state.waterSchedule.notify) }
    var weekdayMinute by rememberSaveable { mutableStateOf(state.waterSchedule.weekdayMinute) }
    var holidayMinute by rememberSaveable { mutableStateOf(state.waterSchedule.holidayMinute) }
    /** Which time the picker edits: true for 주말·공휴일, false for 평일, null when closed. */
    var pickingHoliday by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var notifyDenied by rememberSaveable { mutableStateOf(false) }
    val water = WaterSchedule(waterDays.toIntOrNull() ?: 0, waterNotify, weekdayMinute, holidayMinute)
    val context = LocalContext.current
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        waterNotify = granted
        notifyDenied = !granted
    }
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = vm::back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            state.settingsNotice?.let {
                NoticeCard(
                    title = "API 키를 입력해 주세요",
                    body = it,
                    actionLabel = "키 발급받기",
                    onAction = { uriHandler.openUri("https://aistudio.google.com/apikey") },
                )
            }

            SettingsGroup("환수 주기") {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("며칠마다 물을 갈아 줄까요?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = waterDays,
                            onValueChange = { v -> waterDays = v.filter { it.isDigit() }.take(2) },
                            placeholder = { Text("끄기") },
                            suffix = { Text("일") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(104.dp),
                        )
                        listOf(3, 7, 14).forEach { d ->
                            IntervalButton("${d}일", selected = water.intervalDays == d) { waterDays = d.toString() }
                        }
                    }
                    val due = water.dueDate(lastWaterChange(state.entries))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            if (due != null) "다음 환수 ${due.koreanLabel()} · ${dueLabel(due)}"
                            else "칸을 비우면 환수 주기와 알림이 꺼져요",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            SettingsGroup("알림") {
                Row(
                    Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("환수 알림", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (water.isSet) "예정일에 알리고, 기록할 때까지 매일 다시 알려요" else "환수 주기를 먼저 정해 주세요",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = waterNotify && water.isSet,
                        enabled = water.isSet,
                        onCheckedChange = { on ->
                            notifyDenied = false
                            val needsAsk = on && Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                                PackageManager.PERMISSION_GRANTED
                            if (needsAsk) askNotify.launch(Manifest.permission.POST_NOTIFICATIONS) else waterNotify = on
                        },
                    )
                }
                if (waterNotify && water.isSet) {
                    GroupDivider()
                    TimeRow("평일", "월–금", minuteLabel(weekdayMinute), holiday = false) { pickingHoliday = false }
                    GroupDivider()
                    TimeRow("휴일", "토·일·공휴일 (대체공휴일 포함)", minuteLabel(holidayMinute), holiday = true) { pickingHoliday = true }
                }
                if (notifyDenied) {
                    Text(
                        "알림 권한이 꺼져 있어요. 휴대폰 설정 > 앱 > 어항닥터에서 알림을 허용해 주세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 14.dp),
                    )
                }
            }

            SettingsGroup("어항") {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("어항 크기", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "가로·세로(폭)·높이를 cm로 적으면 AI가 과밀 여부와 환수·약품 양을 리터 기준으로 알려줘요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple("가로", width) { v: String -> width = v },
                            Triple("세로", depth) { v: String -> depth = v },
                            Triple("높이", height) { v: String -> height = v },
                        ).forEach { (label, value, onChange) ->
                            OutlinedTextField(
                                value = value,
                                onValueChange = { v -> onChange(v.filter { it.isDigit() }.take(3)) },
                                label = { Text(label) },
                                suffix = { Text("cm") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Text(
                        if (size.isSet) "물 용량 약 ${size.liters}L" else "세 칸을 모두 채우면 용량이 계산돼요",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (size.isSet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                GroupDivider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        // Save first: leaving this screen drops anything typed above.
                        .clickable {
                            vm.saveSettings(key, model, size, water)
                            vm.openFishEditor()
                        }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("사는 생물", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            state.livingFish.joinToString(" · ") { "${it.name} ${it.count}마리" }
                                .ifBlank { "사진으로 물고기를 등록해 보세요" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    state.stocking?.let {
                        Text(
                            "${it.percent}% · ${it.level.label}",
                            style = MaterialTheme.typography.labelLarge,
                            color = it.level.tone.color(),
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SettingsGroup("AI 분석") {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Gemini API 키", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Google AI Studio에서 무료로 발급받을 수 있어요. 키는 이 휴대폰에만 저장돼요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it.trim() },
                        label = { Text("API 키") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (showKey) "키 숨기기" else "키 보기",
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(
                        onClick = { uriHandler.openUri("https://aistudio.google.com/apikey") },
                        contentPadding = PaddingValues(horizontal = 0.dp),
                    ) {
                        Text("무료 API 키 발급받기 →")
                    }
                }
                GroupDivider()
                Column(Modifier.padding(vertical = 8.dp)) {
                    Text(
                        "분석 모델",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    )
                    MODELS.forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(selected = model == option.id, onClick = { model = option.id }, role = Role.RadioButton)
                                .padding(horizontal = 18.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = model == option.id, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(option.label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    option.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { vm.saveSettings(key, model, size, water) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("저장", fontWeight = FontWeight.SemiBold)
            }

            SettingsGroup("앱 정보") {
                Row(
                    Modifier.padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("현재 버전 v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { vm.checkUpdate(manual = true) }, enabled = state.updateProgress == null) {
                        Text("업데이트 확인")
                    }
                }
                if (state.update == null) {
                    state.updateNotice?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 14.dp),
                        )
                    }
                }
            }
            if (state.update != null) UpdateCard(state, vm, showLater = false)

            Text(
                "진단할 때 사진이 Google Gemini API로 전송돼요. 이 앱은 사진이나 키를 따로 수집하지 않아요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    pickingHoliday?.let { holiday ->
        val current = if (holiday) holidayMinute else weekdayMinute
        val picker = rememberTimePickerState(current / 60, current % 60, is24Hour = false)
        AlertDialog(
            onDismissRequest = { pickingHoliday = null },
            title = { Text(if (holiday) "휴일 알림 시각" else "평일 알림 시각") },
            text = { TimePicker(state = picker) },
            confirmButton = {
                TextButton(onClick = {
                    val picked = picker.hour * 60 + picker.minute
                    if (holiday) holidayMinute = picked else weekdayMinute = picked
                    pickingHoliday = null
                }) { Text("확인") }
            },
            dismissButton = { TextButton(onClick = { pickingHoliday = null }) { Text("취소") } },
        )
    }
}

/** A small grey heading over one white card, as in the grouped settings design. */
@Composable
private fun SettingsGroup(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            content = content,
        )
    }
}

@Composable
private fun GroupDivider() = HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)

@Composable
private fun RowScope.IntervalButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    if (selected) {
        Button(onClick = onClick, shape = shape, modifier = Modifier.weight(1f).height(52.dp), contentPadding = PaddingValues(0.dp)) {
            Text(label, fontWeight = FontWeight.SemiBold)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            shape = shape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.weight(1f).height(52.dp),
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(label)
        }
    }
}

/** One reminder time: a tinted day-kind tag, what it covers, and the time to tap. */
@Composable
private fun TimeRow(tag: String, covers: String, time: String, holiday: Boolean, onClick: () -> Unit) {
    val (tint, ink) = if (holiday) {
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "$tag 알림 시각 바꾸기", onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).background(tint, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Text(tag, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ink)
        }
        Text(covers, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(time, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}
