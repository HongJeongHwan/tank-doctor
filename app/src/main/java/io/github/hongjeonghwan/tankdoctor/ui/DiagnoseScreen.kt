@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.hongjeonghwan.tankdoctor.ui

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.hongjeonghwan.tankdoctor.AppViewModel
import io.github.hongjeonghwan.tankdoctor.MAX_PHOTOS
import io.github.hongjeonghwan.tankdoctor.Screen
import io.github.hongjeonghwan.tankdoctor.UiState
import io.github.hongjeonghwan.tankdoctor.data.TankType

@Composable
fun DiagnoseScreen(state: UiState, vm: AppViewModel) {
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        vm.onCaptureResult(ok)
    }
    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS)) { uris ->
        vm.addPhotos(uris)
    }
    val selected = state.selectedPhoto

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("사진으로 진단", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = vm::back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.open(Screen.SETTINGS) }) {
                        Icon(Icons.Filled.Settings, contentDescription = "설정")
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val recent = state.recentEntries.size
            Text(
                if (recent > 0) "📒 최근 30일 관리 기록 ${recent}개를 사진과 함께 보내서 원인을 찾아요."
                else "📒 관리 기록을 남겨 두면 AI가 원인을 더 정확히 찾아요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.stocking?.let {
                Text(
                    "🐠 사는 생물 ${it.totalCount}마리 · 밀집도 ${it.percent}%(${it.level.label})도 함께 보내요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            PhotoBox(selected?.preview)

            if (state.photos.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "사진 ${state.photos.size}/$MAX_PHOTOS",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = vm::clearPhotos, enabled = !state.loading) { Text("모두 지우기") }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(state.photos, key = { _, p -> p.id }) { i, photo ->
                            PhotoThumb(
                                photo = photo,
                                number = i + 1,
                                selected = photo.id == selected?.id,
                                onClick = { vm.selectPhoto(photo.id) },
                                onRemove = if (state.loading) null else ({ vm.removePhoto(photo.id) }),
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = {
                        try {
                            takePicture.launch(vm.newCaptureUri())
                        } catch (e: ActivityNotFoundException) {
                            vm.showError("카메라 앱을 찾을 수 없어요. 갤러리에서 선택해 주세요.")
                        }
                    },
                    enabled = !state.isFull && !state.loading,
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("촬영")
                }
                OutlinedButton(
                    onClick = {
                        pickImages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    enabled = !state.isFull && !state.loading,
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("갤러리")
                }
            }
            Text(
                "정면 전체 1장 + 아픈 물고기·이끼·수초 근접 사진을 더하면 더 정확해요. (최대 ${MAX_PHOTOS}장)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("어항 종류", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TankType.entries.forEach { type ->
                        FilterChip(
                            selected = state.tankType == type,
                            onClick = { vm.setTankType(type) },
                            label = { Text(type.label) },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.memo,
                onValueChange = vm::setMemo,
                label = { Text("증상·상황 메모 (선택)") },
                placeholder = { Text("예: 구피가 바닥에 가라앉아 있어요. 환수한 지 2주 됐어요.") },
                minLines = 2,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.apiKey.isBlank()) {
                NoticeCard(
                    title = "Gemini API 키가 필요해요",
                    body = "설정에서 무료 API 키를 한 번만 입력하면 돼요.",
                    actionLabel = "설정 열기",
                    onAction = { vm.open(Screen.SETTINGS) },
                )
            }

            state.error?.let { ErrorCard(it, onDismiss = vm::dismissError) }

            Button(
                onClick = vm::diagnose,
                enabled = state.photos.isNotEmpty() && !state.loading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = LocalContentColor.current,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("분석 중… 10~40초 걸려요")
                } else {
                    val label = when (state.photos.size) {
                        0 -> "먼저 사진을 골라 주세요"
                        1 -> "진단하기"
                        else -> "사진 ${state.photos.size}장으로 진단하기"
                    }
                    Text(label, fontSize = 17.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PhotoBox(preview: ImageBitmap?) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (preview != null) {
            Image(
                bitmap = preview,
                contentDescription = "선택한 어항 사진",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(24.dp),
            ) {
                Icon(
                    Icons.Outlined.AddAPhoto,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp),
                )
                Text(
                    "조명을 켠 상태로\n어항 정면 전체가 보이게 찍어 주세요",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
