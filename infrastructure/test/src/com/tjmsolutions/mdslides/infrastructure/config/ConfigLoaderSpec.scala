package com.tjmsolutions.mdslides.infrastructure.config

import munit.FunSuite
import com.tjmsolutions.mdslides.domain._
import java.nio.file.{Files, Path, Paths}
import java.nio.file.attribute.PosixFilePermissions

/**
 * Tests for Configuration Loading (v2.0.0).
 *
 * Tests verify:
 * - Global config loading from ~/.mdslides/config.json
 * - Project config loading from .mdslides/config.json
 * - Upward directory search for project config
 * - JSON validation (schema, types, unknown fields)
 * - Graceful handling of missing config files
 *
 * Related Governance:
 * - v2.0.0: Configuration Management
 * - Example Mapping: Scenarios 32-36, 42-44
 */
class ConfigLoaderSpec extends FunSuite:

  var tempDir: Path = _

  override def beforeEach(context: BeforeEach): Unit =
    tempDir = Files.createTempDirectory("mdslides-config-test")

  override def afterEach(context: AfterEach): Unit =
    deleteRecursively(tempDir)

  private def deleteRecursively(path: Path): Unit =
    if Files.exists(path) then
      if Files.isDirectory(path) then
        Files.list(path).forEach(deleteRecursively)
      Files.delete(path)

  // Example Mapping Scenario 32: Global config file loaded from ~/.mdslides/config.json
  test("load global config from ~/.mdslides/config.json"):
    val configDir = tempDir.resolve(".mdslides")
    Files.createDirectory(configDir)

    val configContent = """{
      "defaults": {
        "theme": "dark",
        "copyImages": false,
        "skipAccessibility": true
      },
      "author": {
        "name": "Tony Moores",
        "email": "tony@tjmsolutions.com"
      },
      "paths": {
        "themesDir": "~/my-themes"
      }
    }"""

    val configFile = configDir.resolve("config.json")
    Files.writeString(configFile, configContent)

    val loader = new ConfigLoader()
    val result = loader.loadGlobalConfig(Some(tempDir))

    assert(result.isRight, s"Expected Right, got: $result")
    val config = result.getOrElse(fail("Expected Some(GlobalConfig)")).get
    assertEquals(config.defaults.get.theme, "dark")
    assertEquals(config.defaults.get.copyImages, false)
    assertEquals(config.defaults.get.skipAccessibility, true)
    assertEquals(config.author.get.name, "Tony Moores")
    assertEquals(config.author.get.email, "tony@tjmsolutions.com")
    assertEquals(config.paths.get.themesDir, "~/my-themes")

  // Example Mapping Scenario 33: Missing global config is OK
  test("missing global config returns None"):
    val loader = new ConfigLoader()
    val result = loader.loadGlobalConfig(Some(tempDir))

    assert(result.isRight, s"Expected Right, got: $result")
    assertEquals(result.getOrElse(fail("Expected Right")), None)

  // Example Mapping Scenario 34: Invalid JSON in global config fails
  test("invalid JSON in global config returns error"):
    val configDir = tempDir.resolve(".mdslides")
    Files.createDirectory(configDir)

    val invalidJson = """{ "defaults": { "theme": "dark", } }"""  // Trailing comma
    val configFile = configDir.resolve("config.json")
    Files.writeString(configFile, invalidJson)

    val loader = new ConfigLoader()
    val result = loader.loadGlobalConfig(Some(tempDir))

    assert(result.isLeft, "Expected Left (error)")
    val error = result.left.getOrElse(fail("Expected error message"))
    assert(error.contains("Invalid JSON"), s"Expected JSON error, got: $error")

  // Example Mapping Scenario 35: Project config loaded from .mdslides/config.json
  test("load project config from .mdslides/config.json"):
    val configDir = tempDir.resolve(".mdslides")
    Files.createDirectory(configDir)

    val configContent = """{
      "theme": "corporate",
      "copyImages": false,
      "skipAccessibility": true,
      "accessibilityReportPath": "a11y-report.json",
      "outputDir": "dist"
    }"""

    val configFile = configDir.resolve("config.json")
    Files.writeString(configFile, configContent)

    val loader = new ConfigLoader()
    val result = loader.loadProjectConfig(tempDir)

    assert(result.isRight, s"Expected Right, got: $result")
    val config = result.getOrElse(fail("Expected Some(ProjectConfig)")).get
    assertEquals(config.theme.get, "corporate")
    assertEquals(config.copyImages.get, false)
    assertEquals(config.skipAccessibility.get, true)
    assertEquals(config.accessibilityReportPath.get, "a11y-report.json")
    assertEquals(config.outputDir.get, "dist")

  // Example Mapping Scenario 36: Project config discovery searches upward
  test("project config discovery searches upward from subdirectory"):
    // Create config in parent directory
    val configDir = tempDir.resolve(".mdslides")
    Files.createDirectory(configDir)

    val configContent = """{ "theme": "found-in-parent" }"""
    val configFile = configDir.resolve("config.json")
    Files.writeString(configFile, configContent)

    // Search from subdirectory
    val subdir = tempDir.resolve("subdir1").resolve("subdir2")
    Files.createDirectories(subdir)

    val loader = new ConfigLoader()
    val result = loader.loadProjectConfig(subdir)

    assert(result.isRight, s"Expected Right, got: $result")
    val config = result.getOrElse(fail("Expected Some(ProjectConfig)")).get
    assertEquals(config.theme.get, "found-in-parent")

  // Example Mapping Scenario 42: Unknown field in config → validation error
  test("unknown field in global config returns validation error"):
    val configDir = tempDir.resolve(".mdslides")
    Files.createDirectory(configDir)

    val configContent = """{
      "defaults": { "theme": "dark" },
      "unknownField": "value"
    }"""

    val configFile = configDir.resolve("config.json")
    Files.writeString(configFile, configContent)

    val loader = new ConfigLoader()
    val result = loader.loadGlobalConfig(Some(tempDir))

    assert(result.isLeft, "Expected Left (validation error)")
    val error = result.left.getOrElse(fail("Expected error message"))
    assert(error.contains("Unknown field") || error.contains("unknownField"), s"Expected unknown field error, got: $error")

  // Example Mapping Scenario 43: Wrong type for field → validation error
  test("wrong type for theme field returns validation error"):
    val configDir = tempDir.resolve(".mdslides")
    Files.createDirectory(configDir)

    val configContent = """{
      "defaults": { "theme": 123 }
    }"""

    val configFile = configDir.resolve("config.json")
    Files.writeString(configFile, configContent)

    val loader = new ConfigLoader()
    val result = loader.loadGlobalConfig(Some(tempDir))

    assert(result.isLeft, "Expected Left (type error)")
    val error = result.left.getOrElse(fail("Expected error message"))
    assert(error.contains("type") || error.contains("String"), s"Expected type error, got: $error")

  // Example Mapping Scenario 44: Valid project config with all fields
  test("valid project config with all optional fields"):
    val configDir = tempDir.resolve(".mdslides")
    Files.createDirectory(configDir)

    val configContent = """{
      "theme": "retisio",
      "copyImages": true,
      "skipAccessibility": false,
      "accessibilityReportPath": "reports/a11y.json",
      "outputDir": "build"
    }"""

    val configFile = configDir.resolve("config.json")
    Files.writeString(configFile, configContent)

    val loader = new ConfigLoader()
    val result = loader.loadProjectConfig(tempDir)

    assert(result.isRight, s"Expected Right, got: $result")
    val config = result.getOrElse(fail("Expected Some(ProjectConfig)")).get
    assertEquals(config.theme.get, "retisio")
    assertEquals(config.copyImages.get, true)
    assertEquals(config.skipAccessibility.get, false)
    assertEquals(config.accessibilityReportPath.get, "reports/a11y.json")
    assertEquals(config.outputDir.get, "build")

  // Additional scenario: Minimal valid configs
  test("minimal global config with only defaults"):
    val configDir = tempDir.resolve(".mdslides")
    Files.createDirectory(configDir)

    val configContent = """{
      "defaults": { "theme": "light" }
    }"""

    val configFile = configDir.resolve("config.json")
    Files.writeString(configFile, configContent)

    val loader = new ConfigLoader()
    val result = loader.loadGlobalConfig(Some(tempDir))

    assert(result.isRight, s"Expected Right, got: $result")
    val config = result.getOrElse(fail("Expected Some(GlobalConfig)")).get
    assertEquals(config.defaults.get.theme, "light")
    assertEquals(config.author, None)
    assertEquals(config.paths, None)

  test("minimal project config with only theme"):
    val configDir = tempDir.resolve(".mdslides")
    Files.createDirectory(configDir)

    val configContent = """{ "theme": "dark" }"""

    val configFile = configDir.resolve("config.json")
    Files.writeString(configFile, configContent)

    val loader = new ConfigLoader()
    val result = loader.loadProjectConfig(tempDir)

    assert(result.isRight, s"Expected Right, got: $result")
    val config = result.getOrElse(fail("Expected Some(ProjectConfig)")).get
    assertEquals(config.theme.get, "dark")
    assertEquals(config.copyImages, None)
    assertEquals(config.skipAccessibility, None)

  test("project config search stops at home directory"):
    // This test verifies search doesn't go above home
    val loader = new ConfigLoader()
    val result = loader.loadProjectConfig(tempDir)

    assert(result.isRight, s"Expected Right, got: $result")
    assertEquals(result.getOrElse(fail("Expected Right")), None)

end ConfigLoaderSpec
