package com.tjmsolutions.mdslides.infrastructure.renderer

import com.tjmsolutions.mdslides.domain.{Slide, SlideDeck, FormattedContent, TextSpan, Link, CodeBlock, ContentImage, Theme, ListElement, UnorderedListElement, OrderedListElement, UnorderedListElementDeprecated, OrderedListElementDeprecated, ContentElement, ParagraphElement, CodeBlockElement, ImageElement, DiagramElement, MermaidDiagram, TemplateConfiguration, TemplateColors, TableElement, Table, SlotName}
import com.tjmsolutions.mdslides.infrastructure.parser.FlexmarkAdapter
import scalatags.Text.all.*
import java.nio.file.Paths

/**
 * HTML renderer for slides using Scalatags.
 *
 * Renders a SlideDeck to standalone HTML with:
 * - Inline CSS for styling (theme-based)
 * - Inline JavaScript for navigation
 * - Keyboard navigation (arrow keys, space, home, end)
 * - Responsive design
 * - Background images (theme-configured)
 *
 * Related Governance:
 * - ADR-006: Rendering Architecture
 * - PDR-005: Accessibility Requirements (WCAG AA)
 * - US-008: Theme System
 * - US-009: Built-in Themes
 */
object HTMLRenderer:

  /**
   * Map MDSlides theme to highlight.js theme name.
   *
   * Theme mapping (v1.3):
   * - light → github (clean, familiar)
   * - dark → monokai-sublime (high contrast)
   * - corporate → github (light background)
   * - other light themes → github (light background)
   * - unknown → github (default fallback)
   *
   * @param theme MDSlides theme
   * @return highlight.js theme name
   */
  private def highlightJsTheme(theme: Theme): String =
    theme.name match
      case "dark" => "monokai-sublime"
      case _ => "github"  // light, corporate, or unknown

  /**
   * Render a complete SlideDeck to standalone HTML with theme.
   *
   * @param deck The slide deck to render
   * @param theme The theme to apply (default: Theme.light)
   * @param preRenderedDiagrams Map of diagram source -> pre-rendered SVG (for offline support)
   * @param headerTemplate Optional header template (v3.0.0)
   * @param footerTemplate Optional footer template (v3.0.0)
   * @param breakScreen Optional break screen image path (v3.0.0)
   * @return Complete HTML document as string
   */
  def renderDeck(
    deck: SlideDeck,
    theme: Theme = Theme.light,
    preRenderedDiagrams: Map[String, String] = Map.empty,
    headerTemplate: Option[String] = None,
    footerTemplate: Option[String] = None,
    breakScreen: Option[String] = None,
    liveReload: Boolean = false
  ): String =
    val slideHtml = deck.toList.zipWithIndex.map { case (slide, index) =>
      renderSlide(slide, index, deck.slideCount, theme, preRenderedDiagrams, headerTemplate, footerTemplate)
    }

    val hljsTheme = highlightJsTheme(theme)
    val hljsCssUrl = s"https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/$hljsTheme.min.css"

    val googleFontLinks: Modifier =
      if theme.fonts.googleFonts.isEmpty then frag()
      else
        val families = theme.fonts.googleFonts.mkString("&family=")
        val sheetUrl = s"https://fonts.googleapis.com/css2?family=$families&display=swap"
        frag(
          link(rel := "preconnect", href := "https://fonts.googleapis.com"),
          link(rel := "preconnect", href := "https://fonts.gstatic.com", attr("crossorigin") := ""),
          link(rel := "stylesheet", href := sheetUrl)
        )

    val liveReloadMeta: Modifier =
      if liveReload then meta(attr("http-equiv") := "refresh", content := "2")
      else frag()

    val document = html(
      head(
        meta(charset := "UTF-8"),
        meta(name := "viewport", content := "width=device-width, initial-scale=1.0, viewport-fit=cover"),
        tag("title")(deck.title.getOrElse("MD-Slides presentation")),
        meta(name := "mdslides-title", content := deck.title.getOrElse("")),
        meta(name := "mdslides-theme", content := theme.name),
        tag("style")(raw(generateCSS(theme))),
        // Syntax highlighting (v1.3)
        link(rel := "stylesheet", href := hljsCssUrl),
        tag("script")(src := "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js"),
        // Load Scala language pack (not in common bundle)
        tag("script")(src := "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/languages/scala.min.js"),
        // Google Fonts (US-017) — injected only when theme.fonts.googleFonts is non-empty
        googleFontLinks,
        // Live reload (US-018) — meta-refresh injected in watch mode
        liveReloadMeta
      ),
      body(
        // Slides container with optional break-screen attribute (v3.0.0)
        breakScreen match {
          case Some(path) => div(cls := "slides", attr("data-break-screen") := path)(slideHtml)
          case None => div(cls := "slides")(slideHtml)
        },
        div(cls := "controls")(
          button(`type` := "button", id := "nav-prev", cls := "nav-btn", attr("aria-label") := "Previous slide")("\u2039"),
          button(`type` := "button", cls := "slide-counter", id := "slide-counter", attr("aria-haspopup") := "dialog",
            attr("aria-label") := s"Slide 1 of ${deck.slideCount}, go to slide")("1 / " + deck.slideCount),
          button(`type` := "button", id := "nav-next", cls := "nav-btn", attr("aria-label") := "Next slide")("\u203A"),
          button(`type` := "button", id := "nav-fullscreen", cls := "nav-btn", attr("aria-label") := "Toggle fullscreen", attr("hidden") := "")("\u26F6")
        ),
        // Speaker view synchronisation (shared with speaker.html)
        tag("script")(src := "sync.js"),
        tag("script")(raw(navigationJS(deck.slideCount))),
        // Initialize syntax highlighting (v1.3)
        tag("script")(raw("document.addEventListener('DOMContentLoaded', () => { hljs.highlightAll(); });")),
        // Note: Mermaid diagrams are server-side pre-rendered, no client-side init needed
        // v3.0.0 feature scripts
        tag("script")(src := "presentation-timer.js"),
        tag("script")(src := "navigation-history.js"),
        tag("script")(src := "goto-function.js"),
        tag("script")(src := "history-logging.js"),
        tag("script")(src := "header-footer.js"),
        tag("script")(src := "break-mode.js")
      )
    )

    "<!DOCTYPE html>\n" + document.render

  /**
   * Render FormattedContent to HTML (for testing).
   *
   * Public method to test formatted content rendering in isolation.
   * Used by HTMLRendererSpec tests.
   *
   * @param content The formatted content to render
   * @return HTML string
   */
  def renderFormattedContent(content: FormattedContent): String =
    val frag = renderFormattedContentWithParagraphs(content)
    frag.render

  /**
   * Resolve background image using fallback chain (US-011, PDR-011).
   *
   * Priority:
   * 1. Slide.backgroundImage (highest - per-slide override)
   * 2. Theme.templateBackgrounds[Slide.templateName] (template-specific)
   * 3. Theme.background.image (theme default)
   * 4. None (use theme background color only)
   *
   * @param slide The slide to resolve background for
   * @param theme The theme containing template and default backgrounds
   * @return Some(imagePath) if background found, None otherwise
   */
  private def resolveBackgroundImage(slide: Slide, theme: Theme): Option[String] =
    import com.tjmsolutions.mdslides.domain.{SlideBackground, BackgroundConfig}

    // 1. Check for per-slide background override (highest priority)
    slide.backgroundImage match
      case Some(bg: String) => Some(bg)
      case Some(bg: BackgroundConfig) => Some(bg.imagePath)
      case None =>
        // 2. Check for template-specific background
        theme.templateBackgrounds.get(slide.templateName) match
          case Some(templateBg) => Some(templateBg)
          case None =>
            // 3. Fall back to theme default background
            theme.background.image

  /**
   * Generate CSS from theme.
   *
   * Converts Theme aggregate to inline CSS.
   * Handles background images, colors, fonts, spacing.
   *
   * @param theme The theme to generate CSS from
   * @return CSS string
   */
  private def generateCSS(theme: Theme): String =
    val backgroundImage = theme.background.image match
      case Some(img) =>
        val position = theme.background.position.getOrElse("center")
        val size = theme.background.size.getOrElse("cover")
        val opacity = theme.background.opacity
        s"""
        background-image: url('$img');
        background-position: $position;
        background-size: $size;
        background-repeat: no-repeat;
        opacity: $opacity;
        """
      case None => ""

    s"""
    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
    }

    body {
      font-family: ${theme.fonts.body};
      background-color: ${theme.background.color};$backgroundImage
      color: ${theme.colors.text};
      overflow: hidden;
      overscroll-behavior: none;
      -webkit-text-size-adjust: 100%;
      touch-action: manipulation;
    }

    .slides {
      width: 100vw;
      height: 100vh;
      height: 100dvh;
    }

    .slide {
      display: none;
      width: 100%;
      height: 100%;
      overflow: hidden;
      padding: ${theme.spacing.slideMargin};
      background-color: ${theme.background.color};
      justify-content: center;
      align-items: center;
      flex-direction: column;
    }

    .slide.active {
      display: flex;
    }

    /* Vertical alignment overrides (v3.0.0) */
    .slide.align-top {
      justify-content: flex-start;
    }

    .slide.align-center {
      justify-content: center;
    }

    .slide.align-bottom {
      justify-content: flex-end;
    }

    /* Slide header/footer structure (v3.0.0) */
    .slide-header {
      width: 100%;
      padding: 0.5rem 1rem;
      font-size: 0.9rem;
      color: ${theme.colors.text};
      background-color: ${theme.background.color};
      opacity: 0.95;
      flex-shrink: 0;
    }

    .slide-content-wrapper {
      flex: 1;
      min-height: 0;
      display: flex;
      flex-direction: column;
      justify-content: inherit;
      align-items: inherit;
      width: 100%;
      overflow: hidden;
    }

    .slide-footer {
      width: 100%;
      padding: 0.5rem 1rem;
      /* Leave room for the floating controls bar (width measured at runtime) */
      padding-right: max(1rem, var(--controls-width, 0px));
      font-size: 0.9rem;
      color: ${theme.colors.text};
      background-color: ${theme.background.color};
      opacity: 0.95;
      flex-shrink: 0;
    }

    /* Multi-element footer positioning (v3.0.0) */
    .slide-footer:has(.footer-left),
    .slide-footer:has(.footer-center),
    .slide-footer:has(.footer-right) {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 1rem;
    }

    .footer-left {
      text-align: left;
      flex: 1;
    }

    .footer-center {
      text-align: center;
      flex: 1;
    }

    .footer-right {
      text-align: right;
      flex: 1;
    }

    /* Presentation timer overlay — injected by presentation-timer.js into body.firstChild */
    .presentation-timer {
      position: fixed;
      top: 0.4rem;
      left: 0.5rem;
      z-index: 999;
      font-family: monospace;
      font-size: 0.75rem;
      color: ${theme.colors.text};
      opacity: 0.8;
      pointer-events: none;
      user-select: none;
    }

    /* Paused: marked by style, not by fading below readable contrast */
    .presentation-timer.timer-paused {
      font-style: italic;
      text-decoration: underline dotted;
    }

    .title-slide {
      text-align: center;
      max-width: 800px;
      justify-content: space-between;
      padding-top: 60px;
      padding-bottom: 60px;
    }

    .title-slide-header {
      margin-bottom: auto;
    }

    .title-slide-footer {
      margin-top: auto;
    }

    .slide-title {
      font-size: 48px;
      font-family: ${theme.fonts.heading};
      color: ${theme.colors.heading};
      margin: ${theme.spacing.headingMargin};
      line-height: ${theme.spacing.lineHeight};
    }

    .slide-subtitle {
      font-size: 36px;
      font-family: ${theme.fonts.heading};
      color: ${theme.colors.heading};
      margin-bottom: 20px;
      font-weight: normal;
    }

    .slide-author {
      font-size: 24px;
      color: ${theme.colors.text};
      margin-top: 20px;
    }

    .content-slide {
      max-width: 900px;
      width: 100%;
    }

    .slide-heading {
      font-size: 36px;
      font-family: ${theme.fonts.heading};
      color: ${theme.colors.heading};
      margin: ${theme.spacing.headingMargin};
    }

    .slide-body {
      font-size: 24px;
      color: ${theme.colors.text};
      line-height: ${theme.spacing.lineHeight};
      overflow-y: auto;
    }

    .slide-body p,
    .column p {
      margin: ${theme.spacing.paragraphMargin};
    }

    /* Inline formatting (US-003) */
    strong {
      font-weight: bold;
    }

    em {
      font-style: italic;
    }

    code {
      background-color: ${theme.colors.codeBackground};
      color: ${theme.colors.codeText};
      border: 1px solid #ddd;
      border-radius: 3px;
      padding: 2px 6px;
      font-family: ${theme.fonts.code};
      font-size: 0.9em;
    }

    /* Code blocks (US-004) */
    pre {
      background-color: ${theme.colors.codeBackground};
      border: 1px solid #ddd;
      border-radius: 5px;
      padding: 15px;
      margin: ${theme.spacing.paragraphMargin};
      overflow-x: auto;
      font-size: 18px;
    }

    pre code {
      background-color: transparent;
      border: none;
      padding: 0;
      font-family: ${theme.fonts.code};
      color: ${theme.colors.codeText};
      font-size: inherit;
    }

    /* Syntax highlighting using theme colors */
    .syntax-keyword { color: ${theme.syntax.keyword}; }
    .syntax-string { color: ${theme.syntax.string}; }
    .syntax-comment { color: ${theme.syntax.comment}; }
    .syntax-function { color: ${theme.syntax.function}; }
    .syntax-number { color: ${theme.syntax.number}; }
    .syntax-operator { color: ${theme.syntax.operator}; }

    /* Content images (US-005) */
    .content-image {
      max-width: 100%;
      height: auto;
      margin: ${theme.spacing.paragraphMargin};
      display: block;
      border-radius: 5px;
    }

    a {
      color: ${theme.colors.link};
      text-decoration: none;
    }

    a:hover {
      color: ${theme.colors.linkHover};
      text-decoration: underline;
    }

    /* Nested list support (US-003.3) — applies to .slide-body and .column (MS-014) */
    .slide-body ul,
    .slide-body ol,
    .column ul,
    .column ol {
      margin: ${theme.spacing.paragraphMargin};
      padding-left: 2em;
    }

    /* Unordered list bullet hierarchy */
    .slide-body ul,
    .column ul {
      list-style-type: disc;  /* Level 1: disc (•) */
    }

    .slide-body ul ul,
    .column ul ul {
      list-style-type: circle;  /* Level 2: circle (◦) */
      margin-top: 0.5em;
    }

    .slide-body ul ul ul,
    .column ul ul ul {
      list-style-type: square;  /* Level 3: square (▪) */
    }

    /* Ordered list numbering hierarchy */
    .slide-body ol,
    .column ol {
      list-style-type: decimal;  /* Level 1: decimal (1, 2, 3...) */
    }

    .slide-body ol ol,
    .column ol ol {
      list-style-type: lower-alpha;  /* Level 2: lower-alpha (a, b, c...) */
      margin-top: 0.5em;
    }

    .slide-body ol ol ol,
    .column ol ol ol {
      list-style-type: lower-roman;  /* Level 3: lower-roman (i, ii, iii...) */
    }

    /* Mixed nesting support */
    .slide-body ul ol,
    .slide-body ol ul,
    .column ul ol,
    .column ol ul {
      margin-top: 0.5em;
    }

    /* List item spacing */
    .slide-body li,
    .column li {
      margin: 0.3em 0;
      line-height: ${theme.spacing.lineHeight};
    }

    /* Table styling — applies to .slide-body and .column (MS-014) */
    .slide-body table,
    .column table {
      width: auto;
      max-width: 90%;
      border-collapse: collapse;
      margin: ${theme.spacing.paragraphMargin};
      margin-left: auto;
      margin-right: auto;
      font-size: 18px;
      line-height: ${theme.spacing.lineHeight};
      border: 2px solid #333;
    }

    .slide-body table th,
    .column table th {
      background-color: ${theme.colors.codeBackground};
      color: ${theme.colors.codeText};
      padding: 12px 16px;
      text-align: left;
      font-weight: bold;
      border: 1px solid #333;
    }

    .slide-body table td,
    .column table td {
      padding: 10px 16px;
      border: 1px solid #333;
    }

    .slide-body table tbody tr:nth-child(odd),
    .column table tbody tr:nth-child(odd) {
      background-color: rgba(0, 0, 0, 0.02);
    }

    .slide-body table tbody tr:hover,
    .column table tbody tr:hover {
      background-color: rgba(0, 0, 0, 0.05);
    }

    .controls {
      position: fixed;
      bottom: 20px;
      bottom: max(20px, env(safe-area-inset-bottom));
      right: 20px;
      right: max(20px, env(safe-area-inset-right));
      display: flex;
      flex-direction: row;
      align-items: center;
      gap: 6px;
      background-color: ${theme.slideCounter.background};
      color: ${theme.slideCounter.color};
      padding: 4px 10px;
      border-radius: 5px;
      font-size: ${theme.slideCounter.fontSize};
    }

    .nav-btn {
      min-width: 44px;
      min-height: 44px;
      border: none;
      background: transparent;
      color: inherit;
      font-size: 1.6em;
      line-height: 1;
      cursor: pointer;
      border-radius: 5px;
    }

    .nav-btn[hidden] {
      display: none;
    }

    /* Slide counter doubles as the goto button (touch screens have no G key) */
    .slide-counter {
      min-height: 44px;
      padding: 0 4px;
      border: none;
      background: transparent;
      color: inherit;
      font: inherit;
      cursor: pointer;
      border-radius: 5px;
    }

    .slide-counter:focus-visible {
      outline: 2px solid currentColor;
    }

    .nav-btn:disabled {
      opacity: 0.3;
      cursor: default;
    }

    .nav-btn:focus-visible {
      outline: 2px solid currentColor;
    }

    /* Fit-to-slide (mobile presenting): natural height must be measurable */
    .fit-managed {
      flex-shrink: 0;
    }

    .fit-managed .slide-body {
      overflow: visible;
    }

    .error {
      color: red;
      font-size: 24px;
      padding: 40px;
    }

    /* Header/Footer (v3.0.0) */
    /* v2.0.0 Template Styles */

    /* Diagram template (Scenarios 18-20) */
    .diagram-slide {
      max-width: 1000px;
      width: 100%;
      text-align: center;
    }

    .diagram-caption {
      font-size: 1rem;
      text-align: center;
      color: ${theme.colors.text};
      margin-top: 1em;
      font-style: italic;
    }

    /* Mermaid diagram container styling */
    .mermaid-diagram {
      display: flex;
      justify-content: center;
      align-items: center;
      width: 100%;
      height: 100%;
      max-height: 70vh;
      overflow: visible;
    }

    .mermaid-diagram svg {
      max-width: 100%;
      max-height: 70vh;
      height: auto;
      object-fit: contain;
    }

    /* Closing template (Scenarios 22-24) */
    .closing-slide {
      text-align: center;
      max-width: 800px;
    }

    .closing-heading {
      font-size: 3rem;
      font-family: ${theme.fonts.heading};
      color: ${theme.colors.heading};
      margin: ${theme.spacing.headingMargin};
      text-align: center;
    }

    /* Section title template (Scenarios 26-27) */
    .section-title-slide {
      text-align: center;
      max-width: 900px;
    }

    .section-title-heading {
      font-size: 4rem;
      font-family: ${theme.fonts.heading};
      color: ${theme.colors.heading};
      margin: ${theme.spacing.headingMargin};
      line-height: 1.2;
    }

    .section-subtitle {
      font-size: 1.5rem;
      color: ${theme.colors.text};
      margin-top: 1.5em;
      font-weight: normal;
    }

    /* Section title two-column variant (v3.0.0) */
    .section-title-two-column {
      max-width: 1200px;
      text-align: left;
    }

    .section-body {
      font-size: 1.5rem;
      line-height: ${theme.spacing.lineHeight};
    }

    /* Two-column template (v3.0.0) */
    .two-column-slide {
      display: flex;
      flex-direction: column;
      max-width: 1200px;
      width: 100%;
    }

    .two-column-slide .slide-heading {
      margin-bottom: 2rem;
      text-align: center;
    }

    .two-column-container {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 2rem;
      align-items: center;
    }

    .column {
      text-align: left;
      display: flex;
      flex-direction: column;
      justify-content: center;
    }

    .column-left {
      padding-right: 1rem;
    }

    .column-right {
      padding-left: 1rem;
    }

    /* Phones and short landscape screens: tighter padding, fluid type */
    @media (max-width: 768px), (max-height: 500px) {
      .slide {
        padding: 4vmin;
        padding-top: max(4vmin, env(safe-area-inset-top));
        padding-bottom: max(4vmin, env(safe-area-inset-bottom));
        padding-left: max(4vmin, env(safe-area-inset-left));
        padding-right: max(4vmin, env(safe-area-inset-right));
      }
      .title-slide { padding-top: 4vmin; padding-bottom: 4vmin; }
      .slide-title { font-size: clamp(24px, 7vmin, 48px); }
      .slide-subtitle { font-size: clamp(18px, 5vmin, 36px); }
      .slide-heading { font-size: clamp(20px, 5.5vmin, 36px); }
      .slide-body { font-size: clamp(14px, 3.6vmin, 24px); }
      .section-body { font-size: clamp(14px, 3.6vmin, 24px); }
      .closing-heading { font-size: clamp(24px, 7vmin, 48px); }
      .section-title-heading { font-size: clamp(28px, 8vmin, 64px); }
    }

    /* Print / Save as PDF: every slide on its own landscape page, no on-screen controls */
    @media print {
      @page { size: landscape; margin: 0; }
      body { overflow: visible; }
      .slides { width: auto; height: auto; }
      .slide {
        display: flex !important;
        width: 100vw;
        height: 100vh;
        break-after: page;
        page-break-after: always;
      }
      .slide:last-child { break-after: auto; page-break-after: auto; }
      .controls, .presentation-timer, #break-mode-overlay, .goto-modal, #mdslides-status { display: none !important; }
      * { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
    }

    /* Portrait phones: stack columns */
    @media (max-width: 600px) and (orientation: portrait) {
      .two-column-container {
        grid-template-columns: 1fr !important;
        gap: 1rem;
      }
      .column-left, .column-right {
        padding-left: 0;
        padding-right: 0;
      }
    }
    """

  /**
   * Resolve colors for a slide using template configuration.
   *
   * Merges template color overrides with theme defaults.
   *
   * @param theme The theme (for default colors)
   * @param templateConfig Optional template configuration
   * @return Map of element -> color
   */
  private def resolveColors(theme: Theme, templateConfig: Option[TemplateConfiguration]): Map[String, String] =
    val themeDefaults = Map(
      "heading" -> theme.colors.heading,
      "subtitle" -> theme.colors.text,
      "body" -> theme.colors.text,
      "author" -> theme.colors.text
    )

    templateConfig.flatMap(_.colors) match
      case Some(templateColors) => templateColors.mergeWithDefaults(themeDefaults)
      case None => themeDefaults

  /**
   * Resolve vertical alignment for a slide using configuration hierarchy.
   *
   * Hierarchy: Slide frontmatter > Theme template config > Default (center)
   *
   * @param slide The slide
   * @param templateConfig Optional template configuration from theme
   * @return Alignment string: "top", "center", or "bottom"
   */
  private def resolveVerticalAlignment(slide: Slide, templateConfig: Option[TemplateConfiguration]): String =
    // 1. Check slide frontmatter (highest priority)
    slide.getSlot("vertical-align") match
      case Some(align) => align.toLowerCase
      case None =>
        // 2. Check theme template config
        templateConfig.flatMap(_.verticalAlign) match
          case Some(align) => align.toString.toLowerCase
          case None => "center"  // 3. Default

  /**
   * Resolve header template for a slide using configuration hierarchy.
   *
   * Hierarchy: Slide frontmatter > Theme template config > Global config > None
   *
   * @param slide The slide
   * @param templateConfig Optional template configuration from theme
   * @param globalHeaderTemplate Global header template
   * @return Optional header template string
   */
  private def resolveHeaderTemplate(
    slide: Slide,
    templateConfig: Option[TemplateConfiguration],
    globalHeaderTemplate: Option[String]
  ): Option[String] =
    slide.getSlot("header")
      .orElse(templateConfig.flatMap(_.header))
      .orElse(globalHeaderTemplate)

  /**
   * Resolve footer template for a slide using configuration hierarchy.
   *
   * Hierarchy: Slide frontmatter > Theme template config > Global config > None
   *
   * @param slide The slide
   * @param templateConfig Optional template configuration from theme
   * @param globalFooterTemplate Global footer template
   * @return Optional footer template string
   */
  private def resolveFooterTemplate(
    slide: Slide,
    templateConfig: Option[TemplateConfiguration],
    globalFooterTemplate: Option[String]
  ): Option[String] =
    slide.getSlot("footer")
      .orElse(templateConfig.flatMap(_.footer))
      .orElse(globalFooterTemplate)

  /**
   * Render a single slide with optional header/footer.
   *
   * @param slide The slide to render
   * @param index The slide index (0-based)
   * @param totalSlides Total number of slides in deck
   * @param theme The theme (for background resolution)
   * @param preRenderedDiagrams Map of diagram source -> pre-rendered SVG
   * @param globalHeaderTemplate Global header template (from config/theme)
   * @param globalFooterTemplate Global footer template (from config/theme)
   * @return HTML div element for the slide
   */
  private def renderSlide(
    slide: Slide,
    index: Int,
    totalSlides: Int,
    theme: Theme,
    preRenderedDiagrams: Map[String, String],
    globalHeaderTemplate: Option[String],
    globalFooterTemplate: Option[String]
  ): Frag =
    // Resolve template configuration (hierarchy: slide frontmatter > theme template config > defaults)
    val templateConfig = theme.templateConfig.get(slide.templateName)

    // Build slide classes: active state + vertical alignment (v3.0.0)
    val baseClass = if index == 0 then "slide active" else "slide"
    val alignClass = resolveVerticalAlignment(slide, templateConfig) match
      case "top" => " align-top"
      case "bottom" => " align-bottom"
      case _ => " align-center"  // default or "center"
    val slideClass = baseClass + alignClass

    // Resolve background using fallback chain (US-011)
    val backgroundModifiers = resolveBackgroundImage(slide, theme) match
      case Some(bgPath) =>
        // Only rewrite content images (per-slide frontmatter); theme backgrounds are already at correct paths
        val finalBg = if slide.backgroundImage.isDefined then rewriteImageUrl(bgPath) else bgPath
        // User-supplied images use cover (fill + crop); theme template backgrounds use 100% 100% (exact fill, no crop)
        val bgSize = if slide.backgroundImage.isDefined then "cover" else "100% 100%"
        Seq(attr("style") := s"background-image: url('$finalBg'); background-size: $bgSize; background-position: center; background-repeat: no-repeat;")
      case None =>
        Seq()  // No inline background, use CSS default

    // Determine header/footer for this slide using configuration hierarchy
    val headerTemplate = resolveHeaderTemplate(slide, templateConfig, globalHeaderTemplate)
    val footerTemplate = resolveFooterTemplate(slide, templateConfig, globalFooterTemplate)

    // Resolve colors for this slide (merge template config with theme defaults)
    val colors = resolveColors(theme, templateConfig)

    div(cls := slideClass, attr("data-slide-index") := index.toString, backgroundModifiers)(
      // Render header if template exists
      headerTemplate.map(template =>
        div(cls := "slide-header", attr("data-template") := template)(raw(template))
      ),
      // Render slide content
      div(cls := "slide-content-wrapper")(
        renderSlideContent(slide, preRenderedDiagrams, colors, templateConfig)
      ),
      // Render footer if template exists
      footerTemplate.map(template =>
        div(cls := "slide-footer", attr("data-template") := template)(raw(template))
      )
    )

  /**
   * Render slide content based on template type.
   *
   * @param slide The slide to render
   * @param preRenderedDiagrams Map of diagram source -> pre-rendered SVG
   * @param colors Resolved colors for this slide
   * @param templateConfig Optional template configuration from theme
   * @return HTML content for the slide
   */
  private def renderSlideContent(slide: Slide, preRenderedDiagrams: Map[String, String], colors: Map[String, String], templateConfig: Option[TemplateConfiguration]): Frag =
    slide.templateName match
      case "title" => renderTitleSlide(slide, preRenderedDiagrams, colors)
      case "content" => renderContentSlide(slide, preRenderedDiagrams, colors)
      case "diagram" => renderDiagramSlide(slide, preRenderedDiagrams, colors)
      case "closing" => renderClosingSlide(slide, preRenderedDiagrams, colors)
      case "section-title" => renderSectionTitleSlide(slide, preRenderedDiagrams, colors, templateConfig)
      case "two-column" => renderTwoColumnSlide(slide, preRenderedDiagrams, colors)
      case _ => div(cls := "error")("Unknown template: " + slide.templateName)

  /**
   * Render a title slide.
   *
   * Title/subtitle are top-justified, author is bottom-justified.
   * This matches standard presentation design patterns.
   */
  private def renderTitleSlide(slide: Slide, preRenderedDiagrams: Map[String, String], colors: Map[String, String]): Frag =
    div(cls := "title-slide")(
      // Header section (top-justified)
      div(cls := "title-slide-header")(
        slide.getSlot(SlotName.Title).map(title =>
          h1(cls := "slide-title", style := s"color: ${colors("heading")}")(renderFormattedText(title, preRenderedDiagrams))
        ),
        slide.getSlot(SlotName.Subtitle).map(subtitle =>
          h2(cls := "slide-subtitle", style := s"color: ${colors("subtitle")}")(renderFormattedText(subtitle, preRenderedDiagrams))
        )
      ),
      // Footer section (bottom-justified)
      slide.getSlot(SlotName.Author).map(author =>
        div(cls := "title-slide-footer")(
          p(cls := "slide-author", style := s"color: ${colors("author")}")(renderFormattedText(author, preRenderedDiagrams))
        )
      )
    )

  /**
   * Render a content slide.
   */
  private def renderContentSlide(slide: Slide, preRenderedDiagrams: Map[String, String], colors: Map[String, String]): Frag =
    div(cls := "content-slide")(
      slide.getSlot(SlotName.Heading).map(heading =>
        h2(cls := "slide-heading", style := s"color: ${colors("heading")}")(renderFormattedText(heading, preRenderedDiagrams))
      ),
      slide.getSlot(SlotName.Body).map(body =>
        div(cls := "slide-body", style := s"color: ${colors("body")}")(
          renderFormattedText(body, preRenderedDiagrams)
        )
      )
    )

  /**
   * Render a diagram slide (v2.0.0).
   *
   * Diagram template optimized for Mermaid diagrams.
   * Layout: heading, diagram content, optional caption.
   *
   * Related: Example Mapping Scenarios 18-20
   */
  private def renderDiagramSlide(slide: Slide, preRenderedDiagrams: Map[String, String], colors: Map[String, String]): Frag =
    div(cls := "diagram-slide")(
      slide.getSlot(SlotName.Heading).map(heading =>
        h2(cls := "slide-heading", style := s"color: ${colors("heading")}")(renderFormattedText(heading, preRenderedDiagrams))
      ),
      // Body contains Mermaid diagram(s)
      slide.getSlot(SlotName.Body).map(body =>
        div(cls := "slide-body", style := s"color: ${colors("body")}")(
          renderFormattedText(body, preRenderedDiagrams)
        )
      ),
      // Optional caption below diagram (Scenario 18)
      slide.getSlot(SlotName.Caption).map(caption =>
        p(cls := "diagram-caption")(renderFormattedText(caption, preRenderedDiagrams))
      )
    )

  /**
   * Render a closing slide (v2.0.0).
   *
   * Closing template for thank you slides and contact information.
   * Layout: large centered heading, optional body with contact info.
   *
   * Related: Example Mapping Scenarios 22-24
   */
  private def renderClosingSlide(slide: Slide, preRenderedDiagrams: Map[String, String], colors: Map[String, String]): Frag =
    div(cls := "closing-slide")(
      slide.getSlot(SlotName.Heading).map(heading =>
        h1(cls := "closing-heading", style := s"color: ${colors("heading")}")(renderFormattedText(heading, preRenderedDiagrams))
      ),
      slide.getSlot(SlotName.Body).map(body =>
        div(cls := "slide-body", style := s"color: ${colors("body")}")(
          renderFormattedText(body, preRenderedDiagrams)
        )
      )
    )

  /**
   * Render a section title slide (v2.0.0).
   *
   * Section title template for dividing long presentations.
   * Layout: very large heading, optional subtitle (body).
   * Supports two-column layout via template configuration (v3.0.0).
   *
   * Related: Example Mapping Scenarios 26-27
   */
  private def renderSectionTitleSlide(slide: Slide, preRenderedDiagrams: Map[String, String], colors: Map[String, String], templateConfig: Option[TemplateConfiguration]): Frag =
    import com.tjmsolutions.mdslides.domain.TemplateLayout

    // Check if template config specifies two-column layout
    val isTwoColumn = templateConfig.flatMap(_.layout).contains(TemplateLayout.TwoColumn)

    if isTwoColumn then
      // Two-column layout: heading in left column, body in right column
      val columnConfig = templateConfig.flatMap(_.columnConfig)
      val leftWidth = columnConfig.flatMap(_.leftColumn.width).getOrElse("40%")
      val rightWidth = columnConfig.flatMap(_.rightColumn.width).getOrElse("60%")

      // Resolve column-specific colors
      val leftColors = columnConfig.flatMap(_.leftColumn.colors).map(_.mergeWithDefaults(Map("heading" -> colors("heading")))).getOrElse(colors)
      val rightColors = columnConfig.flatMap(_.rightColumn.colors).map(_.mergeWithDefaults(Map("body" -> colors("body")))).getOrElse(colors)

      div(cls := "section-title-slide section-title-two-column", attr("role") := "region", attr("aria-label") := "Section title slide")(
        div(cls := "two-column-container", style := s"grid-template-columns: $leftWidth $rightWidth")(
          tag("section")(cls := "column column-left", attr("aria-label") := "Section heading", style := s"color: ${leftColors("heading")}")(
            slide.getSlot(SlotName.Heading).map(heading =>
              h1(cls := "section-title-heading", style := s"color: ${leftColors("heading")}")(renderFormattedText(heading, preRenderedDiagrams))
            ).getOrElse(div())
          ),
          tag("section")(cls := "column column-right", attr("aria-label") := "Section description", style := s"color: ${rightColors("body")}")(
            slide.getSlot(SlotName.Body).map(body =>
              div(cls := "section-body")(renderFormattedText(body, preRenderedDiagrams))
            ).getOrElse(div())
          )
        )
      )
    else
      // Standard centered layout
      div(cls := "section-title-slide")(
        slide.getSlot(SlotName.Heading).map(heading =>
          h1(cls := "section-title-heading", style := s"color: ${colors("heading")}")(renderFormattedText(heading, preRenderedDiagrams))
        ),
        slide.getSlot(SlotName.Body).map(body =>
          p(cls := "section-subtitle", style := s"color: ${colors("body")}")(renderFormattedText(body, preRenderedDiagrams))
        )
      )

  /**
   * Render a two-column slide (v3.0.0).
   *
   * Renders left and right columns side-by-side using CSS Grid.
   * Each column supports full markdown formatting.
   * Optional title rendered above columns if present in frontmatter.
   *
   * @param slide The slide with leftColumn and rightColumn slots, optional title
   * @param preRenderedDiagrams Map of pre-rendered diagrams
   * @param colors Resolved colors for this slide
   * @return HTML div with two-column layout
   */
  private def renderTwoColumnSlide(slide: Slide, preRenderedDiagrams: Map[String, String], colors: Map[String, String]): Frag =
    // AC-12 (Two-Column Layout): ARIA attributes for accessibility
    div(cls := "two-column-slide", attr("role") := "region", attr("aria-label") := "Two-column slide")(
      // Render title if present in frontmatter
      slide.getSlot(SlotName.Title).map(title =>
        h2(cls := "slide-heading", style := s"color: ${colors("heading")}")(renderFormattedText(title, preRenderedDiagrams))
      ),
      // Two-column container
      div(cls := "two-column-container")(
        tag("section")(cls := "column column-left", attr("aria-label") := "Left column", style := s"color: ${colors("body")}")(
          slide.getSlot(SlotName.LeftColumn).map(leftContent =>
            renderFormattedText(leftContent, preRenderedDiagrams)
          ).getOrElse(div(cls := "error")("Missing left column"))
        ),
        tag("section")(cls := "column column-right", attr("aria-label") := "Right column", style := s"color: ${colors("body")}")(
          slide.getSlot(SlotName.RightColumn).map(rightContent =>
            renderFormattedText(rightContent, preRenderedDiagrams)
          ).getOrElse(div(cls := "error")("Missing right column"))
        )
      )
    )

  /**
   * Render formatted text with inline markdown.
   *
   * Parses markdown once and renders all content including links.
   *
   * @param text Plain text or markdown text
   * @return HTML fragments with formatting
   */
  private def renderFormattedText(text: String, preRenderedDiagrams: Map[String, String] = Map.empty): Frag =
    val formatted = FlexmarkAdapter.parseInlineFormatting(text)
    renderFormattedContentWithParagraphs(formatted, preRenderedDiagrams)

  /**
   * Render FormattedContent to HTML with paragraph structure, code blocks, and images.
   *
   * Converts TextSpan, Link, CodeBlock, and ContentImage domain objects to HTML.
   * Splits text into paragraphs and renders code blocks and images separately.
   *
   * @param content Formatted content from domain
   * @return HTML fragments
   */
  private def renderFormattedContentWithParagraphs(
    content: FormattedContent,
    preRenderedDiagrams: Map[String, String] = Map.empty
  ): Frag =
    // Build a map of link text → URL for easy lookup
    val linkMap = content.links.map(link => link.text -> link.url).toMap

    // Split text spans by newlines to create paragraphs
    val spans = content.textSpans
    val paragraphs = scala.collection.mutable.ListBuffer.empty[List[TextSpan]]
    val currentParagraph = scala.collection.mutable.ListBuffer.empty[TextSpan]

    spans.foreach { span =>
      if span.text.contains("\n") then
        // Split on newlines
        val lines = span.text.split("\n", -1)
        lines.zipWithIndex.foreach { case (line, idx) =>
          if line.nonEmpty then
            currentParagraph += span.copy(text = line)

          if idx < lines.length - 1 then
            // Newline encountered - finish current paragraph
            if currentParagraph.nonEmpty then
              paragraphs += currentParagraph.toList
              currentParagraph.clear()
        }
      else
        currentParagraph += span
    }

    // Add final paragraph
    if currentParagraph.nonEmpty then
      paragraphs += currentParagraph.toList

    // BUG-001 REAL FIX: Render from content field which preserves ALL source order
    if content.content.nonEmpty then
      // Use new content field - renders everything in source order
      content.content.map {
        case ParagraphElement(spans) =>
          p(spans.map(span => renderTextSpanWithLinks(span, linkMap)))
        case UnorderedListElement(list) =>
          renderUnorderedList(list, linkMap)
        case OrderedListElement(list) =>
          renderOrderedList(list, linkMap)
        case CodeBlockElement(block) =>
          renderCodeBlock(block)
        case ImageElement(image) =>
          renderContentImage(image)
        case DiagramElement(diagram) =>
          renderMermaidDiagram(diagram, preRenderedDiagrams)
        case TableElement(table) =>
          renderTable(table, linkMap)
      }
    else
      // DEPRECATED: Backward compatibility - old field-by-field rendering
      // This path renders in WRONG order (all text, then code, then images, then lists)
      val paragraphFrags = if paragraphs.isEmpty then
        Seq.empty[Frag]
      else
        paragraphs.toList.map { paraSpans =>
          p(paraSpans.map(span => renderTextSpanWithLinks(span, linkMap)))
        }

      val codeBlockFrags = content.codeBlocks.map(renderCodeBlock)
      val imageFrags = content.contentImages.map(renderContentImage)

      val listFrags = if content.lists.nonEmpty then
        content.lists.map {
          case UnorderedListElementDeprecated(list) => renderUnorderedList(list, linkMap)
          case OrderedListElementDeprecated(list) => renderOrderedList(list, linkMap)
        }
      else
        content.unorderedLists.map(renderUnorderedList(_, linkMap)) ++
        content.orderedLists.map(renderOrderedList(_, linkMap))

      Seq(paragraphFrags, codeBlockFrags, imageFrags, listFrags)

  /**
   * Render a TextSpan, converting to link if text matches a link.
   */
  private def renderTextSpanWithLinks(span: TextSpan, linkMap: Map[String, String]): Frag =
    linkMap.get(span.text) match
      case Some(url) =>
        // This text is a link
        a(href := url)(span.text)
      case None =>
        // Regular text span
        renderTextSpan(span)

  /**
   * Render a single TextSpan to HTML.
   *
   * Applies bold, italic, and code formatting.
   */
  private def renderTextSpan(span: TextSpan): Frag =
    if span.strike && !span.code then tag("del")(renderPlainSpan(span))
    else renderPlainSpan(span)

  private def renderPlainSpan(span: TextSpan): Frag =
    val text: Frag = span.text

    if span.code then
      tag("code")(text)
    else if span.bold && span.italic then
      tag("strong")(tag("em")(text))
    else if span.bold then
      tag("strong")(text)
    else if span.italic then
      tag("em")(text)
    else
      text

  /**
   * Render a code block to HTML.
   *
   * Creates <pre><code> tags with optional language class.
   * For MVP, we're not doing syntax highlighting yet (no highlight.js).
   * Theme colors are applied via CSS classes.
   *
   * @param codeBlock The code block to render
   * @return HTML <pre><code> element
   */
  private def renderCodeBlock(codeBlock: CodeBlock): Frag =
    val languageClass = codeBlock.language.map(lang => s"language-$lang").getOrElse("")
    val codeTag = if languageClass.nonEmpty then
      tag("code")(cls := languageClass)(codeBlock.code)
    else
      tag("code")(codeBlock.code)

    tag("pre")(codeTag)

  /**
   * Rewrite image URL for HTML output.
   *
   * Local images are copied to images/ subdirectory, so we need to rewrite the path.
   * Remote URLs and data URIs are used as-is.
   *
   * @param url Original image URL from markdown
   * @return Rewritten URL for HTML
   */
  private def rewriteImageUrl(url: String): String =
    if url.startsWith("http://") || url.startsWith("https://") || url.startsWith("data:") then
      // Remote URL or data URI - use as-is
      url
    else
      // Local file - extract filename and point to images/ subdirectory
      val filename = Paths.get(url).getFileName.toString
      s"images/$filename"

  /**
   * Render content image to HTML (US-005).
   *
   * Creates <img> tag with alt text for accessibility (PDR-005, PDR-008).
   *
   * @param image Content image from domain
   * @return HTML <img> element
   */
  private def renderContentImage(image: ContentImage): Frag =
    img(
      cls := "content-image",
      src := rewriteImageUrl(image.url),
      alt := image.altText
    )

  /**
   * Render Mermaid diagram to HTML (v2.0.0, US-022).
   *
   * Server-side pre-rendering for offline support.
   * Uses pre-rendered SVG if available, otherwise shows fallback.
   *
   * @param diagram Mermaid diagram from domain
   * @param preRenderedDiagrams Map of diagram source -> pre-rendered SVG
   * @return HTML <div> element with inline SVG or fallback
   */
  private def renderMermaidDiagram(
    diagram: MermaidDiagram,
    preRenderedDiagrams: Map[String, String] = Map.empty
  ): Frag =
    preRenderedDiagrams.get(diagram.source) match
      case Some(svg) =>
        // Use pre-rendered SVG (offline support)
        div(
          cls := "mermaid-diagram",
          diagram.altText.map(alt => attr("aria-label") := alt),
          attr("data-diagram-type") := diagram.diagramType,
          raw(svg)
        )
      case None =>
        // Fallback: show diagram source with warning
        div(
          cls := "mermaid-fallback",
          diagram.altText.map(alt => attr("aria-label") := alt),
          raw(s"""<pre style="background: #f5f5f5; padding: 1em; border-radius: 4px; overflow: auto;"><code>${diagram.source}</code></pre>
                 |<p style="color: #d32f2f; font-size: 0.9em; margin-top: 0.5em;">
                 |  ⚠ Diagram not rendered. Install mermaid-cli: npm install -g @mermaid-js/mermaid-cli
                 |</p>""".stripMargin)
        )

  /**
   * Render an unordered list (bullet points).
   *
   * @param list The UnorderedList to render
   * @param linkMap Map of link text to URLs for rendering links
   * @return HTML <ul> fragment
   */
  private def renderUnorderedList(list: com.tjmsolutions.mdslides.domain.UnorderedList, linkMap: Map[String, String]): Frag =
    ul(
      list.items.map { item =>
        // Build the fragments for this list item (US-003.3)
        val textFrags = item.textSpans.map(span => renderTextSpanWithLinks(span, linkMap))
        val nestedUnorderedFrags = item.nestedUnorderedLists.map(nestedList => renderUnorderedList(nestedList, linkMap))
        val nestedOrderedFrags = item.nestedOrderedLists.map(nestedList => renderOrderedList(nestedList, linkMap))

        // Concatenate all fragments and pass to li()
        li((textFrags ++ nestedUnorderedFrags ++ nestedOrderedFrags): _*)
      }
    )

  /**
   * Render an ordered list (numbered items).
   *
   * @param list The OrderedList to render
   * @param linkMap Map of link text to URLs for rendering links
   * @return HTML <ol> fragment
   */
  private def renderOrderedList(list: com.tjmsolutions.mdslides.domain.OrderedList, linkMap: Map[String, String]): Frag =
    ol(
      list.items.map { item =>
        // Build the fragments for this list item (US-003.3)
        val textFrags = item.textSpans.map(span => renderTextSpanWithLinks(span, linkMap))
        val nestedUnorderedFrags = item.nestedUnorderedLists.map(nestedList => renderUnorderedList(nestedList, linkMap))
        val nestedOrderedFrags = item.nestedOrderedLists.map(nestedList => renderOrderedList(nestedList, linkMap))

        // Concatenate all fragments and pass to li()
        li((textFrags ++ nestedUnorderedFrags ++ nestedOrderedFrags): _*)
      }
    )

  private def renderTable(tbl: Table, linkMap: Map[String, String]): Frag =
    val headerCells = tbl.headers.zipWithIndex.map { case (header, idx) =>
      val alignment = if tbl.alignment.nonEmpty && idx < tbl.alignment.length then
        tbl.alignment(idx) match {
          case "left" => style := "text-align: left"
          case "center" => style := "text-align: center"
          case "right" => style := "text-align: right"
          case _ => style := "text-align: left"
        }
      else
        style := "text-align: left"
      th(alignment)(header)
    }

    val headerRow = tr(headerCells: _*)
    val bodyRows = tbl.rows.map { row =>
      val cells = row.zipWithIndex.map { case (cell, idx) =>
        val alignment = if tbl.alignment.nonEmpty && idx < tbl.alignment.length then
          tbl.alignment(idx) match {
            case "left" => style := "text-align: left"
            case "center" => style := "text-align: center"
            case "right" => style := "text-align: right"
            case _ => style := "text-align: left"
          }
        else
          style := "text-align: left"
        td(alignment)(cell)
      }
      tr(cells: _*)
    }

    tag("table")(
      thead(headerRow),
      tbody(bodyRows: _*)
    )

  /**
   * Navigation JavaScript with code block auto-scaling.
   *
   * @param totalSlides Total number of slides
   * @return JavaScript code for keyboard navigation and code scaling
   */
  private def navigationJS(totalSlides: Int): String = s"""
    let currentSlide = 0;
    const totalSlides = $totalSlides;

    // method: how the change happened (keyboard, swipe, tap, button, link, goto, history, speaker-view).
    // Every change is announced as 'mdslides:slidechange' (session log) and sent to speaker view.
    function showSlide(index, method) {
      index = Math.max(0, Math.min(totalSlides - 1, index));
      const from = currentSlide;
      const slides = document.querySelectorAll('.slide');
      slides.forEach((slide, i) => {
        slide.classList.toggle('active', i === index);
      });

      currentSlide = index;

      const counter = document.getElementById('slide-counter');
      counter.textContent = (index + 1) + ' / ' + totalSlides;
      counter.setAttribute('aria-label', 'Slide ' + (index + 1) + ' of ' + totalSlides + ', go to slide');

      const prevBtn = document.getElementById('nav-prev');
      const nextBtn = document.getElementById('nav-next');
      if (prevBtn) prevBtn.disabled = index <= 0;
      if (nextBtn) nextBtn.disabled = index >= totalSlides - 1;

      try { history.replaceState(null, '', '#' + (index + 1)); } catch (err) { /* ignore */ }

      requestAnimationFrame(fitActiveSlide);

      if (from !== index) {
        const how = method || 'other';
        document.dispatchEvent(new CustomEvent('mdslides:slidechange', { detail: { from: from, to: index, method: how } }));
        if (how !== 'speaker-view' && window.MDSlidesSync) window.MDSlidesSync.sendSlideChange(index);
      }
    }

    function nextSlide(method) { showSlide(currentSlide + 1, method || 'keyboard'); }
    function prevSlide(method) { showSlide(currentSlide - 1, method || 'keyboard'); }
    function firstSlide() { showSlide(0, 'keyboard'); }
    function lastSlide() { showSlide(totalSlides - 1, 'keyboard'); }

    // Follow navigation made in speaker view
    if (window.MDSlidesSync) window.MDSlidesSync.receiveSlideChange((i) => showSlide(i, 'speaker-view'));

    // Auto-scale code blocks that exceed 20 lines (PDR-006)
    function scaleCodeBlocks() {
      document.querySelectorAll('pre code').forEach(codeBlock => {
        const lines = codeBlock.textContent.split('\\n').length;
        if (lines > 20) {
          // Calculate scale factor: 20 lines = 1.0, 40 lines = 0.7, 60+ lines = 0.5
          const scaleFactor = Math.max(0.5, 1.0 - ((lines - 20) * 0.015));
          codeBlock.parentElement.style.fontSize = (18 * scaleFactor) + 'px';
        }
      });
    }

    // ---- Fit-to-slide: scale overflowing tables, diagrams, code and whole content ----
    const FIT_BLOCK_FLOOR = 0.4;
    const FIT_CONTENT_FLOOR = 0.35;

    function useZoom() {
      return !!(window.CSS && CSS.supports && CSS.supports('zoom', '0.5'));
    }

    // fullWidthPx (content only): let the scaled content use the whole visual width of the
    // wrapper, so wide tables/text re-wrap into fewer lines instead of staying capped at the
    // slide's max-width and shrinking uniformly (which never fits very wide tables).
    function applyScale(el, s, isContent, fullWidthPx) {
      if (isContent && fullWidthPx) {
        el.style.maxWidth = 'none';
        el.style.width = (fullWidthPx / s) + 'px';
      }
      if (useZoom()) {
        el.style.zoom = String(s);
      } else {
        const h = el.getBoundingClientRect().height;
        el.style.transformOrigin = isContent ? 'top center' : 'top left';
        el.style.transform = 'scale(' + s + ')';
        el.style.marginBottom = (-(h * (1 - s))) + 'px';
      }
      el.setAttribute('data-fit', s.toFixed(3));
    }

    function clearScale(el) {
      el.style.zoom = '';
      el.style.transform = '';
      el.style.transformOrigin = '';
      el.style.marginBottom = '';
      el.style.maxWidth = '';
      el.style.width = '';
      el.removeAttribute('data-fit');
    }

    // Union of the bounding boxes of an element and all its descendants
    function extentOf(root) {
      const r = root.getBoundingClientRect();
      let left = r.left, top = r.top, right = r.right, bottom = r.bottom;
      root.querySelectorAll('*').forEach(el => {
        // Code blocks scroll internally and SVGs clip their own drawing (Mermaid Gantt charts
        // place off-canvas elements far outside the viewBox): measure those by their own box only
        if (el.parentElement && el.parentElement.closest('pre, svg')) return;
        const b = el.getBoundingClientRect();
        if (b.width === 0 && b.height === 0) return;
        if (b.left < left) left = b.left;
        if (b.top < top) top = b.top;
        if (b.right > right) right = b.right;
        if (b.bottom > bottom) bottom = b.bottom;
      });
      return { width: right - left, height: bottom - top };
    }

    function fitActiveSlide() {
      const slide = document.querySelector('.slide.active');
      if (!slide) return;
      const wrapper = slide.querySelector('.slide-content-wrapper');
      const content = wrapper && wrapper.firstElementChild;
      if (!content) return;

      // Reset any previous fit
      slide.querySelectorAll('[data-fit]').forEach(clearScale);
      wrapper.style.overflowY = '';
      wrapper.style.paddingBottom = '';
      content.classList.add('fit-managed');

      // Keep content clear of the floating controls bar where it overlaps the slide area
      const controls = document.querySelector('.controls');
      if (controls) {
        const cr = controls.getBoundingClientRect();
        const wr = wrapper.getBoundingClientRect();
        const overlap = wr.bottom - cr.top + 8;
        if (cr.width > 0 && cr.left < wr.right && cr.right > wr.left && overlap > 0) {
          wrapper.style.paddingBottom = Math.round(overlap) + 'px';
        }
      }
      const availH = () => wrapper.clientHeight - (parseFloat(wrapper.style.paddingBottom) || 0);

      // Horizontal pass: blocks wider than their container
      content.querySelectorAll('table, pre, svg, img, .mermaid-diagram').forEach(el => {
        if (el.closest('[data-fit]')) return;
        const parent = el.parentElement;
        if (!parent) return;
        const cs = getComputedStyle(parent);
        const available = parent.clientWidth - parseFloat(cs.paddingLeft || 0) - parseFloat(cs.paddingRight || 0);
        const isReplaced = el.tagName.toLowerCase() === 'svg' || el.tagName.toLowerCase() === 'img';
        // Code blocks: highlight.js makes the inner <code> the scroller, so add its hidden overflow
        const inner = el.tagName.toLowerCase() === 'pre' ? el.querySelector('code') : null;
        const hidden = inner ? Math.max(0, inner.scrollWidth - inner.clientWidth) : 0;
        const natural = isReplaced ? el.getBoundingClientRect().width : el.scrollWidth + hidden;
        // 1.5% safety margin: glyph rounding changes slightly once an outer zoom is applied
        const target = available * 0.985;
        if (available > 0 && natural > available + 1) {
          let bs = Math.max(FIT_BLOCK_FLOOR, target / natural);
          applyScale(el, bs, false);
          // Re-measure: font hinting and rewrapping can leave a few pixels over
          for (let i = 0; i < 3 && bs > FIT_BLOCK_FLOOR; i++) {
            const w = el.getBoundingClientRect().width;
            if (w <= target + 0.5) break;
            bs = Math.max(FIT_BLOCK_FLOOR, bs * (target / w) * 0.995);
            clearScale(el);
            applyScale(el, bs, false);
          }
        }
      });

      // Overall pass: largest scale (floor..1) at which the whole content fits the wrapper.
      // Binary search, because scaled content is re-laid out at full wrapper width
      // (see applyScale), so height does not shrink linearly with scale.
      const fitsNow = () => {
        const ext = extentOf(content);
        return ext.height <= availH() + 1 && ext.width <= wrapper.clientWidth + 1;
      };
      const setScale = (s) => {
        clearScale(content);
        if (s < 1) applyScale(content, s, true, wrapper.clientWidth);
      };
      if (!fitsNow()) {
        setScale(FIT_CONTENT_FLOOR);
        if (!fitsNow()) {
          // Still too big at the floor: fall back to scrolling
          setScale(1);
          content.classList.remove('fit-managed');
          wrapper.style.overflowY = 'auto';
        } else {
          let lo = FIT_CONTENT_FLOOR, hi = 1;
          for (let i = 0; i < 7; i++) {
            const mid = (lo + hi) / 2;
            setScale(mid);
            if (fitsNow()) lo = mid; else hi = mid;
          }
          setScale(lo);
        }
      }
    }

    let fitTimer = null;
    function scheduleFit() {
      clearTimeout(fitTimer);
      fitTimer = setTimeout(fitActiveSlide, 120);
    }
    window.addEventListener('resize', scheduleFit);
    window.addEventListener('orientationchange', scheduleFit);
    window.addEventListener('load', () => {
      fitActiveSlide();
      document.querySelectorAll('.slide img').forEach(img => {
        if (!img.complete) img.addEventListener('load', () => {
          if (img.closest('.slide.active')) fitActiveSlide();
        });
      });
    });
    if (document.fonts && document.fonts.ready) {
      document.fonts.ready.then(fitActiveSlide);
    }

    // ---- Input handling ----
    function isGotoModalOpen() {
      const modal = document.querySelector('.goto-modal');
      return !!modal && getComputedStyle(modal).display !== 'none';
    }

    function isTypingTarget(t) {
      return !!(t && t.closest && t.closest('input, textarea, select, [contenteditable]'));
    }

    function toggleFullscreen() {
      try {
        const active = document.fullscreenElement || document.webkitFullscreenElement;
        let p;
        if (active) {
          const exit = document.exitFullscreen || document.webkitExitFullscreen;
          if (exit) p = exit.call(document);
        } else {
          const el = document.documentElement;
          const req = el.requestFullscreen || el.webkitRequestFullscreen;
          if (req) p = req.call(el);
        }
        if (p && p.catch) p.catch(() => {});
      } catch (err) { /* ignore */ }
    }

    document.addEventListener('keydown', (e) => {
      if (e.ctrlKey || e.metaKey || e.altKey) return;
      if (isTypingTarget(e.target)) return;
      if (isGotoModalOpen()) return;
      switch(e.key) {
        case ' ':
        case 'Enter':
          // Let focused buttons/links handle their own activation
          if (e.target && e.target.closest && e.target.closest('button, a, summary')) return;
          e.preventDefault();
          nextSlide();
          break;
        case 'ArrowRight':
        case 'ArrowDown':
        case 'PageDown':
          e.preventDefault();
          nextSlide();
          break;
        case 'ArrowLeft':
        case 'ArrowUp':
        case 'PageUp':
          e.preventDefault();
          prevSlide();
          break;
        case 'Home':
          e.preventDefault();
          firstSlide();
          break;
        case 'End':
          e.preventDefault();
          lastSlide();
          break;
        case 's':
        case 'S':
          e.preventDefault();
          // Open speaker view with current slide index
          window.open(`speaker.html?slide=$${currentSlide}`, 'speaker-view', 'width=1024,height=768');
          break;
        case 't':
        case 'T':
          e.preventDefault();
          if (timerManager) timerManager.togglePause();
          break;
        case 'r':
        case 'R':
          e.preventDefault();
          if (timerManager) timerManager.reset();
          break;
        case 'f':
        case 'F':
          e.preventDefault();
          toggleFullscreen();
          break;
        case 'l':
        case 'L':
          // Save the session log (read by: java -jar md-slides.jar report DECK_NAME)
          e.preventDefault();
          if (window.MDSlidesHistoryLogger) window.MDSlidesHistoryLogger.downloadLog();
          break;
      }
    });

    // Touch gestures: swipe left/right, tap zones for touch/pen only
    let touchStartInfo = null;
    let suppressTapUntil = 0;
    let lastPointerType = 'mouse';

    document.addEventListener('pointerdown', (e) => { lastPointerType = e.pointerType; }, { passive: true, capture: true });

    function isPinchZoomed() {
      return !!(window.visualViewport && window.visualViewport.scale > 1.05);
    }

    function canScrollHorizontally(start, dx) {
      for (let el = start; el && el !== document.documentElement; el = el.parentElement) {
        if (!(el.scrollWidth > el.clientWidth + 1)) continue;
        const ox = getComputedStyle(el).overflowX;
        if (ox !== 'auto' && ox !== 'scroll') continue;
        if (dx < 0 && el.scrollLeft + el.clientWidth < el.scrollWidth - 1) return true;
        if (dx > 0 && el.scrollLeft > 0) return true;
      }
      return false;
    }

    document.addEventListener('touchstart', (e) => {
      if (e.touches.length !== 1) { touchStartInfo = null; return; }
      const t = e.touches[0];
      touchStartInfo = { x: t.clientX, y: t.clientY, time: Date.now(), target: e.target };
    }, { passive: true });

    document.addEventListener('touchcancel', () => { touchStartInfo = null; }, { passive: true });

    document.addEventListener('touchend', (e) => {
      const start = touchStartInfo;
      touchStartInfo = null;
      if (!start || e.touches.length > 0) return;
      const t = e.changedTouches[0];
      const dx = t.clientX - start.x;
      const dy = t.clientY - start.y;
      const dt = Date.now() - start.time;
      const threshold = Math.max(40, 0.08 * window.innerWidth);
      if (Math.abs(dx) < threshold || Math.abs(dx) <= 1.5 * Math.abs(dy) || dt >= 800) return;
      if (isPinchZoomed() || isGotoModalOpen()) return;
      if (canScrollHorizontally(start.target, dx)) return;
      suppressTapUntil = Date.now() + 400;
      if (dx < 0) nextSlide('swipe'); else prevSlide('swipe');
    }, { passive: true });

    document.addEventListener('click', (e) => {
      if (lastPointerType === 'mouse') return;
      const target = e.target;
      if (!target || !target.closest || !target.closest('.slides')) return;
      if (target.closest('a, button, input, select, textarea, label, summary, [contenteditable], .controls, .goto-modal')) return;
      const sel = window.getSelection && window.getSelection();
      if (sel && String(sel).length > 0) return;
      if (Date.now() < suppressTapUntil) return;
      if (isGotoModalOpen() || isPinchZoomed()) return;
      if (e.clientX < window.innerWidth * 0.3) prevSlide('tap'); else nextSlide('tap');
    });

    // Footers keep clear of the controls bar: expose its width to CSS
    function measureControls() {
      const c = document.querySelector('.controls');
      if (c) document.documentElement.style.setProperty('--controls-width', Math.ceil(c.getBoundingClientRect().width + 24) + 'px');
    }
    window.addEventListener('resize', measureControls);
    window.addEventListener('load', measureControls);

    // On-screen buttons
    document.addEventListener('DOMContentLoaded', () => {
      const prevBtn = document.getElementById('nav-prev');
      const nextBtn = document.getElementById('nav-next');
      const fsBtn = document.getElementById('nav-fullscreen');
      if (prevBtn) prevBtn.addEventListener('click', () => prevSlide('button'));
      if (nextBtn) nextBtn.addEventListener('click', () => nextSlide('button'));
      if (fsBtn) {
        if (document.fullscreenEnabled || document.webkitFullscreenEnabled) fsBtn.hidden = false;
        fsBtn.addEventListener('click', toggleFullscreen);
      }
      if (prevBtn) prevBtn.disabled = currentSlide <= 0;
      if (nextBtn) nextBtn.disabled = currentSlide >= totalSlides - 1;
      measureControls();  // after the fullscreen button may have been shown
    });

    // Deep link: #N opens slide N
    function applyHash() {
      const m = /^#([0-9]+)$$/.exec(location.hash);
      if (!m) return;
      const n = parseInt(m[1], 10) - 1;
      if (n >= 0 && n < totalSlides && n !== currentSlide) showSlide(n, 'link');
    }
    window.addEventListener('hashchange', applyHash);

    // Run auto-scaling on page load (PDR-001, PDR-006)
    window.addEventListener('DOMContentLoaded', () => {
      scaleCodeBlocks();
      fitActiveSlide();
      // Defer so other DOMContentLoaded initialisers (history, goto) see the change
      setTimeout(applyHash, 0);
    });
  """

  /**
   * Generate Mermaid initialization JavaScript (v2.0.0).
   *
   * Initializes Mermaid.js with theme-aware configuration.
   * Implements scenarios 9-10 (theme-aware rendering) and 11-12 (graceful degradation).
   *
   * @param theme Current presentation theme
   * @return JavaScript code for Mermaid initialization
   */
  private def mermaidInitJS(theme: Theme): String =
    val mermaidTheme = theme.name match
      case "dark" => "dark"
      case "corporate" => "dark"
      case _ => "default"

    s"""
    // Initialize and run mermaid (v10.9.x)
    if (typeof mermaid !== 'undefined') {
      mermaid.initialize({
        theme: '$mermaidTheme',
        securityLevel: 'loose',
        startOnLoad: false
      });

      // Manually run mermaid since DOM is already loaded
      document.addEventListener('DOMContentLoaded', () => {
        mermaid.run();
      });
    } else {
      // Graceful degradation: CDN failed to load (scenarios 11-12)
      document.addEventListener('DOMContentLoaded', () => {
        console.warn('Mermaid.js CDN unavailable. Diagrams will not render.');
        document.querySelectorAll('.mermaid').forEach(diagram => {
          const source = diagram.textContent;
          diagram.innerHTML = '<pre style="background: #f5f5f5; padding: 1em; border-radius: 4px; overflow: auto;"><code>' +
            source + '</code></pre>' +
            '<p style="color: #d32f2f; font-size: 0.9em; margin-top: 0.5em;">' +
            '⚠ Diagram rendering unavailable. Mermaid.js CDN failed to load. Check internet connection or install locally.' +
            '</p>';
        });
      });
    }
  """

end HTMLRenderer
