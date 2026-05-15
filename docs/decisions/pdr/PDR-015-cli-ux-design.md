# PDR-015: CLI UX Design

**Status:** Proposed
**Date:** 2024-12-26
**Deciders:** Tony Moores (Product Owner)
**Related User Stories:** US-019 (Improved CLI UX)

---

## Context and Problem Statement

The current CLI (v0.4.0) has several UX issues:

1. **Redundancy**: Users must specify both input and output paths
   ```bash
   mdslides slides.md output.html --theme dark
   ```

2. **Directory pollution**: Images are copied to parent directory of output
   - If output is `/tmp/presentation.html`, images go to `/tmp/images/`
   - Pollutes filesystem with loose `images/` directories

3. **Misleading extension**: Output is `.html` but should be directory
   - Future features (speaker view, handouts) need multiple HTML files
   - Single-file output doesn't scale

4. **No convention**: Output name doesn't match input name by default
   - User manually maintains naming consistency

**Decision Driver:** Pre-v1.0 is the only acceptable time for breaking CLI changes.

---

## Decision Drivers

* **UX simplicity** - Minimize required arguments for common case
* **Convention over configuration** - Smart defaults reduce cognitive load
* **Future-proofing** - Directory output enables multi-file features (US-034 speaker view)
* **Modern tool alignment** - CLI should feel like `vite`, `hugo`, `jekyll`
* **Breaking change window** - Must implement before v1.0 public release

---

## Considered Options

### Option 1: Keep Current CLI (No Change)

**Syntax:**
```bash
mdslides INPUT.md OUTPUT.html [--theme THEME]
```

**Pros:**
- No breaking changes
- Existing scripts continue to work (if any users exist)

**Cons:**
- Directory pollution issue remains
- Redundant arguments
- Single-file output blocks future features
- Doesn't scale to multi-file output needs

**Verdict:** ❌ Rejected - Technical debt will compound

---

### Option 2: Add Directory Flag (Incremental)

**Syntax:**
```bash
# Old way (single file)
mdslides slides.md output.html

# New way (directory)
mdslides slides.md output/ --output-dir
```

**Pros:**
- Backward compatible
- Gradual migration path

**Cons:**
- Still redundant (input + output names)
- Two conflicting modes confuse users
- Deprecation cycle needed (complicates codebase)
- Half-measure doesn't solve core UX issue

**Verdict:** ❌ Rejected - Adds complexity without fully solving problem

---

### Option 3: Inferred Output (Recommended) ✅

**Syntax:**
```bash
# Simple form (90% of cases)
mdslides render DECK_NAME [--theme THEME]

# Explicit form (10% of cases)
mdslides render --input FILE --output DIR [--theme THEME]
mdslides render -i FILE -o DIR
```

**Behavior:**

**Simple form:** `mdslides render my-preso --theme dark`
- Reads: `my-preso.md` (or `my-preso.markdown`)
- Creates: `my-preso/` directory
- Writes:
  - `my-preso/index.html`
  - `my-preso/images/` (content images)
  - `my-preso/backgrounds/` (theme backgrounds)

**Explicit form:** `mdslides render -i slides.txt -o dist`
- Reads: `slides.txt`
- Creates: `dist/` directory
- Writes: `dist/index.html`, `dist/images/`, `dist/backgrounds/`

**Pros:**
- ✅ Simple: One argument for common case
- ✅ Conventional: Output name matches input stem
- ✅ Future-proof: Directory structure supports multi-file output
- ✅ Clean: No directory pollution (assets contained in output dir)
- ✅ Modern: Aligns with contemporary tool UX

**Cons:**
- ⚠️ Breaking change (acceptable pre-v1.0)
- ⚠️ Requires updating tutorial examples
- ⚠️ May surprise users expecting single HTML file

**Verdict:** ✅ **SELECTED** - Best long-term UX, acceptable breaking change window

---

## Decision Outcome

**Chosen option:** Option 3 (Inferred Output with Directory Structure)

### Positive Consequences

* **Simpler UX**: `mdslides render my-talk` vs `mdslides my-talk.md my-talk.html`
* **Clean output**: All assets in `my-talk/` directory, shareable as single unit
* **Scales to future**: Speaker view (US-034) can add `my-talk/speaker.html`
* **Modern feel**: CLI matches expectations from other tools (Hugo, Jekyll, Vite)

### Negative Consequences

* **Breaking change**: Existing scripts will break (mitigated: pre-v1.0)
* **Tutorial update**: All examples need revision
* **Directory assumption**: Users expecting single file may be surprised (mitigated: clear docs)

---

## Implementation Details

### Path Resolution Rules

1. **Input resolution** (simple form):
   - `mdslides render DECK_NAME` looks for:
     - `DECK_NAME.md` (first priority)
     - `DECK_NAME.markdown` (fallback)
   - Error if neither exists: "Input file not found: DECK_NAME.md or DECK_NAME.markdown"

2. **Output resolution** (simple form):
   - `mdslides render DECK_NAME` creates:
     - `DECK_NAME/` directory (created if missing)
     - `DECK_NAME/index.html` (main presentation)
     - `DECK_NAME/images/` (content images)
     - `DECK_NAME/backgrounds/` (theme backgrounds)

3. **Explicit paths** (`-i`/`-o` flags):
   - No inference, use paths exactly as specified
   - Output must be directory path (create if missing)
   - Write `OUTPUT_DIR/index.html`

### Asset Path Handling

**Current (v0.4.0):** Absolute paths in HTML
```html
<img src="/tmp/images/diagram.png">
```

**New (v1.0):** Relative paths from `index.html`
```html
<img src="images/diagram.png">
<div style="background-image: url(backgrounds/retisio-title-page.png)">
```

**Benefit:** Output directory is portable (can move/zip/share)

### Error Handling

1. **Input not found:**
   ```
   ✗ Input file not found: my-preso.md or my-preso.markdown

   Usage: mdslides render DECK_NAME [--theme THEME]
          mdslides render -i INPUT -o OUTPUT
   ```

2. **Output directory conflict:**
   ```
   ✗ Output directory exists and is not empty: my-preso/

   Options:
     - Use different deck name
     - Remove existing directory: rm -rf my-preso/
     - Use explicit output: mdslides render my-preso -o my-preso-v2
   ```

3. **Missing theme:**
   ```
   ✗ Theme 'nonexistent' not found in ./themes/

   Available themes:
     - light (default)
     - dark
     - retisio
   ```

### CLI Command Structure

```
mdslides render DECK_NAME [OPTIONS]
mdslides render -i INPUT -o OUTPUT [OPTIONS]

Arguments:
  DECK_NAME              Deck name stem (reads DECK_NAME.md, creates DECK_NAME/)

Options:
  -i, --input FILE       Input markdown file (explicit mode)
  -o, --output DIR       Output directory (explicit mode)
  --theme THEME          Theme name (default: light)
  --no-copy-images       Skip image asset copying
  -h, --help             Show help
  -v, --version          Show version

Examples:
  mdslides render my-talk                    # Simple: my-talk.md → my-talk/index.html
  mdslides render my-talk --theme dark       # With theme
  mdslides render -i slides.txt -o dist      # Explicit paths
```

---

## Compliance and Constraints

### Breaking Change Policy

**Policy:** Breaking changes acceptable before v1.0 public release.

**Mitigation:**
- Update all tutorial examples to new syntax
- Add migration note in v1.0 release notes
- Clear error messages guide users to new syntax

### Backward Compatibility

**Not required** for v1.0 (pre-release).

**Rationale:**
- v0.x versions are development/alpha releases
- No known production users (internal tool so far)
- Clean break is simpler than dual-mode support

---

## Links and References

* **Related User Stories:**
  - US-019: Improved CLI UX
  - US-034: Speaker View (v1.1) - requires directory output

* **Related Governance:**
  - [Product Backlog](../../internal/planning/product-backlog.md) - v1.0 MVP scope

* **Tool Inspiration:**
  - Hugo: `hugo` (infers output from content)
  - Vite: `vite build` (writes to `dist/` by default)
  - Jekyll: `jekyll build` (writes to `_site/`)

---

**Decision Date:** 2024-12-26
**Review Date:** Before v1.0 release
**Status:** Proposed (awaiting implementation)
