package com.tjmsolutions.mdslides.domain

/**
 * Content for a template slot.
 *
 * Wraps FormattedContent (parsed markdown with structure).
 * For v0.1.0 compatibility, also supports plain text (auto-converted).
 *
 * Validation constraints (max lines, max words) use plain text counts.
 *
 * Related Governance:
 * - POL-001: Ubiquitous Language Enforcement
 * - ADR-008: Slot-Based Content Model
 * - US-003: Full Markdown Rendering
 */
opaque type SlotContent = FormattedContent

object SlotContent:
  /**
   * Creates SlotContent from FormattedContent.
   *
   * @param content The formatted content
   * @return SlotContent wrapping the formatted content
   */
  def apply(content: FormattedContent): SlotContent = content

  /**
   * Creates SlotContent from plain text or markdown.
   *
   * For validation purposes, strips fenced code blocks before counting lines/words.
   * This ensures code blocks don't count toward body line limits (PDR-006).
   *
   * Note: This is a simplified version for validation only.
   * Full markdown parsing happens in FlexmarkAdapter for rendering.
   *
   * @param text Plain text or markdown text
   * @return SlotContent with single unformatted text span (code blocks excluded)
   */
  def fromPlainText(text: String): SlotContent =
    if text.isEmpty then FormattedContent.empty
    else
      // Remove fenced code blocks for validation (PDR-006)
      val textWithoutCodeBlocks = stripFencedCodeBlocks(text)
      FormattedContent(
        List(TextSpan(textWithoutCodeBlocks, bold = false, italic = false, code = false)),
        List.empty,
        List.empty
      )

  /**
   * Strip fenced code blocks from markdown text.
   *
   * Removes ```...``` blocks to exclude them from line/word counts.
   * Simple regex-based approach for validation purposes.
   *
   * @param text Markdown text potentially containing code blocks
   * @return Text with code blocks removed
   */
  private def stripFencedCodeBlocks(text: String): String =
    // Pattern matches: ``` (optional language) ... ``` across multiple lines
    // Use (?s) for DOTALL mode so . matches newlines
    val fencedCodePattern = "(?s)```[^\\n]*\\n.*?```".r
    fencedCodePattern.replaceAllIn(text, "")

  /**
   * Empty slot content.
   */
  val empty: SlotContent = FormattedContent.empty

  extension (content: SlotContent)
    /**
     * Get underlying FormattedContent.
     */
    def formatted: FormattedContent = content

    /**
     * Get plain text (strip formatting).
     *
     * Used for validation and display.
     */
    def plainText: String =
      val fc: FormattedContent = content
      fc.plainText

    /**
     * Count lines in plain text (newline-separated).
     */
    def lineCount: Int =
      val fc: FormattedContent = content
      fc.lineCount

    /**
     * Count characters in plain text.
     */
    def charCount: Int =
      val fc: FormattedContent = content
      fc.plainText.length

    /**
     * Count words in plain text (whitespace-separated).
     */
    def wordCount: Int =
      val fc: FormattedContent = content
      fc.wordCount
