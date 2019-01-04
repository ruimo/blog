package helpers

import java.sql.Connection

import models.{Article, ArticleId, Image, ImageId}

object Ogp {
  def description(s: String, maxLength: Int = 100): String =
    (if (s.length < maxLength) s else s.substring(0, maxLength)).map { c =>
      if (c == '\r' || c == '\n') ' ' else c
    }

  def thumbnail(aid: ArticleId)(implicit conn: Connection): Option[ImageId] = Image.getThumbnail(aid)
}
