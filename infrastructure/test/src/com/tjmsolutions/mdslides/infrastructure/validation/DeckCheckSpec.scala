package com.tjmsolutions.mdslides.infrastructure.validation

import java.nio.file.{Files, Path}
import java.util.Comparator

/**
 * Site WQ-077 / the docs' "every problem in one pass": found by the WQ-075 verifier, the v1.1.1 jar stopped at
 * the first structural error, hid density warnings behind it, and reported a missing image only after
 * "Validation passed". DeckCheck runs every check before anything is written and reports everything together.
 */
class DeckCheckSpec extends munit.FunSuite:

  private def slide(template: String, body: String): String =
    s"---\ntemplate: $template\n---\n\n$body\n"

  private val title = slide("title", "# A deck")

  private def tempDir(): Path = Files.createTempDirectory("mdslides-deckcheck")

  private def deleteRecursively(path: Path): Unit =
    if Files.exists(path) then Files.walk(path).sorted(Comparator.reverseOrder()).forEach(p => Files.delete(p))

  test("problems on several slides are all reported in one pass") {
    val md = title +
      slide("two-column", "## Two\n\nLeft\n\n---column---\n\nMiddle\n\n---column---\n\nRight") +
      slide("bogus", "## Unknown template") +
      slide("content", "## Image\n\n![](pic.png)")
    val result = DeckCheck.run(md, None)
    assert(!result.passed)
    assert(result.errors.exists(_.contains("Slide 2")), s"slide 2 missing from ${result.errors}")
    assert(result.errors.exists(_.contains("Slide 3")), s"slide 3 missing from ${result.errors}")
    assert(result.errors.exists(_.contains("Slide 4")), s"slide 4 missing from ${result.errors}")
  }

  test("every image without alt text on one slide is reported") {
    val md = title + slide("content", "## Images\n\n![](one.png)\n\n![](two.png)")
    val result = DeckCheck.run(md, None)
    assert(result.errors.exists(_.contains("one.png")), s"one.png missing from ${result.errors}")
    assert(result.errors.exists(_.contains("two.png")), s"two.png missing from ${result.errors}")
  }

  test("a structural error on one slide does not hide a density warning on another") {
    val dense = (1 to 14).map(i => s"Line $i").mkString("\n\n")
    val md = title + slide("content", "## Image\n\n![](pic.png)") + slide("content", s"## Dense\n\n$dense")
    val result = DeckCheck.run(md, None)
    assert(result.errors.exists(_.contains("Slide 2")), s"alt-text error missing from ${result.errors}")
    assert(result.warnings.exists(_.contains("Slide 3")), s"density warning missing from ${result.warnings}")
  }

  test("a missing image file is reported with the other problems, before anything is written") {
    val dir = tempDir()
    try
      val md = title + slide("content", "## Picture\n\n![a lamp](images/missing.png)")
      val result = DeckCheck.run(md, Some(dir))
      assert(!result.passed)
      assert(result.errors.exists(e => e.contains("images/missing.png") && e.contains("Slide 2")), s"got ${result.errors}")
    finally deleteRecursively(dir)
  }

  test("an existing image file passes, and image files are not checked when copying is off") {
    val dir = tempDir()
    try
      Files.createDirectories(dir.resolve("images"))
      Files.writeString(dir.resolve("images/lamp.png"), "png")
      val md = title + slide("content", "## Picture\n\n![a lamp](./images/lamp.png)\n\n![a chair](images/chair.png)")
      assert(DeckCheck.run(md, Some(dir)).errors.exists(_.contains("images/chair.png")))
      assert(DeckCheck.run(md, None).passed)
    finally deleteRecursively(dir)
  }

  test("a clean deck passes, with the deck and its slide count") {
    val result = DeckCheck.run(title + slide("content", "## Fine\n\nAll good"), None)
    assert(result.passed, s"unexpected errors ${result.errors}")
    assertEquals(result.slideCount, 2)
    assert(result.deck.isDefined)
  }
