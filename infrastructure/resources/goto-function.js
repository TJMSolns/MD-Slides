/**
 * Goto Function - JavaScript Infrastructure Layer
 *
 * Jump directly to any slide by number via keyboard shortcut.
 *
 * Features:
 * - 'G' key: Show goto prompt
 * - Type slide number (1-indexed for user display)
 * - Press Enter to jump
 * - Input validation (reject invalid numbers, out of range)
 * - Clears forward history (new navigation path)
 *
 * Architecture:
 * - Creates modal overlay when 'G' pressed
 * - Validates user input
 * - Navigates via Reveal.js API
 * - Integrates with NavigationHistory
 *
 * Related Artifacts:
 * - Design Spec: doc/internal/planning/v3.0.0-DESIGN-SPECIFICATIONS.md
 * - Domain Model: domain/src/com/tjmsolutions/mdslides/domain/GotoCommand.scala
 *
 * @version 3.0.0
 * @author TJM Solutions
 */

// ============================================================================
// Goto Function Manager
// ============================================================================

class GotoFunction {
  constructor() {
    this.totalSlides = 0;
    this.currentSlideIndex = 0;
    this.modal = null;
  }

  /**
   * Initialize goto function when presentation loads
   * @param {number} totalSlides Total number of slides
   */
  initialize(totalSlides) {
    this.totalSlides = totalSlides;

    // Get current slide from global variable (custom slide system)
    this.currentSlideIndex = typeof currentSlide !== 'undefined' ? currentSlide : 0;

    // Listen to slide changes via MutationObserver (watches for active class changes)
    const slides = document.querySelectorAll('.slide');
    const observer = new MutationObserver(() => {
      // Find which slide has the 'active' class
      slides.forEach((slide, index) => {
        if (slide.classList.contains('active')) {
          this.currentSlideIndex = index;
        }
      });
    });

    // Observe all slides for class changes
    slides.forEach(slide => {
      observer.observe(slide, { attributes: true, attributeFilter: ['class'] });
    });

    // Set up keyboard handler for 'G' key
    this.setupKeyboardHandler();

    // Create modal (hidden by default)
    this.createModal();

    console.log(`[GotoFunction] Initialized for ${totalSlides} slides`);
  }

  /**
   * Create modal overlay for goto prompt with slide selector
   */
  createModal() {
    // Create modal container
    this.modal = document.createElement('div');
    this.modal.className = 'goto-modal';
    this.modal.setAttribute('role', 'dialog');
    this.modal.setAttribute('aria-modal', 'true');
    this.modal.setAttribute('aria-label', 'Go to slide');
    this.modal.style.cssText = `
      display: none;
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(0, 0, 0, 0.8);
      z-index: 10000;
      justify-content: center;
      align-items: center;
    `;

    // Create modal content
    const modalContent = document.createElement('div');
    modalContent.className = 'goto-modal-content';
    modalContent.style.cssText = `
      background: #fff;
      padding: 30px;
      border-radius: 8px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);
      max-width: 600px;
      max-height: 80vh;
      overflow: auto;
    `;

    // Create heading
    const heading = document.createElement('h2');
    heading.textContent = 'Go to Slide';
    heading.style.cssText = 'margin: 0 0 20px 0; color: #333;';

    // Create slide list container
    const slideList = document.createElement('div');
    slideList.className = 'goto-slide-list';
    slideList.style.cssText = `
      max-height: 400px;
      overflow-y: auto;
      margin-bottom: 15px;
      border: 1px solid #ddd;
      border-radius: 4px;
    `;

    // Build slide list from DOM
    this.buildSlideList(slideList);

    // Create cancel button
    const cancelButton = document.createElement('button');
    cancelButton.textContent = 'Cancel';
    cancelButton.style.cssText = `
      padding: 10px 30px;
      font-size: 16px;
      background: #6c757d;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      display: block;
      margin: 0 auto;
    `;
    cancelButton.onclick = () => this.hideModal();

    // Assemble modal
    modalContent.appendChild(heading);
    modalContent.appendChild(slideList);
    modalContent.appendChild(cancelButton);

    this.modal.appendChild(modalContent);
    document.body.appendChild(this.modal);

    // Handle Escape key
    this.modal.addEventListener('keydown', (event) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        this.hideModal();
      }
    });
  }

  /**
   * Build list of clickable slides with numbers and titles
   */
  buildSlideList(container) {
    const slides = document.querySelectorAll('.slide');

    slides.forEach((slide, index) => {
      // Extract slide title from h1 or h2
      const titleElement = slide.querySelector('h1, h2');
      const titleText = titleElement ? titleElement.textContent.trim() : '(No title)';

      // Create slide list item
      const item = document.createElement('div');
      item.className = 'goto-slide-item';
      item.style.cssText = `
        padding: 12px 15px;
        border-bottom: 1px solid #eee;
        cursor: pointer;
        transition: background 0.2s;
      `;
      item.textContent = `${index + 1}. ${titleText}`;

      // Hover effect
      item.addEventListener('mouseenter', () => {
        item.style.background = '#f0f0f0';
      });
      item.addEventListener('mouseleave', () => {
        item.style.background = index === this.currentSlideIndex ? '#e3f2fd' : 'white';
      });

      // Highlight current slide
      if (index === this.currentSlideIndex) {
        item.style.background = '#e3f2fd';
        item.style.fontWeight = 'bold';
      }

      // Click to navigate
      item.addEventListener('click', () => {
        this.gotoSlide(index);
        this.hideModal();
      });

      container.appendChild(item);
    });
  }

  /**
   * Show the goto modal
   */
  showModal() {
    // Pause timer (AC-6: Timer pauses during goto popup)
    if (typeof timerManager !== 'undefined' && timerManager && timerManager.timer.state === 'Running') {
      timerManager.togglePause();
    }

    this.modal.style.display = 'flex';
    console.log('[GotoFunction] Modal shown');
  }

  /**
   * Hide the goto modal
   */
  hideModal() {
    // Resume timer if it was paused by goto
    if (typeof timerManager !== 'undefined' && timerManager && timerManager.timer.state === 'Paused') {
      timerManager.togglePause();
    }

    this.modal.style.display = 'none';
    console.log('[GotoFunction] Modal hidden');
  }

  /**
   * Navigate to specified slide
   * @param {number} slideIndex Slide index (0-based)
   */
  gotoSlide(slideIndex) {
    // Use the global showSlide function from the custom slide system
    if (typeof showSlide === 'function') {
      showSlide(slideIndex);

      // Update global currentSlide variable
      if (typeof currentSlide !== 'undefined') {
        window.currentSlide = slideIndex;
      }

      console.log(`[GotoFunction] Navigated to slide ${slideIndex} (display: ${slideIndex + 1})`);
    } else {
      console.error('[GotoFunction] showSlide function not available');
    }
  }

  /**
   * Set up keyboard handler for 'G' key
   */
  setupKeyboardHandler() {
    document.addEventListener('keydown', (event) => {
      // 'G' key - Show goto prompt (only if modal not already shown)
      if ((event.key === 'g' || event.key === 'G') && this.modal.style.display === 'none') {
        event.preventDefault();

        // Check if break mode is active (AC-6: Goto disabled during break)
        if (typeof window.MDSlidesBreakMode !== 'undefined' && window.MDSlidesBreakMode && window.MDSlidesBreakMode.isBreakModeActive()) {
          // Show error message
          alert('Goto is disabled during break mode');
          console.log('[GotoFunction] Blocked: Break mode is active');
          return;
        }

        this.showModal();
      }

      // Escape key - Hide modal
      if (event.key === 'Escape' && this.modal.style.display !== 'none') {
        event.preventDefault();
        this.hideModal();
      }
    });

    console.log('[GotoFunction] Keyboard handlers registered (G, Escape)');
  }
}

// ============================================================================
// Auto-initialize when DOM ready
// ============================================================================

let gotoFunction = null;

if (typeof document !== 'undefined') {
  document.addEventListener('DOMContentLoaded', () => {
    // Get total slides from global variable or count slides
    const totalSlides = typeof window.totalSlides !== 'undefined' ? window.totalSlides : document.querySelectorAll('.slide').length;
    gotoFunction = new GotoFunction();
    gotoFunction.initialize(totalSlides);
  });
}

// Export for testing
if (typeof module !== 'undefined' && module.exports) {
  module.exports = { GotoFunction };
}
