@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.hongjeonghwan.tankdoctor.ui

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
import androidx.compose.material3.FilterChip
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
    var waterMinute by rememberSaveable { mutableStateOf(state.waterSchedule.minuteOfDay) }
    var pickingTime by rememberSaveable { mutableStateOf(false) }
    var notifyDenied by rememberSaveable { mutableStateOf(false) }
    val water = WaterSchedule(waterDays.toIntOrNull() ?: 0, waterNotify, waterMinute)
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.settingsNotice?.let {
                NoticeCard(
                    title = "API 키를 입력해 주세요",
                    body = it,
                    actionLabel = "키 발급받기",
                    onAction = { uriHandler.openUri("https://aistudio.google.com/apikey") },
                )
            }

            Text("어항 크기", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "가로·세로(폭)·높이를 cm로 적어 주세요. AI가 과밀 여부와 환수·약품 양을 리터 기준으로 알려줘요.",
                style = MaterialTheme.typography.bodyMedium,
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

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text("환수 주기", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "며칠마다 물을 갈아 줄지 정해 주세요. 마지막 환수 기록부터 날짜를 세서 알려줘요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = waterDays,
                onValueChange = { v -> waterDays = v.filter { it.isDigit() }.take(2) },
                label = { Text("주기 (비우면 끄기)") },
                suffix = { Text("일마다") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 7, 14).forEach { d ->
                    FilterChip(
                        selected = water.intervalDays == d,
                        onClick = { waterDays = d.toString() },
                        label = { Text("${d}일") },
                    )
                }
            }
            water.dueDate(lastWaterChange(state.entries))?.let { due ->
                Text(
                    "다음 환수 ${due.koreanLabel()} · ${dueLabel(due)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("환수 알림", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (water.isSet) "예정일 ${water.timeLabel}에 알려주고, 기록할 때까지 매일 다시 알려줘요."
                        else "주기를 먼저 정해 주세요.",
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
                OutlinedButton(onClick = { pickingTime = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("알림 시각 · ${water.timeLabel}")
                }
            }
            if (notifyDenied) {
                Text(
                    "알림 권한이 꺼져 있어요. 휴대폰 설정 > 앱 > 어항닥터에서 알림을 허용해 주세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text("사는 생물", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                state.livingFish.joinToString(" · ") { "${it.name} ${it.count}마리" }
                    .ifBlank { "아직 등록된 생물이 없어요." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.stocking?.let {
                Text(
                    "밀집도 ${it.percent}% · ${it.level.label}",
                    style = MaterialTheme.typography.labelLarge,
                    color = it.level.tone.color(),
                )
            }
            OutlinedButton(
                // Save first: leaving this screen drops anything typed above.
                onClick = {
                    vm.saveSettings(key, model, size, water)
                    vm.openFishEditor()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("사진으로 물고기 등록하기 →")
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text("Gemini API 키", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Google AI Studio에서 무료로 발급받을 수 있어요. 키는 이 휴대폰에만 저장돼요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = key,
                onValueChange = { key = it.trim() },
                label = { Text("API 키") },
                singleLine = true,
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
            TextButton(onClick = { uriHandler.openUri("https://aistudio.google.com/apikey") }) {
                Text("무료 API 키 발급받기 →")
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text("분석 모델", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            MODELS.forEach { option ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected = model == option.id, onClick = { model = option.id }, role = Role.RadioButton)
                        .padding(vertical = 6.dp),
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

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { vm.saveSettings(key, model, size, water) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("저장")
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "현재 버전 v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { vm.checkUpdate(manual = true) }, enabled = state.updateProgress == null) {
                    Text("업데이트 확인")
                }
            }
            if (state.update != null) {
                UpdateCard(state, vm, showLater = false)
            } else {
                state.updateNotice?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                "진단할 때 사진이 Google Gemini API로 전송돼요. 이 앱은 사진이나 키를 따로 수집하지 않아요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    if (pickingTime) {
        val picker = rememberTimePickerState(waterMinute / 60, waterMinute % 60, is24Hour = false)
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            title = { Text("알림 시각") },
            text = { TimePicker(state = picker) },
            confirmButton = {
                TextButton(onClick = {
                    waterMinute = picker.hour * 60 + picker.minute
                    pickingTime = false
                }) { Text("확인") }
            },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text("취소") } },
        )
    }
}
