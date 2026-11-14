package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioStreamRecorder
import com.example.data.DictationPreferences
import com.example.data.DictationPreferencesRepository
import com.example.transcription.GeminiLiveTranscriber
import com.example.transcription.LiveTranscriptionEvent
import com.example.transcription.SmartPunctuationFormatter
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val TAG = "VoiceTypingViewModel"
const val WAVEFORM_BAR_COUNT = 28

sealed interface SessionStatus {
  data object Idle : SessionStatus

  data object Connecting : SessionStatus

  data object Listening : SessionStatus

  data object Finalizing : SessionStatus

  data class Error(val message: String) : SessionStatus
}

data class DictationSnippet(
  val id: String = UUID.randomUUID().toString(),
  val text: String,
  val timestampLabel: String,
  val wordCount: Int,
)

data class DictationUiState(
  val committedText: String = "",
  val interimText: String = "",
  val sessionStatus: SessionStatus = SessionStatus.Idle,
  val amplitudeLevels: List<Float> = List(WAVEFORM_BAR_COUNT) { 0.06f },
  val elapsedSeconds: Long = 0L,
  val canUndo: Boolean = false,
  val preferences: DictationPreferences = DictationPreferences(),
  val sessionSnippets: List<DictationSnippet> = emptyList(),
  val autoCopyEventId: Int = 0,
) {
  val isListening: Boolean
    get() = sessionStatus is SessionStatus.Listening

  val isConnecting: Boolean
    get() = sessionStatus is SessionStatus.Connecting

  val isFinalizing: Boolean
    get() = sessionStatus is SessionStatus.Finalizing

  val isSessionActive: Boolean
    get() = isListening || isConnecting || isFinalizing

  val combinedPreviewText: String
    get() =
      if (interimText.isBlank()) {
        committedText
      } else {
        SmartPunctuationFormatter.joinWithExisting(committedText, interimText)
      }

  val wordCount: Int
    get() {
      val content = combinedPreviewText.trim()
      if (content.isEmpty()) return 0
      return content.split(Regex("""\s+""")).size
    }

  val characterCount: Int
    get() = combinedPreviewText.length
}

@OptIn(PublicPreviewAPI::class)
class VoiceTypingViewModel(application: Application) : AndroidViewModel(application) {

  private val recorder = AudioStreamRecorder(application.applicationContext)
  private val transcriber = GeminiLiveTranscriber()
  private val preferencesRepo = DictationPreferencesRepository(application.applicationContext)

  private val _uiState = MutableStateFlow(DictationUiState())
  val uiState: StateFlow<DictationUiState> = _uiState.asStateFlow()

  private val undoHistory = ArrayDeque<String>()

  private var activeSession: LiveSession? = null
  private var sessionJob: Job? = null
  private var audioSendJob: Job? = null
  private var receiveJob: Job? = null
  private var timerJob: Job? = null

  init {
    viewModelScope.launch {
      preferencesRepo.preferences.collect { prefs ->
        _uiState.update { it.copy(preferences = prefs) }
      }
    }
  }

  fun hasRecordPermission(): Boolean = recorder.hasRecordPermission()

  fun toggleDictation() {
    if (_uiState.value.isSessionActive) {
      stopDictation()
    } else {
      startDictation()
    }
  }

  fun startDictation() {
    if (_uiState.value.isSessionActive) return
    if (!recorder.hasRecordPermission()) {
      _uiState.update {
        it.copy(
          sessionStatus =
            SessionStatus.Error("Microphone permission is required before starting voice typing.")
        )
      }
      return
    }

    sessionJob?.cancel()
    sessionJob =
      viewModelScope.launch {
        _uiState.update {
          it.copy(
            sessionStatus = SessionStatus.Connecting,
            interimText = "",
            elapsedSeconds = 0L,
          )
        }

        try {
          val languageHint = _uiState.value.preferences.languageCode
          val session = transcriber.openSession(languageHint)
          activeSession = session

          _uiState.update { it.copy(sessionStatus = SessionStatus.Listening) }
          startTimer()

          receiveJob =
            launch {
              transcriber.observeTranscriptions(session).collect { event ->
                when (event) {
                  is LiveTranscriptionEvent.InterimUpdate -> {
                    _uiState.update { it.copy(interimText = event.text) }
                  }
                  is LiveTranscriptionEvent.FinalSegment -> {
                    commitSegment(event.text)
                  }
                  is LiveTranscriptionEvent.ServerNotice -> {
                    Log.w(TAG, event.message)
                  }
                }
              }
            }

          audioSendJob =
            launch {
              recorder.streamAudio().collect { frame ->
                _uiState.update { state ->
                  val updatedLevels =
                    (state.amplitudeLevels.drop(1) + maxOf(0.06f, frame.normalizedAmplitude))
                  state.copy(amplitudeLevels = updatedLevels)
                }
                transcriber.sendPcmChunk(session, frame.pcmBytes)
              }
            }

          audioSendJob?.join()
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.e(TAG, "Live voice typing session error", e)
          flushTrailingInterim()
          cleanupSessionResources()
          _uiState.update {
            it.copy(
              sessionStatus =
                SessionStatus.Error(
                  e.localizedMessage
                    ?: "Could not connect to Gemini 3.5 Transcribe Live. Check your network and try again."
                ),
              amplitudeLevels = List(WAVEFORM_BAR_COUNT) { 0.06f },
            )
          }
        }
      }
  }

  fun stopDictation() {
    if (!_uiState.value.isSessionActive) return

    viewModelScope.launch {
      _uiState.update { it.copy(sessionStatus = SessionStatus.Finalizing) }

      timerJob?.cancel()
      audioSendJob?.cancel()
      audioSendJob = null

      val session = activeSession
      if (session != null) {
        transcriber.finishAudioTurn(session)
        // Allow brief window for final punctuated segment from the live stream
        withTimeoutOrNull(650L) { receiveJob?.join() }
      }

      flushTrailingInterim()
      cleanupSessionResources()
      sessionJob?.cancel()
      sessionJob = null

      val shouldAutoCopy =
        _uiState.value.preferences.autoCopyOnStop && _uiState.value.committedText.isNotBlank()

      _uiState.update { state ->
        state.copy(
          sessionStatus = SessionStatus.Idle,
          amplitudeLevels = List(WAVEFORM_BAR_COUNT) { 0.06f },
          autoCopyEventId = if (shouldAutoCopy) state.autoCopyEventId + 1 else state.autoCopyEventId,
        )
      }
    }
  }

  private fun startTimer() {
    timerJob?.cancel()
    timerJob =
      viewModelScope.launch {
        while (true) {
          delay(1_000L)
          _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1L) }
        }
      }
  }

  private fun commitSegment(segment: String) {
    val finalized = SmartPunctuationFormatter.formatFinal(segment)
    if (finalized.isBlank()) {
      _uiState.update { it.copy(interimText = "") }
      return
    }
    _uiState.update { state ->
      pushUndoSnapshot(state.committedText)
      val merged = SmartPunctuationFormatter.joinWithExisting(state.committedText, finalized)
      state.copy(
        committedText = merged,
        interimText = "",
        canUndo = undoHistory.isNotEmpty(),
      )
    }
  }

  private fun flushTrailingInterim() {
    val trailing = _uiState.value.interimText
    if (trailing.isNotBlank()) {
      commitSegment(trailing)
    }
  }

  private suspend fun cleanupSessionResources() {
    timerJob?.cancel()
    timerJob = null
    receiveJob?.cancel()
    receiveJob = null
    val session = activeSession
    activeSession = null
    if (session != null) {
      try {
        session.stopReceiving()
      } catch (e: Exception) {
        Log.w(TAG, "stopReceiving ignored", e)
      }
      try {
        if (!session.isClosed()) {
          session.close()
        }
      } catch (e: Exception) {
        Log.w(TAG, "session.close ignored", e)
      }
    }
  }

  fun onTranscriptEdited(newText: String) {
    // Allow direct user cursor editing in the pad while keeping undo clean
    _uiState.update { it.copy(committedText = newText) }
  }

  fun appendInlineText(textToAppend: String) {
    _uiState.update { state ->
      pushUndoSnapshot(state.committedText)
      val updated =
        when {
          textToAppend == "\n" -> "${state.committedText.trimEnd(' ')}\n"
          textToAppend in listOf(",", ".", "?", "!", "—") -> {
            val base = state.committedText.trimEnd(' ')
            if (textToAppend == "—") "$base — " else "$base$textToAppend "
          }
          else -> state.committedText + textToAppend
        }
      state.copy(committedText = updated, canUndo = undoHistory.isNotEmpty())
    }
  }

  fun undoLastChange() {
    val previous = undoHistory.removeLastOrNull() ?: return
    _uiState.update {
      it.copy(
        committedText = previous,
        interimText = "",
        canUndo = undoHistory.isNotEmpty(),
      )
    }
  }

  fun clearTranscript() {
    val current = _uiState.value.committedText
    if (current.isNotBlank()) {
      pushUndoSnapshot(current)
    }
    _uiState.update {
      it.copy(
        committedText = "",
        interimText = "",
        canUndo = undoHistory.isNotEmpty(),
      )
    }
  }

  fun saveCurrentSnippet() {
    val text = _uiState.value.combinedPreviewText.trim()
    if (text.isEmpty()) return
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    val snippet =
      DictationSnippet(
        text = text,
        timestampLabel = formatter.format(Date()),
        wordCount = text.split(Regex("""\s+""")).size,
      )
    _uiState.update { state ->
      state.copy(sessionSnippets = listOf(snippet) + state.sessionSnippets)
    }
  }

  fun restoreSnippet(snippet: DictationSnippet) {
    _uiState.update { state ->
      if (state.committedText.isNotBlank()) {
        pushUndoSnapshot(state.committedText)
      }
      state.copy(
        committedText = snippet.text,
        interimText = "",
        canUndo = undoHistory.isNotEmpty(),
      )
    }
  }

  fun deleteSnippet(id: String) {
    _uiState.update { state ->
      state.copy(sessionSnippets = state.sessionSnippets.filterNot { it.id == id })
    }
  }

  fun dismissError() {
    if (_uiState.value.sessionStatus is SessionStatus.Error) {
      _uiState.update { it.copy(sessionStatus = SessionStatus.Idle) }
    }
  }

  fun setLanguageCode(code: String) {
    viewModelScope.launch { preferencesRepo.setLanguageCode(code) }
  }

  fun setAutoCopyOnStop(enabled: Boolean) {
    viewModelScope.launch { preferencesRepo.setAutoCopyOnStop(enabled) }
  }

  fun setHapticFeedback(enabled: Boolean) {
    viewModelScope.launch { preferencesRepo.setHapticFeedback(enabled) }
  }

  fun setKeepScreenOn(enabled: Boolean) {
    viewModelScope.launch { preferencesRepo.setKeepScreenOn(enabled) }
  }

  private fun pushUndoSnapshot(snapshot: String) {
    if (undoHistory.lastOrNull() == snapshot) return
    if (undoHistory.size >= 25) {
      undoHistory.removeFirst()
    }
    undoHistory.addLast(snapshot)
  }

  override fun onCleared() {
    super.onCleared()
    timerJob?.cancel()
    audioSendJob?.cancel()
    receiveJob?.cancel()
    sessionJob?.cancel()
    activeSession?.let { session ->
      try {
        session.stopReceiving()
      } catch (_: Exception) {
      }
    }
  }
}
