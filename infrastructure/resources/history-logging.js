/**
 * History Logging - session analytics recorded in the browser.
 *
 * Records the session while you present: slide changes (with how they happened),
 * timer pause/resume and break mode. Press L to save the log as
 * `<deck>-YYYYMMDD-HHMMSS.log.json`; `java -jar md-slides.jar report <deck>` reads it.
 *
 * Event JSON matches the domain's HistoryLog.eventToJson (SessionStarted, SlideChanged,
 * TimerPaused, TimerResumed, BreakModeActivated, BreakModeDeactivated, SessionEnded).
 *
 * Inputs (all from the page):
 * - 'mdslides:slidechange' {from, to, method}   dispatched by showSlide()
 * - 'mdslides:timer:pause' {elapsedMillis}, 'mdslides:timer:resume'
 * - 'mdslides:breakmode:activate', 'mdslides:breakmode:deactivate'
 */

class HistoryLogger {
  constructor({ deck, title, theme, totalSlides, slideTitles, startSlide, now }) {
    this.deck = deck;
    this.title = title;
    this.theme = theme;
    this.totalSlides = totalSlides;
    this.slideTitles = slideTitles;
    this.startedAt = now;
    this.breakStartedAt = null;
    this.events = [{
      eventType: 'SessionStarted',
      timestamp: now,
      totalSlides,
      metadata: { deck, title, startSlide: String(startSlide) }
    }];
  }

  record(event) {
    this.events.push(event);
  }

  slideChanged(from, to, method, now) {
    this.record({ eventType: 'SlideChanged', timestamp: now, fromSlide: from, toSlide: to, navigationMethod: method });
  }

  timerPaused(elapsedMillis, now) {
    this.record({ eventType: 'TimerPaused', timestamp: now, elapsedMillis: Math.round(elapsedMillis || 0) });
  }

  timerResumed(now) {
    this.record({ eventType: 'TimerResumed', timestamp: now });
  }

  breakStarted(now) {
    if (this.breakStartedAt !== null) return;
    this.breakStartedAt = now;
    this.record({ eventType: 'BreakModeActivated', timestamp: now });
  }

  breakEnded(now) {
    if (this.breakStartedAt === null) return;
    this.record({ eventType: 'BreakModeDeactivated', timestamp: now, breakDurationMillis: now - this.breakStartedAt });
    this.breakStartedAt = null;
  }

  /** The log as saved: everything so far plus a SessionEnded marker for this moment. */
  snapshot(now) {
    return {
      format: 'mdslides-session-log',
      version: 1,
      deck: this.deck,
      title: this.title,
      theme: this.theme,
      totalSlides: this.totalSlides,
      slideTitles: this.slideTitles,
      events: this.events.concat([{ eventType: 'SessionEnded', timestamp: now, totalElapsedMillis: now - this.startedAt }])
    };
  }

  fileName(now) {
    const d = new Date(now);
    const pad = (n) => String(n).padStart(2, '0');
    const stamp = `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}-${pad(d.getHours())}${pad(d.getMinutes())}${pad(d.getSeconds())}`;
    return `${this.deck}-${stamp}.log.json`;
  }

  /** Save the log as a download, and say so on screen. */
  downloadLog() {
    const now = Date.now();
    const name = this.fileName(now);
    const blob = new Blob([JSON.stringify(this.snapshot(now), null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = name;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    announce(`Session log saved: ${name}`);
  }
}

/** Deck name: the output folder name (…/my-talk/index.html → my-talk). */
function deckNameFromLocation(loc) {
  const parts = loc.pathname.split('/').filter(Boolean);
  if (parts.length && /\.html?$/i.test(parts[parts.length - 1])) parts.pop();
  const name = parts.length ? decodeURIComponent(parts[parts.length - 1]) : 'presentation';
  return name.replace(/[^A-Za-z0-9._-]+/g, '-') || 'presentation';
}

/** Brief on-screen confirmation, also read by screen readers. */
function announce(message) {
  let el = document.getElementById('mdslides-status');
  if (!el) {
    el = document.createElement('div');
    el.id = 'mdslides-status';
    el.setAttribute('role', 'status');
    el.setAttribute('aria-live', 'polite');
    el.style.cssText = 'position:fixed;left:50%;bottom:4.5rem;transform:translateX(-50%);z-index:1000;' +
      'background:#1f1f20;color:#fff;padding:0.6rem 1rem;border-radius:6px;font:14px/1.4 system-ui,sans-serif;' +
      'max-width:90vw;box-shadow:0 2px 8px rgba(0,0,0,0.3);';
    document.body.appendChild(el);
  }
  el.textContent = message;
  el.hidden = false;
  clearTimeout(announce.timer);
  announce.timer = setTimeout(() => { el.hidden = true; }, 4000);
}

if (typeof document !== 'undefined') {
  document.addEventListener('DOMContentLoaded', () => {
    const meta = (name) => (document.querySelector(`meta[name="${name}"]`) || {}).content || '';
    const slides = Array.from(document.querySelectorAll('.slide'));
    const hash = /^#([0-9]+)$/.exec(location.hash);
    const startSlide = hash ? Math.min(Math.max(parseInt(hash[1], 10) - 1, 0), Math.max(slides.length - 1, 0)) : 0;

    const logger = new HistoryLogger({
      deck: deckNameFromLocation(location),
      title: meta('mdslides-title'),
      theme: meta('mdslides-theme'),
      totalSlides: slides.length,
      slideTitles: slides.map((s) => {
        const h = s.querySelector('h1, h2, h3');
        return h ? h.textContent.trim() : '';
      }),
      startSlide,
      now: Date.now()
    });

    document.addEventListener('mdslides:slidechange', (e) => logger.slideChanged(e.detail.from, e.detail.to, e.detail.method, Date.now()));
    document.addEventListener('mdslides:timer:pause', (e) => logger.timerPaused(e.detail && e.detail.elapsedMillis, Date.now()));
    document.addEventListener('mdslides:timer:resume', () => logger.timerResumed(Date.now()));
    document.addEventListener('mdslides:breakmode:activate', () => logger.breakStarted(Date.now()));
    document.addEventListener('mdslides:breakmode:deactivate', () => logger.breakEnded(Date.now()));

    window.MDSlidesHistoryLogger = logger;
  });
}

if (typeof module !== 'undefined' && module.exports) {
  module.exports = { HistoryLogger, deckNameFromLocation };
}
