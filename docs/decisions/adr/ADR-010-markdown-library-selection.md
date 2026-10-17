# ADR-010: Markdown Library Selection

**Status:** Accepted
**Date:** 2024-12-21
**Deciders:** Development Team
**Related:** US-003, US-004, ADR-007 (Anticorruption Layer)

## Context

MDSlides v0.1.0-MVP preserves markdown text as-is without rendering formatting. For v0.2.0, we need to parse and render full markdown including:

- **Inline formatting**: bold, italic, strikethrough, inline code
- **Links**: `[text](url)`
- **Lists**: unordered (`-`) and ordered (`1.`)
- **Code blocks**: fenced code blocks with language hints
- **Images**: `![alt](url)`
- **GitHub Flavored Markdown (GFM)**: tables, task lists, strikethrough
- **Mermaid diagrams**: ````mermaid` code blocks for diagrams

### User Requirements

From Event Storming (December 21, 2024):
- Must support **everything GitHub supports** (GFM + extensions)
- Must support **Mermaid diagrams**
- Must work **completely offline** (no external CDN dependencies)

### Candidate Libraries

1. **Flexmark** (`com.vladsch.flexmark:flexmark-all`)
   - Full-featured markdown processor
   - Supports GFM via extensions
   - Supports Mermaid (passthrough to HTML)
   - ~5MB added to JAR
   - Mature, actively maintained

2. **CommonMark-java** (`org.commonmark:commonmark`)
   - Lightweight core spec implementation
   - ~500KB added to JAR
   - No built-in GFM support (requires custom extensions)
   - No Mermaid support
   - Focused on spec compliance

3. **Laika** (`org.planet42:laika-core`)
   - Pure Scala markdown processor
   - ~2MB added to JAR
   - Limited GFM support
   - No Mermaid support
   - Requires custom renderers

## Decision

We will use **Flexmark** (`com.vladsch.flexmark:flexmark-all:0.64.8`) for markdown parsing and rendering.

## Rationale

### Why Flexmark?

1. **GFM Support**: Flexmark has built-in extensions for GitHub Flavored Markdown (tables, task lists, strikethrough, autolinks)

2. **Mermaid Passthrough**: Flexmark can identify ````mermaid` code blocks and pass them through to HTML for client-side rendering by mermaid.js

3. **Extension Ecosystem**: Rich ecosystem of extensions for advanced features (footnotes, definition lists, etc.)

4. **Anticorruption Layer Compatibility**: Flexmark's API can be wrapped cleanly in our anticorruption layer (ADR-007)

5. **JAR Size Acceptable**: 5MB is acceptable given the feature completeness. Our JAR is already 44MB due to Scala runtime.

6. **Mature & Maintained**: Flexmark is battle-tested and actively maintained

### Why Not CommonMark-java?

- No built-in GFM support (would require custom extensions)
- No Mermaid support
- Saving 4.5MB isn't worth the reduced functionality
- Would require significant custom development

### Why Not Laika?

- Limited GFM support
- No Mermaid support
- Pure Scala is nice but doesn't justify missing features
- Custom renderers required for HTML output

## Consequences

### Positive

- **Full GFM compatibility**: Users can write markdown exactly like GitHub
- **Mermaid diagram support**: Technical presentations benefit from diagrams
- **Rich feature set**: Tables, task lists, footnotes, etc. come "for free"
- **Clean anticorruption layer**: Flexmark AST can be mapped to domain FormattedContent

### Negative

- **JAR size increase**: ~5MB added (44MB → 49MB)
- **Dependency on external library**: Flexmark updates may require adaptation
- **Learning curve**: Team needs to learn Flexmark API

### Mitigation

- **Anticorruption layer** (ADR-007) isolates domain from Flexmark API
- **Wrapper abstractions** in infrastructure layer prevent Flexmark types from leaking to domain
- **Version pinning** prevents unexpected breaking changes

## Implementation Notes

### Flexmark Extensions Required

```scala
// build.sc
def ivyDeps = Agg(
  ivy"com.vladsch.flexmark:flexmark-all:0.64.8"  // Includes all extensions
)
```

### Anticorruption Layer

Create `infrastructure/parser/FlexmarkAdapter.scala`:

```scala
object FlexmarkAdapter:
  private val parser = Parser.builder()
    .extensions(
      java.util.Arrays.asList(
        TablesExtension.create(),
        StrikethroughExtension.create(),
        TaskListExtension.create(),
        AutolinkExtension.create()
      )
    )
    .build()

  def parseToFormattedContent(markdown: String): FormattedContent =
    val document = parser.parse(markdown)
    // Map Flexmark AST to domain FormattedContent
    mapToFormattedContent(document)
```

### Mermaid Handling

Flexmark will identify ````mermaid` blocks and preserve them in HTML:

```html
<pre class="mermaid">
graph TD
  A --> B
</pre>
```

Client-side mermaid.js (bundled inline) will render these at load time.

## Alternatives Considered

### Server-Side Mermaid Rendering

**Rejected**: Requires headless browser (Puppeteer, Playwright) or native library, significantly complicates build and deployment.

**Chosen**: Client-side rendering with bundled mermaid.js (offline-capable).

## References

- [Flexmark Documentation](https://github.com/vsch/flexmark-java)
- [GitHub Flavored Markdown Spec](https://github.github.com/gfm/)
- [Mermaid Documentation](https://mermaid.js.org/)
- [ADR-007: Anticorruption Layer](ADR-007-anticorruption-layer.md)
- [US-003: Full Markdown Rendering](../CEREMONIES-v0.2.0.md#us-003-full-markdown-rendering)
- [US-004: Code Block Support](../CEREMONIES-v0.2.0.md#us-004-code-block-support)

## Revision History

- **2024-12-21**: Initial version (v0.2.0)
