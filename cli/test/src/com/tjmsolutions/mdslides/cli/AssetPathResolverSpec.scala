package com.tjmsolutions.mdslides.cli

import munit.FunSuite
import java.nio.file.{Path, Paths}

class AssetPathResolverSpec extends FunSuite:

  // Phase 3: Asset Path Resolution Tests

  test("AssetPathResolver.relativeImagePath - generates relative path"):
    val imagePath = Paths.get("images/diagram.png")
    val relative = AssetPathResolver.relativeImagePath(imagePath)

    assertEquals(relative, "images/diagram.png")

  test("AssetPathResolver.relativeImagePath - handles nested paths"):
    val imagePath = Paths.get("images/diagrams/architecture.svg")
    val relative = AssetPathResolver.relativeImagePath(imagePath)

    assertEquals(relative, "images/diagrams/architecture.svg")

  test("AssetPathResolver.relativeBackgroundPath - generates relative path"):
    val bgPath = Paths.get("backgrounds/title.png")
    val relative = AssetPathResolver.relativeBackgroundPath(bgPath)

    assertEquals(relative, "backgrounds/title.png")

  test("AssetPathResolver.relativeBackgroundPath - handles nested paths"):
    val bgPath = Paths.get("backgrounds/retisio/title-page.png")
    val relative = AssetPathResolver.relativeBackgroundPath(bgPath)

    assertEquals(relative, "backgrounds/retisio/title-page.png")

  test("AssetPathResolver.relativeImagePath - normalizes path separators"):
    // Ensure consistent forward slashes for HTML/CSS
    val imagePath = Paths.get("images", "subfolder", "image.png")
    val relative = AssetPathResolver.relativeImagePath(imagePath)

    // Should use forward slashes for web compatibility
    assert(relative.contains("/"), s"Should contain forward slashes: $relative")
    assert(!relative.contains("\\"), s"Should not contain backslashes: $relative")

end AssetPathResolverSpec
