/**
 * Presentation Timer - JavaScript Infrastructure Layer
 *
 * Integrates PresentationTimer domain model with HTML presentation runtime.
 *
 * Features:
 * - Auto-start timer when presentation loads
 * - Updates display every second (hh:mm:ss format)
 * - Pause/resume via 'B' key (toggles break mode)
 * - Cross-window sync via BroadcastChannel
 * - Drift correction (recalculates from epoch every 60 seconds)
 *
 * Architecture:
 * - Domain model (Scala) defines timer logic
 * - This module (JavaScript) handles UI integration and browser APIs
 *
 * Related Artifacts:
 * - Domain Model: doc/domain-models/aggregates/presentation-timer-aggregate.md
 * - BDD Scenarios: features/presentation-timer.feature
 * - Acceptance Criteria: doc/scenarios/acceptance-criteria/presentation-timer-acceptance.md
 *
 * @version 3.0.0
 * @author TJM Solutions
 */

// ============================================================================
// Timer State (mirrors Scala domain model)
// ============================================================================

const TimerState = {
  NOT_STARTED: 'NotStarted',
  RUNNING: 'Running',
  PAUSED: 'Paused'
};

// ============================================================================
// PresentationTimer (JavaScript implementation of domain model)
// ============================================================================

class PresentationTimer {
  /**
   * Create a new timer in NotStarted state
   */
  constructor() {
    this.state = TimerState.NOT_STARTED;
    this.startTimestamp = 0;
    this.totalPausedDuration = 0;
    this.lastPauseTimestamp = null;
  }

  /**
   * Start the timer (NotStarted → Running)
   * @returns {PresentationTimer} New timer instance in Running state
   * @throws {Error} If timer already started
   */
  start() {
    if (this.state !== TimerState.NOT_STARTED) {
      throw new Error('Timer already started');
    }

    const newTimer = new PresentationTimer();
    newTimer.state = TimerState.RUNNING;
    newTimer.startTimestamp = Date.now();
    newTimer.totalPausedDuration = 0;
    newTimer.lastPauseTimestamp = null;
    return newTimer;
  }

  /**
   * Pause the timer (Running → Paused)
   * @returns {PresentationTimer} New timer instance in Paused state
   * @throws {Error} If timer not running
   */
  pause() {
    if (this.state !== TimerState.RUNNING) {
      throw new Error(`Cannot pause when ${this.state}`);
    }

    const newTimer = Object.assign(new PresentationTimer(), this);
    newTimer.state = TimerState.PAUSED;
    newTimer.lastPauseTimestamp = Date.now();
    return newTimer;
  }

  /**
   * Resume the timer (Paused → Running)
   * @returns {PresentationTimer} New timer instance in Running state
   * @throws {Error} If timer not paused
   */
  resume() {
    // If already running, just return this (idempotent for break mode integration)
    if (this.state === TimerState.RUNNING) {
      console.log('[PresentationTimer] Already running, ignoring resume()');
      return this;
    }

    if (this.state !== TimerState.PAUSED) {
      throw new Error(`Cannot resume when ${this.state}`);
    }

    if (this.lastPauseTimestamp === null) {
      throw new Error('Invalid state: Paused but no pause timestamp');
    }

    const now = Date.now();
    const pauseDuration = now - this.lastPauseTimestamp;

    const newTimer = Object.assign(new PresentationTimer(), this);
    newTimer.state = TimerState.RUNNING;
    newTimer.totalPausedDuration = this.totalPausedDuration + pauseDuration;
    newTimer.lastPauseTimestamp = null;
    return newTimer;
  }

  /**
   * Calculate elapsed time in seconds (excluding paused time)
   * @returns {number} Seconds elapsed since start (0 if not started)
   */
  elapsedSeconds() {
    if (this.startTimestamp === 0) {
      return 0;
    }

    const now = Date.now();
    const totalRuntime = now - this.startTimestamp;

    // If currently paused, include current pause duration
    let effectivePausedDuration = this.totalPausedDuration;
    if (this.state === TimerState.PAUSED && this.lastPauseTimestamp !== null) {
      const currentPauseDuration = now - this.lastPauseTimestamp;
      effectivePausedDuration += currentPauseDuration;
    }

    const effectiveRuntime = totalRuntime - effectivePausedDuration;
    return Math.floor(effectiveRuntime / 1000);
  }

  /**
   * Get formatted time in hh:mm:ss format
   * @returns {string} Formatted time (e.g., "00:15:30")
   */
  formattedTime() {
    const seconds = this.elapsedSeconds();
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;

    const pad = (num) => String(num).padStart(2, '0');
    return `${pad(hours)}:${pad(minutes)}:${pad(secs)}`;
  }

  /**
   * Get current state
   * @returns {string} NotStarted, Running, or Paused
   */
  currentState() {
    return this.state;
  }
}

// ============================================================================
// Timer Manager (Singleton for presentation instance)
// ============================================================================

class TimerManager {
  constructor() {
    this.timer = new PresentationTimer();
    this.timerInterval = null;
    this.driftCorrectionCounter = 0;
    this.channel = null;
    this.footerElement = null;
  }

  /**
   * Initialize timer when presentation loads
   */
  initialize() {
    // Find or create footer timer element
    this.footerElement = document.querySelector('.presentation-timer');
    if (!this.footerElement) {
      this.footerElement = this.createFooterElement();
    }

    // Set up BroadcastChannel for cross-window sync
    if (typeof BroadcastChannel !== 'undefined') {
      this.channel = new BroadcastChannel('mdslides-timer-sync');
      this.channel.onmessage = (event) => this.handleSyncMessage(event);
    }

    // Auto-start timer
    this.startTimer();

    // Set up keyboard handler for 'B' key (pause/resume)
    this.setupKeyboardHandler();

    console.log('[PresentationTimer] Initialized');
  }

  /**
   * Create footer timer element
   * @returns {HTMLElement} Timer display element
   */
  createFooterElement() {
    const footer = document.querySelector('footer') || document.body;
    const timerDiv = document.createElement('div');
    timerDiv.className = 'presentation-timer';
    timerDiv.setAttribute('role', 'timer');
    timerDiv.setAttribute('aria-live', 'off'); // Don't announce every second
    timerDiv.setAttribute('aria-label', 'Presentation elapsed time');
    timerDiv.textContent = '00:00:00';

    footer.insertBefore(timerDiv, footer.firstChild);
    return timerDiv;
  }

  /**
   * Start the timer
   */
  startTimer() {
    if (this.timer.state !== TimerState.NOT_STARTED) {
      console.warn('[PresentationTimer] Timer already started');
      return;
    }

    this.timer = this.timer.start();
    this.updateDisplay();
    this.startInterval();
    this.syncToOtherWindows();

    console.log('[PresentationTimer] Started');
  }

  /**
   * Toggle pause/resume (triggered by 'B' key)
   */
  togglePause() {
    try {
      if (this.timer.state === TimerState.RUNNING) {
        this.timer = this.timer.pause();
        this.stopInterval();
        this.footerElement.classList.add('timer-paused');
        console.log('[PresentationTimer] Paused');
      } else if (this.timer.state === TimerState.PAUSED) {
        this.timer = this.timer.resume();
        this.startInterval();
        this.footerElement.classList.remove('timer-paused');
        console.log('[PresentationTimer] Resumed');
      }

      this.updateDisplay();
      this.syncToOtherWindows();
    } catch (error) {
      console.error('[PresentationTimer] Toggle pause failed:', error);
    }
  }

  /**
   * Reset timer to 00:00:00 (triggered by 'R' key)
   */
  reset() {
    this.stopInterval();
    this.timer = new PresentationTimer();
    this.timer = this.timer.start();
    this.footerElement.classList.remove('timer-paused');
    this.startInterval();
    this.updateDisplay();
    this.syncToOtherWindows();
    console.log('[PresentationTimer] Reset');
  }

  /**
   * Start interval for display updates (every 1 second)
   */
  startInterval() {
    if (this.timerInterval !== null) {
      return; // Already running
    }

    this.timerInterval = setInterval(() => {
      this.updateDisplay();

      // Drift correction every 60 seconds
      this.driftCorrectionCounter++;
      if (this.driftCorrectionCounter >= 60) {
        this.driftCorrectionCounter = 0;
        console.log('[PresentationTimer] Drift correction applied');
      }

      // Sync every 5 seconds while running
      if (this.driftCorrectionCounter % 5 === 0) {
        this.syncToOtherWindows();
      }
    }, 1000);
  }

  /**
   * Stop interval (when paused)
   */
  stopInterval() {
    if (this.timerInterval !== null) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

  /**
   * Update footer display with current time
   */
  updateDisplay() {
    if (!this.footerElement) {
      return;
    }

    const formatted = this.timer.formattedTime();
    this.footerElement.textContent = formatted;

    // Add pause indicator if paused
    if (this.timer.state === TimerState.PAUSED) {
      this.footerElement.textContent += ' ⏸';
    }
  }

  /**
   * Sync timer state to other windows via BroadcastChannel
   */
  syncToOtherWindows() {
    if (!this.channel) {
      return; // BroadcastChannel not available
    }

    this.channel.postMessage({
      type: 'timer-sync',
      state: this.timer.state,
      startTimestamp: this.timer.startTimestamp,
      totalPausedDuration: this.timer.totalPausedDuration,
      lastPauseTimestamp: this.timer.lastPauseTimestamp,
      elapsedSeconds: this.timer.elapsedSeconds(),
      formattedTime: this.timer.formattedTime()
    });
  }

  /**
   * Handle sync message from other windows
   * @param {MessageEvent} event BroadcastChannel message
   */
  handleSyncMessage(event) {
    if (event.data.type !== 'timer-sync') {
      return;
    }

    // Reconstruct timer from sync data
    const syncedTimer = new PresentationTimer();
    syncedTimer.state = event.data.state;
    syncedTimer.startTimestamp = event.data.startTimestamp;
    syncedTimer.totalPausedDuration = event.data.totalPausedDuration;
    syncedTimer.lastPauseTimestamp = event.data.lastPauseTimestamp;

    this.timer = syncedTimer;
    this.updateDisplay();

    // Sync interval state with timer state
    if (this.timer.state === TimerState.RUNNING && this.timerInterval === null) {
      this.startInterval();
      this.footerElement.classList.remove('timer-paused');
    } else if (this.timer.state === TimerState.PAUSED && this.timerInterval !== null) {
      this.stopInterval();
      this.footerElement.classList.add('timer-paused');
    }

    console.log('[PresentationTimer] Synced from other window:', event.data.formattedTime);
  }

  /**
   * Set up keyboard handler for 'B' key
   *
   * NOTE: 'B' key is now handled by break-mode.js, which calls timer.pause()/resume()
   * This method is kept for backward compatibility but does nothing.
   */
  setupKeyboardHandler() {
    // Break mode (break-mode.js) now handles 'B' key and controls timer pause/resume
    // No keyboard handler needed here
  }

  /**
   * Clean up on window unload
   */
  cleanup() {
    this.stopInterval();
    if (this.channel) {
      this.channel.close();
    }

    console.log('[PresentationTimer] Cleanup complete. Final time:', this.timer.formattedTime());
  }
}

// ============================================================================
// Auto-initialize when DOM ready
// ============================================================================

let timerManager = null;

if (typeof document !== 'undefined') {
  document.addEventListener('DOMContentLoaded', () => {
    timerManager = new TimerManager();
    timerManager.initialize();
  });

  window.addEventListener('beforeunload', () => {
    if (timerManager) {
      timerManager.cleanup();
    }
  });
}

// Export for testing (if module system available)
if (typeof module !== 'undefined' && module.exports) {
  module.exports = { PresentationTimer, TimerManager, TimerState };
}
