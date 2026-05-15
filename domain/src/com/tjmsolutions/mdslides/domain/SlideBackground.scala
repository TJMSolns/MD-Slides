package com.tjmsolutions.mdslides.domain

/**
 * Slide background configuration.
 *
 * Supports two forms:
 * 1. Simple string path: "backgrounds/custom.png"
 * 2. Extended configuration: BackgroundConfig with opacity and future extensions
 *
 * Design Rationale (PDR-011):
 * - Background images are presentation properties, not content (not a slot)
 * - Belongs with templateName (metadata) not slots (content)
 * - Per-slide backgrounds override template and theme defaults
 *
 * Fallback Chain:
 * 1. Slide.backgroundImage (highest priority - per-slide override)
 * 2. Theme.templateBackgrounds[Slide.templateName] (template-specific)
 * 3. Theme.background.image (theme default)
 * 4. Theme.background.color (always present - color-only background)
 *
 * Related:
 * - US-011: Per-Slide Background Images
 * - PDR-011: Background Image Architecture
 */
type SlideBackground = String | BackgroundConfig

/**
 * Extended background configuration supporting opacity and future extensions.
 *
 * @param image Path to background image (relative to presentation directory)
 * @param opacity Optional opacity value between 0.0 (transparent) and 1.0 (opaque)
 *
 * Future extensions could include:
 * - position: BackgroundPosition (cover, contain, center, etc.)
 * - blendMode: String ("multiply", "screen", "overlay", etc.)
 * - gradient: Option[Gradient] for gradient overlays
 */
case class BackgroundConfig(
  image: String,
  opacity: Option[Double] = None
):
  require(
    opacity.forall(o => o >= 0.0 && o <= 1.0),
    s"opacity must be between 0.0 and 1.0, got: ${opacity.getOrElse("N/A")}"
  )

  /**
   * Get the image path from this background configuration.
   */
  def imagePath: String = image

end BackgroundConfig

object SlideBackground:
  /**
   * Create a simple string-based background.
   */
  def fromString(path: String): SlideBackground = path

  /**
   * Create a background from a BackgroundConfig.
   */
  def fromConfig(config: BackgroundConfig): SlideBackground = config

  /**
   * Extract the image path from any SlideBackground (string or config).
   */
  def getImagePath(background: SlideBackground): String =
    background match
      case s: String => s
      case c: BackgroundConfig => c.imagePath

  /**
   * Get opacity from SlideBackground (None for simple string backgrounds).
   */
  def getOpacity(background: SlideBackground): Option[Double] =
    background match
      case _: String => None
      case c: BackgroundConfig => c.opacity

end SlideBackground
