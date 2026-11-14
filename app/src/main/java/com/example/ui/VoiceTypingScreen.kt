package com.example.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardReturn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicNone
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.components.AudioWaveform
import com.example.ui.components.LanguagePickerSheet
import com.example.ui.components.SessionSnippetsSheet
import com.example.ui.components.SmartPunctuationSheet
import com.example.ui.components.StudioSettingsSheet
import com.example.ui.theme.AmberConnecting
import kotlinx.coroutines.launch

@Composable
fun VoiceTypingScreen(
  viewModel: VoiceTypingViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val view = LocalView.current
  val haptic = LocalHapticFeedback.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  var hasMicPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    )
  }
  var permissionDeniedOnce by remember { mutableStateOf(false) }

  var showSmartPunctuationSheet by remember { mutableStateOf(false) }
  var showLanguageSheet by remember { mutableStateOf(false) }
  var showSettingsSheet by remember { mutableStateOf(false) }
  var showSnippetsSheet by remember { mutableStateOf(false) }

  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        hasMicPermission =
          ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  // Keep screen awake during active live dictation if preference is enabled
  DisposableEffect(uiState.isSessionActive, uiState.preferences.keepScreenOn) {
    view.keepScreenOn = uiState.isSessionActive && uiState.preferences.keepScreenOn
    onDispose { view.keepScreenOn = false }
  }

  // Handle auto-copy on stop preference
  LaunchedEffect(uiState.autoCopyEventId) {
    if (uiState.autoCopyEventId > 0 && uiState.committedText.isNotBlank()) {
      copyToClipboard(context, uiState.committedText)
      snackbarHostState.showSnackbar("Auto-copied to clipboard")
    }
  }

  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasMicPermission = granted
      if (granted) {
        permissionDeniedOnce = false
        viewModel.startDictation()
      } else {
        permissionDeniedOnce = true
      }
    }

  fun performHaptic() {
    if (uiState.preferences.hapticFeedback) {
      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
  }

  fun handleMicTap() {
    performHaptic()
    if (!hasMicPermission) {
      permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    } else {
      viewModel.toggleDictation()
    }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    contentWindowInsets = WindowInsets.safeDrawing,
    containerColor = MaterialTheme.colorScheme.background,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .imePadding(),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .widthIn(max = 680.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        StudioTopBar(
          sessionStatus = uiState.sessionStatus,
          snippetCount = uiState.sessionSnippets.size,
          onOpenSnippets = { showSnippetsSheet = true },
          onOpenSettings = { showSettingsSheet = true },
        )

        StatusPillRow(
          sessionStatus = uiState.sessionStatus,
          elapsedSeconds = uiState.elapsedSeconds,
          onSmartPunctuationClick = { showSmartPunctuationSheet = true },
        )

        AnimatedVisibility(
          visible = !hasMicPermission,
          enter = expandVertically() + fadeIn(),
          exit = shrinkVertically() + fadeOut(),
        ) {
          PermissionPromptCard(
            permissionDeniedOnce = permissionDeniedOnce,
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            onOpenSettings = {
              val intent =
                Intent(
                  Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                  Uri.fromParts("package", context.packageName, null),
                )
              context.startActivity(intent)
            },
          )
        }

        AnimatedVisibility(
          visible = uiState.sessionStatus is SessionStatus.Error,
          enter = expandVertically() + fadeIn(),
          exit = shrinkVertically() + fadeOut(),
        ) {
          val errorMsg = (uiState.sessionStatus as? SessionStatus.Error)?.message.orEmpty()
          ErrorBanner(
            message = errorMsg,
            onRetry = {
              viewModel.dismissError()
              handleMicTap()
            },
            onDismiss = { viewModel.dismissError() },
          )
        }

        TranscriptCanvasCard(
          uiState = uiState,
          onTextChanged = viewModel::onTranscriptEdited,
          onAppendToken = { token ->
            performHaptic()
            viewModel.appendInlineText(token)
          },
          onUndo = {
            performHaptic()
            viewModel.undoLastChange()
          },
          onClear = {
            performHaptic()
            viewModel.clearTranscript()
          },
          modifier = Modifier.weight(1f).fillMaxWidth(),
        )

        QuickOutputActionRow(
          hasContent = uiState.combinedPreviewText.isNotBlank(),
          onCopyAll = {
            performHaptic()
            val text = uiState.combinedPreviewText
            copyToClipboard(context, text)
            scope.launch {
              snackbarHostState.showSnackbar(context.getString(R.string.action_copied))
            }
          },
          onShare = {
            performHaptic()
            shareText(context, uiState.combinedPreviewText)
          },
          onSaveSnippet = {
            performHaptic()
            viewModel.saveCurrentSnippet()
            scope.launch {
              snackbarHostState.showSnackbar("Saved to session snippets")
            }
          },
        )

        StudioMicDock(
          uiState = uiState,
          onMicTap = ::handleMicTap,
          onLanguageTap = { showLanguageSheet = true },
        )
      }
    }
  }

  if (showSmartPunctuationSheet) {
    SmartPunctuationSheet(onDismiss = { showSmartPunctuationSheet = false })
  }

  if (showLanguageSheet) {
    LanguagePickerSheet(
      selectedCode = uiState.preferences.languageCode,
      onSelectLanguage = { option -> viewModel.setLanguageCode(option.code) },
      onDismiss = { showLanguageSheet = false },
    )
  }

  if (showSettingsSheet) {
    StudioSettingsSheet(
      preferences = uiState.preferences,
      onAutoCopyChange = viewModel::setAutoCopyOnStop,
      onHapticChange = viewModel::setHapticFeedback,
      onKeepScreenOnChange = viewModel::setKeepScreenOn,
      onDismiss = { showSettingsSheet = false },
    )
  }

  if (showSnippetsSheet) {
    SessionSnippetsSheet(
      snippets = uiState.sessionSnippets,
      onCopySnippet = { snippet ->
        performHaptic()
        copyToClipboard(context, snippet.text)
        scope.launch { snackbarHostState.showSnackbar("Snippet copied") }
      },
      onRestoreSnippet = { snippet ->
        performHaptic()
        viewModel.restoreSnippet(snippet)
      },
      onDeleteSnippet = { snippet ->
        performHaptic()
        viewModel.deleteSnippet(snippet.id)
      },
      onDismiss = { showSnippetsSheet = false },
    )
  }
}

@Composable
private fun StudioTopBar(
  sessionStatus: SessionStatus,
  snippetCount: Int,
  onOpenSnippets: () -> Unit,
  onOpenSettings: () -> Unit,
) {
  val dotColor by
    animateColorAsState(
      targetValue =
        when (sessionStatus) {
          is SessionStatus.Listening -> MaterialTheme.colorScheme.primary
          is SessionStatus.Connecting, is SessionStatus.Finalizing -> AmberConnecting
          is SessionStatus.Error -> MaterialTheme.colorScheme.error
          is SessionStatus.Idle -> MaterialTheme.colorScheme.tertiary
        },
      label = "status_dot",
    )

  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = stringResource(R.string.app_name),
          style = MaterialTheme.typography.headlineMedium,
        )
        Box(
          modifier =
            Modifier
              .size(9.dp)
              .background(dotColor, CircleShape)
        )
      }
      Text(
        text = stringResource(R.string.model_badge_label),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    IconButton(
      onClick = onOpenSnippets,
      modifier = Modifier.minimumInteractiveComponentSize().testTag("history_button"),
    ) {
      BadgedBox(
        badge = {
          if (snippetCount > 0) {
            Badge { Text(snippetCount.toString()) }
          }
        }
      ) {
        Icon(
          imageVector = Icons.Outlined.Bookmarks,
          contentDescription = stringResource(R.string.cd_open_history),
        )
      }
    }

    IconButton(
      onClick = onOpenSettings,
      modifier = Modifier.minimumInteractiveComponentSize().testTag("settings_button"),
    ) {
      Icon(
        imageVector = Icons.Outlined.Tune,
        contentDescription = stringResource(R.string.cd_open_settings),
      )
    }
  }
}

@Composable
private fun StatusPillRow(
  sessionStatus: SessionStatus,
  elapsedSeconds: Long,
  onSmartPunctuationClick: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Surface(
      color = MaterialTheme.colorScheme.tertiaryContainer,
      shape = RoundedCornerShape(999.dp),
      modifier =
        Modifier
          .minimumInteractiveComponentSize()
          .clickable(onClick = onSmartPunctuationClick)
          .testTag("smart_punctuation_badge"),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.AutoAwesome,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onTertiaryContainer,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = stringResource(R.string.smart_punctuation_on),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
      }
    }

    val statusText =
      when (sessionStatus) {
        is SessionStatus.Idle -> stringResource(R.string.status_ready)
        is SessionStatus.Connecting -> stringResource(R.string.status_connecting)
        is SessionStatus.Listening -> "LIVE • ${formatDuration(elapsedSeconds)}"
        is SessionStatus.Finalizing -> stringResource(R.string.status_finalizing)
        is SessionStatus.Error -> "Session paused"
      }

    Surface(
      color =
        if (sessionStatus is SessionStatus.Listening) {
          MaterialTheme.colorScheme.primaryContainer
        } else {
          MaterialTheme.colorScheme.surfaceVariant
        },
      shape = RoundedCornerShape(999.dp),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        if (sessionStatus is SessionStatus.Listening) {
          Icon(
            imageVector = Icons.Rounded.GraphicEq,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(15.dp),
          )
        }
        Text(
          text = statusText,
          style = MaterialTheme.typography.labelSmall,
          color =
            if (sessionStatus is SessionStatus.Listening) {
              MaterialTheme.colorScheme.onPrimaryContainer
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
      }
    }
  }
}

@Composable
private fun PermissionPromptCard(
  permissionDeniedOnce: Boolean,
  onRequestPermission: () -> Unit,
  onOpenSettings: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.secondaryContainer,
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CircleShape,
      ) {
        Icon(
          imageVector = Icons.Rounded.MicNone,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(10.dp),
        )
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = stringResource(R.string.mic_permission_title),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(
          text = stringResource(R.string.mic_permission_body),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
        )
      }
      if (permissionDeniedOnce) {
        TextButton(onClick = onOpenSettings) {
          Text(stringResource(R.string.mic_permission_settings))
        }
      } else {
        FilledTonalButton(onClick = onRequestPermission) {
          Text(stringResource(R.string.mic_permission_grant))
        }
      }
    }
  }
}

@Composable
private fun ErrorBanner(
  message: String,
  onRetry: () -> Unit,
  onDismiss: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.ErrorOutline,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onPrimaryContainer,
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.weight(1f),
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
      )
      TextButton(onClick = onRetry) {
        Text("Retry", color = MaterialTheme.colorScheme.onPrimaryContainer)
      }
      TextButton(onClick = onDismiss) {
        Text("Dismiss", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
      }
    }
  }
}

@Composable
private fun TranscriptCanvasCard(
  uiState: DictationUiState,
  onTextChanged: (String) -> Unit,
  onAppendToken: (String) -> Unit,
  onUndo: () -> Unit,
  onClear: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val borderColor by
    animateColorAsState(
      targetValue =
        if (uiState.isListening) {
          MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
        } else {
          MaterialTheme.colorScheme.outline
        },
      label = "canvas_border",
    )

  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(24.dp),
    tonalElevation = 2.dp,
    border = BorderStroke(if (uiState.isListening) 1.5.dp else 1.dp, borderColor),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      Box(
        modifier =
          Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
      ) {
        if (uiState.isSessionActive && uiState.interimText.isNotBlank()) {
          // Live streaming preview showing committed text + real-time punctuated interim text
          val annotated = buildAnnotatedString {
            if (uiState.committedText.isNotBlank()) {
              append(uiState.committedText.trimEnd())
              append(" ")
            }
            withStyle(
              SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
              )
            ) {
              append(uiState.interimText)
            }
          }
          Text(
            text = annotated,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
        } else {
          BasicTextField(
            value = uiState.committedText,
            onValueChange = onTextChanged,
            readOnly = uiState.isSessionActive,
            textStyle =
              MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
              ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxSize().testTag("transcript_editor"),
            decorationBox = { innerTextField ->
              if (uiState.committedText.isEmpty()) {
                Text(
                  text = stringResource(R.string.editor_placeholder),
                  style = MaterialTheme.typography.bodyLarge,
                  color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                )
              }
              innerTextField()
            },
          )
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

      // Quick punctuation + metrics toolbar inside the editor card
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          listOf(",", ".", "?", "!", "—").forEach { symbol ->
            PunctuationChip(
              label = symbol,
              enabled = !uiState.isSessionActive,
              onClick = { onAppendToken(symbol) },
            )
          }
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp),
            modifier =
              Modifier
                .clickable(enabled = !uiState.isSessionActive) { onAppendToken("\n") },
          ) {
            Icon(
              imageVector = Icons.Rounded.KeyboardReturn,
              contentDescription = stringResource(R.string.action_newline),
              tint =
                if (!uiState.isSessionActive) {
                  MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                },
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp).size(16.dp),
            )
          }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Text(
          text = "${uiState.wordCount}w",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(end = 4.dp),
        )

        IconButton(
          onClick = onUndo,
          enabled = uiState.canUndo && !uiState.isSessionActive,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("undo_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.Undo,
            contentDescription = stringResource(R.string.action_undo),
          )
        }

        IconButton(
          onClick = onClear,
          enabled = uiState.combinedPreviewText.isNotEmpty() && !uiState.isSessionActive,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("clear_button"),
        ) {
          Icon(
            imageVector = Icons.Outlined.DeleteSweep,
            contentDescription = stringResource(R.string.action_clear),
          )
        }
      }
    }
  }
}

@Composable
private fun PunctuationChip(
  label: String,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    shape = RoundedCornerShape(10.dp),
    modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color =
        if (enabled) {
          MaterialTheme.colorScheme.onSurfaceVariant
        } else {
          MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        },
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    )
  }
}

@Composable
private fun QuickOutputActionRow(
  hasContent: Boolean,
  onCopyAll: () -> Unit,
  onShare: () -> Unit,
  onSaveSnippet: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    FilledTonalButton(
      onClick = onCopyAll,
      enabled = hasContent,
      contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
      modifier = Modifier.weight(1.2f).testTag("copy_button"),
    ) {
      Icon(
        imageVector = Icons.Outlined.ContentCopy,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(text = stringResource(R.string.action_copy), maxLines = 1)
    }

    FilledTonalButton(
      onClick = onShare,
      enabled = hasContent,
      contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
      modifier = Modifier.weight(1f).testTag("share_button"),
    ) {
      Icon(
        imageVector = Icons.Outlined.Share,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(text = stringResource(R.string.action_share), maxLines = 1)
    }

    OutlinedButton(
      onClick = onSaveSnippet,
      enabled = hasContent,
      contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
      modifier = Modifier.weight(1f).testTag("save_snippet_button"),
    ) {
      Icon(
        imageVector = Icons.Outlined.BookmarkAdd,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(text = stringResource(R.string.action_save_snippet), maxLines = 1)
    }
  }
}

@Composable
private fun StudioMicDock(
  uiState: DictationUiState,
  onMicTap: () -> Unit,
  onLanguageTap: () -> Unit,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
  val pulseScale by
    infiniteTransition.animateFloat(
      initialValue = 1.0f,
      targetValue = 1.24f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 900),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "pulse_scale",
    )

  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(28.dp),
    tonalElevation = 3.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(
            Brush.verticalGradient(
              colors =
                listOf(
                  MaterialTheme.colorScheme.surface,
                  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                )
            )
          )
          .padding(horizontal = 16.dp, vertical = 14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      AudioWaveform(
        amplitudes = uiState.amplitudeLevels,
        status = uiState.sessionStatus,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        // Left: Language Hint Button
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(16.dp),
          modifier =
            Modifier
              .weight(1f)
              .minimumInteractiveComponentSize()
              .clickable(enabled = !uiState.isSessionActive, onClick = onLanguageTap)
              .testTag("language_selector_button"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Translate,
              contentDescription = stringResource(R.string.cd_language_picker),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = uiState.preferences.selectedLanguage.displayName,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        // Center: Hero Microphone CTA
        Box(
          modifier = Modifier.padding(horizontal = 16.dp),
          contentAlignment = Alignment.Center,
        ) {
          if (uiState.isListening) {
            Box(
              modifier =
                Modifier
                  .size(76.dp)
                  .scale(pulseScale)
                  .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                    shape = CircleShape,
                  )
            )
          }

          Button(
            onClick = onMicTap,
            enabled = !uiState.isFinalizing,
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            colors =
              ButtonDefaults.buttonColors(
                containerColor =
                  if (uiState.isSessionActive) {
                    MaterialTheme.colorScheme.primary
                  } else {
                    MaterialTheme.colorScheme.onBackground
                  },
                contentColor =
                  if (uiState.isSessionActive) {
                    MaterialTheme.colorScheme.onPrimary
                  } else {
                    MaterialTheme.colorScheme.background
                  },
              ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
            modifier = Modifier.size(72.dp).testTag("mic_button"),
          ) {
            when {
              uiState.isConnecting || uiState.isFinalizing -> {
                CircularProgressIndicator(
                  color = Color.White,
                  strokeWidth = 3.dp,
                  modifier = Modifier.size(28.dp),
                )
              }
              uiState.isListening -> {
                Icon(
                  imageVector = Icons.Rounded.Stop,
                  contentDescription = stringResource(R.string.cd_mic_stop),
                  modifier = Modifier.size(34.dp),
                )
              }
              else -> {
                Icon(
                  imageVector = Icons.Rounded.Mic,
                  contentDescription = stringResource(R.string.cd_mic_start),
                  modifier = Modifier.size(32.dp),
                )
              }
            }
          }
        }

        // Right: Live Prompt / Char Counter
        Column(
          modifier = Modifier.weight(1f),
          horizontalAlignment = Alignment.End,
        ) {
          Text(
            text =
              when (uiState.sessionStatus) {
                is SessionStatus.Listening -> "Tap to stop"
                is SessionStatus.Connecting -> "Connecting…"
                is SessionStatus.Finalizing -> "Polishing…"
                else -> "Tap to speak"
              },
            style = MaterialTheme.typography.labelLarge,
            color =
              if (uiState.isListening) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurface
              },
            maxLines = 1,
          )
          Text(
            text = "${uiState.characterCount} chars",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

private fun formatDuration(totalSeconds: Long): String {
  val minutes = totalSeconds / 60L
  val seconds = totalSeconds % 60L
  return String.format("%02d:%02d", minutes, seconds)
}

private fun copyToClipboard(context: Context, text: String) {
  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  clipboard.setPrimaryClip(ClipData.newPlainText("VoxType Transcript", text))
}

private fun shareText(context: Context, text: String) {
  if (text.isBlank()) return
  val sendIntent =
    Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_TEXT, text)
    }
  context.startActivity(Intent.createChooser(sendIntent, "Share dictated text"))
}
