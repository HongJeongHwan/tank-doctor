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
import androidx.compose.material3.Button
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
import androidx.compose.runtime.remember
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
import io.github.hongjeonghwan.tankdoctor.Screen
import io.github.hongjeonghwan.tankdoctor.UiState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import io.github.hongjeonghwan.tankdoctor.data.MODELS
import io.github.hongjeonghwan.tankdoctor.data.TankSize
import io.github.hongjeonghwan.tankdoctor.data.FishInfo

@Composable
fun SettingsScreen(state: UiState, vm: AppViewModel) {
    var key by rememberSaveable { mutableStateOf(state.apiKey) }
    var model by rememberSaveable { mutableStateOf(state.model) }
    var showKey by rememberSaveable { mutableStateOf(false) }
    fun cm(v: Int) = if (v > 0) v.toString() else ""
    var width by rememberSaveable { mutableStateOf(cm(state.tankSize.width)) }
    var depth by rememberSaveable { mutableStateOf(cm(state.tankSize.depth)) }
    var height by rememberSaveable { mutableStateOf(cm(state.tankSize.height)) }
    var fish by remember { mutableStateOf(state.fish) }
    val size = TankSize(width.toIntOrNull() ?: 0, depth.toIntOrNull() ?: 0, height.toIntOrNull() ?: 0)
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

            Text("물고기 정보", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "현재 어항에 있는 물고기 종류와 마릿수를 입력해 주세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            fish.forEachIndexed { index, item ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = item.name,
                        onValueChange = { value -> fish = fish.toMutableList().also { it[index] = item.copy(name = value.take(30)) } },
                        label = { Text("종류") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = if (item.count > 0) item.count.toString() else "",
                        onValueChange = { value -> fish = fish.toMutableList().also { it[index] = item.copy(count = value.filter(Char::isDigit).take(3).toIntOrNull() ?: 0) } },
                        label = { Text("마릿수") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(100.dp),
                    )
                    TextButton(onClick = { fish = fish.filterIndexed { i, _ -> i != index } }) { Text("삭제") }
                }
            }
            TextButton(onClick = { fish = fish + FishInfo("", 0) }) { Text("+ 물고기 추가") }

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
                onClick = { vm.saveSettings(key, model, size, fish.filter { it.name.isNotBlank() && it.count > 0 }) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("저장")
            }
            Text(
                "진단할 때 사진이 Google Gemini API로 전송돼요. 이 앱은 사진이나 키를 따로 수집하지 않아요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
