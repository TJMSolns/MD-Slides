/**
 * Header/Footer Placeholder Updater - JavaScript Infrastructure Layer
 *
 * Updates dynamic placeholders in per-slide header/footer elements.
 *
 * Placeholders (AC-2, AC-3, AC-5, AC-6, AC-7):
 * - {{title}}: Presentation title (resolved server-side)
 * - {{pageNumber}}: Current slide number (1-indexed, updated client-side)
 * - {{totalPages}}: Total number of slides (static, resolved server-side)
 * - {{timer}}: Elapsed presentation time (hh:mm:ss, updated client-side)
 *
 * Architecture (v3.0.0 redesign):
 * - Each slide has its own .slide-header and .slide-footer elements
 * - Headers/footers are part of the slide, not floating overlays
 * - Reads data-template attribute from each header/footer element
 * - Updates placeholders in all slides (each with its own page number)
 * - Integrates with PresentationTimer for elapsed time
 *
 * Related Artifacts:
 * - AC: doc/acceptance-criteria/header-footer-acceptance-criteria.md
 * - Design Spec: doc/internal/planning/v3.0.0-DESIGN-SPECIFICATIONS.md
 * - Domain Model: domain/src/com/tjmsolutions/mdslides/domain/HeaderFooter.scala
 *
 * @version 3.0.0
 * @author TJM Solutions
 */

class HeaderFooterUpdater {
  constructor() {
    this.totalSlides = 0;
    this.slides = [];
  }

  /**
   * Initialize header/footer updater when presentation loads
   */
  initialize() {
    // Get all slides
    this.slides = Array.from(document.querySelectorAll('.slide'));
    this.totalSlides = this.slides.length;

    // Update all slides immediately
    this.updateAll();

    // Update every second for elapsed time
    setInterval(() => this.updateAll(), 1000);

    console.log('[HeaderFooterUpdater] Initialized - per-slide headers/footers');
  }

  /**
   * Update all slide headers/footers with current values
   */
  updateAll() {
    const elapsedTime = this.getElapsedTime();

    this.slides.forEach((slide, index) => {
      // Update header if present
      const header = slide.querySelector('.slide-header');
      if (header) {
        const template = header.getAttribute('data-template');
        if (template) {
          header.innerHTML = this.resolvePlaceholders(
            template,
            index,
            this.totalSlides,
            elapsedTime
          );
        }
      }

      // Update footer if present
      const footer = slide.querySelector('.slide-footer');
      if (footer) {
        const template = footer.getAttribute('data-template');
        if (template) {
          footer.innerHTML = this.resolvePlaceholders(
            template,
            index,
            this.totalSlides,
            elapsedTime
          );
        }
      }
    });
  }

  /**
   * Resolve dynamic placeholders in template
   *
   * AC-2: Static placeholder resolution (render time)
   * AC-3: Dynamic placeholder resolution (runtime)
   * AC-5: Page number placeholder (1-indexed)
   * AC-6: Total pages placeholder (static)
   *
   * @param {string} template Template string with placeholders
   * @param {number} slideIndex Slide index (0-based)
   * @param {number} totalSlides Total number of slides
   * @param {string} elapsedTime Formatted elapsed time
   * @returns {string} Template with resolved placeholders
   */
  resolvePlaceholders(template, slideIndex, totalSlides, elapsedTime) {
    let result = template;

    // AC-5: Page number placeholder (1-indexed for user display)
    result = result.replace(/\{\{pageNumber\}\}/g, (slideIndex + 1).toString());

    // AC-6: Total pages placeholder (static for all slides)
    result = result.replace(/\{\{totalPages\}\}/g, totalSlides.toString());

    // AC-3, AC-4: Timer placeholder (dynamic, updated every second)
    result = result.replace(/\{\{timer\}\}/g, elapsedTime);

    // AC-7: Title placeholder (static, resolved server-side but may update if needed)
    // {{title}} is already resolved server-side in HeaderFooter.resolvePlaceholders()

    return result;
  }

  /**
   * Get elapsed time from PresentationTimer if available
   *
   * @returns {string} Formatted time or "00:00:00" if timer not available
   */
  getElapsedTime() {
    // Try to get time from global timerManager if it exists
    if (typeof timerManager !== 'undefined' && timerManager) {
      return timerManager.timer.formattedTime();
    }

    // Fallback: return 00:00:00
    return '00:00:00';
  }
}

// ============================================================================
// Auto-initialize when DOM ready
// ============================================================================

let headerFooterUpdater = null;

if (typeof document !== 'undefined') {
  document.addEventListener('DOMContentLoaded', () => {
    headerFooterUpdater = new HeaderFooterUpdater();
    headerFooterUpdater.initialize();
  });
}

// Export for testing
if (typeof module !== 'undefined' && module.exports) {
  module.exports = { HeaderFooterUpdater };
}
