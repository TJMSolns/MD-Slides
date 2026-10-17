package com.tjmsolutions.mdslides.cli

import munit.FunSuite
import java.nio.file.{Files, Path}
import scala.util.Using
import cats.effect.unsafe.implicits.global  // For IO.unsafeRunSync() in tests

class PathResolverSpec extends FunSuite:

  // Helper to create temp files for testing
  def withTempFile[A](filename: String)(f: Path => A): A =
    val tempDir = Files.createTempDirectory("mdslides-test")
    val tempFile = tempDir.resolve(filename)
    Files.createFile(tempFile)
    try
      f(tempFile.getParent)
    finally
      Files.deleteIfExists(tempFile)
      Files.deleteIfExists(tempDir)

  // Phase 2: Path Resolution Tests

  test("PathResolver.findInputFile - finds .md file"):
    withTempFile("my-preso.md") { dir =>
      val result = PathResolver.findInputFile("my-preso", Some(dir))

      assert(result.isRight, s"Expected to find my-preso.md but got: $result")
      result.foreach { path =>
        assert(path.toString.endsWith("my-preso.md"), s"Expected .md extension: $path")
      }
    }

  test("PathResolver.findInputFile - fallback to .markdown"):
    withTempFile("my-preso.markdown") { dir =>
      val result = PathResolver.findInputFile("my-preso", Some(dir))

      assert(result.isRight, s"Expected to find my-preso.markdown but got: $result")
      result.foreach { path =>
        assert(path.toString.endsWith("my-preso.markdown"), s"Expected .markdown extension: $path")
      }
    }

  test("PathResolver.findInputFile - prefers .md over .markdown"):
    val tempDir = Files.createTempDirectory("mdslides-test")
    val mdFile = tempDir.resolve("talk.md")
    val markdownFile = tempDir.resolve("talk.markdown")
    Files.createFile(mdFile)
    Files.createFile(markdownFile)

    try
      val result = PathResolver.findInputFile("talk", Some(tempDir))

      assert(result.isRight)
      result.foreach { path =>
        assert(path.toString.endsWith("talk.md"),
          s"Should prefer .md over .markdown: $path")
      }
    finally
      Files.deleteIfExists(mdFile)
      Files.deleteIfExists(markdownFile)
      Files.deleteIfExists(tempDir)

  test("PathResolver.findInputFile - error when not found"):
    val tempDir = Files.createTempDirectory("mdslides-test")
    try
      val result = PathResolver.findInputFile("nonexistent", Some(tempDir))

      assert(result.isLeft, "Expected error when file not found")
      result.left.foreach { error =>
        assert(error.contains("not found"), s"Error should mention 'not found': $error")
        assert(error.contains("nonexistent.md"), s"Error should mention filename: $error")
      }
    finally
      Files.deleteIfExists(tempDir)

  test("PathResolver.determineOutputDir - from deck name"):
    val outputDir = PathResolver.determineOutputDir("my-preso")

    assertEquals(outputDir.getFileName.toString, "my-preso",
      "Output directory should match deck name")

  test("PathResolver.determineOutputDir - preserves path separators"):
    val outputDir = PathResolver.determineOutputDir("presentations/my-talk")

    assert(outputDir.toString.contains("presentations"),
      "Should preserve path components")
    assert(outputDir.toString.endsWith("my-talk"),
      "Should end with deck name")

  test("PathResolver.ensureOutputDirExists - creates directory"):
    val tempDir = Files.createTempDirectory("mdslides-test")
    val outputDir = tempDir.resolve("new-output")

    try
      // Directory should not exist initially
      assert(!Files.exists(outputDir), "Directory should not exist initially")

      // Run the effect to create it
      val result = PathResolver.ensureOutputDirExists(outputDir).attempt.unsafeRunSync()

      assert(result.isRight, s"Expected success but got: $result")
      assert(Files.exists(outputDir), "Directory should exist after creation")
      assert(Files.isDirectory(outputDir), "Should be a directory")
    finally
      Files.deleteIfExists(outputDir)
      Files.deleteIfExists(tempDir)

  test("PathResolver.ensureOutputDirExists - creates parent directories"):
    val tempDir = Files.createTempDirectory("mdslides-test")
    val nestedOutput = tempDir.resolve("parent/child/output")

    try
      val result = PathResolver.ensureOutputDirExists(nestedOutput).attempt.unsafeRunSync()

      assert(result.isRight)
      assert(Files.exists(nestedOutput), "Nested directory should exist")
      assert(Files.exists(nestedOutput.getParent), "Parent directory should exist")
    finally
      Files.deleteIfExists(nestedOutput)
      Files.deleteIfExists(nestedOutput.getParent)
      Files.deleteIfExists(nestedOutput.getParent.getParent)
      Files.deleteIfExists(tempDir)

  test("PathResolver.ensureOutputDirExists - succeeds when directory already exists"):
    val tempDir = Files.createTempDirectory("mdslides-test")

    try
      // Run twice - second time directory already exists
      PathResolver.ensureOutputDirExists(tempDir).unsafeRunSync()
      val result = PathResolver.ensureOutputDirExists(tempDir).attempt.unsafeRunSync()

      assert(result.isRight, "Should succeed when directory already exists")
    finally
      Files.deleteIfExists(tempDir)

end PathResolverSpec
