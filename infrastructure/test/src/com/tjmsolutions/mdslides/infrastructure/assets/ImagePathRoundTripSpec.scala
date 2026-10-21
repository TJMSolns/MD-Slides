package com.tjmsolutions.mdslides.infrastructure.assets

import com.tjmsolutions.mdslides.domain.{Slide, SlideDeck, SlideId, SlotName}
import com.tjmsolutions.mdslides.infrastructure.renderer.HTMLRenderer
import cats.data.NonEmptyList
import java.nio.file.{Files, Path}
import java.util.Comparator

/**
 * MS-030: the path the renderer writes into the HTML must be the path the copier wrote the image to.
 *
 * Found by the MS-028 verifier: `![alt](./images/x.png)` was copied to `images/images/x.png` while the
 * HTML pointed at `images/x.png`, so the documented example rendered a broken image.
 */
class ImagePathRoundTripSpec extends munit.FunSuite:

  private def deleteRecursively(path: Path): Unit =
    if Files.exists(path) then
      Files.walk(path).sorted(Comparator.reverseOrder()).forEach(p => Files.delete(p))

  /** Lays out a deck folder with the image at `relativeToDeck`, copies, renders, and returns (outputDir, src). */
  private def roundTrip(markdownUrl: String, relativeToDeck: String): (Path, String) =
    val root = Files.createTempDirectory("mdslides-roundtrip")
    val deckDir = root.resolve("deck")
    Files.createDirectories(deckDir)
    val source = deckDir.resolve(relativeToDeck).normalize()
    Files.createDirectories(source.getParent)
    Files.writeString(source, "<svg>pic</svg>")

    val outputDir = root.resolve("out")
    val copied = ImageAssetCopier.copyImages(List(markdownUrl), deckDir, outputDir.resolve("images"))
    assert(copied.isRight, s"copy failed: $copied")

    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "content",
      slots = Map(SlotName.Heading -> "Images", SlotName.Body -> s"![a picture]($markdownUrl)")
    )
    val html = HTMLRenderer.renderDeck(SlideDeck(NonEmptyList.one(slide)))
    val src = """<img class="content-image" src="([^"]+)"""".r
      .findFirstMatchIn(html)
      .map(_.group(1))
      .getOrElse(fail(s"no content image in the HTML for $markdownUrl"))
    (outputDir, src)

  private def assertImageResolves(markdownUrl: String, relativeToDeck: String)(using munit.Location): Unit =
    val (outputDir, src) = roundTrip(markdownUrl, relativeToDeck)
    try
      val target = outputDir.resolve(src).normalize()
      assert(target.startsWith(outputDir), s"$markdownUrl: src '$src' points outside the output folder")
      assert(Files.exists(target), s"$markdownUrl: HTML src '$src' does not exist in the output folder")
    finally deleteRecursively(outputDir.getParent)

  test("a dot-prefixed relative image path renders an image that exists (the documented example)") {
    assertImageResolves("./images/pic.png", "images/pic.png")
  }

  test("a relative image path in a subfolder renders an image that exists") {
    assertImageResolves("images/pic.png", "images/pic.png")
  }

  test("a bare image file name renders an image that exists") {
    assertImageResolves("pic.png", "pic.png")
  }

  test("a nested relative image path renders an image that exists") {
    assertImageResolves("images/icons/arrow.png", "images/icons/arrow.png")
  }

  test("an image outside the deck folder is copied inside the output folder and resolves") {
    assertImageResolves("../shared/logo.png", "../shared/logo.png")
  }

  test("two images with the same file name in different folders stay distinct") {
    val root = Files.createTempDirectory("mdslides-roundtrip-two")
    try
      val deckDir = root.resolve("deck")
      for dir <- List("a", "b") do
        Files.createDirectories(deckDir.resolve(dir))
        Files.writeString(deckDir.resolve(s"$dir/logo.png"), s"<svg>$dir</svg>")
      val outputDir = root.resolve("out")
      val result = ImageAssetCopier.copyImages(List("a/logo.png", "./b/logo.png"), deckDir, outputDir.resolve("images"))
      assert(result.isRight, s"copy failed: $result")
      assertEquals(Files.readString(outputDir.resolve("images").resolve(ImageAssetCopier.outputPath("a/logo.png"))), "<svg>a</svg>")
      assertEquals(Files.readString(outputDir.resolve("images").resolve(ImageAssetCopier.outputPath("./b/logo.png"))), "<svg>b</svg>")
    finally deleteRecursively(root)
  }

  test("outputPath keeps a relative path inside the folder and normalizes it") {
    assertEquals(ImageAssetCopier.outputPath("./images/pic.png"), "images/pic.png")
    assertEquals(ImageAssetCopier.outputPath("images/icons/arrow.svg"), "images/icons/arrow.svg")
    assertEquals(ImageAssetCopier.outputPath("pic.png"), "pic.png")
  }

  test("outputPath never escapes the output folder") {
    assertEquals(ImageAssetCopier.outputPath("../shared/logo.png"), "logo.png")
    assertEquals(ImageAssetCopier.outputPath("/home/someone/pictures/photo.jpg"), "photo.jpg")
  }
