package com.tjmsolutions.mdslides.domain

/**
 * Test suite for ColumnConfiguration and ColumnSpec value objects
 *
 * Domain Model: doc/domain-models/aggregates/template-configuration-aggregate.md
 * Acceptance Criteria: doc/acceptance-criteria/template-configuration-acceptance-criteria.md (AC-6)
 */
class ColumnConfigurationSpec extends munit.FunSuite:

  test("isValidCSSWidth should accept percentage widths") {
    assert(ColumnSpec.isValidCSSWidth("40%"))
    assert(ColumnSpec.isValidCSSWidth("50%"))
    assert(ColumnSpec.isValidCSSWidth("100%"))
  }

  test("isValidCSSWidth should accept pixel widths") {
    assert(ColumnSpec.isValidCSSWidth("300px"))
    assert(ColumnSpec.isValidCSSWidth("400px"))
  }

  test("isValidCSSWidth should accept rem/em widths") {
    assert(ColumnSpec.isValidCSSWidth("20rem"))
    assert(ColumnSpec.isValidCSSWidth("15em"))
  }

  test("isValidCSSWidth should reject invalid widths") {
    assert(!ColumnSpec.isValidCSSWidth("invalid"))
    assert(!ColumnSpec.isValidCSSWidth("40"))  // Missing unit
    assert(!ColumnSpec.isValidCSSWidth(""))
  }

  test("widthOrDefault should return width when specified") {
    val spec = ColumnSpec(width = Some("40%"), colors = None)
    assertEquals(spec.widthOrDefault, "40%")
  }

  test("widthOrDefault should return 50% when width not specified") {
    val spec = ColumnSpec(width = None, colors = None)
    assertEquals(spec.widthOrDefault, "50%")
  }

  test("ColumnSpec.validate should accept valid widths") {
    val spec = ColumnSpec(width = Some("40%"), colors = None)
    assertEquals(spec.validate(), Right(spec))
  }

  test("ColumnSpec.validate should reject invalid widths") {
    val spec = ColumnSpec(width = Some("invalid"), colors = None)
    assert(spec.validate().isLeft)
  }

  test("ColumnConfiguration.validate should accept valid config") {
    val config = ColumnConfiguration(
      leftColumn = ColumnSpec(width = Some("40%"), colors = None),
      rightColumn = ColumnSpec(width = Some("60%"), colors = None)
    )
    assertEquals(config.validate(), Right(config))
  }

  test("ColumnConfiguration.validate should reject invalid widths") {
    val config = ColumnConfiguration(
      leftColumn = ColumnSpec(width = Some("invalid"), colors = None),
      rightColumn = ColumnSpec(width = Some("60%"), colors = None)
    )
    assert(config.validate().isLeft)
  }

  test("validateTotalWidth should warn when total exceeds 100%") {
    val config = ColumnConfiguration(
      leftColumn = ColumnSpec(width = Some("70%"), colors = None),
      rightColumn = ColumnSpec(width = Some("50%"), colors = None)
    )
    val warning = config.validateTotalWidth()
    assert(warning.isDefined)
    assert(warning.get.contains("120%"))
  }

  test("validateTotalWidth should not warn when total is 100%") {
    val config = ColumnConfiguration(
      leftColumn = ColumnSpec(width = Some("40%"), colors = None),
      rightColumn = ColumnSpec(width = Some("60%"), colors = None)
    )
    assertEquals(config.validateTotalWidth(), None)
  }
