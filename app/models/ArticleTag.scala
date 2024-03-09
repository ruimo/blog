package models

import anorm._
import java.sql.Connection

import scala.collection.{immutable => imm}
import scala.collection.immutable.{Seq, Vector}

case class ArticleTagId(value: Long) extends AnyVal

case class ArticleTag(
  id: Option[ArticleTagId],
  articleId: ArticleId,
  tagName: String
)

object ArticleTag {
  val simple = {
    SqlParser.get[Long]("article_tag.article_tag_id") ~
    SqlParser.get[Long]("article_tag.article_id") ~
    SqlParser.get[String]("article_tag.tag_name") map {
      case id~articleId~tagName => ArticleTag(
        Some(ArticleTagId(id)), ArticleId(articleId), tagName
      )
    }
  }

  def create(articleId: ArticleId, tagName: String)(implicit conn: Connection): Unit = {
    SQL(
      """
      insert into article_tag(
        article_tag_id, article_id, tag_name
      ) values (
        (select nextval('article_tag_seq')), {articleId}, {tagName}
      )
      """
    ).on(
      "articleId" -> articleId.value,
      "tagName" -> tagName
    ).executeUpdate()

    val id = SQL("select currval('article_tag_seq')").as(SqlParser.scalar[Long].single)
    ArticleTag(Some(ArticleTagId(id)), articleId, tagName)
  }

  def list(articleId: ArticleId)(implicit conn: Connection): Seq[ArticleTag] = {
    import scala.language.postfixOps

    SQL(
      """
      select * from article_tag where article_id = {articleId} order by tag_name
      """
    ).on(
      "articleId" -> articleId.value
    ).as(
      simple.*
    )
  }
}
