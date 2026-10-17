package com.tjmsolutions.mdslides.cli

import java.nio.file.Files

/** The break-screen image ends up inside the output folder, so the rendered deck can show it. */
class BreakScreenAssetSpec extends munit.FunSuite:

  test("a local break image next to the deck is copied into images/ and referenced from there") {
    val deckDir = Files.createTempDirectory("deck")
    val out = Files.createTempDirectory("out")
    Files.createDirectories(deckDir.resolve("pics"))
    Files.writeString(deckDir.resolve("pics/break.png"), "png")
    val result = BreakScreenAsset.place(Some("pics/break.png"), deckDir, deckDir, out)
    assertEquals(result, Right(Some("images/break.png")))
    assert(Files.exists(out.resolve("images/break.png")))
  }

  test("a break image relative to the current folder is found too") {
    val cwd = Files.createTempDirectory("cwd")
    val deckDir = Files.createTempDirectory("deck")
    val out = Files.createTempDirectory("out")
    Files.writeString(cwd.resolve("b.jpg"), "jpg")
    assertEquals(BreakScreenAsset.place(Some("b.jpg"), deckDir, cwd, out), Right(Some("images/b.jpg")))
  }

  test("web and data URLs are used as they are") {
    val d = Files.createTempDirectory("x")
    assertEquals(BreakScreenAsset.place(Some("https://example.com/b.png"), d, d, d), Right(Some("https://example.com/b.png")))
    assertEquals(BreakScreenAsset.place(Some("data:image/png;base64,AA=="), d, d, d), Right(Some("data:image/png;base64,AA==")))
  }

  test("a missing break image is reported, not rendered as a broken link") {
    val d = Files.createTempDirectory("x")
    assert(BreakScreenAsset.place(Some("nope.png"), d, d, d).left.exists(_.contains("nope.png")))
  }

  test("no break image configured means the default break screen") {
    val d = Files.createTempDirectory("x")
    assertEquals(BreakScreenAsset.place(None, d, d, d), Right(None))
  }
