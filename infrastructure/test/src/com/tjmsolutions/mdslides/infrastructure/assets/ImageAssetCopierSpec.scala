package com.tjmsolutions.mdslides.infrastructure.assets

import munit.FunSuite
import java.nio.file.{Path, Paths}

/**
 * Tests for ImageAssetCopier - automatic image asset copying functionality.
 *
 * Test-first development for US-006: Image Asset Copying
 * Related: ADR-012, PDR-009, PDR-010
 */
class ImageAssetCopierSpec extends FunSuite:

  // ============================================================================
  // isLocalPath Tests (5 tests)
  // ============================================================================

  test("isLocalPath: relative path with directory") {
    assert(ImageAssetCopier.isLocalPath("images/logo.svg"))
  }

  test("isLocalPath: relative path with dot prefix") {
    assert(ImageAssetCopier.isLocalPath("./images/logo.svg"))
  }

  test("isLocalPath: relative path with parent directory") {
    assert(ImageAssetCopier.isLocalPath("../assets/logo.svg"))
  }

  test("isLocalPath: absolute path (Unix)") {
    assert(ImageAssetCopier.isLocalPath("/home/user/images/logo.svg"))
  }

  test("isLocalPath: absolute path (Windows)") {
    assert(ImageAssetCopier.isLocalPath("C:\\Users\\user\\images\\logo.svg"))
  }

  test("isLocalPath: http URL should be false") {
    assert(!ImageAssetCopier.isLocalPath("http://example.com/logo.png"))
  }

  test("isLocalPath: https URL should be false") {
    assert(!ImageAssetCopier.isLocalPath("https://example.com/logo.png"))
  }

  test("isLocalPath: data URL should be false") {
    assert(!ImageAssetCopier.isLocalPath("data:image/svg+xml;base64,PHN2Zy8+"))
  }

  test("isLocalPath: protocol-relative URL should be false") {
    assert(!ImageAssetCopier.isLocalPath("//cdn.example.com/logo.png"))
  }

  test("isLocalPath: empty string should be false") {
    assert(!ImageAssetCopier.isLocalPath(""))
  }

  // ============================================================================
  // resolvePath Tests (5 tests)
  // ============================================================================

  test("resolvePath: relative path resolves against base directory") {
    val baseDir = Paths.get("/home/user/project")
    val result = ImageAssetCopier.resolvePath("images/logo.svg", baseDir)

    result match
      case Right(path) =>
        assertEquals(path, Paths.get("/home/user/project/images/logo.svg"))
      case Left(error) =>
        fail(s"Expected success, got error: $error")
  }

  test("resolvePath: dot-prefixed relative path") {
    val baseDir = Paths.get("/home/user/project")
    val result = ImageAssetCopier.resolvePath("./images/logo.svg", baseDir)

    result match
      case Right(path) =>
        assertEquals(path, Paths.get("/home/user/project/images/logo.svg"))
      case Left(error) =>
        fail(s"Expected success, got error: $error")
  }

  test("resolvePath: parent directory relative path") {
    val baseDir = Paths.get("/home/user/project/slides")
    val result = ImageAssetCopier.resolvePath("../images/logo.svg", baseDir)

    result match
      case Right(path) =>
        assertEquals(path, Paths.get("/home/user/project/images/logo.svg"))
      case Left(error) =>
        fail(s"Expected success, got error: $error")
  }

  test("resolvePath: absolute path returns as-is") {
    val baseDir = Paths.get("/home/user/project")
    val absolutePath = "/var/www/images/logo.svg"
    val result = ImageAssetCopier.resolvePath(absolutePath, baseDir)

    result match
      case Right(path) =>
        assertEquals(path, Paths.get(absolutePath))
      case Left(error) =>
        fail(s"Expected success, got error: $error")
  }

  test("resolvePath: http URL returns error") {
    val baseDir = Paths.get("/home/user/project")
    val result = ImageAssetCopier.resolvePath("http://example.com/logo.png", baseDir)

    result match
      case Right(_) =>
        fail("Expected error for HTTP URL, got success")
      case Left(error) =>
        assert(error.contains("Cannot resolve external URL"))
  }

  test("resolvePath: data URL returns error") {
    val baseDir = Paths.get("/home/user/project")
    val result = ImageAssetCopier.resolvePath("data:image/svg+xml;base64,PHN2Zy8+", baseDir)

    result match
      case Right(_) =>
        fail("Expected error for data URL, got success")
      case Left(error) =>
        assert(error.contains("Cannot resolve data URL"))
  }

  // ============================================================================
  // copyImages Tests - Phase 2 (File Copying)
  // ============================================================================

  test("copyImages: successfully copies single image file") {
    import java.nio.file.Files
    import scala.util.Using

    // Create temporary source directory with test image
    val tempSourceDir = Files.createTempDirectory("mdslides-test-source")
    val tempOutputDir = Files.createTempDirectory("mdslides-test-output")

    try
      // Create test image file
      val imageDir = tempSourceDir.resolve("images")
      Files.createDirectories(imageDir)
      val sourceImage = imageDir.resolve("logo.svg")
      Files.writeString(sourceImage, "<svg>test</svg>")

      // Copy images
      val images = List("images/logo.svg")
      val result = ImageAssetCopier.copyImages(images, tempSourceDir, tempOutputDir)

      result match
        case Right(copiedImages) =>
          assertEquals(copiedImages.length, 1)
          assertEquals(copiedImages.head.originalPath, "images/logo.svg")

          // Verify file was actually copied
          val destPath = tempOutputDir.resolve("images/logo.svg")
          assert(Files.exists(destPath))
          assertEquals(Files.readString(destPath), "<svg>test</svg>")

        case Left(error) =>
          fail(s"Expected success, got error: $error")

    finally
      // Cleanup
      deleteRecursively(tempSourceDir)
      deleteRecursively(tempOutputDir)
  }

  test("copyImages: preserves directory structure") {
    import java.nio.file.Files

    val tempSourceDir = Files.createTempDirectory("mdslides-test-source")
    val tempOutputDir = Files.createTempDirectory("mdslides-test-output")

    try
      // Create nested directory structure
      val iconDir = tempSourceDir.resolve("images/icons")
      Files.createDirectories(iconDir)
      val sourceIcon = iconDir.resolve("arrow.svg")
      Files.writeString(sourceIcon, "<svg>arrow</svg>")

      val images = List("images/icons/arrow.svg")
      val result = ImageAssetCopier.copyImages(images, tempSourceDir, tempOutputDir)

      result match
        case Right(copiedImages) =>
          assertEquals(copiedImages.length, 1)

          // Verify nested structure preserved
          val destPath = tempOutputDir.resolve("images/icons/arrow.svg")
          assert(Files.exists(destPath))
          assertEquals(Files.readString(destPath), "<svg>arrow</svg>")

        case Left(error) =>
          fail(s"Expected success, got error: $error")

    finally
      deleteRecursively(tempSourceDir)
      deleteRecursively(tempOutputDir)
  }

  test("copyImages: handles multiple images") {
    import java.nio.file.Files

    val tempSourceDir = Files.createTempDirectory("mdslides-test-source")
    val tempOutputDir = Files.createTempDirectory("mdslides-test-output")

    try
      // Create multiple test images
      val imageDir = tempSourceDir.resolve("images")
      Files.createDirectories(imageDir)
      Files.writeString(imageDir.resolve("logo.svg"), "<svg>logo</svg>")
      Files.writeString(imageDir.resolve("diagram.png"), "fake-png-data")
      Files.writeString(imageDir.resolve("photo.jpg"), "fake-jpg-data")

      val images = List("images/logo.svg", "images/diagram.png", "images/photo.jpg")
      val result = ImageAssetCopier.copyImages(images, tempSourceDir, tempOutputDir)

      result match
        case Right(copiedImages) =>
          assertEquals(copiedImages.length, 3)

          // Verify all files copied
          assert(Files.exists(tempOutputDir.resolve("images/logo.svg")))
          assert(Files.exists(tempOutputDir.resolve("images/diagram.png")))
          assert(Files.exists(tempOutputDir.resolve("images/photo.jpg")))

        case Left(error) =>
          fail(s"Expected success, got error: $error")

    finally
      deleteRecursively(tempSourceDir)
      deleteRecursively(tempOutputDir)
  }

  test("copyImages: skips external URLs") {
    import java.nio.file.Files

    val tempSourceDir = Files.createTempDirectory("mdslides-test-source")
    val tempOutputDir = Files.createTempDirectory("mdslides-test-output")

    try
      // Create local image
      val imageDir = tempSourceDir.resolve("images")
      Files.createDirectories(imageDir)
      Files.writeString(imageDir.resolve("logo.svg"), "<svg>logo</svg>")

      // Mix of local and external URLs
      val images = List(
        "images/logo.svg",
        "https://example.com/external.png",
        "data:image/svg+xml;base64,PHN2Zy8+"
      )
      val result = ImageAssetCopier.copyImages(images, tempSourceDir, tempOutputDir)

      result match
        case Right(copiedImages) =>
          // Only local image should be copied
          assertEquals(copiedImages.length, 1)
          assertEquals(copiedImages.head.originalPath, "images/logo.svg")

        case Left(error) =>
          fail(s"Expected success, got error: $error")

    finally
      deleteRecursively(tempSourceDir)
      deleteRecursively(tempOutputDir)
  }

  test("copyImages: errors on missing source file") {
    import java.nio.file.Files

    val tempSourceDir = Files.createTempDirectory("mdslides-test-source")
    val tempOutputDir = Files.createTempDirectory("mdslides-test-output")

    try
      val images = List("images/missing.svg")
      val result = ImageAssetCopier.copyImages(images, tempSourceDir, tempOutputDir)

      result match
        case Right(_) =>
          fail("Expected error for missing file, got success")
        case Left(error) =>
          assert(error.contains("does not exist") || error.contains("not found"))

    finally
      deleteRecursively(tempSourceDir)
      deleteRecursively(tempOutputDir)
  }

  // Helper method to recursively delete directories
  private def deleteRecursively(path: Path): Unit =
    import java.nio.file.{Files, FileVisitOption}
    import scala.jdk.StreamConverters._

    if Files.exists(path) then
      Files.walk(path, FileVisitOption.FOLLOW_LINKS)
        .toScala(LazyList)
        .sorted(Ordering.by[Path, Int](_.getNameCount).reverse)
        .foreach(Files.delete)

end ImageAssetCopierSpec
