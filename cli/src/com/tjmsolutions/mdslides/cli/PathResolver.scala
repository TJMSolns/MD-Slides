package com.tjmsolutions.mdslides.cli

import cats.effect.IO
import java.nio.file.{Files, Path, Paths}
import scala.jdk.CollectionConverters.*

/**
 * Path resolution service for MDSlides CLI.
 *
 * Handles:
 * - Finding input markdown files (try .md then .markdown)
 * - Determining output directories from deck names
 * - Creating output directories (with parent directories)
 */
object PathResolver:

  /**
   * Find input markdown file from deck name.
   *
   * Standard CLI resolution order (best practice):
   * 1. Try path as-is (allows any extension or no extension)
   * 2. If not found, try adding `.md`
   * 3. If not found, try adding `.markdown`
   * 4. Error if none found
   *
   * @param deckName Deck name (with or without extension)
   * @param baseDir Directory to search in (defaults to current directory)
   * @return Right(Path) if found, Left(error message) if not found
   */
  def findInputFile(deckName: String, baseDir: Option[Path] = None): Either[String, Path] =
    val searchDir = baseDir.getOrElse(Paths.get("."))

    // 1. Try the path exactly as given (standard CLI behavior)
    val directPath = searchDir.resolve(deckName)
    if Files.exists(directPath) && Files.isRegularFile(directPath) then
      return Right(directPath)

    // 2. Try adding .md extension
    val mdPath = searchDir.resolve(s"$deckName.md")
    if Files.exists(mdPath) && Files.isRegularFile(mdPath) then
      return Right(mdPath)

    // 3. Try adding .markdown extension
    val markdownPath = searchDir.resolve(s"$deckName.markdown")
    if Files.exists(markdownPath) && Files.isRegularFile(markdownPath) then
      return Right(markdownPath)

    // 4. Not found - build helpful error message
    val availableFiles = listMarkdownFiles(searchDir)
    val availableList = if availableFiles.nonEmpty then
      "\n\nFound in current directory:\n" + availableFiles.map(f => s"  - $f").mkString("\n")
    else
      ""

    Left(s"✗ Input file not found: $deckName (tried as-is, .md, .markdown)$availableList")

  /**
   * Determine output directory path from deck name.
   *
   * Strips file extensions (.md, .markdown) to get clean directory name.
   * Standard CLI behavior: file.md → file/
   *
   * Examples:
   * - "my-preso.md" → Path("my-preso")
   * - "talks/presentation.markdown" → Path("talks/presentation")
   * - "deck" → Path("deck")
   *
   * @param deckName Deck name (may include path separators and file extensions)
   * @return Output directory path (without file extension)
   */
  def determineOutputDir(deckName: String): Path =
    val withoutExtension =
      if deckName.endsWith(".md") then
        deckName.stripSuffix(".md")
      else if deckName.endsWith(".markdown") then
        deckName.stripSuffix(".markdown")
      else
        deckName
    Paths.get(withoutExtension)

  /**
   * Ensure output directory exists (create if missing).
   *
   * Creates parent directories as needed (mkdir -p behavior).
   * Succeeds if directory already exists.
   *
   * @param outputDir Path to output directory
   * @return IO effect that creates directory
   */
  def ensureOutputDirExists(outputDir: Path): IO[Unit] =
    IO {
      if !Files.exists(outputDir) then
        Files.createDirectories(outputDir)
      ()
    }

  /**
   * List available markdown files in directory.
   *
   * Used for helpful error messages when input file not found.
   *
   * @param dir Directory to search
   * @return List of .md and .markdown filenames
   */
  private def listMarkdownFiles(dir: Path): List[String] =
    if !Files.isDirectory(dir) then
      List.empty
    else
      try
        Files.list(dir)
          .iterator()
          .asScala
          .toList
          .filter(p => Files.isRegularFile(p))
          .map(_.getFileName.toString)
          .filter(name => name.endsWith(".md") || name.endsWith(".markdown"))
          .sorted
      catch
        case _: Exception => List.empty

end PathResolver
