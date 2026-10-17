package com.tjmsolutions.mdslides.domain

import munit.FunSuite

/**
 * Tests for ValidationError line number support (US-019).
 *
 * Rules:
 * - R1: lineNumber field optional, defaults to None
 * - R2: displayMessage includes (line N) when lineNumber=Some(N)
 * - R2: displayMessage omits location when lineNumber=None
 */
class ValidationErrorLineNumberSpec extends FunSuite:

  private val id = SlideId.unsafe(3)

  // R1 — StructureError default
  test("US-019 / R1 — StructureError lineNumber defaults to None"):
    val err = ValidationError.StructureError(id, "Required slot missing")
    assertEquals(err.lineNumber, None)

  // R1 — ContentError default
  test("US-019 / R1 — ContentError lineNumber defaults to None"):
    val err = ValidationError.ContentError(id, "body", "Too long")
    assertEquals(err.lineNumber, None)

  // R1 — DensityWarning default
  test("US-019 / R1 — DensityWarning lineNumber defaults to None"):
    val err = ValidationError.DensityWarning(id, "body", "Dense content")
    assertEquals(err.lineNumber, None)

  // R2 — StructureError with line number in displayMessage
  test("US-019 / R2 — StructureError displayMessage includes (line N) when set"):
    val err = ValidationError.StructureError(id, "Required slot missing", lineNum = Some(42))
    assert(
      err.displayMessage.contains("42"),
      s"displayMessage must include line number 42, got: ${err.displayMessage}"
    )

  // R2 — ContentError with line number in displayMessage
  test("US-019 / R2 — ContentError displayMessage includes (line N) when set"):
    val err = ValidationError.ContentError(id, "body", "Too long", lineNum = Some(17))
    assert(
      err.displayMessage.contains("17"),
      s"displayMessage must include line number 17, got: ${err.displayMessage}"
    )

  // R2 — DensityWarning with line number in displayMessage
  test("US-019 / R2 — DensityWarning displayMessage includes (line N) when set"):
    val err = ValidationError.DensityWarning(id, "body", "Dense", lineNum = Some(99))
    assert(
      err.displayMessage.contains("99"),
      s"displayMessage must include line number 99, got: ${err.displayMessage}"
    )

  // R2 — no line number → no change from current format
  test("US-019 / R2 — displayMessage without lineNumber omits location info"):
    val err = ValidationError.StructureError(id, "Missing slot")
    assert(!err.displayMessage.contains("line"), "displayMessage with no lineNumber must not contain 'line'")
