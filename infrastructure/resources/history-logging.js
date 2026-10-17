/**
 * History Logging - JavaScript Infrastructure Layer
 *
 * Captures presentation session analytics for post-presentation analysis.
 *
 * Features:
 * - Session start/end events
 * - Slide change events with timestamps
 * - Navigation method tracking (arrow key, goto, history navigation)
 * - Timer events (pause/resume)
 * - Break mode events
 * - Export to JSON format
 *
 * Architecture:
 * - Listens to presentation events
 * - Stores events in memory during session
 * - Exports via console or download on session end
 *
 * Related Artifacts:
 * - Design Spec: doc/internal/planning/v3.0.0-DESIGN-SPECIFICATIONS.md
 * - Domain Model: domain/src/com/tjmsolutions/mdslides/domain/HistoryLog.scala
 *
 * @version 3.0.0
 * @author TJM Solutions
 */

// ============================================================================
// History Logger
// ============================================================================

class HistoryLogger {
  constructor() {
    this.events = [];
    this.sessionStartTime = null;
    this.currentSlideIndex = 0;
    this.slideEntryTime = null;
    this.presentationName = '';
    this.themeName = '';
    this.totalSlides = 0;
  }

  /**
   * Initialize history logging when presentation loads
   * @param {object} config Configuration with presentationName, themeName, totalSlides
   */
  initialize(config) {
    this.presentationName = config.presentationName || 'untitled';
    this.themeName = config.themeName || 'unknown';
    this.totalSlides = config.totalSlides || 0;

    // Record session start
    this.recordSessionStart();

    // Set up event listeners
    this.setupEventListeners();

    // Log to console on window unload
    window.addEventListener('beforeunload', () => {
      this.recordSessionEnd();
      this.exportToConsole();
    });

    console.log('[HistoryLogger] Initialized for presentation:', this.presentationName);
  }

  /**
   * Record session start event
   */
  recordSessionStart() {
    this.sessionStartTime = Date.now();
    this.slideEntryTime = this.sessionStartTime;

    this.events.push({
      eventType: 'SessionStarted',
      timestamp: this.sessionStartTime,
      presentationName: this.presentationName,
      themeName: this.themeName,
      totalSlides: this.totalSlides
    });

    console.log('[HistoryLogger] Session started');
  }

  /**
   * Record session end event
   */
  recordSessionEnd() {
    const endTime = Date.now();
    const duration = endTime - this.sessionStartTime;

    this.events.push({
      eventType: 'SessionEnded',
      timestamp: endTime,
      durationMs: duration,
      durationFormatted: this.formatDuration(duration)
    });

    console.log('[HistoryLogger] Session ended, duration:', this.formatDuration(duration));
  }

  /**
   * Record slide change event
   * @param {number} fromSlide Previous slide index
   * @param {number} toSlide New slide index
   * @param {string} navigationMethod How the navigation occurred
   */
  recordSlideChange(fromSlide, toSlide, navigationMethod) {
    const now = Date.now();
    const timeOnSlide = now - this.slideEntryTime;

    this.events.push({
      eventType: 'SlideChanged',
      timestamp: now,
      fromSlide,
      toSlide,
      timeOnPreviousSlide: timeOnSlide,
      navigationMethod
    });

    // Update current state
    this.currentSlideIndex = toSlide;
    this.slideEntryTime = now;

    console.log(`[HistoryLogger] Slide ${fromSlide} → ${toSlide} via ${navigationMethod}, time on slide: ${timeOnSlide}ms`);
  }

  /**
   * Record timer event (pause/resume)
   * @param {string} action 'pause' or 'resume'
   */
  recordTimerEvent(action) {
    this.events.push({
      eventType: 'TimerEvent',
      timestamp: Date.now(),
      action
    });

    console.log(`[HistoryLogger] Timer ${action}`);
  }

  /**
   * Record break mode event
   * @param {string} action 'activate' or 'deactivate'
   */
  recordBreakModeEvent(action) {
    this.events.push({
      eventType: 'BreakModeEvent',
      timestamp: Date.now(),
      action
    });

    console.log(`[HistoryLogger] Break mode ${action}`);
  }

  /**
   * Record goto event
   * @param {number} targetSlide The slide navigated to
   */
  recordGotoEvent(targetSlide) {
    this.events.push({
      eventType: 'GotoEvent',
      timestamp: Date.now(),
      fromSlide: this.currentSlideIndex,
      toSlide: targetSlide
    });

    console.log(`[HistoryLogger] Goto: slide ${targetSlide}`);
  }

  /**
   * Set up event listeners for all logged events
   */
  setupEventListeners() {
    // Listen to Reveal.js slide changes
    if (typeof Reveal !== 'undefined') {
      Reveal.on('slidechanged', (event) => {
        const toSlide = event.indexh;
        const fromSlide = event.previousSlide ?
          parseInt(event.previousSlide.getAttribute('data-index-h') || '0') :
          this.currentSlideIndex;

        // Determine navigation method based on keys pressed
        const navigationMethod = this.guessNavigationMethod(fromSlide, toSlide);

        this.recordSlideChange(fromSlide, toSlide, navigationMethod);
      });
    }

    // Listen for custom events from other modules
    document.addEventListener('mdslides:timer:pause', () => {
      this.recordTimerEvent('pause');
    });

    document.addEventListener('mdslides:timer:resume', () => {
      this.recordTimerEvent('resume');
    });

    document.addEventListener('mdslides:breakmode:activate', () => {
      this.recordBreakModeEvent('activate');
    });

    document.addEventListener('mdslides:breakmode:deactivate', () => {
      this.recordBreakModeEvent('deactivate');
    });

    document.addEventListener('mdslides:goto', (event) => {
      this.recordGotoEvent(event.detail.targetSlide);
    });

    console.log('[HistoryLogger] Event listeners registered');
  }

  /**
   * Guess navigation method based on slide transition
   * @param {number} fromSlide Previous slide
   * @param {number} toSlide New slide
   * @returns {string} Navigation method
   */
  guessNavigationMethod(fromSlide, toSlide) {
    const diff = toSlide - fromSlide;

    if (diff === 1) return 'arrow-forward';
    if (diff === -1) return 'arrow-backward';
    if (Math.abs(diff) > 1) return 'goto-or-history';

    return 'unknown';
  }

  /**
   * Format duration in milliseconds to HH:MM:SS
   * @param {number} ms Duration in milliseconds
   * @returns {string} Formatted duration
   */
  formatDuration(ms) {
    const seconds = Math.floor(ms / 1000);
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;

    const pad = (num) => String(num).padStart(2, '0');
    return `${pad(hours)}:${pad(minutes)}:${pad(secs)}`;
  }

  /**
   * Export events to console (for debugging)
   */
  exportToConsole() {
    console.log('[HistoryLogger] Session log:');
    console.log(JSON.stringify(this.getSessionReport(), null, 2));
  }

  /**
   * Get session report as structured object
   * @returns {object} Session report
   */
  getSessionReport() {
    return {
      metadata: {
        presentationName: this.presentationName,
        themeName: this.themeName,
        totalSlides: this.totalSlides,
        startTime: new Date(this.sessionStartTime).toISOString(),
        eventCount: this.events.length
      },
      events: this.events
    };
  }

  /**
   * Download session log as JSON file
   */
  downloadLog() {
    const report = this.getSessionReport();
    const json = JSON.stringify(report, null, 2);
    const blob = new Blob([json], { type: 'application/json' });
    const url = URL.createObjectURL(blob);

    const a = document.createElement('a');
    a.href = url;
    a.download = `${this.presentationName}-${Date.now()}.log.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);

    URL.revokeObjectURL(url);

    console.log('[HistoryLogger] Log downloaded');
  }
}

// ============================================================================
// Auto-initialize when DOM ready
// ============================================================================

let historyLogger = null;

if (typeof document !== 'undefined') {
  document.addEventListener('DOMContentLoaded', () => {
    // Wait for Reveal.js to initialize
    if (typeof Reveal !== 'undefined') {
      Reveal.on('ready', (event) => {
        const totalSlides = Reveal.getTotalSlides();

        // Extract presentation name from document title or URL
        const presentationName = document.title.replace('MDSlides Presentation', '').trim() ||
          window.location.pathname.split('/').filter(Boolean).pop() ||
          'untitled';

        historyLogger = new HistoryLogger();
        historyLogger.initialize({
          presentationName,
          themeName: 'unknown',  // Could be injected from server
          totalSlides
        });

        // Expose logger for manual download
        window.MDSlidesHistoryLogger = historyLogger;
      });
    }
  });
}

// Export for testing
if (typeof module !== 'undefined' && module.exports) {
  module.exports = { HistoryLogger };
}
