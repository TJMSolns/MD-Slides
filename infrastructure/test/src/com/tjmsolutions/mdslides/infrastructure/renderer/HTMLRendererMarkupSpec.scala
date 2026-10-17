package com.tjmsolutions.mdslides.infrastructure.renderer

import com.tjmsolutions.mdslides.infrastructure.parser.MarkdownParser

/** Valid HTML for headings and captions, and a visible fallback for diagrams that could not be pre-rendered. */
class HTMLRendererMarkupSpec extends munit.FunSuite:

  private def render(md: String, diagrams: Map[String, String] = Map.empty) =
    HTMLRenderer.renderDeck(MarkdownParser.parse(md).toOption.get, preRenderedDiagrams = diagrams)

  private val diagramDeck =
    "---\ntemplate: diagram\ncaption: The **pipeline**\n---\n\n## Flow *now*\n\n```mermaid\ngraph LR\n  A-->B\n```\n"

  test("headings and captions contain inline text, not paragraphs") {
    val html = render(diagramDeck)
    assert(!html.contains("<h2 class=\"slide-heading\"") || !html.matches("(?s).*<h2 class=\"slide-heading\"[^>]*><p>.*"), "h2 contains <p>")
    assert(html.contains("<em>now</em></h2>"), "heading inline formatting")
    assert(html.contains("""<p class="diagram-caption">The <strong>pipeline</strong></p>"""), "caption")
  }

  test("a diagram that could not be pre-rendered shows its source and a notice") {
    val html = render(diagramDeck, Map("graph LR\n  A-->B" -> ""))
    assert(html.contains("mermaid-fallback"), "fallback container")
    assert(html.contains("A--&gt;B"), "escaped source shown")
    assert(html.contains("mermaid-cli"), "install notice")
  }

  test("the page declares its language (WCAG 3.1.1)") {
    assert(render(diagramDeck).contains("<html lang=\"en\""))
  }

  test("dark theme code colours meet WCAG 2.1 AA on the code background") {
    import com.tjmsolutions.mdslides.domain.{ContrastCalculator, Theme}
    val html = HTMLRenderer.renderDeck(MarkdownParser.parse(diagramDeck).toOption.get, Theme.dark)
    List("#fa4485", "#918c75").foreach { c =>
      assert(html.contains(c), s"override $c missing")
      assert(ContrastCalculator.contrastRatio(c, "#23241f") >= 4.5, c)
    }
  }

  test("light theme code colours meet WCAG 2.1 AA on light and corporate code backgrounds") {
    import com.tjmsolutions.mdslides.domain.{ContrastCalculator, Theme}
    val html = HTMLRenderer.renderDeck(MarkdownParser.parse(diagramDeck).toOption.get, Theme.light)
    for c <- List("#c72838", "#ad4b07", "#626a73", "#1e7834"); bg <- List("#f4f4f4", "#e9ecef") do
      assert(html.contains(c), s"override $c missing")
      assert(ContrastCalculator.contrastRatio(c, bg) >= 4.5, s"$c on $bg")
  }
