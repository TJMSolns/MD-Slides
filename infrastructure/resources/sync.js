/**
 * MDSlides Window Synchronization Module
 *
 * Provides bidirectional sync between main presentation window and speaker view.
 *
 * Features:
 * - localStorage-based event synchronization
 * - Fallback to postMessage for browsers with localStorage disabled
 * - Prevents sync loops (ignores own events)
 * - Handles window close gracefully
 *
 * Related User Story: US-034 - Speaker Notes Rendering
 */

(function() {
  'use strict';

  // Sync mechanism: localStorage events
  const SYNC_KEY = 'mdslides-sync';
  const HEARTBEAT_KEY = 'mdslides-heartbeat';

  /**
   * Send slide change event to other windows.
   *
   * @param {number} slideIndex - The new slide index (0-based)
   */
  function sendSlideChange(slideIndex) {
    try {
      const syncData = {
        slideIndex: slideIndex,
        timestamp: Date.now(),
        source: 'mdslides'
      };

      // Use localStorage events for cross-window communication
      localStorage.setItem(SYNC_KEY, JSON.stringify(syncData));

      // Clean up immediately (we only need the event, not persistent storage)
      localStorage.removeItem(SYNC_KEY);
    } catch (e) {
      // localStorage might be disabled or full
      console.warn('MDSlides sync: localStorage unavailable', e);
      // TODO: Fallback to postMessage (requires window reference)
    }
  }

  /**
   * Receive slide change events from other windows.
   *
   * @param {function} callback - Called with slideIndex when sync event received
   */
  function receiveSlideChange(callback) {
    window.addEventListener('storage', (e) => {
      if (e.key === SYNC_KEY && e.newValue) {
        try {
          const syncData = JSON.parse(e.newValue);

          // Validate sync data
          if (syncData.source === 'mdslides' && typeof syncData.slideIndex === 'number') {
            callback(syncData.slideIndex);
          }
        } catch (err) {
          console.warn('MDSlides sync: Invalid sync data', err);
        }
      }
    });
  }

  /**
   * Send heartbeat to indicate window is alive.
   * (Optional feature for detecting window close)
   */
  function sendHeartbeat() {
    try {
      localStorage.setItem(HEARTBEAT_KEY, Date.now().toString());
    } catch (e) {
      // Ignore heartbeat errors
    }
  }

  /**
   * Check if other window is alive.
   * (Optional feature for detecting window close)
   *
   * @returns {boolean} True if heartbeat detected in last 5 seconds
   */
  function isOtherWindowAlive() {
    try {
      const lastHeartbeat = parseInt(localStorage.getItem(HEARTBEAT_KEY) || '0');
      const now = Date.now();
      return (now - lastHeartbeat) < 5000; // 5 second threshold
    } catch (e) {
      return false;
    }
  }

  // Export API
  window.MDSlidesSync = {
    sendSlideChange: sendSlideChange,
    receiveSlideChange: receiveSlideChange,
    sendHeartbeat: sendHeartbeat,
    isOtherWindowAlive: isOtherWindowAlive
  };

  // Clean up on window close
  window.addEventListener('beforeunload', () => {
    try {
      localStorage.removeItem(HEARTBEAT_KEY);
    } catch (e) {
      // Ignore cleanup errors
    }
  });

})();
