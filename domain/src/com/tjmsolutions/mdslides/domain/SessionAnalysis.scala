package com.tjmsolutions.mdslides.domain

/**
 * Time spent on one slide during a session.
 *
 * @param slideIndex 0-indexed slide
 * @param millis     Presenting time on the slide (breaks excluded), summed over all visits
 * @param visits     How many times the slide was shown
 */
final case class SlideTime(slideIndex: Int, millis: Long, visits: Int)

/**
 * What happened in a recorded presentation session.
 *
 * Invariant: `slideTimes.map(_.millis).sum + breakMillis == wallMillis`.
 *
 * @param startedAt        Epoch millis of SessionStarted
 * @param endedAt          Epoch millis of SessionEnded, or of the last event if the session was saved mid-way
 * @param ended            Whether the log contains SessionEnded
 * @param totalSlides      Slides in the deck
 * @param wallMillis       endedAt - startedAt
 * @param breakMillis      Time in break mode, clipped to the session
 * @param breakCount       Number of breaks taken
 * @param slideTimes       Per-slide presenting time, ordered by slide index, visited slides only
 * @param path             Slides shown, in order, starting with the opening slide
 * @param navigationCounts SlideChanged events per navigation method
 */
final case class SessionAnalysis(
  startedAt: Long,
  endedAt: Long,
  ended: Boolean,
  totalSlides: Int,
  wallMillis: Long,
  breakMillis: Long,
  breakCount: Int,
  slideTimes: List[SlideTime],
  path: List[Int],
  navigationCounts: Map[String, Int]
):
  def presentingMillis: Long = wallMillis - breakMillis
  def slidesViewed: Int = slideTimes.size

object SessionAnalysis:

  private final case class Interval(from: Long, to: Long):
    def length: Long = math.max(0L, to - from)
    def overlap(other: Interval): Long =
      math.max(0L, math.min(to, other.to) - math.max(from, other.from))

  /**
   * Analyse a session log. Pure: no I/O, no clock.
   *
   * @return Left if the log has no SessionStarted event
   */
  def analyze(log: HistoryLog): Either[String, SessionAnalysis] =
    val events = log.events.sortBy(timestampOf)
    events.collectFirst { case s: HistoryEvent.SessionStarted => s } match
      case None => Left("The session log has no SessionStarted event")
      case Some(start) =>
        val startedAt  = start.timestamp
        val endEvent   = events.collectFirst { case e: HistoryEvent.SessionEnded => e }
        val endedAt    = endEvent.map(_.timestamp).getOrElse(events.map(timestampOf).max)
        val session    = Interval(startedAt, endedAt)
        val startSlide = start.metadata.get("startSlide").flatMap(_.toIntOption).getOrElse(0)

        val changes = events.collect { case c: HistoryEvent.SlideChanged
          if c.timestamp >= startedAt && c.timestamp <= endedAt => c }

        val breaks = breakIntervals(events, endedAt).map(b => Interval(math.max(b.from, startedAt), math.min(b.to, endedAt)))
          .filter(_.length > 0)

        // Each slide is on screen from its arrival until the next change (or the end)
        val arrivals = (startedAt -> startSlide) :: changes.map(c => c.timestamp -> c.toSlide)
        val shown = arrivals.zip(arrivals.drop(1).map(_._1) :+ endedAt).map { case ((from, slide), to) =>
          slide -> Interval(from, to)
        }

        val slideTimes = shown.groupBy(_._1).toList.sortBy(_._1).map { case (slide, intervals) =>
          val millis = intervals.map { case (_, iv) => iv.length - breaks.map(iv.overlap).sum }.sum
          SlideTime(slide, millis, intervals.size)
        }

        val path = arrivals.map(_._2).foldLeft(List.empty[Int]) { (acc, s) =>
          if acc.headOption.contains(s) then acc else s :: acc
        }.reverse

        Right(SessionAnalysis(
          startedAt        = startedAt,
          endedAt          = endedAt,
          ended            = endEvent.isDefined,
          totalSlides      = start.totalSlides,
          wallMillis       = session.length,
          breakMillis      = breaks.map(_.length).sum,
          breakCount       = breaks.size,
          slideTimes       = slideTimes,
          path             = path,
          navigationCounts = changes.groupBy(_.navigationMethod).view.mapValues(_.size).toMap
        ))

  /** Pair each BreakModeActivated with the next BreakModeDeactivated; an open break runs to `endedAt`. */
  private def breakIntervals(events: List[HistoryEvent], endedAt: Long): List[Interval] =
    events.foldLeft((List.empty[Interval], Option.empty[Long])) {
      case ((done, None), HistoryEvent.BreakModeActivated(ts))          => (done, Some(ts))
      case ((done, Some(from)), HistoryEvent.BreakModeDeactivated(ts, _)) => (Interval(from, ts) :: done, None)
      case (acc, _)                                                      => acc
    } match
      case (done, Some(open)) => (Interval(open, endedAt) :: done).reverse
      case (done, None)       => done.reverse

  private def timestampOf(event: HistoryEvent): Long = event match
    case HistoryEvent.SessionStarted(ts, _, _)       => ts
    case HistoryEvent.SlideChanged(ts, _, _, _)      => ts
    case HistoryEvent.TimerStarted(ts)               => ts
    case HistoryEvent.TimerPaused(ts, _)             => ts
    case HistoryEvent.TimerResumed(ts)               => ts
    case HistoryEvent.BreakModeActivated(ts)         => ts
    case HistoryEvent.BreakModeDeactivated(ts, _)    => ts
    case HistoryEvent.SessionEnded(ts, _)            => ts
