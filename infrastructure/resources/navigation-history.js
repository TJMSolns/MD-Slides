/**
 * Navigation History - JavaScript Infrastructure Layer
 *
 * Two-stack navigation system for Previous/Next history.
 *
 * Features:
 * - 'P' key: Pop from visit stack (LIFO) and navigate to previous slide
 * - 'N' key: Pop from forward stack (redo) or advance linearly
 * - Visit stack: Stores previously visited slides (LIFO)
 * - Forward stack: Stores forward navigation (cleared on new navigation)
 *
 * Architecture:
 * - TWO separate stacks (not browser-style single array with pointer)
 * - visitStack: Pushed on each navigation (except P/N)
 * - forwardStack: P pushes current to forward, N pops from forward
 * - Forward stack cleared on non-P/N navigation
 *
 * Related Artifacts:
 * - AC: doc/acceptance-criteria/navigation-history-acceptance-criteria.md
 * - Design Spec: doc/internal/planning/v3.0.0-DESIGN-SPECIFICATIONS.md
 * - Domain Model: domain/src/com/tjmsolutions/mdslides/domain/NavigationHistory.scala
 *
 * @version 3.0.0
 * @author TJM Solutions
 */

// ============================================================================
// Navigation History Manager (Two-Stack Architecture)
// ============================================================================

class NavigationHistoryManager {
  constructor() {
    this.visitStack = []; // AC-5: Stores previously visited slides (LIFO)
    this.forwardStack = []; // AC-6: Stores forward navigation (redo capability)
    this.currentSlide = 0; // Current slide index (0-based)
    this.totalSlides = 0;
    this.isNavigatingProgrammatically = false; // Flag to prevent recording P/N navigations
  }

  /**
   * Initialize navigation history when presentation loads
   * @param {number} totalSlides Total number of slides
   */
  initialize(totalSlides) {
    this.totalSlides = totalSlides;
    this.currentSlide = 0; // Start at slide 0

    // Listen to slide changes via MutationObserver
    this.setupSlideChangeTracking();

    // Set up keyboard handlers for P and N keys
    this.setupKeyboardHandlers();

    console.log(`[NavigationHistory] Initialized with two-stack architecture for ${totalSlides} slides`);
  }

  /**
   * Set up slide change tracking via MutationObserver
   *
   * Records navigation to visitStack, clears forwardStack (AC-7).
   * Does NOT record P/N key navigations (those manipulate the stacks directly).
   */
  setupSlideChangeTracking() {
    const slides = document.querySelectorAll('.slide');
    let lastActiveIndex = 0;

    const observer = new MutationObserver(() => {
      slides.forEach((slide, index) => {
        if (slide.classList.contains('active') && index !== lastActiveIndex) {
          lastActiveIndex = index;

          // Only record if NOT navigating programmatically via P/N keys
          if (!this.isNavigatingProgrammatically) {
            this.recordNormalNavigation(index);
          } else {
            // Just update current slide, don't modify stacks
            this.currentSlide = index;
          }
        }
      });
    });

    slides.forEach(slide => {
      observer.observe(slide, { attributes: true, attributeFilter: ['class'] });
    });
  }

  /**
   * Record normal navigation (arrow keys, space, goto, etc.)
   *
   * AC-5: Push current slide to visit stack
   * AC-7: Clear forward stack on new navigation
   *
   * @param {number} newSlideIndex New slide index (0-based)
   */
  recordNormalNavigation(newSlideIndex) {
    // AC-5: Push current slide to visit stack (before navigating away)
    this.visitStack.push(this.currentSlide);

    // AC-7: Clear forward stack (new navigation path)
    this.forwardStack = [];

    // Update current slide
    this.currentSlide = newSlideIndex;

    console.log(`[NavigationHistory] Normal navigation to slide ${newSlideIndex}, visitStack: [${this.visitStack}], forwardStack: [${this.forwardStack}]`);
  }

  /**
   * Navigate to previous slide in history (P key)
   *
   * AC-1: Pop from visit stack (LIFO)
   * AC-6: Push current slide to forward stack
   * AC-2: If visit stack empty, fall back to slide 0
   */
  navigatePrevious() {
    if (this.visitStack.length > 0) {
      // AC-6: Push current slide to forward stack
      this.forwardStack.push(this.currentSlide);

      // AC-1: Pop from visit stack (LIFO)
      const previousSlide = this.visitStack.pop();

      // Navigate to previous slide without recording
      this.navigateToSlideWithoutRecording(previousSlide);

      console.log(`[NavigationHistory] P key: Navigated to slide ${previousSlide}, visitStack: [${this.visitStack}], forwardStack: [${this.forwardStack}]`);
    } else {
      // AC-2: Empty history fallback - navigate to slide 0
      if (this.currentSlide !== 0) {
        this.forwardStack.push(this.currentSlide);
        this.navigateToSlideWithoutRecording(0);
        console.log(`[NavigationHistory] P key: Empty visit stack, navigated to slide 0 (fallback)`);
      } else {
        console.log('[NavigationHistory] P key: Already at slide 0, no action taken');
      }
    }
  }

  /**
   * Navigate to next slide in history (N key)
   *
   * AC-3: If forward stack not empty, pop from forward stack (redo)
   * AC-3: If forward stack empty, advance linearly (currentSlide + 1)
   */
  navigateNext() {
    if (this.forwardStack.length > 0) {
      // AC-3: Redo - pop from forward stack
      this.visitStack.push(this.currentSlide);
      const nextSlide = this.forwardStack.pop();
      this.navigateToSlideWithoutRecording(nextSlide);
      console.log(`[NavigationHistory] N key (redo): Navigated to slide ${nextSlide}, visitStack: [${this.visitStack}], forwardStack: [${this.forwardStack}]`);
    } else {
      // AC-3: Linear next - advance forward
      if (this.currentSlide < this.totalSlides - 1) {
        const nextSlide = this.currentSlide + 1;
        this.visitStack.push(this.currentSlide);
        this.navigateToSlideWithoutRecording(nextSlide);
        console.log(`[NavigationHistory] N key (linear): Navigated to slide ${nextSlide}, visitStack: [${this.visitStack}], forwardStack: [${this.forwardStack}]`);
      } else {
        console.log('[NavigationHistory] N key: Already at last slide, no action taken');
      }
    }
  }

  /**
   * Navigate to slide without recording to history stacks
   *
   * Used by P/N keys to prevent infinite recursion.
   * Sets flag to prevent MutationObserver from recording navigation.
   *
   * @param {number} slideIndex Slide index (0-based)
   */
  navigateToSlideWithoutRecording(slideIndex) {
    this.isNavigatingProgrammatically = true;

    if (typeof showSlide === 'function') {
      showSlide(slideIndex);

      // Update global currentSlide variable
      if (typeof currentSlide !== 'undefined') {
        window.currentSlide = slideIndex;
      }

      // Update internal state
      this.currentSlide = slideIndex;
    } else {
      console.error('[NavigationHistory] showSlide function not available');
    }

    // Reset flag after brief delay to allow MutationObserver to process
    setTimeout(() => {
      this.isNavigatingProgrammatically = false;
    }, 50);
  }

  /**
   * Set up keyboard handlers for P and N keys
   */
  setupKeyboardHandlers() {
    document.addEventListener('keydown', (event) => {
      // P key - Navigate to previous slide in history
      if (event.key === 'p' || event.key === 'P') {
        // AC-10: Check if break mode or goto popup is active
        if (this.isBreakModeActive() || this.isGotoPopupActive()) {
          console.log('[NavigationHistory] P key blocked: Break mode or goto popup active');
          return;
        }

        event.preventDefault();
        this.navigatePrevious();
      }

      // N key - Navigate to next slide in history
      else if (event.key === 'n' || event.key === 'N') {
        // AC-10: Check if break mode or goto popup is active
        if (this.isBreakModeActive() || this.isGotoPopupActive()) {
          console.log('[NavigationHistory] N key blocked: Break mode or goto popup active');
          return;
        }

        event.preventDefault();
        this.navigateNext();
      }
    });

    console.log('[NavigationHistory] Keyboard handlers registered (P, N)');
  }

  /**
   * Check if break mode is active
   * @returns {boolean} True if break mode active
   */
  isBreakModeActive() {
    return typeof window.MDSlidesBreakMode !== 'undefined' &&
           window.MDSlidesBreakMode &&
           window.MDSlidesBreakMode.isBreakModeActive();
  }

  /**
   * Check if goto popup is active
   * @returns {boolean} True if goto popup visible
   */
  isGotoPopupActive() {
    const gotoModal = document.querySelector('.goto-modal');
    return gotoModal && gotoModal.style.display !== 'none';
  }

  /**
   * Get current state (for debugging/testing)
   * @returns {object} Current state
   */
  getState() {
    return {
      currentSlide: this.currentSlide,
      visitStack: [...this.visitStack],
      forwardStack: [...this.forwardStack],
      totalSlides: this.totalSlides
    };
  }
}

// ============================================================================
// Auto-initialize when DOM ready
// ============================================================================

let navigationHistoryManager = null;

if (typeof document !== 'undefined') {
  document.addEventListener('DOMContentLoaded', () => {
    const totalSlides = typeof window.totalSlides !== 'undefined' ? window.totalSlides : document.querySelectorAll('.slide').length;
    navigationHistoryManager = new NavigationHistoryManager();
    navigationHistoryManager.initialize(totalSlides);

    // Expose globally for debugging
    window.navigationHistoryManager = navigationHistoryManager;
  });
}

// Export for testing
if (typeof module !== 'undefined' && module.exports) {
  module.exports = { NavigationHistoryManager };
}
