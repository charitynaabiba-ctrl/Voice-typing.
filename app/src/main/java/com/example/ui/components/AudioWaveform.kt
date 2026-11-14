package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.ui.SessionStatus
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun AudioWaveform(
  amplitudes: List<Float>,
  status: SessionStatus,
  modifier: Modifier = Modifier,
) {
  val primaryColor = MaterialTheme.colorScheme.primary
  val tertiaryColor = MaterialTheme.colorScheme.tertiary
  val idleColor = MaterialTheme.colorScheme.outline

  val infiniteTransition = rememberInfiniteTransition(label = "waveform_idle")
  val phase by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = (2 * PI).toFloat(),
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "wave_phase",
    )

  Canvas(
    modifier =
      modifier
        .fillMaxWidth()
        .height(44.dp)
  ) {
    val barCount = amplitudes.size.coerceAtLeast(1)
    val slotWidth = size.width / barCount
    val barWidth = (slotWidth * 0.52f).coerceIn(3f, 10f)
    val maxBarHeight = size.height * 0.88f
    val minBarHeight = size.height * 0.14f
    val centerY = size.height / 2f

    val activeBrush =
      Brush.verticalGradient(
        colors = listOf(primaryColor, tertiaryColor.copy(alpha = 0.85f), primaryColor),
      )

    for (index in 0 until barCount) {
      val fraction = index.toFloat() / barCount.toFloat()
      val level =
        when (status) {
          is SessionStatus.Listening -> {
            // Subtle edge taper so bars form an organic acoustic envelope
            val edgeEnvelope = sin(fraction * PI).toFloat().coerceIn(0.35f, 1f)
            (amplitudes[index] * edgeEnvelope).coerceIn(0.08f, 1f)
          }
          is SessionStatus.Connecting, is SessionStatus.Finalizing -> {
            val shimmer = (sin(phase + fraction * 6f) + 1f) / 2f
            0.12f + shimmer * 0.25f
          }
          is SessionStatus.Idle, is SessionStatus.Error -> {
            val breathe = (sin(phase + fraction * 4f) + 1f) / 2f
            0.05f + breathe * 0.06f
          }
        }

      val barHeight = minBarHeight + (maxBarHeight - minBarHeight) * level
      val x = index * slotWidth + (slotWidth - barWidth) / 2f
      val y = centerY - barHeight / 2f

      if (status is SessionStatus.Listening) {
        drawRoundRect(
          brush = activeBrush,
          topLeft = Offset(x, y),
          size = Size(barWidth, barHeight),
          cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
        )
      } else {
        val barColor =
          when (status) {
            is SessionStatus.Connecting, is SessionStatus.Finalizing ->
              primaryColor.copy(alpha = 0.55f)
            else -> idleColor.copy(alpha = 0.7f)
          }
        drawRoundRect(
          color = barColor,
          topLeft = Offset(x, y),
          size = Size(barWidth, barHeight),
          cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
        )
      }
    }
  }
}
