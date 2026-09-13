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
import io.github.hongjeonghwan.tankdoctor.data.MODELS

@Composable
fun SettingsScreen(state: UiState, vm: AppViewModel) {
    var key by rememberSaveable { mutableStateOf(state.apiKey) }
    var model by rememberSaveable { mutableStateOf(state.model) }
    var showKey by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { vm.open(Screen.HOME) }) {
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
                onClick = { vm.saveSettings(key, model) },
                enabled = key.isNotBlank(),
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
