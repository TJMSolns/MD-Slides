# ADR-011: Syntax Highlighting Approach

**Status:** Accepted
**Date:** 2024-12-21
**Deciders:** Development Team
**Related:** US-004, ADR-010 (Markdown Library), ADR-006 (Rendering Architecture)

## Context

MDSlides v0.2.0 will support code blocks with syntax highlighting. We need to decide whether syntax highlighting should happen at **build time** (JVM-based) or **render time** (browser-based with JavaScript).

### User Requirements

From Event Storming (December 21, 2024):
- Must support **180+ programming languages** (all highlight.js languages)
- Must support **Mermaid diagrams** (requires JavaScript in browser)
- Must work **completely offline** (no external CDN dependencies)
- Code blocks should **auto-scale font size** to fit more than 20 lines on a slide

### Candidate Approaches

#### 1. JVM-Based (Build-Time)

**Libraries:**
- **Pygments via Jython** - Python syntax highlighter on JVM
- **Java-based highlighters** - Limited language support

**Pros:**
- Pure HTML output (no JavaScript required for code highlighting)
- Highlighting guaranteed to work (no browser dependency)

**Cons:**
- Limited language support (~50-80 languages)
- Adds significant JAR size (10-20MB for Jython)
- Complex integration (requires Python runtime on JVM)
- **Cannot coexist with Mermaid** (Mermaid requires JavaScript anyway)
- Font auto-scaling still requires JavaScript

#### 2. JavaScript-Based (Render-Time)

**Libraries:**
- **highlight.js** - Supports 180+ languages, widely used
- **Prism.js** - Similar features, slightly smaller

**Pros:**
- Comprehensive language support (180+ languages)
- Small footprint (~100KB bundled, minified)
- Works offline when bundled inline in HTML
- **Consistent with Mermaid approach** (both use client-side JavaScript)
- Font auto-scaling easily implemented in same JavaScript
- Widely tested and battle-proven

**Cons:**
- Requires JavaScript enabled in browser
- Highlighting happens on page load (slight delay)

## Decision

We will use **JavaScript-based syntax highlighting with highlight.js**, bundled inline in the generated HTML for offline support.

## Rationale

### Why JavaScript-Based?

1. **Mermaid Consistency**: Since Mermaid diagrams require JavaScript (ADR-010), we already need JavaScript runtime. Using JavaScript for syntax highlighting creates a consistent approach.

2. **Offline Support**: By bundling highlight.js inline in the HTML, presentations work completely offline with no CDN dependencies.

3. **Language Coverage**: highlight.js supports 180+ languages out of the box, far exceeding JVM-based options.

4. **Font Auto-Scaling**: User requirement for auto-scaling code block fonts is naturally implemented in JavaScript alongside highlighting.

5. **JAR Size**: Keeps our JAR lean. HTML file size increases by ~100KB (acceptable for standalone presentations).

6. **Simplicity**: No complex JVM integration, just include JavaScript in rendered HTML.

### Why Not JVM-Based?

- **Mermaid requires JavaScript anyway**: We can't avoid JavaScript if we want Mermaid support
- **Limited language support**: 50-80 languages vs 180+ with highlight.js
- **JAR bloat**: Adding 10-20MB for Jython is excessive
- **Complexity**: Integrating Python runtime adds significant complexity
- **Font auto-scaling still needs JavaScript**: So we'd need both JVM and JS solutions

## Consequences

### Positive

- **Rich language support**: 180+ programming languages
- **Offline-capable**: Bundled inline, no CDN required
- **Consistent architecture**: All client-side features (highlighting, Mermaid, font scaling) use JavaScript
- **Small JAR**: No additional JVM dependencies
- **Easy maintenance**: highlight.js is actively maintained

### Negative

- **JavaScript required**: Presentations won't highlight code if JavaScript is disabled
- **HTML file size**: Each presentation grows by ~100KB due to bundled highlight.js
- **Page load time**: Slight delay while highlighting runs (typically <100ms)

### Mitigation

- **Graceful degradation**: Code blocks render as plain `<pre><code>` if JavaScript is disabled
- **Minified bundles**: Use minified highlight.js to minimize file size impact
- **Lazy initialization**: Only load highlight.js if code blocks are present in presentation

## Implementation Notes

### Bundling highlight.js Inline

The HTMLRenderer will include highlight.js inline in the `<script>` tag:

```scala
object HTMLRenderer:
  private val highlightJsCode: String =
    // Load from resources/highlight.min.js
    scala.io.Source.fromResource("highlight.min.js").mkString

  def renderDeck(deck: SlideDeck): String =
    html(
      head(...),
      body(
        // Slides...
        tag("script")(raw(highlightJsCode)),
        tag("script")(raw("""
          document.addEventListener('DOMContentLoaded', (event) => {
            document.querySelectorAll('pre code').forEach((el) => {
              hljs.highlightElement(el);
              autoScaleCodeBlock(el);  // Font auto-scaling
            });
          });
        """))
      )
    )
```

### Font Auto-Scaling

Implement alongside highlight.js:

```javascript
function autoScaleCodeBlock(el) {
  const maxLines = 20;
  const lineCount = el.textContent.split('\n').length;

  if (lineCount > maxLines) {
    const scaleFactor = maxLines / lineCount;
    const baseFontSize = 16; // px
    const newFontSize = Math.max(10, baseFontSize * scaleFactor);
    el.style.fontSize = newFontSize + 'px';
  }
}
```

### Resource Files

Add to `infrastructure/resources/`:
- `highlight.min.js` (~100KB) - Core library
- `mermaid.min.js` (~200KB) - Diagram rendering

Both bundled inline in HTML output.

### Language Support

highlight.js automatically detects language from code fence:

````markdown
```scala
def hello(name: String): String = s"Hello, $name!"
```
````

Renders as:

```html
<pre><code class="language-scala hljs">
  <span class="hljs-keyword">def</span> hello(name: <span class="hljs-type">String</span>): ...
</code></pre>
```

### Mermaid Integration

Mermaid works the same way:

````markdown
```mermaid
graph TD
  A --> B
```
````

Renders as:

```html
<pre class="mermaid">
graph TD
  A --> B
</pre>
<script>
  mermaid.initialize({ startOnLoad: true, theme: 'default' });
</script>
```

## Alternatives Considered

### Hybrid Approach (JVM for common languages, JS for others)

**Rejected**: Adds complexity without significant benefit. Inconsistent user experience (some languages highlighted at build time, others at render time).

### Server-Side Rendering with Headless Browser

**Rejected**: Requires Puppeteer/Playwright, significantly complicates build. Offline requirement eliminates this option.

### No Syntax Highlighting

**Rejected**: User explicitly requested syntax highlighting. Code blocks without highlighting are hard to read in presentations.

## References

- [highlight.js Documentation](https://highlightjs.org/)
- [Mermaid Documentation](https://mermaid.js.org/)
- [ADR-010: Markdown Library Selection](ADR-010-markdown-library-selection.md)
- [ADR-006: Rendering Architecture](ADR-006-rendering-architecture.md)
- [US-004: Code Block Support](../CEREMONIES-v0.2.0.md#us-004-code-block-support)

## Revision History

- **2024-12-21**: Initial version (v0.2.0)
