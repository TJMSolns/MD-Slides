package com.tjmsolutions.mdslides.domain

/**
 * Test suite for VerticalAlignment value object
 *
 * Domain Model: doc/domain-models/aggregates/template-configuration-aggregate.md
 * Acceptance Criteria: doc/acceptance-criteria/template-configuration-acceptance-criteria.md (AC-2)
 */
class VerticalAlignmentSpec extends munit.FunSuite:

  test("VerticalAlignment should define exactly three alignment values") {
    val alignments = VerticalAlignment.values
    assertEquals(alignments.length, 3)
    assertEquals(alignments.map(_.toString).toSet, Set("Top", "Center", "Bottom"))
  }

  test("VerticalAlignment.Top should map to flex-start CSS value") {
    assertEquals(VerticalAlignment.Top.toCSSValue, "flex-start")
  }

  test("VerticalAlignment.Center should map to center CSS value") {
    assertEquals(VerticalAlignment.Center.toCSSValue, "center")
  }

  test("VerticalAlignment.Bottom should map to flex-end CSS value") {
    assertEquals(VerticalAlignment.Bottom.toCSSValue, "flex-end")
  }

  test("fromString should parse 'top' case-insensitively") {
    assertEquals(VerticalAlignment.fromString("top"), Right(VerticalAlignment.Top))
    assertEquals(VerticalAlignment.fromString("Top"), Right(VerticalAlignment.Top))
    assertEquals(VerticalAlignment.fromString("TOP"), Right(VerticalAlignment.Top))
  }

  test("fromString should parse 'center' case-insensitively") {
    assertEquals(VerticalAlignment.fromString("center"), Right(VerticalAlignment.Center))
    assertEquals(VerticalAlignment.fromString("Center"), Right(VerticalAlignment.Center))
    assertEquals(VerticalAlignment.fromString("CENTER"), Right(VerticalAlignment.Center))
  }

  test("fromString should parse 'bottom' case-insensitively") {
    assertEquals(VerticalAlignment.fromString("bottom"), Right(VerticalAlignment.Bottom))
    assertEquals(VerticalAlignment.fromString("Bottom"), Right(VerticalAlignment.Bottom))
    assertEquals(VerticalAlignment.fromString("BOTTOM"), Right(VerticalAlignment.Bottom))
  }

  test("fromString should reject 'middle' as invalid") {
    val result = VerticalAlignment.fromString("middle")
    assert(result.isLeft)
    val Left(error) = result: @unchecked
    assert(error.contains("Invalid vertical alignment"))
    assert(error.contains("middle"))
    assert(error.contains("top, center, bottom"))
  }

  test("fromString should reject invalid values") {
    assert(VerticalAlignment.fromString("left").isLeft)
    assert(VerticalAlignment.fromString("right").isLeft)
    assert(VerticalAlignment.fromString("invalid").isLeft)
    assert(VerticalAlignment.fromString("").isLeft)
  }
