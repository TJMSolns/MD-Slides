package com.tjmsolutions.mdslides.infrastructure.assets

import java.nio.file.{Path, Paths, Files, StandardCopyOption}

/**
 * Result of copying an image file.
 *
 * @param originalPath Original path as specified in markdown
 * @param destinationPath Relative path in output directory
 * @param sizeBytes Size of the copied file in bytes
 */
case class CopiedImage(
  originalPath: String,
  destinationPath: String,
  sizeBytes: Long
)

/**
 * Copies image assets referenced in slides to output directory.
 *
 * Anticorruption Layer for file I/O operations.
 *
 * Implements US-006: Image Asset Copying
 * Related: ADR-012, PDR-009, PDR-010
 */
object ImageAssetCopier:

  /**
   * Detect if URL is a local file path.
   *
   * Returns true for:
   * - Relative paths: "images/logo.svg", "./images/logo.svg", "../assets/logo.svg"
   * - Absolute paths: "/home/user/images/logo.svg", "C:\\Users\\user\\images\\logo.svg"
   *
   * Returns false for:
   * - HTTP/HTTPS URLs: "http://example.com/logo.png", "https://example.com/logo.png"
   * - Data URLs: "data:image/svg+xml;base64,..."
   * - Protocol-relative URLs: "//cdn.example.com/logo.png"
   * - Empty strings
   *
   * @param url The URL or path to check
   * @return true if URL is a local file path, false otherwise
   */
  def isLocalPath(url: String): Boolean =
    if url.isEmpty then
      false
    else if url.startsWith("http://") || url.startsWith("https://") then
      false
    else if url.startsWith("data:") then
      false
    else if url.startsWith("//") then
      false
    else
      true

  /**
   * Resolve relative path to absolute path.
   *
   * - Relative paths are resolved against baseDir
   * - Absolute paths are returned as-is
   * - HTTP/HTTPS URLs return error
   * - Data URLs return error
   *
   * @param url The URL or path to resolve
   * @param baseDir Base directory for resolving relative paths
   * @return Either error message or resolved absolute path
   */
  def resolvePath(url: String, baseDir: Path): Either[String, Path] =
    if url.startsWith("http://") || url.startsWith("https://") then
      Left(s"Cannot resolve external URL: $url")
    else if url.startsWith("data:") then
      Left(s"Cannot resolve data URL: $url")
    else if url.startsWith("//") then
      Left(s"Cannot resolve protocol-relative URL: $url")
    else
      try
        val imagePath = Paths.get(url)
        val resolved = if imagePath.isAbsolute then
          imagePath
        else
          baseDir.resolve(imagePath).normalize()

        Right(resolved)
      catch
        case e: Exception =>
          Left(s"Invalid path: $url (${e.getMessage})")

  /**
   * Copy images to output directory.
   *
   * - Detects local vs external URLs
   * - Copies only local image files
   * - Preserves directory structure
   * - Validates source files exist
   *
   * @param images List of image URLs from slides
   * @param sourceDir Base directory for resolving relative paths
   * @param outputDir Destination directory for copied images
   * @return Either error message or list of copied files
   */
  def copyImages(
    images: List[String],
    sourceDir: Path,
    outputDir: Path
  ): Either[String, List[CopiedImage]] =
    try
      // Filter to only local paths
      val localImages = images.filter(isLocalPath)

      // Process each local image
      val results = localImages.map { imagePath =>
        for
          // Resolve source path
          sourcePath <- resolvePath(imagePath, sourceDir)

          // Validate source file exists
          _ <- if Files.exists(sourcePath) then
            Right(())
          else
            Left(s"Image file does not exist: $imagePath (expected at: $sourcePath)")

          // Calculate destination path (preserve relative structure)
          relativePath = Paths.get(imagePath)
          destPath = outputDir.resolve(relativePath)

          // Create parent directories if needed
          _ <- try
            Files.createDirectories(destPath.getParent)
            Right(())
          catch
            case e: Exception =>
              Left(s"Failed to create directory: ${destPath.getParent} (${e.getMessage})")

          // Copy file
          _ <- try
            Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
            Right(())
          catch
            case e: Exception =>
              Left(s"Failed to copy image: $imagePath (${e.getMessage})")

          // Get file size
          sizeBytes = Files.size(destPath)

        yield CopiedImage(
          originalPath = imagePath,
          destinationPath = relativePath.toString,
          sizeBytes = sizeBytes
        )
      }

      // Collect results - fail fast on first error
      results.foldLeft[Either[String, List[CopiedImage]]](Right(List.empty)) {
        case (Right(acc), Right(copied)) => Right(acc :+ copied)
        case (Left(error), _) => Left(error)
        case (_, Left(error)) => Left(error)
      }

    catch
      case e: Exception =>
        Left(s"Unexpected error during image copying: ${e.getMessage}")

end ImageAssetCopier
