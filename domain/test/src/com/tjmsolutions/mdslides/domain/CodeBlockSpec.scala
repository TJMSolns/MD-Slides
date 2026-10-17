package com.tjmsolutions.mdslides.domain

import munit.FunSuite

/**
 * Tests for CodeBlock value object.
 *
 * Related Governance:
 * - US-004: Code Block Support
 * - PDR-006: Code Block Rendering Limits
 * - ADR-011: Syntax Highlighting Approach
 */
class CodeBlockSpec extends FunSuite:

  // ===== Basic Code Block Creation =====

  test("CodeBlock - create with code only"):
    val code = "def hello() = println(\"Hello, World!\")"
    val block = CodeBlock(code)
    assertEquals(block.code, code)
    assertEquals(block.language, None)

  test("CodeBlock - create with language"):
    val code = "def hello() = println(\"Hello, World!\")"
    val block = CodeBlock(code, Some("scala"))
    assertEquals(block.code, code)
    assertEquals(block.language, Some("scala"))

  test("CodeBlock - create with empty code"):
    val block = CodeBlock("")
    assertEquals(block.code, "")
    assertEquals(block.lineCount, 0)

  // ===== Line Counting =====

  test("CodeBlock.lineCount - single line"):
    val code = "val x = 42"
    val block = CodeBlock(code)
    assertEquals(block.lineCount, 1)

  test("CodeBlock.lineCount - multiple lines"):
    val code = """def add(a: Int, b: Int): Int =
                 |  a + b""".stripMargin
    val block = CodeBlock(code)
    assertEquals(block.lineCount, 2)

  test("CodeBlock.lineCount - empty lines count"):
    val code = """def example():
                 |
                 |  println("test")""".stripMargin
    val block = CodeBlock(code)
    assertEquals(block.lineCount, 3)

  test("CodeBlock.lineCount - trailing newline"):
    val code = "val x = 1\nval y = 2\n"
    val block = CodeBlock(code)
    assertEquals(block.lineCount, 3) // Includes trailing newline as empty line

  // ===== 20-Line Guideline =====

  test("CodeBlock.exceedsGuideline - under 20 lines"):
    val code = (1 to 15).map(i => s"line $i").mkString("\n")
    val block = CodeBlock(code)
    assertEquals(block.exceedsGuideline, false)

  test("CodeBlock.exceedsGuideline - exactly 20 lines"):
    val code = (1 to 20).map(i => s"line $i").mkString("\n")
    val block = CodeBlock(code)
    assertEquals(block.exceedsGuideline, false)

  test("CodeBlock.exceedsGuideline - over 20 lines"):
    val code = (1 to 25).map(i => s"line $i").mkString("\n")
    val block = CodeBlock(code)
    assertEquals(block.exceedsGuideline, true)

  test("CodeBlock.exceedsGuideline - significantly over (50 lines)"):
    val code = (1 to 50).map(i => s"line $i").mkString("\n")
    val block = CodeBlock(code)
    assertEquals(block.exceedsGuideline, true)

  // ===== Common Languages =====

  test("CodeBlock - Scala example"):
    val code = """case class User(id: Int, name: String)
                 |
                 |val user = User(1, "Alice")""".stripMargin
    val block = CodeBlock(code, Some("scala"))
    assertEquals(block.language, Some("scala"))
    assertEquals(block.lineCount, 3)

  test("CodeBlock - Python example"):
    val code = """def greet(name):
                 |    return f"Hello, {name}!" """.stripMargin
    val block = CodeBlock(code, Some("python"))
    assertEquals(block.language, Some("python"))

  test("CodeBlock - JavaScript example"):
    val code = """const add = (a, b) => a + b;
                 |console.log(add(2, 3));""".stripMargin
    val block = CodeBlock(code, Some("javascript"))
    assertEquals(block.language, Some("javascript"))

  test("CodeBlock - SQL example"):
    val code = """SELECT name, age
                 |FROM users
                 |WHERE age > 18;""".stripMargin
    val block = CodeBlock(code, Some("sql"))
    assertEquals(block.language, Some("sql"))

  // ===== Language Normalization =====

  test("CodeBlock - language preserved as-is"):
    val block = CodeBlock("code", Some("Scala"))
    assertEquals(block.language, Some("Scala"))

  test("CodeBlock - empty language treated as None"):
    val block1 = CodeBlock("code", Some(""))
    val block2 = CodeBlock("code", None)
    assertEquals(block1.language, Some(""))
    assert(block1.language != block2.language)

end CodeBlockSpec
