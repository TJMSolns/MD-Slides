package com.tjmsolutions.mdslides.domain

/**
 * Code block value object - represents a fenced code block in markdown.
 *
 * Supports syntax highlighting via language hint and auto-scaling for long blocks.
 *
 * Invariants:
 * - Code can be empty string (valid case)
 * - Language is optional (None for plain code)
 * - Line count based on newline characters
 *
 * Validation Guidelines:
 * - 20-line guideline (soft limit, warning not error)
 * - Font auto-scaling for blocks > 20 lines
 * - Code blocks don't count toward body's 12-line limit
 *
 * Related Governance:
 * - US-004: Code Block Support
 * - PDR-006: Code Block Rendering Limits (20-line guideline)
 * - ADR-011: Syntax Highlighting Approach (client-side, future enhancement)
 */
case class CodeBlock(
  code: String,
  language: Option[String] = None
):
  /**
   * Count lines in code block.
   *
   * Empty string = 0 lines
   * Single line with no newline = 1 line
   * Lines separated by \n = count of lines
   * Trailing newline creates an additional empty line
   */
  def lineCount: Int =
    if code.isEmpty then 0
    else code.count(_ == '\n') + 1

  /**
   * Check if code block exceeds 20-line guideline.
   *
   * Used for validation warnings (not errors).
   * Blocks > 20 lines trigger auto-scaling in renderer.
   */
  def exceedsGuideline: Boolean =
    lineCount > 20

end CodeBlock

object CodeBlock:
  /**
   * Empty code block.
   */
  val empty: CodeBlock = CodeBlock("")

end CodeBlock
