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
import io.github.hongjeonghwan.tankdoctor.data.LogCategory
import io.github.hongjeonghwan.tankdoctor.data.LogEntry
import io.github.hongjeonghwan.tankdoctor.data.LogStore
import io.github.hongjeonghwan.tankdoctor.data.SettingsStore
import io.github.hongjeonghwan.tankdoctor.data.TankSize
import io.github.hongjeonghwan.tankdoctor.data.TankType
import io.github.hongjeonghwan.tankdoctor.data.daysAgo
import io.github.hongjeonghwan.tankdoctor.data.relativeDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

enum class Screen { LOG, EDITOR, DIAGNOSE, RESULT, SETTINGS }

const val MAX_PHOTOS = 5
const val HISTORY_DAYS = 30L

class Photo(val id: Long, val preview: ImageBitmap, val jpeg: ByteArray)

data class UiState(
    val screen: Screen = Screen.LOG,
    val settingsReturn: Screen = Screen.LOG,
    // care log
    val entries: List<LogEntry> = emptyList(),
    val filter: LogCategory? = null,
    val editing: LogEntry? = null,
    val newCategory: LogCategory = LogCategory.WATER,
    // diagnosis input
    val photos: List<Photo> = emptyList(),
    val selectedId: Long? = null,
    val tankType: TankType = TankType.FRESH,
    val tankSize: TankSize = TankSize(),
    val memo: String = "",
    val loading: Boolean = false,
    // diagnosis output
    val result: Diagnosis? = null,
    val resultPhotos: List<Photo> = emptyList(),
    val resultEntryId: Long? = null,
    val justSaved: Boolean = false,
    // misc
    val error: String? = null,
    val settingsNotice: String? = null,
    val apiKey: String = "",
    val model: String = "",
) {
    val selectedPhoto: Photo? get() = photos.firstOrNull { it.id == selectedId } ?: photos.lastOrNull()
    val isFull: Boolean get() = photos.size >= MAX_PHOTOS
    val recentEntries: List<LogEntry> get() = entries.filter { daysAgo(it.date) <= HISTORY_DAYS }
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = SettingsStore(app)
    private val store = LogStore(app)
    val photoDir: File get() = store.photoDir

    private val _state = MutableStateFlow(
        UiState(apiKey = settings.apiKey, model = settings.model, tankType = settings.tankType, tankSize = settings.tankSize)
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var pendingCapture: Uri? = null
    private var nextPhotoId = 0L

    init {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { store.load() }
            _state.update { it.copy(entries = sorted(loaded)) }
        }
    }

    // ---------- navigation ----------

    fun open(screen: Screen) = _state.update {
        it.copy(
            screen = screen,
            settingsReturn = if (screen == Screen.SETTINGS && it.screen != Screen.SETTINGS) it.screen else it.settingsReturn,
            settingsNotice = null,
        )
    }

    fun back() = _state.update { s ->
        when (s.screen) {
            Screen.SETTINGS -> s.copy(screen = s.settingsReturn, settingsNotice = null)
            Screen.RESULT -> s.copy(screen = Screen.LOG, result = null, resultPhotos = emptyList(), resultEntryId = null, justSaved = false)
            else -> s.copy(screen = Screen.LOG, error = null)
        }
    }

    // ---------- care log ----------

    fun setFilter(category: LogCategory?) = _state.update { it.copy(filter = category) }

    fun startNewEntry(category: LogCategory = LogCategory.WATER) =
        _state.update { it.copy(screen = Screen.EDITOR, editing = null, newCategory = category) }

    fun openEntry(entry: LogEntry) {
        if (entry.category == LogCategory.DIAGNOSIS) {
            openDiagnosisEntry(entry)
        } else {
            _state.update { it.copy(screen = Screen.EDITOR, editing = entry) }
        }
    }

    fun saveEntry(id: Long?, date: LocalDate, category: LogCategory, note: String) {
        val current = _state.value.entries
        val updated = if (id == null) {
            current + LogEntry(newId(), date, category, note.trim())
        } else {
            current.map { if (it.id == id) it.copy(date = date, category = category, note = note.trim()) else it }
        }
        commit(updated)
        _state.update { it.copy(screen = Screen.LOG, editing = null) }
    }

    fun deleteEntry(id: Long) {
        val target = _state.value.entries.firstOrNull { it.id == id } ?: return
        commit(_state.value.entries.filterNot { it.id == id })
        if (target.photos.isNotEmpty()) store.deletePhotosAsync(target.photos)
        _state.update {
            it.copy(screen = Screen.LOG, editing = null, result = null, resultPhotos = emptyList(), resultEntryId = null)
        }
    }

    private fun openDiagnosisEntry(entry: LogEntry) {
        val diagnosis = entry.diagnosis ?: return showError("저장된 진단 결과를 읽지 못했어요.")
        viewModelScope.launch {
            val photos = withContext(Dispatchers.IO) {
                entry.photos.mapIndexedNotNull { i, name ->
                    store.readPhoto(name)?.let { (bmp, bytes) -> Photo(i.toLong(), bmp.asImageBitmap(), bytes) }
                }
            }
            _state.update {
                it.copy(screen = Screen.RESULT, result = diagnosis, resultPhotos = photos, resultEntryId = entry.id, justSaved = false)
            }
        }
    }

    private fun commit(entries: List<LogEntry>) {
        val list = sorted(entries)
        _state.update { it.copy(entries = list) }
        store.saveAsync(list)
    }

    private fun sorted(entries: List<LogEntry>) =
        entries.sortedWith(compareByDescending<LogEntry> { it.date }.thenByDescending { it.id })

    private fun newId(): Long =
        maxOf(System.currentTimeMillis(), (_state.value.entries.maxOfOrNull { it.id } ?: 0L) + 1)

    /** Recent log as plain lines for the prompt, e.g. "- 9월 12일(어제) [환수] 30% 환수". */
    private fun historyText(entries: List<LogEntry>): String = entries.take(60).joinToString("\n") { e ->
        val whenText = "${e.date.monthValue}월 ${e.date.dayOfMonth}일(${relativeDay(daysAgo(e.date))})"
        val body = if (e.category == LogCategory.DIAGNOSIS) {
            e.diagnosis?.let { "${it.score}점 ${it.level.label} - ${it.headline}" } ?: "진단"
        } else {
            e.note.ifBlank { "(내용 없음)" }
        }
        "- $whenText [${e.category.label}] $body"
    }

    // ---------- diagnosis input ----------

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

    fun setTankType(type: TankType) {
        settings.tankType = type
        _state.update { it.copy(tankType = type) }
    }

    fun setMemo(memo: String) = _state.update { it.copy(memo = memo.take(500)) }

    fun showError(message: String) = _state.update { it.copy(error = message) }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun saveSettings(apiKey: String, model: String, tankSize: TankSize) {
        settings.apiKey = apiKey.trim()
        settings.model = model
        settings.tankSize = tankSize
        _state.update {
            it.copy(
                apiKey = settings.apiKey,
                model = settings.model,
                tankSize = tankSize,
                screen = it.settingsReturn,
                settingsNotice = null,
                error = null,
            )
        }
    }

    fun diagnose() {
        val s = _state.value
        if (s.apiKey.isBlank()) {
            _state.update {
                it.copy(
                    screen = Screen.SETTINGS,
                    settingsReturn = Screen.DIAGNOSE,
                    settingsNotice = "진단하려면 먼저 Gemini API 키를 입력해 주세요.",
                )
            }
            return
        }
        if (s.photos.isEmpty() || s.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val jpegs = s.photos.map { it.jpeg }
                val result = GeminiClient.diagnose(
                    s.apiKey, s.model, jpegs, s.tankType, s.memo, historyText(s.recentEntries),
                    tankSize = if (s.tankSize.isSet) s.tankSize.label else "",
                )
                val id = newId()
                val names = withContext(Dispatchers.IO) { store.savePhotos(id, jpegs) }
                commit(
                    _state.value.entries + LogEntry(
                        id = id,
                        date = LocalDate.now(),
                        category = LogCategory.DIAGNOSIS,
                        note = s.memo.trim(),
                        photos = names,
                        diagnosisJson = result.raw,
                    )
                )
                _state.update {
                    it.copy(
                        loading = false,
                        screen = Screen.RESULT,
                        result = result,
                        resultPhotos = s.photos,
                        resultEntryId = id,
                        justSaved = true,
                        photos = emptyList(),
                        selectedId = null,
                        memo = "",
                    )
                }
            } catch (e: GeminiException) {
                _state.update { it.copy(loading = false, error = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "알 수 없는 오류: ${e.message}") }
            }
        }
    }
}
