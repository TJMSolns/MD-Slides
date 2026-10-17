package com.tjmsolutions.mdslides.infrastructure.session

import com.tjmsolutions.mdslides.domain.{HistoryEvent, HistoryLog}
import io.circe.{ACursor, Decoder, HCursor, Json}
import io.circe.parser.parse as parseJson

/**
 * A session log saved by a presentation (press L), as read back by `report`.
 *
 * @param deck        Deck name (the output directory name)
 * @param title       Deck title, if the deck has one
 * @param theme       Theme the deck was rendered with
 * @param slideTitles Per-slide titles, by slide index (may be empty)
 * @param log         The recorded events
 */
final case class SessionLogFile(
  deck: String,
  title: Option[String],
  theme: Option[String],
  slideTitles: List[String],
  log: HistoryLog
)

/**
 * Anticorruption layer between the browser's JSON session log and the domain's HistoryLog.
 * Event JSON matches `HistoryLog.eventToJson`.
 */
object SessionLogParser:

  val Format = "mdslides-session-log"

  def parse(content: String): Either[String, SessionLogFile] =
    for
      json   <- parseJson(content).left.map(e => s"Not valid JSON: ${e.message}")
      c       = json.hcursor
      _      <- c.get[String]("format").toOption.filter(_ == Format)
                  .toRight("Not an MD-Slides session log (missing \"format\": \"mdslides-session-log\")")
      deck   <- c.get[String]("deck").left.map(_ => "Session log has no \"deck\" name")
      raw     = c.downField("events").focus.flatMap(_.asArray).getOrElse(Vector.empty)
      events <- decodeEvents(raw.toList)
    yield SessionLogFile(
      deck        = deck,
      title       = c.get[String]("title").toOption.filter(_.nonEmpty),
      theme       = c.get[String]("theme").toOption.filter(_.nonEmpty),
      slideTitles = c.get[List[String]]("slideTitles").getOrElse(Nil),
      log         = HistoryLog(events)
    )

  private def decodeEvents(raw: List[Json]): Either[String, List[HistoryEvent]] =
    raw.zipWithIndex.foldLeft[Either[String, List[HistoryEvent]]](Right(Nil)) {
      case (Right(acc), (json, i)) =>
        decodeEvent(json.hcursor) match
          case Right(Some(event)) => Right(event :: acc)
          case Right(None)        => Right(acc)
          case Left(reason)       => Left(s"Session log event ${i + 1} is malformed: $reason")
      case (left, _) => left
    }.map(_.reverse)

  /** Right(None) for event types this version does not know. */
  private def decodeEvent(c: HCursor): Either[String, Option[HistoryEvent]] =
    def f[A: Decoder](name: String): Either[String, A] = c.get[A](name).left.map(_ => s"missing or invalid \"$name\"")
    c.get[String]("eventType").left.map(_ => "missing \"eventType\"").flatMap {
      case "SessionStarted" =>
        for ts <- f[Long]("timestamp"); n <- f[Int]("totalSlides")
        yield Some(HistoryEvent.SessionStarted(ts, n, c.get[Map[String, String]]("metadata").getOrElse(Map.empty)))
      case "SlideChanged" =>
        for ts <- f[Long]("timestamp"); from <- f[Int]("fromSlide"); to <- f[Int]("toSlide")
        yield Some(HistoryEvent.SlideChanged(ts, from, to, c.get[String]("navigationMethod").getOrElse("unknown")))
      case "TimerStarted"         => f[Long]("timestamp").map(ts => Some(HistoryEvent.TimerStarted(ts)))
      case "TimerPaused"          =>
        for ts <- f[Long]("timestamp") yield Some(HistoryEvent.TimerPaused(ts, c.get[Long]("elapsedMillis").getOrElse(0L)))
      case "TimerResumed"         => f[Long]("timestamp").map(ts => Some(HistoryEvent.TimerResumed(ts)))
      case "BreakModeActivated"   => f[Long]("timestamp").map(ts => Some(HistoryEvent.BreakModeActivated(ts)))
      case "BreakModeDeactivated" =>
        for ts <- f[Long]("timestamp")
        yield Some(HistoryEvent.BreakModeDeactivated(ts, c.get[Long]("breakDurationMillis").getOrElse(0L)))
      case "SessionEnded"         =>
        for ts <- f[Long]("timestamp")
        yield Some(HistoryEvent.SessionEnded(ts, c.get[Long]("totalElapsedMillis").getOrElse(0L)))
      case _ => Right(None)
    }
