# PDR-003: Slide Deck Size Limits

**Status**: Accepted
**Date**: 2024-12-20
**Decider**: Tony Moores (Product Owner)
**Consulted**: Tony Moores (Architect, Team)
**Related Ceremony**: US-011 (Structure Validation)

---

## Context

Presentations vary widely in size:
- **Short talks**: 5-10 slides (lightning talks, pitches)
- **Standard presentations**: 20-50 slides (conference talks, sales decks)
- **Long-form content**: 100+ slides (training materials, courses, webinars)
- **Books**: 500+ slides (effectively slide-based books)

**Business Question**: What are reasonable min/max limits for slide decks?

**User Problems**:
1. **Empty decks**: Authors accidentally create 0-slide decks (parse errors)
2. **Massive decks**: 500+ slide "presentations" are actually courses (wrong tool)
3. **Performance**: Rendering 500 slides takes time, large HTML files

**Constraints**:
- MDSlides targets **presentations**, not training courses or books
- Validation should catch errors early (empty decks)
- Performance target: < 500ms rendering for typical decks

---

## Decision

**Slide Deck Limits**:
```
Minimum: 1 slide
Maximum: 200 slides
```

**Validation Behavior**:
- **0 slides**: Structure validation ERROR (deck must have at least 1 slide)
- **1-200 slides**: Valid (no errors)
- **201+ slides**: Structure validation ERROR (exceeds maximum deck size)

**Rationale**:

### Minimum: 1 Slide
- **Empty deck meaningless**: A presentation with 0 slides has no content
- **Likely parse error**: 0 slides usually indicates parsing failed (no valid markdown found)
- **Early failure**: Better to fail validation than generate empty HTML

### Maximum: 200 Slides
- **Presentation vs. Course**: Anything over 200 slides is training material, not a presentation
  - **Conference talk**: 30-60 slides (30 min @ 1 slide/30 sec)
  - **Sales pitch**: 10-20 slides (shorter is better)
  - **Training course**: 100-500 slides (wrong tool - use LMS, not MDSlides)
- **Performance**: Rendering 200 slides @ 24px font = ~10 MB HTML (acceptable)
- **Usability**: Nobody sits through 200-slide presentation (bad UX)
- **Edge Cases**: Some legitimate long decks exist (200 is generous buffer)

---

## Consequences

### Positive

1. **Catch Errors Early**: Empty decks fail validation (not silent failure)
2. **Performance Guardrails**: 200-slide limit prevents huge HTML files
3. **Guided Authoring**: Encourages authors to split long content into multiple decks
4. **Clear Boundaries**: Tool is for presentations, not courses

### Negative

1. **Edge Case Rejection**: Some legitimate use cases blocked (e.g., 250-slide reference deck)
2. **Workaround Needed**: Long-form authors must split content or use different tool

### Mitigations

1. **Clear Error Messages**: Explain WHY deck exceeds limit, suggest splitting
2. **Override Option**: v1.1+ could add `--allow-large-deck` flag for power users
3. **Documentation**: Explain limit rationale, suggest alternatives (multi-deck structure)

---

## Alternatives Considered

### Alternative A: No Maximum Limit
**Rationale**: Let authors create arbitrarily large decks
**Why Rejected**:
- No guardrails (authors create 1000-slide "presentations")
- Performance issues (10+ second renders, 50+ MB HTML files)
- Wrong tool for the job (MDSlides targets presentations, not courses)

### Alternative B: Stricter Maximum (50 Slides)
**Rationale**: Force brevity (TED talk limit)
**Why Rejected**:
- Too strict (many valid presentations are 60-100 slides)
- Corporate decks often 80-120 slides (comprehensive product reviews)
- 200-slide limit is generous (allows edge cases)

### Alternative C: Looser Maximum (500 Slides)
**Rationale**: Accommodate training materials
**Why Rejected**:
- Encourages wrong tool usage (courses should use LMS)
- Performance concerns (500 slides @ 100 KB/slide = 50 MB HTML)
- Rendering time (500ms target × 500 slides = 250 seconds? No.)

### Alternative D: Dynamic Limit Based on Complexity
**Approach**: Allow 500 simple slides, 100 complex slides (based on content density)
**Why Rejected**:
- Over-engineering (complex heuristic)
- Confusing for users ("Why did 150 slides fail but 200 passed?")
- Unpredictable behavior

### Alternative E: No Minimum Limit (Allow 0 Slides)
**Rationale**: Let empty decks pass validation
**Why Rejected**:
- Empty deck has no value (nothing to render)
- Likely indicates bug (parsing failed silently)
- Better to fail loudly than succeed silently

---

## Implementation Notes

### Validation Logic

```scala
def validateStructure(deck: SlideDeck): Either[NonEmptyList[StructureError], SlideDeck] = {
  val errors = List.newBuilder[StructureError]

  // Minimum: 1 slide
  if (deck.slides.isEmpty) {
    errors += StructureError(
      slideId = 0,
      message = "Slide deck is empty (must have at least 1 slide). " +
                "Check that markdown contains valid slide content separated by '---'."
    )
  }

  // Maximum: 200 slides
  if (deck.slides.length > 200) {
    errors += StructureError(
      slideId = deck.slides.length,
      message = s"Slide deck exceeds maximum size (${deck.slides.length} slides, limit: 200). " +
                "Consider splitting into multiple decks. " +
                "For training materials, use a Learning Management System instead."
    )
  }

  NonEmptyList.fromList(errors.result()) match {
    case Some(errs) => Left(errs)
    case None       => Right(deck)
  }
}
```

### Error Messages

**Empty Deck**:
```
❌ Structure Validation Failed

Slide 0:
  ❌ Slide deck is empty (must have at least 1 slide).
     → Check that markdown contains valid slide content separated by '---'.
     → Example:
       # Slide 1 Title
       Content here.
       ---
       # Slide 2 Title
       More content.
```

**Exceeds Maximum**:
```
❌ Structure Validation Failed

Slide 250:
  ❌ Slide deck exceeds maximum size (250 slides, limit: 200).
     → MDSlides is designed for presentations, not training courses.
     → Consider splitting into multiple decks:
       - part-1.md (slides 1-100)
       - part-2.md (slides 101-200)
       - part-3.md (slides 201-250)
     → For long-form training materials, use a Learning Management System.
```

---

## User Experience

### Success Path (Valid Deck)

```bash
# 50-slide deck (valid)
mdslides presentation.md output.html
✓ Structure validation passed (50 slides)
✓ Generated output.html
```

### Failure Path (Empty Deck)

```bash
# Empty deck (0 slides)
mdslides empty.md output.html
❌ Structure validation failed

Slide 0:
  ❌ Slide deck is empty (must have at least 1 slide).

No HTML generated.
```

**Likely causes**:
- Markdown file is empty
- No `---` separators (entire file treated as metadata)
- Parsing error (Flexmark failed to recognize content)

### Failure Path (Exceeds Maximum)

```bash
# 250-slide deck (exceeds limit)
mdslides massive.md output.html
❌ Structure validation failed

Slide 250:
  ❌ Slide deck exceeds maximum size (250 slides, limit: 200).

No HTML generated.
```

**User options**:
1. **Split into multiple decks** (recommended):
   ```bash
   split-markdown.sh massive.md 100  # Create 3 files (100 slides each)
   mdslides massive-part1.md part1.html
   mdslides massive-part2.md part2.html
   mdslides massive-part3.md part3.html
   ```

2. **Remove slides** (reduce content):
   - Remove redundant slides
   - Combine related slides
   - Move appendix/reference slides to separate deck

3. **Use different tool** (if training course):
   - Moodle (LMS for courses)
   - Obsidian Publish (knowledge base)
   - Docusaurus (documentation site)

---

## Edge Cases

### Case 1: Single-Slide Deck
```markdown
# Only Slide

This is a one-slide presentation.
```
**Result**: Valid (1 slide, within limits)

### Case 2: Exactly 200 Slides
```markdown
# Slide 1
---
# Slide 2
---
...
---
# Slide 200
```
**Result**: Valid (200 slides, at maximum)

### Case 3: 201 Slides
```markdown
# Slide 1
---
...
---
# Slide 201
```
**Result**: Invalid (exceeds maximum)

### Case 4: Markdown with No Separators
```markdown
# Title

This is a single slide with no separators.
Multiple paragraphs are fine.
```
**Result**: Valid (1 slide, no separators needed)

---

## Performance Considerations

### Rendering Time (Target: < 500ms for 50 slides)

| Deck Size | Estimated Render Time | HTML File Size |
|-----------|----------------------|----------------|
| 10 slides | ~100ms | ~500 KB |
| 50 slides | ~500ms | ~2.5 MB |
| 100 slides | ~1000ms (1s) | ~5 MB |
| 200 slides | ~2000ms (2s) | ~10 MB |

**Mitigation for Large Decks**:
- Parallel rendering (Cats Effect `parTraverse`)
- Lazy HTML generation (render slides on-demand in browser - v2.0 feature)
- Slide pagination (split HTML into multiple files - v2.0 feature)

### Memory Usage

**200 slides × 5 KB/slide (average) = 1 MB in memory** (acceptable for JVM)

---

## Future Enhancements (Not v1.0)

**v1.1**: Override Flag
```bash
mdslides massive.md --allow-large-deck
⚠️  Warning: Deck has 250 slides (exceeds recommended limit of 200)
✓ Generated output.html (12 MB)
```

**v1.2**: Deck Splitting Tool
```bash
mdslides split presentation.md --max-slides 50
✓ Created presentation-part1.md (50 slides)
✓ Created presentation-part2.md (50 slides)
✓ Created presentation-part3.md (30 slides)
```

**v2.0**: Multi-Deck Compilation
```bash
mdslides compile part*.md --output full-deck.html
✓ Compiled 3 decks into single HTML (130 slides total)
```

---

## Related Ceremonies

- **US-011**: Structure Validation (Three Amigos + Example Mapping - deck size limits discussed)
- **Event Storming**: "DeckParsed" event includes slide count

---

## Related Governance

- **ADR-002**: Validation Pipeline Architecture (structure validation includes size checks)
- **POL-001**: Ubiquitous Language (use "Slide Deck" not "Presentation" or "Deck")

---

**Decision Owner**: Tony Moores (Product Owner)
**Business Impact**: Low (limits affect edge cases only)
**User Impact**: Low (most presentations well under 200 slides)
**Performance Impact**: High (prevents huge decks from degrading performance)
**Reversibility**: Medium (changing limits is non-breaking, but raises user expectations)
