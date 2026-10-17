package com.tjmsolutions.mdslides.cli

import munit.FunSuite

/**
 * Tests for --watch flag parsing (US-018).
 *
 * Rules:
 * - R1: --watch sets watch=true
 * - R2: -w sets watch=true
 * - R3: watch defaults to false when flag absent
 */
class CLIArgumentsWatchSpec extends FunSuite:

  // R3 — default
  test("US-018 / R3 — watch defaults to false when --watch flag absent"):
    val result = CLIArguments.parse(Array("render", "my-deck"))
    assert(result.isRight, s"Expected Right but got: $result")
    assertEquals(result.map(_.watch), Right(false))

  // R1 — long flag
  test("US-018 / R1 — --watch flag sets watch=true"):
    val result = CLIArguments.parse(Array("render", "my-deck", "--watch"))
    assert(result.isRight, s"Expected Right but got: $result")
    assertEquals(result.map(_.watch), Right(true))

  // R2 — short flag
  test("US-018 / R2 — -w flag sets watch=true"):
    val result = CLIArguments.parse(Array("render", "my-deck", "-w"))
    assert(result.isRight, s"Expected Right but got: $result")
    assertEquals(result.map(_.watch), Right(true))

  // R1 — flag works in explicit form too
  test("US-018 / R1 — --watch works with explicit -i/-o form"):
    val result = CLIArguments.parse(Array("render", "-i", "deck.md", "-o", "out/", "--watch"))
    assert(result.isRight, s"Expected Right but got: $result")
    assertEquals(result.map(_.watch), Right(true))
