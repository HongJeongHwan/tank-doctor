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

data class UiState(
    val screen: Screen = Screen.HOME,
    val preview: ImageBitmap? = null,
    val jpeg: ByteArray? = null,
    val tankType: TankType = TankType.FRESH,
    val memo: String = "",
    val loading: Boolean = false,
    val result: Diagnosis? = null,
    val error: String? = null,
    val settingsNotice: String? = null,
    val apiKey: String = "",
    val model: String = "",
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = SettingsStore(app)
    private val _state = MutableStateFlow(UiState(apiKey = settings.apiKey, model = settings.model))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var pendingCapture: Uri? = null

    fun newCaptureUri(): Uri {
        val app = getApplication<Application>()
        val dir = File(app.cacheDir, "images").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "tank_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
            .also { pendingCapture = it }
    }

    fun onCaptureResult(success: Boolean) {
        val uri = pendingCapture
        if (success && uri != null) onImagePicked(uri)
    }

    fun onImagePicked(uri: Uri) {
        viewModelScope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) { ImageUtil.load(getApplication(), uri) }
                _state.update { it.copy(preview = loaded.bitmap.asImageBitmap(), jpeg = loaded.jpeg, error = null) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "사진을 불러오지 못했어요: ${e.message}") }
            }
        }
    }

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
        val jpeg = s.jpeg ?: return
        if (s.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val result = GeminiClient.diagnose(s.apiKey, s.model, jpeg, s.tankType, s.memo)
                _state.update { it.copy(loading = false, result = result, screen = Screen.RESULT) }
            } catch (e: GeminiException) {
                _state.update { it.copy(loading = false, error = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "알 수 없는 오류: ${e.message}") }
            }
        }
    }
}
