package com.example.transcription

/**
 * Always-on Smart Punctuation engine that acts as a deterministic safety net on top of Gemini 3.5
 * Transcribe Live output, guaranteeing clean spacing, capitalization, spoken punctuation handling,
 * and terminal punctuation.
 */
object SmartPunctuationFormatter {

  private val SPACE_BEFORE_PUNCT_REGEX = Regex("""\s+([,.;:?!])""")
  private val MISSING_SPACE_AFTER_PUNCT_REGEX = Regex("""([,.;:?!])([A-Za-z0-9\"'])""")
  private val MULTI_SPACE_REGEX = Regex("""[ \t]{2,}""")
  private val STANDALONE_I_REGEX = Regex("""\bi\b""")
  private val I_CONTRACTION_REGEX = Regex("""\bi(['’](?:m|ll|ve|d))\b""")
  private val SENTENCE_START_REGEX = Regex("""(^|[.!?]\s+|\n+\s*)([a-z])""")

  private val SPOKEN_COMMANDS: List<Pair<Regex, String>> =
    listOf(
      Regex("""(?i)[ \t]*\bnew paragraph\b\.?[ \t]*""") to "\n\n",
      Regex("""(?i)[ \t]*\bnew line\b\.?[ \t]*""") to "\n",
      Regex("""(?i)\s+\bquestion mark\b""") to "?",
      Regex("""(?i)\s+\bexclamation (?:point|mark)\b""") to "!",
      Regex("""(?i)\s+\bsemicolon\b""") to ";",
      Regex("""(?i)\s+\bcolon\b""") to ":",
      Regex("""(?i)\s+\bcomma\b""") to ",",
      Regex("""(?i)\s+\bperiod\b""") to ".",
    )

  /**
   * Formats streaming/interim text without forcing a terminal period at the very end (since the
   * speaker may still be mid-sentence).
   */
  fun formatLive(rawText: String): String {
    if (rawText.isBlank()) return ""
    return normalizeCore(rawText)
  }

  /**
   * Formats a completed utterance with full smart punctuation, including terminal punctuation if
   * the utterance ends without closing punctuation.
   */
  fun formatFinal(rawText: String): String {
    val cleaned = normalizeCore(rawText).trim()
    if (cleaned.isEmpty()) return ""
    val lastChar = cleaned.last()
    return if (lastChar in charArrayOf('.', '!', '?', ':', ';', '—', '"', '”', '\n')) {
      cleaned
    } else {
      "$cleaned."
    }
  }

  /**
   * Appends a new smart-punctuated utterance onto existing editor content with context-aware
   * spacing and capitalization.
   */
  fun joinWithExisting(existingText: String, newUtterance: String): String {
    val incoming = newUtterance.trim()
    if (incoming.isEmpty()) return existingText
    if (existingText.isBlank()) return incoming

    val trimmedRight = existingText.trimEnd(' ', '\t')
    val lastChar = trimmedRight.lastOrNull() ?: return incoming

    val separator =
      when {
        lastChar == '\n' -> ""
        else -> " "
      }

    val adjustedIncoming =
      if (lastChar in charArrayOf('.', '!', '?', '\n')) {
        incoming.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
      } else {
        incoming
      }

    return "$trimmedRight$separator$adjustedIncoming"
  }

  private fun normalizeCore(input: String): String {
    var text = input

    // 1. Convert explicit spoken punctuation commands if any passed through raw ASR
    for ((pattern, replacement) in SPOKEN_COMMANDS) {
      text = pattern.replace(text, replacement)
    }

    // 2. Collapse redundant spaces before punctuation and ensure space after punctuation
    text = SPACE_BEFORE_PUNCT_REGEX.replace(text, "$1")
    text = MISSING_SPACE_AFTER_PUNCT_REGEX.replace(text) { match ->
      val punct = match.groupValues[1]
      val next = match.groupValues[2]
      // Avoid breaking decimals like 3.5 or times like 10:30 or contractions
      if ((punct == "." || punct == ":") && next.first().isDigit()) {
        "$punct$next"
      } else if (punct == "." && next == "'") {
        "$punct $next"
      } else {
        "$punct $next"
      }
    }

    // 3. Collapse extra spaces while preserving newlines
    text = MULTI_SPACE_REGEX.replace(text, " ")

    // 4. Capitalize standalone pronoun "I" and its contractions ("I'm", "I'll", "I've", "I'd")
    text = I_CONTRACTION_REGEX.replace(text) { "I${it.groupValues[1]}" }
    text = STANDALONE_I_REGEX.replace(text, "I")

    // 5. Capitalize the start of every sentence and new line
    text =
      SENTENCE_START_REGEX.replace(text) { match ->
        match.groupValues[1] + match.groupValues[2].uppercase()
      }

    return text.trimStart(' ', '\t')
  }
}
