package models

import java.time.Instant
import anorm._
import java.sql.Connection

case class CommentId(value: Long) extends AnyVal

case class Comment(
  id: Option[CommentId],
  articleId: ArticleId,
  name: Option[String],
  body: String,
  authorized: Boolean,
  createdTime: Long = System.currentTimeMillis
)

object Comment {
  val simple = {
    SqlParser.get[Option[Long]]("comment.comment_id") ~
    SqlParser.get[Long]("comment.article_id") ~
    SqlParser.get[Option[String]]("comment.name") ~
    SqlParser.get[String]("comment.body") ~
    SqlParser.get[Boolean]("comment.authorized") ~
    SqlParser.get[Instant]("comment.created_time") map {
      case id~articleId~name~body~authorized~createdTime => Comment(
        id.map(CommentId.apply), ArticleId(articleId), name, body, authorized, createdTime.toEpochMilli
      )
    }
  }

  def create(
    articleId: ArticleId, name: Option[String], body: String, createdTime: Long = System.currentTimeMillis
  )(implicit conn: Connection): Comment = {
    SQL(
      """
      insert into comment(
        comment_id, article_id, name, body, created_time
      ) values (
        (select nextval('comment_seq')),
        {articleId}, {name}, {body}, {createdTime}
      )
      """
    ).on(
      "articleId" -> articleId.value,
      "name" -> name,
      "body" -> body,
      "createdTime" -> Instant.ofEpochMilli(createdTime)
    ).executeUpdate()

    val id = SQL("select currval('comment_seq')").as(SqlParser.scalar[Long].single)
    Comment(
      Some(CommentId(id)), articleId, name, body, false, createdTime
    )
  }

  def authorizeComment(id: CommentId)(implicit conn: Connection): Unit = {
    SQL(
      "update comment set authorized = true where comment_id = {id}"
    ).on(
      "id" -> id.value
    ).executeUpdate()
  }

  def remove(id: CommentId)(implicit conn: Connection): Int = {
    import scala.language.postfixOps

    SQL(
      """
      delete from comment where comment_id = {id}
      """
    ).on(
      "id" -> id.value
    ).executeUpdate()
  }
}

  
