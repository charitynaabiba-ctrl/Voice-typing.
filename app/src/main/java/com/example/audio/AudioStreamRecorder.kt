package com.example.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min
import kotlin.math.sqrt

private const val TAG = "AudioStreamRecorder"

data class AudioFrame(
  val pcmBytes: ByteArray,
  val normalizedAmplitude: Float,
)

class AudioStreamRecorder(private val context: Context) {

  companion object {
    const val SAMPLE_RATE_HZ = 16_000
    const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
    // ~80ms chunk at 16kHz mono 16-bit PCM = 2560 bytes (1280 samples)
    private const val CHUNK_SIZE_BYTES = 2_560
  }

  fun hasRecordPermission(): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
      PackageManager.PERMISSION_GRANTED

  fun streamAudio(): Flow<AudioFrame> =
    flow {
      if (!hasRecordPermission()) {
        throw SecurityException("Microphone permission is required for live voice typing.")
      }

      val minBufferSize =
        AudioRecord.getMinBufferSize(SAMPLE_RATE_HZ, CHANNEL_CONFIG, AUDIO_ENCODING)
      if (minBufferSize <= 0) {
        throw IllegalStateException("Audio device returned invalid buffer size ($minBufferSize).")
      }

      val bufferSize = maxOf(minBufferSize * 2, CHUNK_SIZE_BYTES * 2)
      val audioRecord = createAudioRecord(bufferSize)

      if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
        audioRecord.release()
        throw IllegalStateException("Unable to initialize microphone recorder.")
      }

      val buffer = ByteArray(CHUNK_SIZE_BYTES)
      try {
        audioRecord.startRecording()
        if (audioRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
          throw IllegalStateException("Microphone recording failed to start.")
        }

        while (currentCoroutineContext().isActive) {
          val bytesRead = audioRecord.read(buffer, 0, buffer.size)
          if (bytesRead > 0) {
            val chunk = buffer.copyOf(bytesRead)
            val amplitude = computeNormalizedRms(chunk, bytesRead)
            emit(AudioFrame(pcmBytes = chunk, normalizedAmplitude = amplitude))
          } else if (bytesRead < 0) {
            Log.w(TAG, "AudioRecord.read returned error code: $bytesRead")
            throw IllegalStateException("Microphone stream interrupted (code $bytesRead).")
          }
        }
      } finally {
        try {
          if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            audioRecord.stop()
          }
        } catch (e: IllegalStateException) {
          Log.w(TAG, "AudioRecord stop ignored", e)
        }
        audioRecord.release()
      }
    }
      .flowOn(Dispatchers.IO)

  @Suppress("MissingPermission")
  private fun createAudioRecord(bufferSize: Int): AudioRecord {
    val sources =
      listOf(
        MediaRecorder.AudioSource.VOICE_RECOGNITION,
        MediaRecorder.AudioSource.MIC,
      )
    for (source in sources) {
      try {
        val candidate =
          AudioRecord.Builder()
            .setAudioSource(source)
            .setAudioFormat(
              AudioFormat.Builder()
                .setEncoding(AUDIO_ENCODING)
                .setSampleRate(SAMPLE_RATE_HZ)
                .setChannelMask(CHANNEL_CONFIG)
                .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .build()
        if (candidate.state == AudioRecord.STATE_INITIALIZED) {
          return candidate
        }
        candidate.release()
      } catch (e: Exception) {
        Log.w(TAG, "AudioSource $source failed to initialize", e)
      }
    }
    throw IllegalStateException("No compatible microphone audio source found.")
  }

  private fun computeNormalizedRms(pcmBytes: ByteArray, length: Int): Float {
    val sampleCount = length / 2
    if (sampleCount == 0) return 0f
    val shortBuffer = ByteBuffer.wrap(pcmBytes, 0, length).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
    var sumSquares = 0.0
    for (i in 0 until sampleCount) {
      val sample = shortBuffer.get(i).toDouble()
      sumSquares += sample * sample
    }
    val rms = sqrt(sumSquares / sampleCount)
    // Scale speech RMS (~500..10000) smoothly into 0f..1f for responsive UI metering
    val normalized = (rms / 8500.0).toFloat()
    return min(1f, maxOf(0f, normalized))
  }
}
