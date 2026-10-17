package com.tjmsolutions.mdslides.infrastructure.session

import com.tjmsolutions.mdslides.domain.SessionAnalysis
import java.nio.file.{Files, Path}
import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter
import scala.jdk.CollectionConverters.*
import scala.util.Try

/** Plain-text session report for the `report` command. Pure formatting. */
object SessionReport:

  def render(file: SessionLogFile, a: SessionAnalysis, zone: ZoneId): String =
    val rule  = "═" * 70
    val name  = file.title.fold(file.deck)(t => s"$t (${file.deck})")
    val when  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zone).format(Instant.ofEpochMilli(a.startedAt))
    val breaks = a.breakCount match
      case 0 => "no breaks"
      case 1 => s"1 break, ${hms(a.breakMillis)}"
      case n => s"$n breaks, ${hms(a.breakMillis)}"
    val pct   = if a.totalSlides > 0 then a.slidesViewed * 100 / a.totalSlides else 0
    def title(i: Int) = file.slideTitles.lift(i).filter(_.nonEmpty).getOrElse(s"Slide ${i + 1}")

    val perSlide = a.slideTimes.map { st =>
      f"  ${st.slideIndex + 1}%4d  ${title(st.slideIndex).take(40).padTo(40, ' ')}  ${hms(st.millis)}  ${st.visits}%6d"
    }
    val longest = a.slideTimes.sortBy(-_.millis).take(5).zipWithIndex.map { case (st, i) =>
      s"  ${i + 1}. Slide ${st.slideIndex + 1}: ${title(st.slideIndex)} (${hms(st.millis)})"
    }
    val nav = a.navigationCounts.toList.sortBy((m, n) => (-n, m)).map((m, n) => s"$m $n").mkString(" · ")

    (List(
      rule,
      s"  Session report: $name",
      rule,
      s"Started:      $when",
      s"Length:       ${hms(a.wallMillis)} (presenting ${hms(a.presentingMillis)}; $breaks)",
      s"Saved:        ${if a.ended then "at the end of the session" else "saved mid-session (no end recorded)"}",
      s"Slides shown: ${a.slidesViewed} of ${a.totalSlides} ($pct%)",
      "",
      "Time per slide",
      "   No.  Title                                     Time      Visits"
    ) ++ perSlide ++ List(
      "",
      "Longest slides"
    ) ++ longest ++ List(
      "",
      "Path",
      "  " + a.path.map(_ + 1).mkString(" → "),
      "",
      "Navigation",
      "  " + (if nav.isEmpty then "no slide changes recorded" else nav),
      rule
    )).mkString("\n")

  private def hms(millis: Long): String =
    val s = millis / 1000
    f"${s / 3600}%02d:${(s % 3600) / 60}%02d:${s % 60}%02d"

/** Finds the newest session log a deck saved (`<deck>-*.log.json`) in a list of folders. */
object SessionLogLocator:

  def newest(deck: String, folders: List[Path]): Option[Path] =
    // Browsers may add " (1)" to repeated downloads of the same name
    val pattern = (java.util.regex.Pattern.quote(deck) + """-\d{8}-\d{6}( \(\d+\))?\.log\.json""").r
    folders.filter(Files.isDirectory(_)).flatMap { dir =>
      Try(Files.list(dir).iterator().asScala.toList).getOrElse(Nil)
    }.filter(p => pattern.matches(p.getFileName.toString))
      .maxByOption(p => Files.getLastModifiedTime(p).toMillis)
