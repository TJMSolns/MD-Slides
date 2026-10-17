package com.tjmsolutions.mdslides.domain

import cats.data.NonEmptyList

/**
 * The deck title: used for the page title, the {{title}} placeholder and session logs.
 */
class SlideDeckTitleSpec extends munit.FunSuite:

  private def deck(slides: Slide*) = SlideDeck(NonEmptyList.fromListUnsafe(slides.toList))

  test("the deck title is the title of the first title slide") {
    val d = deck(
      Slide(SlideId.unsafe(1), "content", Map(SlotName.Heading -> "Agenda", SlotName.Body -> "x")),
      Slide(SlideId.unsafe(2), "title", Map(SlotName.Title -> "Quarterly Review"))
    )
    assertEquals(d.title, Some("Quarterly Review"))
  }

  test("without a title slide, the deck title is the first slide heading") {
    val d = deck(Slide(SlideId.unsafe(1), "content", Map(SlotName.Heading -> "Agenda", SlotName.Body -> "x")))
    assertEquals(d.title, Some("Agenda"))
  }

  test("inline markdown is removed from the deck title") {
    val d = deck(Slide(SlideId.unsafe(1), "title", Map(SlotName.Title -> "**Bold** `code` and [a link](https://example.com)")))
    assertEquals(d.title, Some("Bold code and a link"))
  }

  test("a deck with no title or heading has no title") {
    val d = deck(Slide(SlideId.unsafe(1), "content", Map(SlotName.Body -> "just text")))
    assertEquals(d.title, None)
  }
