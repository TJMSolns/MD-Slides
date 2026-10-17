# PDR-009: Image Copy Default Behavior

**Status:** Accepted
**Date:** 2025-12-22
**Deciders:** Product Team
**Related:** US-006, ADR-012, PDR-010

## Context

With the introduction of automatic image asset copying (US-006), we must decide the default behavior for MDSlides CLI when no explicit flags are provided.

**Two possible defaults:**

1. **Copy by default** (opt-out): `mdslides slides.md output.html` copies images automatically
2. **No copy by default** (opt-in): `mdslides slides.md output.html` requires `--copy-images` flag

## Decision

**Image copying is ENABLED BY DEFAULT.**

When users run:
```bash
mdslides slides.md output.html
```

MDSlides will:
1. Parse slides and detect image references
2. Copy referenced local image files to output directory
3. Rewrite image paths in HTML to match copied locations
4. Generate self-contained presentation

Users can opt-out with `--no-copy-images`:
```bash
mdslides slides.md output.html --no-copy-images
```

## Rationale

### User Expectation

**Most users expect presentations to "just work":**
- When I generate a presentation, I expect all assets to be included
- When I share a folder, I expect it to contain everything needed
- When I open the HTML, I expect images to display

**Copying by default meets this expectation.**

### Pain Point Frequency

Based on user feedback and common issues:
- **High frequency:** "My images don't show up when I share presentations"
- **Low frequency:** "I don't want images copied" (power users with specific workflows)

**Optimize for the common case (copying), allow power users to opt-out.**

### Principle of Least Surprise

Users familiar with other tools (Hugo, Jekyll, Pandoc) expect static site generators to handle assets automatically.

**Default copying aligns with industry norms.**

### Safety vs Convenience

Two failure modes:

1. **Copy when not wanted:** Minor inconvenience (extra files, disk space)
2. **Don't copy when needed:** Broken presentation, frustrated users

**Broken presentations are worse than extra disk space. Prefer safe default.**

## Consequences

### Positive

1. **Better UX for New Users:**
   - Presentations work immediately
   - No manual file management required
   - Lower barrier to entry

2. **Fewer Broken Presentations:**
   - Images always included
   - Validation catches missing files
   - Reduces support burden

3. **Consistent with User Mental Model:**
   - "Generate presentation" means "make it ready to use"
   - Output directory is self-contained
   - Easy to share

### Negative

1. **Power Users Must Opt-Out:**
   - Users with custom deployment workflows need `--no-copy-images`
   - One extra flag for advanced use cases
   - **Mitigation:** Well-documented, clear flag name

2. **Slightly Slower Builds:**
   - File I/O adds latency (typically <1 second)
   - **Mitigation:** Acceptable for better UX
   - **Future:** Cache/skip unchanged images

3. **Disk Space Duplication:**
   - Images copied to output directory
   - **Mitigation:** Only output affected, source unchanged
   - **Acceptable:** Disk space cheap, portability valuable

## Use Cases

### Use Case 1: New User Creating First Presentation

**Scenario:** User creates slides with images, generates presentation

**With copy-by-default:**
```bash
mdslides my-slides.md presentation.html
open presentation.html  # Images work!
```
✅ **Works immediately**

**Without copy-by-default:**
```bash
mdslides my-slides.md presentation.html
open presentation.html  # Broken images ❌
# User confused, searches docs, copies images manually
```
❌ **Requires troubleshooting**

### Use Case 2: Sharing Presentation with Colleagues

**Scenario:** User generates presentation, zips directory, shares

**With copy-by-default:**
```bash
mdslides slides.md output/index.html
zip -r presentation.zip output/
# Colleague unzips, opens index.html, images work ✅
```

**Without copy-by-default:**
```bash
mdslides slides.md output/index.html
# User forgets to copy images
zip -r presentation.zip output/
# Colleague unzips, opens index.html, broken images ❌
```

### Use Case 3: Power User with Custom Workflow

**Scenario:** User deploys to web server, images already hosted

**With copy-by-default:**
```bash
mdslides slides.md output.html --no-copy-images
# One flag, user aware of advanced usage
```
✅ **Easy opt-out**

**Without copy-by-default:**
```bash
mdslides slides.md output.html
# Works as expected for power user
```
✅ **Works, but breaks common case**

**Decision:** Optimize for use cases 1 & 2 (majority), provide opt-out for case 3 (minority).

## Alternative Behaviors Considered

### Alternative 1: Ask User on First Run

**Approach:** Prompt user to choose default behavior on first invocation

**Pros:**
- User-controlled default
- No assumptions

**Cons:**
- Interrupts workflow
- Adds complexity (config file, state management)
- Confusing for new users ("I don't know what I want yet")

**Decision:** Rejected. Too complex, worse UX.

### Alternative 2: Smart Detection

**Approach:** Detect if images are in same directory structure, copy only if not

**Pros:**
- "Do what I mean" behavior
- No flags needed

**Cons:**
- Non-deterministic behavior (confusing)
- Hard to predict when copying happens
- Breaks principle of least surprise

**Decision:** Rejected. Explicit > implicit.

### Alternative 3: Separate Command

**Approach:** `mdslides build` (no copy) vs `mdslides package` (with copy)

**Pros:**
- Clear distinction
- No flags needed

**Cons:**
- Two commands to learn
- More documentation
- Inconsistent with simple CLI philosophy

**Decision:** Rejected. Flags are simpler than subcommands for single-feature toggle.

## Configuration

**No configuration file.** Behavior controlled entirely by CLI flags:

```bash
# Copy images (default)
mdslides slides.md output.html
mdslides slides.md output.html --copy-images

# Don't copy images
mdslides slides.md output.html --no-copy-images

# Custom output directory for images
mdslides slides.md output.html --images-dir assets/images
```

**Rationale:** Simple tool, simple configuration. Avoid hidden config files.

## Migration

### From v0.2.0 to v0.3.0

**Breaking Change?** No.

**Behavior Change:**
- v0.2.0: Images not copied (user must copy manually)
- v0.3.0: Images copied by default (user can opt-out)

**Impact:**
- Existing scripts work unchanged
- Presentations now self-contained (improvement)
- Users who DON'T want copying must add `--no-copy-images`

**Mitigation:**
- Document new behavior in CHANGELOG
- Add migration note for power users
- Clear error messages if copying fails

### Documentation Updates

Update README.md, getting-started guides:
```markdown
## Working with Images

MDSlides automatically copies referenced images to the output directory:

```bash
mdslides slides.md presentation.html
```

This creates a self-contained presentation. To disable copying:

```bash
mdslides slides.md presentation.html --no-copy-images
```
\```
```

## Success Metrics

**Target:**
- <5% of users need `--no-copy-images`
- <1% of presentations have broken images (down from ~20% in v0.2.0)
- Support requests about images reduced by 80%

## Related Documents

- [ADR-012: Image Asset Copying Strategy](../adr/ADR-012-image-asset-copying-strategy.md)
- [PDR-010: Image Size Limits](PDR-010-image-size-limits.md)
- [US-006: Image Asset Copying](../../ceremonies/v0.3.0.md#us-006-image-asset-copying)

---

**Decision Date:** 2025-12-22
**Review Date:** After v0.3.0 user feedback
