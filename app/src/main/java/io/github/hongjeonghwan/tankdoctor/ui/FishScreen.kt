@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.hongjeonghwan.tankdoctor.ui

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.hongjeonghwan.tankdoctor.AppViewModel
import io.github.hongjeonghwan.tankdoctor.MAX_FISH_PHOTOS
import io.github.hongjeonghwan.tankdoctor.Screen
import io.github.hongjeonghwan.tankdoctor.UiState
import io.github.hongjeonghwan.tankdoctor.data.FishInfo
import io.github.hongjeonghwan.tankdoctor.data.FishKind
import io.github.hongjeonghwan.tankdoctor.data.FishScan
import io.github.hongjeonghwan.tankdoctor.data.TankType
import io.github.hongjeonghwan.tankdoctor.data.trimNumber

/** Registers what lives in the tank, by photo or by hand, and shows the resulting density. */
@Composable
fun FishScreen(state: UiState, vm: AppViewModel) {
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        vm.onCaptureResult(ok)
    }
    val pickImages = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_FISH_PHOTOS)
    ) { uris -> vm.addFishPhotos(uris) }
    val busy = state.scanning

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("물고기 등록", fontWeight = FontWeight.Bold) },
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
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionCard("📸  사진으로 인식하기") {
                Text(
                    "어항 사진을 넣으면 AI가 종류를 구분해서 마릿수를 세어 줘요. " +
                        "물고기가 흩어져 있을 때 한 장, 무리가 잘 보일 때 한 장이면 충분해요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.fishPhotos.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(state.fishPhotos, key = { _, p -> p.id }) { i, photo ->
                            PhotoThumb(
                                photo = photo,
                                number = i + 1,
                                onRemove = if (busy) null else ({ vm.removeFishPhoto(photo.id) }),
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(
                        onClick = {
                            try {
                                takePicture.launch(vm.newFishCaptureUri())
                            } catch (e: ActivityNotFoundException) {
                                vm.showError("카메라 앱을 찾을 수 없어요. 갤러리에서 선택해 주세요.")
                            }
                        },
                        enabled = state.fishPhotos.size < MAX_FISH_PHOTOS && !busy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("촬영")
                    }
                    OutlinedButton(
                        onClick = {
                            pickImages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        enabled = state.fishPhotos.size < MAX_FISH_PHOTOS && !busy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("갤러리")
                    }
                }
                Button(
                    onClick = vm::scanFish,
                    enabled = state.fishPhotos.isNotEmpty() && !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = LocalContentColor.current,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("세는 중… 10~30초 걸려요")
                    } else {
                        Text(
                            if (state.fishPhotos.isEmpty()) "먼저 사진을 넣어 주세요"
                            else "사진 ${state.fishPhotos.size}장에서 생물 찾기",
                            fontSize = 16.sp,
                        )
                    }
                }
            }

            state.error?.let { ErrorCard(it, onDismiss = vm::dismissError) }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                Text(
                    "종류에 따라 같은 용량이라도 권장 마릿수가 달라져요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            StockingCard(state.draftStocking, state.tankType) { vm.open(Screen.SETTINGS) }

            Text("사는 생물", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (state.fishDraft.isEmpty()) {
                Text(
                    "아직 등록된 생물이 없어요. 위에서 사진으로 인식하거나 아래에서 직접 추가해 주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.fishDraft.forEachIndexed { index, item ->
                FishRow(
                    item = item,
                    onChange = { vm.setFishRow(index, it) },
                    onRemove = { vm.removeFishRow(index) },
                )
            }
            TextButton(onClick = vm::addFishRow) { Text("+ 직접 추가") }

            Button(
                onClick = vm::saveFish,
                enabled = !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                Text("저장", fontSize = 17.sp)
            }
            Text(
                "밀집도는 성어 크기 기준의 어림값이에요. 여과 성능과 수초 양에 따라 실제 한계는 달라져요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    state.scanResult?.let { scan ->
        ScanDialog(
            scan = scan,
            hasExisting = state.fishDraft.any { it.isValid },
            onMerge = { vm.applyScan(replace = false) },
            onReplace = { vm.applyScan(replace = true) },
            onDismiss = vm::dismissScan,
        )
    }
}

@Composable
private fun FishRow(item: FishInfo, onChange: (FishInfo) -> Unit, onRemove: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = item.name,
                    onValueChange = { onChange(item.copy(name = it.take(30))) },
                    label = { Text("종류") },
                    placeholder = { Text("구피") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Close, contentDescription = "${item.name.ifBlank { "이 줄" }} 삭제")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KindPicker(item.kind) { onChange(item.copy(kind = it)) }
                OutlinedTextField(
                    value = if (item.count > 0) item.count.toString() else "",
                    onValueChange = { v -> onChange(item.copy(count = v.filter(Char::isDigit).take(3).toIntOrNull() ?: 0)) },
                    label = { Text("마릿수") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = if (item.sizeCm > 0) trimNumber(item.sizeCm) else "",
                    onValueChange = { v ->
                        val cleaned = v.filter { c -> c.isDigit() || c == '.' }.take(4)
                        onChange(item.copy(sizeCm = cleaned.toDoubleOrNull()?.coerceIn(0.0, 200.0) ?: 0.0))
                    },
                    label = { Text("성어") },
                    suffix = { Text("cm") },
                    placeholder = { Text(trimNumber(item.kind.defaultSizeCm)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun KindPicker(kind: FishKind, onPick: (FishKind) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Text("${kind.emoji} ${kind.label}")
            Icon(Icons.Filled.ArrowDropDown, contentDescription = "종류 바꾸기")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            FishKind.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text("${option.emoji} ${option.label}") },
                    onClick = {
                        onPick(option)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ScanDialog(
    scan: FishScan,
    hasExisting: Boolean,
    onMerge: () -> Unit,
    onReplace: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("이렇게 보여요") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                scan.species.forEach { found ->
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${found.info.kind.emoji} ${found.info.name} ${found.info.count}마리",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                found.confidence.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (found.note.isNotBlank()) {
                            Text(
                                found.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (scan.note.isNotBlank()) {
                    Text(
                        scan.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "가져온 뒤에도 마릿수와 크기는 직접 고칠 수 있어요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onReplace) { Text(if (hasExisting) "이 목록으로 바꾸기" else "가져오기") }
        },
        dismissButton = {
            if (hasExisting) {
                TextButton(onClick = onMerge) { Text("기존 목록에 더하기") }
            } else {
                TextButton(onClick = onDismiss) { Text("취소") }
            }
        },
    )
}
