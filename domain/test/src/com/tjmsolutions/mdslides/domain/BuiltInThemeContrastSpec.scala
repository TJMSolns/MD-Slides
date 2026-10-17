package com.tjmsolutions.mdslides.domain

/** Every built-in theme meets WCAG 2.1 AA contrast, as the docs promise. */
class BuiltInThemeContrastSpec extends munit.FunSuite:

  List(Theme.light, Theme.dark, Theme.corporate).foreach { theme =>
    test(s"built-in theme '${theme.name}' passes WCAG 2.1 AA contrast") {
      val result = ThemeValidator(theme).validateContrast()
      assert(result.passes, result.warnings.map(_.message).mkString("; "))
    }
  }
