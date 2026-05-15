package com.tjmsolutions.mdslides.cli

import munit.FunSuite
import java.nio.file.Path

class CLIArgumentsSpec extends FunSuite:

  // Phase 1: Argument Parsing Tests

  test("CLIArguments.parse - simple form with deck name"):
    val args = Array("render", "my-preso")
    val result = CLIArguments.parse(args)

    assert(result.isRight, s"Expected Right but got: $result")
    result.foreach { cliArgs =>
      assertEquals(cliArgs.deckName, Some("my-preso"))
      assertEquals(cliArgs.inputFile, None)
      assertEquals(cliArgs.outputDir, None)
      assertEquals(cliArgs.themeName, "light")  // default theme
      assertEquals(cliArgs.copyImages, true)     // default true
    }

  test("CLIArguments.parse - explicit form with -i and -o"):
    val args = Array("render", "-i", "slides.md", "-o", "output")
    val result = CLIArguments.parse(args)

    assert(result.isRight, s"Expected Right but got: $result")
    result.foreach { cliArgs =>
      assertEquals(cliArgs.deckName, None)
      assertEquals(cliArgs.inputFile, Some(Path.of("slides.md")))
      assertEquals(cliArgs.outputDir, Some(Path.of("output")))
      assertEquals(cliArgs.themeName, "light")
      assertEquals(cliArgs.copyImages, true)
    }

  test("CLIArguments.parse - simple form with --theme"):
    val args = Array("render", "my-preso", "--theme", "dark")
    val result = CLIArguments.parse(args)

    assert(result.isRight)
    result.foreach { cliArgs =>
      assertEquals(cliArgs.deckName, Some("my-preso"))
      assertEquals(cliArgs.themeName, "dark")
    }

  test("CLIArguments.parse - explicit form with --theme"):
    val args = Array("render", "-i", "slides.md", "-o", "output", "--theme", "retisio")
    val result = CLIArguments.parse(args)

    assert(result.isRight)
    result.foreach { cliArgs =>
      assertEquals(cliArgs.inputFile, Some(Path.of("slides.md")))
      assertEquals(cliArgs.themeName, "retisio")
    }

  test("CLIArguments.parse - --no-copy-images flag"):
    val args = Array("render", "my-preso", "--no-copy-images")
    val result = CLIArguments.parse(args)

    assert(result.isRight)
    result.foreach { cliArgs =>
      assertEquals(cliArgs.copyImages, false)
    }

  test("CLIArguments.parse - long form --input and --output"):
    val args = Array("render", "--input", "slides.md", "--output", "dist")
    val result = CLIArguments.parse(args)

    assert(result.isRight)
    result.foreach { cliArgs =>
      assertEquals(cliArgs.inputFile, Some(Path.of("slides.md")))
      assertEquals(cliArgs.outputDir, Some(Path.of("dist")))
    }

  test("CLIArguments.parse - error when mixing simple and explicit forms"):
    val args = Array("render", "my-preso", "-i", "slides.md")
    val result = CLIArguments.parse(args)

    assert(result.isLeft, "Expected Left (error) when mixing forms")
    result.left.foreach { error =>
      assert(error.contains("Cannot mix"), s"Error message should mention mixing: $error")
    }

  test("CLIArguments.parse - error when missing deck name in simple form"):
    val args = Array("render")
    val result = CLIArguments.parse(args)

    assert(result.isLeft, "Expected Left (error) when missing deck name")
    result.left.foreach { error =>
      assert(error.contains("required") || error.contains("DECK_NAME"),
        s"Error should mention missing argument: $error")
    }

  test("CLIArguments.parse - error when missing -i in explicit form"):
    val args = Array("render", "-o", "output")
    val result = CLIArguments.parse(args)

    assert(result.isLeft, "Expected Left (error) when missing -i")
    result.left.foreach { error =>
      assert(error.contains("-i") || error.contains("input"),
        s"Error should mention missing input: $error")
    }

  test("CLIArguments.parse - error when missing -o in explicit form"):
    val args = Array("render", "-i", "slides.md")
    val result = CLIArguments.parse(args)

    assert(result.isLeft, "Expected Left (error) when missing -o")
    result.left.foreach { error =>
      assert(error.contains("-o") || error.contains("output"),
        s"Error should mention missing output: $error")
    }

end CLIArgumentsSpec
