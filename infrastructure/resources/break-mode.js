/**
 * Break Mode - JavaScript Infrastructure Layer
 *
 * Allows presenter to take a break without showing slide content to the audience.
 *
 * Features:
 * - B key toggles break mode on/off
 * - Pauses presentation timer during break
 * - Displays break screen (configurable image or black screen)
 * - Main view shows break screen
 * - Speaker view shows current slide + "BREAK MODE ACTIVE" indicator
 * - Navigation works during break (speaker view only)
 * - Goto (G key) disabled during break
 * - Cross-window sync via BroadcastChannel
 *
 * Architecture:
 * - Integrates with PresentationTimer for pause/resume
 * - Integrates with Goto function for disable during break
 * - Uses BroadcastChannel for cross-window sync
 * - Break screen path injected from server-side config
 *
 * Related Artifacts:
 * - Design Spec: doc/internal/planning/v3.0.0-DESIGN-SPECIFICATIONS.md (Section 2)
 * - Domain Model: domain/src/com/tjmsolutions/mdslides/domain/BreakMode.scala
 * - Acceptance Criteria: doc/acceptance-criteria/break-mode-acceptance-criteria.md
 *
 * @version 3.0.0
 * @author TJM Solutions
 */

// ============================================================================
// Break Mode Manager
// ============================================================================

class BreakModeManager {
  constructor() {
    this.isActive = false;
    this.breakScreenPath = null;
    this.breakOverlay = null;
    this.isSpeakerView = false;
    this.channel = null;
    this.breakCount = 0; // AC-5: Track number of break sessions
  }

  /**
   * Initialize break mode when presentation loads
   * @param {object} config Configuration with breakScreenPath
   */
  initialize(config) {
    this.breakScreenPath = config.breakScreenPath || null;
    this.isSpeakerView = window.location.search.includes('speaker=true');

    // Set up keyboard listener for B key
    document.addEventListener('keydown', (event) => {
      if (event.key === 'b' || event.key === 'B') {
        this.toggle();
      }
    });

    // Set up cross-window sync
    if (typeof BroadcastChannel !== 'undefined') {
      this.channel = new BroadcastChannel('mdslides-sync');
      this.channel.addEventListener('message', (event) => {
        if (event.data.type === 'break-mode-changed') {
          if (event.data.isActive && !this.isActive) {
            this.activate(false); // Don't broadcast back
          } else if (!event.data.isActive && this.isActive) {
            this.deactivate(false); // Don't broadcast back
          }
        }
      });
    }

    console.log('[BreakMode] Initialized, breakScreenPath:', this.breakScreenPath);
  }

  /**
   * Toggle break mode on/off
   */
  toggle() {
    if (this.isActive) {
      this.deactivate(true);
    } else {
      this.activate(true);
    }
  }

  /**
   * Activate break mode
   * @param {boolean} broadcast Whether to broadcast to other windows
   */
  activate(broadcast = true) {
    if (this.isActive) {
      console.warn('[BreakMode] Already active');
      return;
    }

    console.log('[BreakMode] activate() called, isSpeakerView:', this.isSpeakerView);

    this.isActive = true;
    this.breakCount++; // AC-5: Increment break count for each session

    // Pause timer (reassign because timer is immutable)
    if (typeof timerManager !== 'undefined' && timerManager) {
      console.log('[BreakMode] Pausing timer');
      timerManager.timer = timerManager.timer.pause();
    }

    // Show break screen (main view only)
    if (!this.isSpeakerView) {
      console.log('[BreakMode] Main view - calling showBreakScreen()');
      this.showBreakScreen();
    } else {
      // Speaker view: show indicator
      console.log('[BreakMode] Speaker view - calling showSpeakerIndicator()');
      this.showSpeakerIndicator();
    }

    // Dispatch custom event for history logging
    document.dispatchEvent(new CustomEvent('mdslides:breakmode:activate'));

    // Broadcast to other windows
    if (broadcast && this.channel) {
      this.channel.postMessage({
        type: 'break-mode-changed',
        isActive: true,
        timestamp: Date.now()
      });
    }

    console.log('[BreakMode] Activated');
  }

  /**
   * Deactivate break mode
   * @param {boolean} broadcast Whether to broadcast to other windows
   */
  deactivate(broadcast = true) {
    if (!this.isActive) {
      console.warn('[BreakMode] Not active');
      return;
    }

    console.log('[BreakMode] deactivate() called, isSpeakerView:', this.isSpeakerView);

    this.isActive = false;

    // Resume timer (reassign because timer is immutable)
    if (typeof timerManager !== 'undefined' && timerManager) {
      console.log('[BreakMode] Resuming timer');
      timerManager.timer = timerManager.timer.resume();
    }

    // Hide break screen (main view only)
    if (!this.isSpeakerView) {
      console.log('[BreakMode] Main view - calling hideBreakScreen()');
      this.hideBreakScreen();
    } else {
      // Speaker view: hide indicator
      console.log('[BreakMode] Speaker view - calling hideSpeakerIndicator()');
      this.hideSpeakerIndicator();
    }

    // Dispatch custom event for history logging
    document.dispatchEvent(new CustomEvent('mdslides:breakmode:deactivate'));

    // Broadcast to other windows
    if (broadcast && this.channel) {
      this.channel.postMessage({
        type: 'break-mode-changed',
        isActive: false,
        timestamp: Date.now()
      });
    }

    console.log('[BreakMode] Deactivated');
  }

  /**
   * Show break screen overlay (main view)
   */
  showBreakScreen() {
    console.log('[BreakMode] showBreakScreen() called');

    // Create overlay if it doesn't exist
    if (!this.breakOverlay) {
      console.log('[BreakMode] Creating new overlay element');
      this.breakOverlay = document.createElement('div');
      this.breakOverlay.id = 'break-mode-overlay';
      this.breakOverlay.setAttribute('aria-label', 'Break mode active');
      this.breakOverlay.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        z-index: 10000;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #000000;
      `;

      // Add break screen image if configured
      if (this.breakScreenPath) {
        const img = document.createElement('img');
        img.src = this.breakScreenPath;
        img.style.cssText = `
          max-width: 100%;
          max-height: 100%;
          object-fit: contain;
        `;
        img.onerror = () => {
          console.warn(`[BreakMode] Break screen file not found: ${this.breakScreenPath}, using default`);
          // Fallback to black screen (already set)
        };
        this.breakOverlay.appendChild(img);
      }

      document.body.appendChild(this.breakOverlay);
      console.log('[BreakMode] Overlay appended to body, element:', this.breakOverlay);
    } else {
      console.log('[BreakMode] Showing existing overlay');
      this.breakOverlay.style.display = 'flex';
    }

    // Announce to screen readers
    this.announceToScreenReader('Break mode activated');
  }

  /**
   * Hide break screen overlay (main view)
   */
  hideBreakScreen() {
    console.log('[BreakMode] hideBreakScreen() called, overlay exists:', !!this.breakOverlay);

    if (this.breakOverlay) {
      console.log('[BreakMode] Setting overlay display to none');
      this.breakOverlay.style.display = 'none';
      console.log('[BreakMode] Overlay display after setting:', this.breakOverlay.style.display);
    } else {
      console.warn('[BreakMode] No overlay to hide!');
    }

    // Announce to screen readers
    this.announceToScreenReader('Break mode deactivated');
  }

  /**
   * Show "BREAK MODE ACTIVE" indicator (speaker view)
   */
  showSpeakerIndicator() {
    let indicator = document.getElementById('break-mode-indicator');
    if (!indicator) {
      indicator = document.createElement('div');
      indicator.id = 'break-mode-indicator';
      indicator.textContent = '⏸️ BREAK MODE ACTIVE';
      indicator.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        background: #ff6b35;
        color: white;
        padding: 12px;
        text-align: center;
        font-weight: bold;
        font-size: 18px;
        z-index: 10000;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
      `;
      document.body.appendChild(indicator);
    } else {
      indicator.style.display = 'block';
    }
  }

  /**
   * Hide "BREAK MODE ACTIVE" indicator (speaker view)
   */
  hideSpeakerIndicator() {
    const indicator = document.getElementById('break-mode-indicator');
    if (indicator) {
      indicator.style.display = 'none';
    }
  }

  /**
   * Announce message to screen readers
   * @param {string} message Message to announce
   */
  announceToScreenReader(message) {
    const announcement = document.createElement('div');
    announcement.setAttribute('role', 'status');
    announcement.setAttribute('aria-live', 'polite');
    announcement.style.cssText = `
      position: absolute;
      left: -10000px;
      width: 1px;
      height: 1px;
      overflow: hidden;
    `;
    announcement.textContent = message;
    document.body.appendChild(announcement);

    // Remove after announcement
    setTimeout(() => {
      document.body.removeChild(announcement);
    }, 1000);
  }

  /**
   * Check if break mode is active (for goto function integration)
   * @returns {boolean} True if break mode is active
   */
  isBreakModeActive() {
    return this.isActive;
  }
}

// ============================================================================
// Auto-initialize when DOM ready
// ============================================================================

let breakModeManager = null;

if (typeof document !== 'undefined') {
  document.addEventListener('DOMContentLoaded', () => {
    // Extract break screen path from data attribute (injected server-side)
    const slidesDiv = document.querySelector('.slides');
    const breakScreenPath = slidesDiv ? slidesDiv.getAttribute('data-break-screen') : null;

    breakModeManager = new BreakModeManager();
    breakModeManager.initialize({
      breakScreenPath
    });

    // Expose for other modules (goto function)
    window.MDSlidesBreakMode = breakModeManager;
  });
}

// Export for testing
if (typeof module !== 'undefined' && module.exports) {
  module.exports = { BreakModeManager };
}
