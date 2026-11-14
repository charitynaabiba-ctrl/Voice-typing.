package com.example.transcription

import org.junit.Assert.assertEquals
import org.junit.Test

class SmartPunctuationFormatterTest {

  @Test
  fun `formatLive capitalizes sentence starts and standalone I`() {
    val input = "hello world. i'm testing live voice typing , and i love it"
    val expected = "Hello world. I'm testing live voice typing, and I love it"
    assertEquals(expected, SmartPunctuationFormatter.formatLive(input))
  }

  @Test
  fun `formatFinal appends period when missing terminal punctuation`() {
    val input = "let's meet at 10:30 tomorrow morning"
    val expected = "Let's meet at 10:30 tomorrow morning."
    assertEquals(expected, SmartPunctuationFormatter.formatFinal(input))
  }

  @Test
  fun `formatFinal preserves existing question or exclamation marks`() {
    assertEquals(
      "Are you ready to test this?",
      SmartPunctuationFormatter.formatFinal("are you ready to test this?"),
    )
    assertEquals(
      "That worked great!",
      SmartPunctuationFormatter.formatFinal("that worked great!"),
    )
  }

  @Test
  fun `formatLive translates spoken punctuation commands`() {
    val input = "hi team comma let's ship this today period new line thanks"
    val expected = "Hi team, let's ship this today.\nThanks"
    assertEquals(expected, SmartPunctuationFormatter.formatLive(input))
  }

  @Test
  fun `joinWithExisting appends smoothly with spacing and capitalization`() {
    val existing = "First sentence."
    val incoming = "second sentence."
    assertEquals(
      "First sentence. Second sentence.",
      SmartPunctuationFormatter.joinWithExisting(existing, incoming),
    )
  }
}
