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
    this.modal = document.createElement('div');
    this.modal.className = 'goto-modal';
    this.modal.setAttribute('role', 'dialog');
    this.modal.setAttribute('aria-modal', 'true');
    this.modal.setAttribute('aria-labelledby', 'goto-heading');
    this.modal.style.cssText = 'display:none;position:fixed;inset:0;background:rgba(0,0,0,0.8);z-index:10000;' +
      'justify-content:center;align-items:center;padding:1rem;box-sizing:border-box;';

    const content = document.createElement('div');
    content.className = 'goto-modal-content';
    content.style.cssText = 'background:#fff;color:#1f1f20;padding:1.5rem;border-radius:8px;box-shadow:0 4px 20px rgba(0,0,0,0.5);' +
      'width:min(600px,100%);max-height:85vh;display:flex;flex-direction:column;gap:0.75rem;font:16px/1.4 system-ui,sans-serif;';

    const heading = document.createElement('h2');
    heading.id = 'goto-heading';
    heading.textContent = 'Go to slide';
    heading.style.cssText = 'margin:0;font-size:1.25rem;color:#1f1f20;';

    // Number entry: type a slide number and press Enter
    const form = document.createElement('form');
    form.style.cssText = 'display:flex;gap:0.5rem;align-items:center;flex-wrap:wrap;';
    const label = document.createElement('label');
    label.htmlFor = 'goto-number';
    label.textContent = `Slide number (1–${this.totalSlides})`;
    this.input = document.createElement('input');
    this.input.id = 'goto-number';
    this.input.type = 'number';
    this.input.inputMode = 'numeric';
    this.input.min = '1';
    this.input.max = String(this.totalSlides);
    this.input.style.cssText = 'width:6rem;font-size:1rem;padding:0.4rem;border:1px solid #666;border-radius:4px;';
    const go = document.createElement('button');
    go.type = 'submit';
    go.textContent = 'Go';
    go.style.cssText = 'padding:0.45rem 1.2rem;font-size:1rem;background:#1f1f20;color:#fff;border:0;border-radius:4px;cursor:pointer;';
    this.error = document.createElement('span');
    this.error.setAttribute('role', 'alert');
    this.error.style.cssText = 'color:#a00000;font-size:0.9rem;';
    form.append(label, this.input, go, this.error);
    form.addEventListener('submit', (event) => {
      event.preventDefault();
      const n = parseInt(this.input.value, 10);
      if (Number.isInteger(n) && n >= 1 && n <= this.totalSlides) {
        this.hideModal();
        this.gotoSlide(n - 1);
      } else {
        this.error.textContent = `Enter a number from 1 to ${this.totalSlides}.`;
      }
    });

    // Or pick from the list (buttons: keyboard and touch accessible)
    const list = document.createElement('div');
    list.className = 'goto-slide-list';
    list.setAttribute('role', 'list');
    list.style.cssText = 'overflow-y:auto;border:1px solid #ccc;border-radius:4px;min-height:0;flex:1;';
    this.buildSlideList(list);

    const cancel = document.createElement('button');
    cancel.type = 'button';
    cancel.textContent = 'Cancel';
    cancel.style.cssText = 'align-self:center;padding:0.5rem 1.8rem;font-size:1rem;background:#595959;color:#fff;border:0;border-radius:4px;cursor:pointer;';
    cancel.addEventListener('click', () => this.hideModal());

    content.append(heading, form, list, cancel);
    this.modal.appendChild(content);
    document.body.appendChild(this.modal);

    // Escape closes; clicking the backdrop closes
    this.modal.addEventListener('keydown', (event) => {
      if (event.key === 'Escape') { event.preventDefault(); this.hideModal(); }
    });
    this.modal.addEventListener('click', (event) => { if (event.target === this.modal) this.hideModal(); });
  }

  buildSlideList(container) {
    this.items = [];
    document.querySelectorAll('.slide').forEach((slide, index) => {
      const titleElement = slide.querySelector('h1, h2, h3');
      const titleText = titleElement ? titleElement.textContent.trim() : '(No title)';
      const item = document.createElement('button');
      item.type = 'button';
      item.className = 'goto-slide-item';
      item.setAttribute('role', 'listitem');
      item.textContent = `${index + 1}. ${titleText}`;
      item.style.cssText = 'display:block;width:100%;text-align:left;padding:0.7rem 0.9rem;border:0;border-bottom:1px solid #eee;' +
        'background:#fff;color:#1f1f20;font:inherit;cursor:pointer;';
      item.addEventListener('click', () => {
        this.hideModal();
        this.gotoSlide(index);
      });
      container.appendChild(item);
      this.items.push(item);
    });
  }

  showModal() {
    // Pause timer while choosing (AC-6), resume on close
    this.pausedTimer = false;
    if (typeof timerManager !== 'undefined' && timerManager && timerManager.timer.state === 'Running') {
      timerManager.togglePause();
      this.pausedTimer = true;
    }
    this.items.forEach((item, i) => {
      const current = i === this.currentSlideIndex;
      item.style.background = current ? '#e3f2fd' : '#fff';
      item.style.fontWeight = current ? 'bold' : 'normal';
      if (current) item.setAttribute('aria-current', 'true'); else item.removeAttribute('aria-current');
    });
    this.returnFocus = document.activeElement;
    this.error.textContent = '';
    this.input.value = '';
    this.modal.style.display = 'flex';
    if (this.items[this.currentSlideIndex]) this.items[this.currentSlideIndex].scrollIntoView({ block: 'nearest' });
    this.input.focus();
  }

  hideModal() {
    if (this.pausedTimer && typeof timerManager !== 'undefined' && timerManager && timerManager.timer.state === 'Paused') {
      timerManager.togglePause();
    }
    this.pausedTimer = false;
    this.modal.style.display = 'none';
    if (this.returnFocus && this.returnFocus.focus) this.returnFocus.focus();
  }

  /**
   * Navigate to specified slide
   * @param {number} slideIndex Slide index (0-based)
   */
  gotoSlide(slideIndex) {
    // Use the global showSlide function from the custom slide system
    if (typeof showSlide === 'function') {
      showSlide(slideIndex, 'goto');

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
  /** Open goto, unless break mode is on (AC-6). */
  open() {
    if (window.MDSlidesBreakMode && window.MDSlidesBreakMode.isBreakModeActive()) return;
    this.showModal();
  }

  setupKeyboardHandler() {
    document.addEventListener('keydown', (event) => {
      if ((event.key === 'g' || event.key === 'G') && this.modal.style.display === 'none') {
        if (event.ctrlKey || event.metaKey || event.altKey) return;
        if (event.target && event.target.closest && event.target.closest('input, textarea, select, [contenteditable]')) return;
        event.preventDefault();
        this.open();
      }
    });
    // The slide counter opens goto too (touch screens have no G key)
    const counter = document.getElementById('slide-counter');
    if (counter) counter.addEventListener('click', () => this.open());
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
