package io.github.hongjeonghwan.tankdoctor

import android.app.Application
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.hongjeonghwan.tankdoctor.data.Diagnosis
import io.github.hongjeonghwan.tankdoctor.data.GeminiClient
import io.github.hongjeonghwan.tankdoctor.data.GeminiException
import io.github.hongjeonghwan.tankdoctor.data.ImageUtil
import io.github.hongjeonghwan.tankdoctor.data.SettingsStore
import io.github.hongjeonghwan.tankdoctor.data.TankType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class Screen { HOME, RESULT, SETTINGS }

const val MAX_PHOTOS = 5

class Photo(val id: Long, val preview: ImageBitmap, val jpeg: ByteArray)

data class UiState(
    val screen: Screen = Screen.HOME,
    val photos: List<Photo> = emptyList(),
    val selectedId: Long? = null,
    val tankType: TankType = TankType.FRESH,
    val memo: String = "",
    val loading: Boolean = false,
    val result: Diagnosis? = null,
    val error: String? = null,
    val settingsNotice: String? = null,
    val apiKey: String = "",
    val model: String = "",
) {
    val selectedPhoto: Photo? get() = photos.firstOrNull { it.id == selectedId } ?: photos.lastOrNull()
    val isFull: Boolean get() = photos.size >= MAX_PHOTOS
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = SettingsStore(app)
    private val _state = MutableStateFlow(UiState(apiKey = settings.apiKey, model = settings.model))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var pendingCapture: Uri? = null
    private var nextPhotoId = 0L

    fun newCaptureUri(): Uri {
        val app = getApplication<Application>()
        val dir = File(app.cacheDir, "images").apply { mkdirs() }
        // Earlier captures are already decoded into memory, so the files can go.
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "tank_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
            .also { pendingCapture = it }
    }

    fun onCaptureResult(success: Boolean) {
        val uri = pendingCapture
        if (success && uri != null) addPhotos(listOf(uri))
    }

    fun addPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val room = MAX_PHOTOS - _state.value.photos.size
        if (room <= 0) {
            showError("사진은 최대 ${MAX_PHOTOS}장까지 넣을 수 있어요.")
            return
        }
        val picked = uris.take(room)
        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) {
                picked.map { uri -> runCatching { ImageUtil.load(getApplication(), uri) } }
            }
            val photos = results.mapNotNull { it.getOrNull() }
                .map { Photo(nextPhotoId++, it.bitmap.asImageBitmap(), it.jpeg) }
            val failures = results.mapNotNull { it.exceptionOrNull() }
            val skipped = uris.size - picked.size
            val message = listOfNotNull(
                failures.firstOrNull()?.let { e ->
                    "사진 ${failures.size}장을 불러오지 못했어요. (${e.message ?: e.javaClass.simpleName})"
                },
                if (skipped > 0) "최대 ${MAX_PHOTOS}장까지라 ${skipped}장은 빠졌어요." else null,
            ).joinToString(" ").ifBlank { null }
            _state.update { s ->
                s.copy(
                    photos = (s.photos + photos).take(MAX_PHOTOS),
                    selectedId = photos.lastOrNull()?.id ?: s.selectedId,
                    error = message,
                )
            }
        }
    }

    fun selectPhoto(id: Long) = _state.update { it.copy(selectedId = id) }

    fun removePhoto(id: Long) = _state.update { s ->
        val rest = s.photos.filterNot { it.id == id }
        s.copy(photos = rest, selectedId = if (s.selectedId == id) rest.lastOrNull()?.id else s.selectedId)
    }

    fun clearPhotos() = _state.update { it.copy(photos = emptyList(), selectedId = null) }

    fun setTankType(type: TankType) = _state.update { it.copy(tankType = type) }

    fun setMemo(memo: String) = _state.update { it.copy(memo = memo.take(500)) }

    fun showError(message: String) = _state.update { it.copy(error = message) }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun open(screen: Screen) = _state.update { it.copy(screen = screen, settingsNotice = null) }

    fun saveSettings(apiKey: String, model: String) {
        settings.apiKey = apiKey.trim()
        settings.model = model
        _state.update {
            it.copy(apiKey = settings.apiKey, model = settings.model, screen = Screen.HOME, settingsNotice = null, error = null)
        }
    }

    fun diagnose() {
        val s = _state.value
        if (s.apiKey.isBlank()) {
            _state.update { it.copy(screen = Screen.SETTINGS, settingsNotice = "진단하려면 먼저 Gemini API 키를 입력해 주세요.") }
            return
        }
        if (s.photos.isEmpty() || s.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val result = GeminiClient.diagnose(s.apiKey, s.model, s.photos.map { it.jpeg }, s.tankType, s.memo)
                _state.update { it.copy(loading = false, result = result, screen = Screen.RESULT) }
            } catch (e: GeminiException) {
                _state.update { it.copy(loading = false, error = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "알 수 없는 오류: ${e.message}") }
            }
        }
    }
}
