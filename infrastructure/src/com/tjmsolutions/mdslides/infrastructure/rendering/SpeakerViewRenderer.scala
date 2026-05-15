package com.tjmsolutions.mdslides.infrastructure.rendering

import com.tjmsolutions.mdslides.domain.SlideDeck
import io.circe.syntax.*
import io.circe.{Encoder, Json}

/**
 * Renderer for speaker.html (speaker view).
 *
 * Creates a dual-pane interface with:
 * - Current slide notes
 * - Next slide preview
 * - Elapsed time timer
 * - Slide counter
 *
 * Embeds slide data as JSON for JavaScript consumption.
 * References sync.js for cross-window synchronization.
 *
 * Related User Story: US-034 - Speaker Notes Rendering
 */
object SpeakerViewRenderer:

  /**
   * Render speaker view HTML.
   *
   * @param deck The slide deck
   * @param themeName The theme name (for consistency with main presentation)
   * @return Complete speaker.html content
   */
  def render(deck: SlideDeck, themeName: String): String =
    val slideData = encodeSlidesAsJson(deck)

    s"""<!DOCTYPE html>
       |<html lang="en">
       |<head>
       |  <meta charset="UTF-8">
       |  <meta name="viewport" content="width=device-width, initial-scale=1.0">
       |  <title>Speaker View - MDSlides</title>
       |  <style>
       |    * {
       |      margin: 0;
       |      padding: 0;
       |      box-sizing: border-box;
       |    }
       |
       |    body {
       |      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
       |      background: #1a1a1a;
       |      color: #ffffff;
       |      height: 100vh;
       |      overflow: hidden;
       |    }
       |
       |    .speaker-view {
       |      display: grid;
       |      grid-template-columns: 1fr 1fr;
       |      grid-template-rows: auto 1fr;
       |      gap: 1rem;
       |      padding: 1rem;
       |      height: 100vh;
       |    }
       |
       |    .header {
       |      grid-column: 1 / -1;
       |      display: flex;
       |      justify-content: space-between;
       |      align-items: center;
       |      padding: 0.5rem 1rem;
       |      background: #2a2a2a;
       |      border-radius: 8px;
       |    }
       |
       |    .slide-counter {
       |      font-size: 1.25rem;
       |      font-weight: 600;
       |    }
       |
       |    .timer {
       |      font-size: 2rem;
       |      font-weight: 700;
       |      font-family: 'Courier New', monospace;
       |      color: #4ade80;
       |    }
       |
       |    .notes-panel {
       |      background: #2a2a2a;
       |      border-radius: 8px;
       |      padding: 1.5rem;
       |      overflow-y: auto;
       |    }
       |
       |    .notes-panel h2 {
       |      font-size: 1.125rem;
       |      margin-bottom: 1rem;
       |      color: #a3a3a3;
       |      text-transform: uppercase;
       |      letter-spacing: 0.05em;
       |    }
       |
       |    .notes-area {
       |      font-size: 1.125rem;
       |      line-height: 1.75;
       |      white-space: pre-wrap;
       |      word-wrap: break-word;
       |    }
       |
       |    .notes-area.empty {
       |      color: #737373;
       |      font-style: italic;
       |    }
       |
       |    .preview-panel {
       |      background: #2a2a2a;
       |      border-radius: 8px;
       |      padding: 1.5rem;
       |      overflow-y: auto;
       |    }
       |
       |    .preview-panel h2 {
       |      font-size: 1.125rem;
       |      margin-bottom: 1rem;
       |      color: #a3a3a3;
       |      text-transform: uppercase;
       |      letter-spacing: 0.05em;
       |    }
       |
       |    .preview-area {
       |      font-size: 1.5rem;
       |      line-height: 1.5;
       |      color: #d4d4d4;
       |    }
       |
       |    .preview-area.end {
       |      color: #737373;
       |      font-style: italic;
       |    }
       |  </style>
       |</head>
       |<body>
       |  <div class="speaker-view">
       |    <div class="header">
       |      <div id="slide-counter" class="slide-counter">Slide 1 / ${deck.slideCount}</div>
       |      <div id="timer" class="timer">00:00</div>
       |    </div>
       |
       |    <div class="notes-panel">
       |      <h2>Speaker Notes</h2>
       |      <div id="notes-area" class="notes-area">
       |        <!-- Notes will be populated by JavaScript -->
       |      </div>
       |    </div>
       |
       |    <div class="preview-panel">
       |      <h2>Next Slide</h2>
       |      <div id="preview-area" class="preview-area">
       |        <!-- Preview will be populated by JavaScript -->
       |      </div>
       |    </div>
       |  </div>
       |
       |  <!-- Embedded slide data -->
       |  <script id="slide-data" type="application/json">
       |$slideData
       |  </script>
       |
       |  <!-- Sync module for cross-window communication -->
       |  <script src="sync.js"></script>
       |
       |  <!-- Speaker view controller -->
       |  <script>
       |    // Parse embedded slide data
       |    const slideDataElement = document.getElementById('slide-data');
       |    const slideData = JSON.parse(slideDataElement.textContent);
       |
       |    // Initialize slide index from URL parameter or default to 0
       |    const urlParams = new URLSearchParams(window.location.search);
       |    let currentSlideIndex = parseInt(urlParams.get('slide') || '0', 10);
       |    let timerStartTime = null;
       |    let timerInterval = null;
       |
       |    // Update display
       |    function updateDisplay() {
       |      const slide = slideData.slides[currentSlideIndex];
       |
       |      // Update slide counter
       |      document.getElementById('slide-counter').textContent =
       |        `Slide $${currentSlideIndex + 1} / $${slideData.totalSlides}`;
       |
       |      // Update notes
       |      const notesArea = document.getElementById('notes-area');
       |      if (slide.notes === null || slide.notes === undefined) {
       |        notesArea.textContent = 'No notes for this slide';
       |        notesArea.className = 'notes-area empty';
       |      } else if (slide.notes === '') {
       |        notesArea.textContent = '';
       |        notesArea.className = 'notes-area';
       |      } else {
       |        notesArea.textContent = slide.notes;
       |        notesArea.className = 'notes-area';
       |      }
       |
       |      // Update preview
       |      const previewArea = document.getElementById('preview-area');
       |      if (currentSlideIndex < slideData.totalSlides - 1) {
       |        const nextSlide = slideData.slides[currentSlideIndex + 1];
       |        const nextHeading = nextSlide.heading || nextSlide.title || 'Next Slide';
       |        previewArea.textContent = `Next: $${nextHeading}`;
       |        previewArea.className = 'preview-area';
       |      } else {
       |        previewArea.textContent = 'End of presentation';
       |        previewArea.className = 'preview-area end';
       |      }
       |    }
       |
       |    // Start timer
       |    function startTimer() {
       |      if (timerStartTime === null) {
       |        timerStartTime = Date.now();
       |        timerInterval = setInterval(updateTimer, 1000);
       |        updateTimer(); // Immediate update
       |      }
       |    }
       |
       |    // Update timer display
       |    function updateTimer() {
       |      if (timerStartTime === null) return;
       |
       |      const elapsed = Math.floor((Date.now() - timerStartTime) / 1000);
       |      const minutes = Math.floor(elapsed / 60);
       |      const seconds = elapsed % 60;
       |
       |      const formattedTime =
       |        String(minutes).padStart(2, '0') + ':' +
       |        String(seconds).padStart(2, '0');
       |
       |      document.getElementById('timer').textContent = formattedTime;
       |    }
       |
       |    // Navigate to slide
       |    function goToSlide(index) {
       |      if (index >= 0 && index < slideData.totalSlides) {
       |        if (timerStartTime === null) {
       |          startTimer();
       |        }
       |        currentSlideIndex = index;
       |        updateDisplay();
       |      }
       |    }
       |
       |    // Keyboard navigation
       |    document.addEventListener('keydown', (e) => {
       |      switch(e.key) {
       |        case 'ArrowRight':
       |        case ' ':
       |          e.preventDefault();
       |          goToSlide(currentSlideIndex + 1);
       |          MDSlidesSync.sendSlideChange(currentSlideIndex + 1);
       |          break;
       |        case 'ArrowLeft':
       |          e.preventDefault();
       |          goToSlide(currentSlideIndex - 1);
       |          MDSlidesSync.sendSlideChange(currentSlideIndex - 1);
       |          break;
       |        case 'Home':
       |          e.preventDefault();
       |          goToSlide(0);
       |          MDSlidesSync.sendSlideChange(0);
       |          break;
       |        case 'End':
       |          e.preventDefault();
       |          goToSlide(slideData.totalSlides - 1);
       |          MDSlidesSync.sendSlideChange(slideData.totalSlides - 1);
       |          break;
       |        case 'Escape':
       |          window.close();
       |          break;
       |      }
       |    });
       |
       |    // Initialize display
       |    updateDisplay();
       |
       |    // Listen for sync events from main window
       |    MDSlidesSync.receiveSlideChange((slideIndex) => {
       |      goToSlide(slideIndex);
       |    });
       |  </script>
       |</body>
       |</html>""".stripMargin

  /**
   * Encode slides as JSON for embedding in HTML.
   *
   * Creates a JSON object with:
   * - totalSlides: Int
   * - slides: Array of slide objects with notes and headings
   */
  private def encodeSlidesAsJson(deck: SlideDeck): String =
    val slidesJson = deck.toList.map { slide =>
      Json.obj(
        "notes" -> (slide.notes match {
          case Some(notes) => Json.fromString(notes)
          case None => Json.Null
        }),
        "title" -> Json.fromString(slide.getSlot("title").getOrElse("")),
        "heading" -> Json.fromString(slide.getSlot("heading").getOrElse(""))
      )
    }

    val dataJson = Json.obj(
      "totalSlides" -> Json.fromInt(deck.slideCount),
      "slides" -> Json.fromValues(slidesJson)
    )

    // Pretty print with 2-space indentation
    dataJson.spaces2

end SpeakerViewRenderer
