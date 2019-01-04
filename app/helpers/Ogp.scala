package helpers

import java.sql.Connection
import java.util.regex.Pattern

import models.{Article, ArticleId, Image, ImageId}

object Ogp {
  val RemoveTagPattern = Pattern.compile("<[^>]+>") // Naive implementation...

  def description(s: String, maxLength: Int = 400): String =
    RemoveTagPattern.matcher(
      (if (s.length < maxLength) s else s.substring(0, maxLength)).map { c =>
        if (c == '\r' || c == '\n') ' ' else c
      }
    ).replaceAll(" ")

  def thumbnail(aid: ArticleId)(implicit conn: Connection): Option[ImageId] = Image.getThumbnail(aid)
}
