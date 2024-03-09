package controllers

import java.time.ZoneOffset.UTC
import java.net.URLDecoder
import play.api.mvc._
import java.time.{Instant, ZoneOffset, ZoneId, LocalDateTime}
import java.time.format.DateTimeFormatter
import scala.collection.{immutable => imm, mutable => mut}

trait TimeZoneSupport {
  def zoneId(implicit req: RequestHeader): ZoneId =
    req.cookies.get("tz").map(tz => ZoneId.of(URLDecoder.decode(tz.value, "utf-8"))).getOrElse(UTC)

  def toLocalDateTime(epochMillis: Long)(implicit req: RequestHeader) = LocalDateTime.ofInstant(
    Instant.ofEpochMilli(epochMillis), zoneId
  )
}

object TimeZoneSupport {
  private val map = mut.HashMap[String, DateTimeFormatter]()

  def formatter(pattern: String): DateTimeFormatter = map.synchronized {
    map.getOrElseUpdate(pattern, DateTimeFormatter.ofPattern(pattern))
  }
}
