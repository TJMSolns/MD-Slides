# ADR-012: Image Asset Copying Strategy

**Status:** Accepted
**Date:** 2025-12-22
**Deciders:** Development Team
**Related:** US-006, PDR-009, PDR-010

## Context

In v0.2.0, we added image embedding support (US-005), allowing users to reference images in their presentations using markdown syntax `![alt](path)`. However, the current implementation has several pain points:

1. **Manual Image Management:** Users must manually copy image files alongside the generated HTML
2. **Broken Images:** Easy to forget images when distributing presentations, resulting in broken `<img>` tags
3. **Non-Portable:** Presentations are not self-contained; distribution requires separate handling of HTML and image files
4. **Error-Prone:** No validation that referenced images actually exist until viewing in browser

**Current Workflow (v0.2.0):**
```bash
mdslides slides.md presentation.html
# User must remember to:
cp -r images/ output-directory/
# Or risk broken images
```

**User Pain Points:**
- "I shared my presentation but forgot the images folder"
- "Images work on my machine but not when I send the HTML"
- "I have to zip the directory to share it"

## Decision

We will **automatically copy referenced local image files to the output directory** when generating presentations.

### Key Design Choices

1. **Default Behavior:** Image copying is **enabled by default**
   - User gets portable presentations without extra steps
   - Can opt-out with `--no-copy-images` flag

2. **Path Detection:** Distinguish image types and copy only local files
   - **Copy:** Relative paths (`images/logo.png`), absolute local paths (`/home/user/logo.png`)
   - **Skip:** External URLs (`https://...`), data URLs (`data:image/...`)

3. **Directory Structure Preservation:** Maintain relative paths when copying
   - Input: `![Logo](images/icons/logo.svg)`
   - Output: `output-dir/images/icons/logo.svg` (structure preserved)

4. **Validation:** Fail fast if referenced images don't exist
   - Error during generation (not at viewing time)
   - Clear error messages pointing to missing files

5. **CLI Flags:**
   ```bash
   # Default: copy images
   mdslides slides.md output.html

   # Explicit copy
   mdslides slides.md output.html --copy-images

   # Disable copying (legacy behavior)
   mdslides slides.md output.html --no-copy-images

   # Custom output directory for images
   mdslides slides.md output.html --images-dir assets/images
   ```

### Architecture

**New Infrastructure Component:** `ImageAssetCopier`

```scala
package com.tjmsolutions.mdslides.infrastructure.assets

object ImageAssetCopier:
  def copyImages(
    images: List[String],
    sourceDir: Path,
    outputDir: Path
  ): IO[Either[String, List[CopiedImage]]]
```

**Responsibilities:**
- Detect local vs external URLs
- Resolve relative paths based on source markdown location
- Validate image files exist and are readable
- Copy files preserving directory structure
- Report errors clearly

**Integration Point:** CLI layer (`Main.scala`)
- Extract images from parsed slides
- Call `ImageAssetCopier.copyImages()` before rendering
- Update HTMLRenderer with rewritten paths

## Consequences

### Positive

1. **Better User Experience:** Presentations "just work" when shared
   - No manual file management
   - Self-contained output directory
   - Fewer support requests

2. **Early Error Detection:** Missing images caught during generation
   - Clear error messages
   - Prevents broken presentations

3. **Portability:** Output directory contains everything needed
   - Easy to distribute (single folder)
   - Works offline (images copied locally)

4. **Backward Compatible:** No breaking changes
   - New default is helpful, not disruptive
   - Legacy behavior available with `--no-copy-images`

### Negative

1. **Build Time:** File I/O adds latency
   - **Mitigation:** Only copy changed images (future optimization)
   - **Acceptable:** Most presentations have <10 images

2. **Disk Space:** Images duplicated in output directory
   - **Mitigation:** Only output directory affected (source unchanged)
   - **Acceptable:** Disk space cheap, portability valuable

3. **Complexity:** More CLI options and logic
   - **Mitigation:** Well-tested, clear error messages
   - **Acceptable:** Complexity hidden from most users

### Neutral

1. **External URLs Not Copied:** Users must ensure internet connectivity
   - Expected behavior (can't copy remote files)
   - Documented clearly

2. **Symlinks Resolved:** Symlinked images copied as regular files
   - Prevents broken links when moving presentation
   - Acceptable trade-off for portability

## Alternatives Considered

### Alternative 1: Embed All Images as Data URLs

**Approach:** Convert all images to base64 data URLs and inline in HTML

**Pros:**
- True single-file output
- No broken paths ever

**Cons:**
- Large HTML files (base64 = +33% size)
- No browser caching
- Harder to replace/edit images
- Slower page load

**Decision:** Rejected. File copying better balances portability and performance.

### Alternative 2: Create Symbolic Links

**Approach:** Create symlinks in output directory pointing to source images

**Pros:**
- No disk space duplication
- Images stay in sync with source

**Cons:**
- Breaks when moving presentation directory
- Doesn't work on Windows without admin privileges
- Not portable

**Decision:** Rejected. Portability is primary goal.

### Alternative 3: Package as ZIP Archive

**Approach:** Output a `.zip` file containing HTML + images

**Pros:**
- Single file distribution
- Preserves directory structure

**Cons:**
- Requires extraction before viewing
- More complex user workflow
- Harder to inspect/edit

**Decision:** Deferred. Could be future enhancement (`--output-zip` flag), but copying is better default.

### Alternative 4: Document Manual Process

**Approach:** Keep current behavior, improve documentation

**Pros:**
- No code changes
- Simple implementation

**Cons:**
- Doesn't solve user pain points
- Still error-prone
- Poor user experience

**Decision:** Rejected. Problem too common to ignore.

## Implementation Notes

### Cross-Platform Compatibility

Use `java.nio.file.Path` and `Files.copy()` for platform-independent file operations:

```scala
import java.nio.file.{Files, Path, StandardCopyOption}

Files.copy(
  sourcePath,
  destPath,
  StandardCopyOption.REPLACE_EXISTING,
  StandardCopyOption.COPY_ATTRIBUTES
)
```

### Error Handling

Comprehensive error messages:

```
✗ Image file not found: images/logo.png
  Referenced in: slides.md (slide 3)
  Expected location: /home/user/project/images/logo.png

✗ Cannot read image file: images/diagram.svg
  Reason: Permission denied

✗ Cannot copy image: images/photo.jpg
  Reason: No space left on device
```

### Progress Reporting

For presentations with many images:

```
Copying images...
✓ Copied images/logo.svg (4.2 KB)
✓ Copied images/diagram.png (128 KB)
✓ Copied screenshots/demo.jpg (512 KB)
✓ Copied 3 images (644.2 KB total)
```

## Validation

### Acceptance Criteria

1. ✅ Local images automatically copied to output directory
2. ✅ Directory structure preserved (relative paths maintained)
3. ✅ External URLs and data URLs skipped (not copied)
4. ✅ Clear error if referenced image doesn't exist
5. ✅ CLI flags control behavior (`--copy-images`, `--no-copy-images`, `--images-dir`)
6. ✅ Path rewriting in HTML to match copied locations

### Test Coverage

- Unit tests: Path detection, resolution, copying logic
- Integration tests: End-to-end copy workflow, error handling
- Property-based tests: Path resolution invariants

**Target:** 35+ new tests

## Related Documents

- [US-006: Image Asset Copying](../../ceremonies/v0.3.0.md#us-006-image-asset-copying)
- [PDR-009: Image Copy Default Behavior](../pdr/PDR-009-image-copy-default-behavior.md)
- [PDR-010: Image Size Limits](../pdr/PDR-010-image-size-limits.md)
- [PDR-008: Image Policy](../pdr/PDR-008-image-policy.md) (v0.2.0)
- [US-005: Image Embedding](../../ceremonies/v0.2.0.md#us-005-image-embedding) (v0.2.0)

## References

- [java.nio.file.Files Documentation](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Files.html)
- [Cross-Platform Path Handling Best Practices](https://docs.oracle.com/javase/tutorial/essential/io/pathOps.html)

---

**Decision Date:** 2025-12-22
**Next Review:** After v0.3.0 implementation
