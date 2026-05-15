# PDR-010: Image Size Limits

**Status:** Accepted
**Date:** 2025-12-22
**Deciders:** Product Team
**Related:** US-006, ADR-012, PDR-009, PDR-008

## Context

When automatically copying images (US-006), we must decide whether to enforce size limits and how to handle large image files.

**Concerns:**
1. **Page Load Time:** Large images slow down presentation viewing
2. **Disk Space:** Excessive image assets consume storage
3. **Network Transfer:** Large presentations harder to share via email/chat
4. **User Autonomy:** Users may have legitimate reasons for large images (high-DPI displays, print quality)

**Questions:**
1. Should we limit individual image file sizes?
2. Should we limit total image payload?
3. Should limits be hard (errors) or soft (warnings)?
4. What are reasonable thresholds?

## Decision

**Use SOFT LIMITS (warnings only, no errors) for image sizes:**

### Individual Image Size
- **Warn** if single image file exceeds **5MB**
- **Do not fail** the build

### Total Image Payload
- **Warn** if total size of all images exceeds **20MB**
- **Do not fail** the build

### Rationale for Warnings (Not Errors)

**Users know their constraints better than we do:**
- Conference presentations: Often need compression (slow wifi)
- Local presentations: Can handle large images (no network)
- Print materials: Require high-resolution images
- Demo videos: May include large screenshots

**Blocking builds is too aggressive.** Trust users, inform them.

## Implementation

### Warning Messages

**Individual Image Warning:**
```
⚠ Large image file: images/photo.jpg (8.2 MB)
  Consider optimizing this image for faster page load.
  Typical presentations use images <5MB.
```

**Total Payload Warning:**
```
⚠ Total image payload is large: 24.5 MB (5 images)
  This may result in slow page load times.
  Consider optimizing or reducing number of images.
```

### No Errors

**Never fail the build due to image size.** Users can always override concerns.

### CLI Flag for Strict Mode (Future)

Reserve option for future strict mode:
```bash
# Future: fail on warnings
mdslides slides.md output.html --strict-size-limits
```

Not implemented in v0.3.0, but design allows it.

## Threshold Justification

### Individual Image: 5MB Threshold

**Analysis:**
- Typical presentation image: 50-500KB (screenshots, diagrams)
- High-quality photo: 1-3MB (compressed JPEG)
- Uncompressed screenshot: 5-10MB (needs optimization)
- Very large image: >10MB (likely needs optimization)

**5MB chosen because:**
- Above typical use (allows high-quality images)
- Below "obviously needs optimization" range
- Aligns with common email attachment limits
- Reasonable for modern networks (5MB = ~5 seconds on slow wifi)

### Total Payload: 20MB Threshold

**Analysis:**
- Typical presentation: 5-10 images, 1-5MB total
- Image-heavy presentation: 20-30 images, 10-20MB total
- Excessive presentation: 50+ images, >50MB total

**20MB chosen because:**
- Allows 4 high-quality images (5MB each)
- Allows 20 typical images (1MB each)
- Below "email attachment too large" errors (usually 25MB)
- Still fits on slow connections (<30 seconds to load)

### Evidence from Similar Tools

**Hugo (static site generator):**
- No hard limits, optimization suggestions only

**Pandoc (document converter):**
- No image size limits

**reveal.js (presentation framework):**
- No built-in limits, community recommends <5MB per image

**Decision:** Follow industry norm (soft guidance, no blocking).

## Consequences

### Positive

1. **User Autonomy:** Users decide based on their needs
2. **No False Positives:** High-DPI presentations don't fail builds
3. **Graceful Degradation:** Warnings inform without blocking
4. **Future-Proof:** Can add strict mode later if needed

### Negative

1. **Users May Ignore Warnings:** Some will ship slow presentations
   - **Mitigation:** Clear, actionable warning messages
   - **Acceptable:** User choice, not our decision

2. **No Hard Safety Net:** Very large files (>100MB) not blocked
   - **Mitigation:** OS/filesystem handles extreme cases
   - **Acceptable:** Rare edge case, user clearly intentional

## Alternative Approaches Considered

### Alternative 1: Hard Limits (Fail Build)

**Approach:** Error if image >5MB or total >20MB

**Pros:**
- Forces optimization
- Prevents slow presentations
- Clear constraints

**Cons:**
- Breaks legitimate use cases (print materials, high-DPI)
- Frustrating for users with good reasons
- Requires workarounds (resize images manually)

**Decision:** Rejected. Too restrictive, poor UX.

### Alternative 2: No Limits or Warnings

**Approach:** Copy all images without any size checking

**Pros:**
- Simplest implementation
- No user friction

**Cons:**
- Users unaware of performance implications
- Missed opportunity to educate
- Slow presentations reflect poorly on tool

**Decision:** Rejected. Warnings help users without blocking them.

### Alternative 3: Automatic Image Optimization

**Approach:** Resize/compress large images automatically

**Pros:**
- Best of both worlds (performance + convenience)
- No user friction

**Cons:**
- Complex implementation (image processing library)
- May degrade quality unintentionally
- Slows build significantly
- Users lose control

**Decision:** Rejected for v0.3.0. Future enhancement possible (`--optimize-images` flag).

### Alternative 4: Different Thresholds

**Approach:** Warn at 10MB (individual), 50MB (total)

**Pros:**
- More permissive, fewer warnings

**Cons:**
- 10MB images are almost certainly unoptimized
- 50MB total is already very slow on typical networks

**Decision:** Rejected. 5MB/20MB thresholds align better with real-world constraints.

## Edge Cases

### Edge Case 1: Data URLs

**Question:** Do data URLs (base64-encoded images) count toward limits?

**Decision:** No, data URLs already in HTML, not copied.
- Warning logic only applies to copied image files
- Data URLs already inline, user made intentional choice

### Edge Case 2: External URLs

**Question:** Do external URLs (https://...) count toward limits?

**Decision:** No, external images not copied, not under our control.
- Cannot check size without fetching (slow, unreliable)
- User responsible for external resource performance

### Edge Case 3: Symbolic Links

**Question:** Do symbolic links count toward limits?

**Decision:** Yes, based on target file size.
- Symlinks resolved during copy
- Warning based on actual file size, not link size

### Edge Case 4: SVG Files

**Question:** Should vector graphics (SVG) have different limits?

**Decision:** No, same limits apply.
- SVG files can be large (complex paths, embedded data)
- Typical SVG: <100KB, warning at 5MB still appropriate

## User Education

### Documentation

Add to README.md:

```markdown
### Image Size Recommendations

For best performance:
- **Individual images:** <5MB (typical: 50-500KB)
- **Total image payload:** <20MB for presentation
- **Optimization tools:** ImageOptim, TinyPNG, SVGO

MDSlides warns if images exceed these thresholds but does not prevent generation.
```

### Warning Messages

Warnings include actionable advice:

```
⚠ Large image: images/photo.jpg (8.2 MB)

  Recommendations:
  - Compress JPEG: jpegoptim --max=85 images/photo.jpg
  - Resize: convert images/photo.jpg -resize 1920x1080> images/photo.jpg
  - Use PNG optimizer: optipng -o7 images/photo.png
  - Convert SVG: svgo images/diagram.svg

  Or use --no-size-warnings to suppress this message.
```

### Future: CLI Flag to Suppress Warnings

Reserve flag for users who know what they're doing:
```bash
mdslides slides.md output.html --no-size-warnings
```

Not implemented in v0.3.0, but design allows it.

## Testing Strategy

### Test Cases

1. **Below Threshold:** 3MB image, no warning
2. **Above Threshold:** 8MB image, warning displayed
3. **Total Payload:** 5 images, 4MB each, warning displayed
4. **Mixed:** 3 small images + 1 large image, only large image warned
5. **Data URLs:** Not counted toward limits
6. **External URLs:** Not counted toward limits

### Success Criteria

- Warnings displayed at appropriate thresholds
- Build never fails due to image size
- Warning messages are clear and actionable
- Users can proceed despite warnings

## Metrics

**Target Metrics:**
- <10% of presentations trigger size warnings
- Of those warned, <5% ignore and ship slow presentations
- User satisfaction: Warnings helpful, not annoying

**Measurement:**
- Telemetry: Count warnings displayed (if telemetry added)
- User feedback: Survey on warning usefulness

## Future Enhancements

### Potential v0.4.0 Features

1. **Automatic Optimization:** `--optimize-images` flag
   - Resize images to reasonable dimensions
   - Compress without visible quality loss
   - Report savings ("Optimized 8.2 MB → 2.1 MB")

2. **Strict Mode:** `--strict-size-limits` flag
   - Fail build if limits exceeded
   - For CI/CD pipelines with hard requirements

3. **Custom Thresholds:** `--max-image-size 10MB` flag
   - User-defined limits
   - Per-project configuration

4. **Size Report:** `--image-report` flag
   - Detailed breakdown of image sizes
   - Suggestions for optimization

## Related Documents

- [ADR-012: Image Asset Copying Strategy](../adr/ADR-012-image-asset-copying-strategy.md)
- [PDR-009: Image Copy Default Behavior](PDR-009-image-copy-default-behavior.md)
- [PDR-008: Image Policy](PDR-008-image-policy.md) (Visual density warnings)
- [US-006: Image Asset Copying](../../ceremonies/v0.3.0.md#us-006-image-asset-copying)

---

**Decision Date:** 2025-12-22
**Review Date:** After v0.3.0 user feedback
