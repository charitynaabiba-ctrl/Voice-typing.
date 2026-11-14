package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BubbleChart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RestorePage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DictationPreferences
import com.example.data.SUPPORTED_LANGUAGES
import com.example.data.SpeechLanguageOption
import com.example.ui.DictationSnippet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartPunctuationSheet(onDismiss: () -> Unit) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 24.dp)
          .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          color = MaterialTheme.colorScheme.tertiaryContainer,
          shape = RoundedCornerShape(14.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(10.dp),
          )
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.smart_punctuation_sheet_title),
            style = MaterialTheme.typography.titleLarge,
          )
          Text(
            text = stringResource(R.string.smart_punctuation_sheet_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Surface(
          color = MaterialTheme.colorScheme.tertiaryContainer,
          shape = RoundedCornerShape(999.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Lock,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onTertiaryContainer,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = "ALWAYS ON",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

      PunctuationRuleRow(
        title = "Natural Sentence & Clause Punctuation",
        description = "Automatically adds periods, commas, question marks, exclamation marks, colons, and apostrophes from your cadence.",
      )
      PunctuationRuleRow(
        title = "Intelligent Capitalization",
        description = "Capitalizes sentence starts, proper nouns, acronyms, and standalone \"I\" contractions automatically.",
      )
      PunctuationRuleRow(
        title = "Disfluency & Self-Correction Cleanup",
        description = "Strips filler words like \"um\" or \"uh\" and resolves mid-thought corrections cleanly.",
      )
      PunctuationRuleRow(
        title = "Spoken Formatting Commands",
        description = "Say \"new line\", \"new paragraph\", \"comma\", or \"question mark\" anytime for explicit layout control.",
      )
    }
  }
}

@Composable
private fun PunctuationRuleRow(
  title: String,
  description: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Icon(
      imageVector = Icons.Rounded.CheckCircle,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.tertiary,
      modifier = Modifier.padding(top = 2.dp).size(18.dp),
    )
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(text = title, style = MaterialTheme.typography.titleMedium)
      Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerSheet(
  selectedCode: String,
  onSelectLanguage: (SpeechLanguageOption) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(bottom = 16.dp),
    ) {
      Text(
        text = "Speech Language Hint",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
      )
      Text(
        text = "Gemini 3.5 Transcribe Live handles multilingual speech automatically. Pick a primary hint if desired.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 12.dp),
      )
      LazyColumn {
        items(SUPPORTED_LANGUAGES, key = { it.code }) { option ->
          val isSelected = option.code == selectedCode
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .clickable {
                  onSelectLanguage(option)
                  onDismiss()
                }
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = option.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              )
              Text(
                text = option.nativeHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            if (isSelected) {
              Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioSettingsSheet(
  preferences: DictationPreferences,
  onAutoCopyChange: (Boolean) -> Unit,
  onHapticChange: (Boolean) -> Unit,
  onKeepScreenOnChange: (Boolean) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 24.dp)
          .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        text = stringResource(R.string.settings_title),
        style = MaterialTheme.typography.titleLarge,
      )

      // Locked Smart Punctuation status card
      Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.AutoFixHigh,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Smart Punctuation",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
              text = "Always active for every dictation turn",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
            )
          }
          Surface(
            color = MaterialTheme.colorScheme.tertiary,
            shape = RoundedCornerShape(999.dp),
          ) {
            Text(
              text = "LOCKED ON",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onTertiary,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }
      }

      PreferenceSwitchRow(
        title = "Auto-copy when stopping",
        subtitle = "Automatically copies transcript to clipboard when dictation finishes.",
        checked = preferences.autoCopyOnStop,
        onCheckedChange = onAutoCopyChange,
        testTag = "switch_auto_copy",
      )

      PreferenceSwitchRow(
        title = "Haptic feedback",
        subtitle = "Tactile vibration when starting, stopping, or copying text.",
        checked = preferences.hapticFeedback,
        onCheckedChange = onHapticChange,
        testTag = "switch_haptics",
      )

      PreferenceSwitchRow(
        title = "Keep screen awake while dictating",
        subtitle = "Prevents screen timeout during long live transcription sessions.",
        checked = preferences.keepScreenOn,
        onCheckedChange = onKeepScreenOnChange,
        testTag = "switch_keep_awake",
      )

      // Build 1 informational scope note (non-interactive)
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.Top,
        ) {
          Icon(
            imageVector = Icons.Rounded.BubbleChart,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp),
          )
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = stringResource(R.string.build_roadmap_title),
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text = stringResource(R.string.build_roadmap_body),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PreferenceSwitchRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, style = MaterialTheme.typography.titleMedium)
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.testTag(testTag),
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSnippetsSheet(
  snippets: List<DictationSnippet>,
  onCopySnippet: (DictationSnippet) -> Unit,
  onRestoreSnippet: (DictationSnippet) -> Unit,
  onDeleteSnippet: (DictationSnippet) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 24.dp)
          .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = stringResource(R.string.history_title),
        style = MaterialTheme.typography.titleLarge,
      )

      if (snippets.isEmpty()) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = stringResource(R.string.history_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp),
          )
        }
      } else {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          items(snippets, key = { it.id }) { snippet ->
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = snippet.text,
                  style = MaterialTheme.typography.bodyMedium,
                  maxLines = 4,
                  overflow = TextOverflow.Ellipsis,
                )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = "${snippet.timestampLabel} • ${snippet.wordCount} words",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                  )
                  IconButton(
                    onClick = { onCopySnippet(snippet) },
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.ContentCopy,
                      contentDescription = "Copy snippet",
                    )
                  }
                  Spacer(modifier = Modifier.width(2.dp))
                  IconButton(
                    onClick = {
                      onRestoreSnippet(snippet)
                      onDismiss()
                    },
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.RestorePage,
                      contentDescription = "Load into editor",
                    )
                  }
                  Spacer(modifier = Modifier.width(2.dp))
                  IconButton(
                    onClick = { onDeleteSnippet(snippet) },
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.DeleteOutline,
                      contentDescription = "Delete snippet",
                    )
                  }
                }
              }
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(4.dp))
    }
  }
}
