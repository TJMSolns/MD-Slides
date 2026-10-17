package com.tjmsolutions.mdslides.cli

import java.nio.file.Path

/**
 * Asset path resolution for HTML output.
 *
 * Generates relative paths for assets (images, backgrounds) to ensure
 * output directory is portable (can be moved/zipped/shared).
 *
 * All paths use forward slashes for web compatibility (HTML/CSS).
 */
object AssetPathResolver:

  /**
   * Generate relative path for content image.
   *
   * Input: Path("images/diagram.png") or Path("images", "diagram.png")
   * Output: "images/diagram.png" (relative to index.html)
   *
   * @param imagePath Path to image (relative or absolute)
   * @return Relative path string with forward slashes
   */
  def relativeImagePath(imagePath: Path): String =
    // Convert to string and normalize path separators to forward slashes
    imagePath.toString.replace('\\', '/')

  /**
   * Generate relative path for background image.
   *
   * Input: Path("backgrounds/title.png")
   * Output: "backgrounds/title.png" (relative to index.html)
   *
   * @param backgroundPath Path to background image
   * @return Relative path string with forward slashes
   */
  def relativeBackgroundPath(backgroundPath: Path): String =
    // Same logic as image paths - all assets relative to index.html
    backgroundPath.toString.replace('\\', '/')

end AssetPathResolver
