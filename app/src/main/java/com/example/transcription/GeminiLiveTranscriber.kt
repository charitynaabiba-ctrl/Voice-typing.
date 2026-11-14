package com.example.transcription

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.LiveGenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AudioTranscriptionConfig
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.InlineData
import com.google.firebase.ai.type.LiveServerContent
import com.google.firebase.ai.type.LiveServerGoAway
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.TextPart
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "GeminiLiveTranscriber"

sealed interface LiveTranscriptionEvent {
  data class InterimUpdate(val text: String) : LiveTranscriptionEvent

  data class FinalSegment(val text: String) : LiveTranscriptionEvent

  data class ServerNotice(val message: String) : LiveTranscriptionEvent
}

@OptIn(PublicPreviewAPI::class)
class GeminiLiveTranscriber {

  companion object {
    const val MODEL_NAME = "gemini-3.5-transcribe-live"
    private const val CONNECT_TIMEOUT_MS = 30_000L
  }

  private val firebaseAi by lazy {
    Firebase.ai(backend = GenerativeBackend.googleAI())
  }

  private fun createLiveModel(languageHint: String): LiveGenerativeModel {
    val languageGuidance =
      if (languageHint == "auto" || languageHint.isBlank()) {
        "Automatically detect the spoken language (including multilingual code-switching)."
      } else {
        "Primary spoken language hint: $languageHint (still handle natural code-switching cleanly)."
      }

    val smartPunctuationInstruction =
      """
      You are a dedicated real-time voice typing transcription engine powered by Gemini 3.5 Transcribe Live.
      Your ONLY job is to convert the user's spoken audio into clean, ready-to-paste written text.

      SMART PUNCTUATION IS ALWAYS ON:
      - Always apply accurate punctuation automatically: periods, commas, question marks, exclamation points, colons, apostrophes, quotation marks, and dashes.
      - Always apply proper capitalization for sentence starts, proper nouns, acronyms, and the pronoun "I".
      - Remove spoken filler hesitations ("um", "uh", "er") unless clearly intentional, and cleanly resolve natural self-corrections (e.g., "Tuesday—actually make it Wednesday" -> "Wednesday").
      - Format spoken structural cues naturally when clearly intended (e.g., "new paragraph", "new line", "bullet point").
      - $languageGuidance

      STRICT OUTPUT RULES:
      - Output ONLY the transcribed, smart-punctuated text.
      - Never reply conversationally, never greet the user, never explain anything, and never wrap output in quotes or markdown.
      """.trimIndent()

    return firebaseAi.liveModel(
      modelName = MODEL_NAME,
      generationConfig =
        liveGenerationConfig {
          responseModality = ResponseModality.TEXT
          inputAudioTranscription = AudioTranscriptionConfig()
        },
      systemInstruction = content { text(smartPunctuationInstruction) },
    )
  }

  suspend fun openSession(languageHint: String): LiveSession {
    val model = createLiveModel(languageHint)
    return try {
      withTimeout(CONNECT_TIMEOUT_MS) { model.connect() }
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Connection to $MODEL_NAME timed out", e)
      throw IllegalStateException("Connection to Gemini 3.5 Transcribe Live timed out. Please try again.", e)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Failed to connect to $MODEL_NAME", e)
      throw e
    }
  }

  suspend fun sendPcmChunk(session: LiveSession, pcmBytes: ByteArray) {
    if (session.isClosed()) return
    session.sendAudioRealtime(InlineData(data = pcmBytes, mimeType = "audio/pcm"))
  }

  suspend fun finishAudioTurn(session: LiveSession) {
    if (session.isClosed()) return
    try {
      session.sendStopActivityRealtime()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(TAG, "sendStopActivityRealtime ignored on close", e)
    }
  }

  fun observeTranscriptions(session: LiveSession): Flow<LiveTranscriptionEvent> = flow {
    val modelTurnBuffer = StringBuilder()
    val inputTranscriptBuffer = StringBuilder()

    try {
      session.receive().collect { message ->
        when (message) {
          is LiveServerContent -> {
            val modelChunk =
              message.content
                ?.parts
                ?.filterIsInstance<TextPart>()
                ?.joinToString(separator = "") { it.text }
                .orEmpty()

            if (modelChunk.isNotEmpty()) {
              modelTurnBuffer.append(modelChunk)
            }

            val inputChunk = message.inputTranscription?.text.orEmpty()
            if (inputChunk.isNotEmpty()) {
              inputTranscriptBuffer.append(inputChunk)
            }

            val activeRaw =
              if (modelTurnBuffer.isNotBlank()) {
                modelTurnBuffer.toString()
              } else {
                inputTranscriptBuffer.toString()
              }

            if (activeRaw.isNotBlank()) {
              emit(LiveTranscriptionEvent.InterimUpdate(SmartPunctuationFormatter.formatLive(activeRaw)))
            }

            if (message.turnComplete || message.generationComplete) {
              if (activeRaw.isNotBlank()) {
                emit(LiveTranscriptionEvent.FinalSegment(SmartPunctuationFormatter.formatFinal(activeRaw)))
              }
              modelTurnBuffer.clear()
              inputTranscriptBuffer.clear()
            }
          }
          is LiveServerGoAway -> {
            val notice = "Live session ending soon (${message.timeLeft})."
            Log.w(TAG, notice)
            emit(LiveTranscriptionEvent.ServerNotice(notice))
          }
        }
      }

      // Flush any remaining buffered text when stream completes normally
      val trailingRaw =
        if (modelTurnBuffer.isNotBlank()) {
          modelTurnBuffer.toString()
        } else {
          inputTranscriptBuffer.toString()
        }
      if (trailingRaw.isNotBlank()) {
        emit(LiveTranscriptionEvent.FinalSegment(SmartPunctuationFormatter.formatFinal(trailingRaw)))
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Error receiving live transcription stream", e)
      throw e
    }
  }
}
