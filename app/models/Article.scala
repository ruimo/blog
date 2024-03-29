package models

import java.time.Instant

import anorm._
import java.sql.Connection

import scala.collection.{immutable => imm}
import scala.collection.immutable.{Seq, Vector}

case class ArticleId(value: Long) extends AnyVal

case class Article(
  id: ArticleId,
  bloggerId: BloggerId, title: String = "", body: String = "",
  createdTime: Long = System.currentTimeMillis,
  updatedTime: Long = System.currentTimeMillis,
  publishTime: Long = System.currentTimeMillis
)

object Article {
  val simple = {
    SqlParser.get[Long]("article.article_id") ~
    SqlParser.get[Long]("article.blogger_id") ~
    SqlParser.get[String]("article.title") ~
    SqlParser.get[String]("article.body") ~
    SqlParser.get[Instant]("article.publish_time") ~
    SqlParser.get[Instant]("article.updated_time") ~
    SqlParser.get[Instant]("article.created_time") map {
      case id~bloggerId~title~body~publishTime~updatedTime~createdTime => Article(
        ArticleId(id), BloggerId(bloggerId), title, body,
        createdTime.toEpochMilli, updatedTime.toEpochMilli, publishTime.toEpochMilli
      )
    }
  }

  def createId()(implicit conn: Connection): ArticleId = {
    ArticleId(SQL("select nextval('article_seq')").as(SqlParser.scalar[Long].single))
  }

  def create(article: Article)(implicit conn: Connection): Unit = {
    SQL(
      """
      insert into article(
        article_id, title, body, blogger_id, created_time, publish_time
      ) values (
        {id}, {title}, {body}, {bloggerId}, {createdTime}, {publishTime}
      )
      """
    ).on(
      "id" -> article.id.value,
      "title" -> article.title,
      "body" -> article.body,
      "bloggerId" -> article.bloggerId.value,
      "publishTime" -> Instant.ofEpochMilli(article.publishTime),
      "createdTime" -> Instant.ofEpochMilli(article.createdTime)
    ).executeUpdate()
  }

  def update(article: Article)(implicit conn: Connection): Unit = {
    SQL(
      """
      update article set 
        title = {title},
        body = {body},
        blogger_id = {bloggerId},
        publish_time = {publishTime},
        updated_time = current_timestamp
      where article_id = {id}
      """
    ).on(
      "title" -> article.title,
      "body" -> article.body,
      "bloggerId" -> article.bloggerId.value,
      "publishTime" -> Instant.ofEpochMilli(article.publishTime),
      "id" -> article.id.value
    ).executeUpdate()
  }

  def get(id: ArticleId)(implicit conn: Connection): Option[Article] = SQL(
    """
    select * from article where article_id = {id}
    """
  ).on(
    "id" -> id.value
  ).as(
    simple.singleOpt
  )

  def list(
    page: Int = 0, pageSize: Int = 10, orderBy: OrderBy = OrderBy("publish_time", Desc),
    now: Long = System.currentTimeMillis, tagName: Option[String] = None
  )(
    implicit conn: Connection
  ): PagedRecords[Article] = {
    import scala.language.postfixOps

    val offset: Int = pageSize * page
    val where = """
      where publish_time <= {now}
    """ + tagName.map { tag =>
      "and article_id in (select article_id from article_tag where tag_name = {tagName})"
    }.getOrElse("")

    val records: Seq[Article] = SQL(
      "select * from article " + where + s"""
      order by $orderBy limit {pageSize} offset {offset}
      """
    ).on(
      "now" -> Instant.ofEpochMilli(now),
      "pageSize" -> pageSize,
      "offset" -> offset,
      "tagName" -> tagName.getOrElse("")
    ).as(
      simple.*
    )

    val count = SQL(
      "select count(*) from article " + where
    ).on(
      "now" -> Instant.ofEpochMilli(now),
      "tagName" -> tagName.getOrElse("")
    ).as(SqlParser.scalar[Long].single)

    PagedRecords(page, pageSize, (count + pageSize - 1) / pageSize, orderBy, records)
  }

  val withComment = simple ~ Comment.simple.? map {
    case article~comment => (article, comment)
  }

  def listFrom(
    page: Int = 0, pageSize: Int = 10, orderBy: OrderBy = OrderBy("publish_time", Desc),
    now: Long = System.currentTimeMillis, fromId: ArticleId, tagName: Option[String] = None
  )(
    implicit conn: Connection
  ): PagedRecords[Article] = {
    import scala.language.postfixOps

    val offset: Int = pageSize * page
    val where = """
      where publish_time <= (select publish_time from article where article_id = {fromId})
    """ + tagName.map { tag =>
      "and article_id in (select article_id from article_tag where tag_name = {tagName})"
    }.getOrElse("")

    val records: Seq[Article] = SQL(
      "select * from article" + where + s"""
      and publish_time <= {now}
      order by $orderBy limit {pageSize} offset {offset}
      """
    ).on(
      "now" -> Instant.ofEpochMilli(now),
      "pageSize" -> pageSize,
      "offset" -> offset,
      "fromId" -> fromId.value,
      "tagName" -> tagName.getOrElse("")
    ).as(
      simple.*
    )

    val count = SQL(
      "select count(*) from article " + where
    ).on(
      "now" -> Instant.ofEpochMilli(now),
      "fromId" -> fromId.value,
      "tagName" -> tagName.getOrElse("")
    ).as(SqlParser.scalar[Long].single)

    PagedRecords(page, pageSize, (count + pageSize - 1) / pageSize, orderBy, records)
  }

  def listWithComment(
    page: Int = 0, pageSize: Int = 10, orderBy: OrderBy = OrderBy("publish_time", Desc),
    now: Long = System.currentTimeMillis, commentsPerArticle: Int = 5, tagName: Option[String] = None
  )(
    implicit conn: Connection
  ): PagedRecords[(Article, imm.Seq[Comment])] = {
    import scala.language.postfixOps

    val offset: Int = pageSize * page
    val where = """
      where publish_time <= {now}
    """ + tagName.map { tag =>
      "and a0.article_id in (select article_id from article_tag where tag_name = {tagName})"
    }.getOrElse("")
    val recs: List[(Article, Option[Comment])] = SQL(
      """
      select * from article a0
      left join comment c0 on a0.article_id = c0.article_id
      """ + where + s"""
      and (
        c0.comment_id in (
          select comment_id from comment c1 where c0.article_id = c1.article_id order by c1.created_time limit {commentsPerArticle}
        )
        or c0.comment_id is null
      )
      order by $orderBy, c0.created_time desc
      limit {pageSize} offset {offset}
      """
    ).on(
      "now" -> Instant.ofEpochMilli(now),
      "commentsPerArticle" -> commentsPerArticle,
      "pageSize" -> pageSize,
      "offset" -> offset,
      "tagName" -> tagName.getOrElse("")
    ).as(
      withComment.*
    )

    accumRecords(page, pageSize, orderBy, now, recs, where, tagName, None)
  }

  def articleWithCommentSql(where: String) = s"""
    select * from article a0
    left join comment c0 on a0.article_id = c0.article_id
    where $where
    and a0.publish_time <= {now}
    and (
      c0.comment_id in (
        select comment_id from comment c1 where c0.article_id = c1.article_id order by c1.created_time limit {commentsPerArticle}
      )
      or c0.comment_id is null
    )
  """

  def listWithCommentFrom(
    page: Int = 0, pageSize: Int = 10, orderBy: OrderBy = OrderBy("publish_time", Desc),
    now: Long = System.currentTimeMillis, commentsPerArticle: Int = 5, fromId: ArticleId, tagName: Option[String] = None
  )(
    implicit conn: Connection
  ): PagedRecords[(Article, imm.Seq[Comment])] = {
    import scala.language.postfixOps

    val offset: Int = pageSize * page
    val where = """
      a0.publish_time <= (select publish_time from article where article_id = {fromId})
    """ + tagName.map { tag =>
      "and a0.article_id in (select article_id from article_tag where tag_name = {tagName})"
    }.getOrElse("")
    val recs: List[(Article, Option[Comment])] = SQL(
      articleWithCommentSql(where) +
      s"""
      order by $orderBy, c0.created_time desc
      limit {pageSize} offset {offset}
      """
    ).on(
      "now" -> Instant.ofEpochMilli(now),
      "commentsPerArticle" -> commentsPerArticle,
      "pageSize" -> pageSize,
      "offset" -> offset,
      "fromId" -> fromId.value,
      "tagName" -> tagName.getOrElse("")
    ).as(
      withComment.*
    )

    accumRecords(page, pageSize, orderBy, now, recs, where, tagName, Some(fromId))
  }

  def showWithComment(
    id: ArticleId, now: Long = System.currentTimeMillis, commentsPerArticle: Int = 20
  )(
    implicit conn: Connection
  ): (Article, imm.Seq[Comment]) = {
    import scala.language.postfixOps

    val recs = SQL(
      articleWithCommentSql("a0.article_id = {id}") +
      "order by c0.created_time desc"
    ).on(
      "now" -> Instant.ofEpochMilli(now),
      "commentsPerArticle" -> commentsPerArticle,
      "id" -> id.value
    ).as(
      withComment.*
    )

    (
      recs.head._1, recs.foldLeft(Vector[Comment]()) { (sum, e) => sum ++ e._2 }
    )
  }

  def accumRecords(
    page: Int, pageSize: Int, orderBy: OrderBy, now: Long, recs: List[(Article, Option[Comment])],
    where: String, tagName: Option[String], fromId: Option[ArticleId]
  )(implicit conn: Connection): PagedRecords[(Article, Seq[Comment])] = {
    if (recs.isEmpty) {
      PagedRecords(page, pageSize, 0, orderBy, imm.Seq())
    }
    else {
      def accum(
        records: List[(Article, Option[Comment])], article: Article, comments: Vector[Comment],
        result: Vector[(Article, Seq[Comment])] = Vector()
      ): Seq[(Article, Seq[Comment])] = records match {
        case List() => result
        case List(r) =>
          if (r._1 == article) {
            result :+(article, comments ++ r._2)
          }
          else {
            result :+(article, comments) :+(r._1, r._2.toList)
          }
        case h :: t =>
          if (h._1 == article) accum(t, article, comments ++ h._2, result)
          else accum(t, h._1, h._2.toVector, result :+(article, comments))
      }

      import scala.language.postfixOps

      val fromIdParm: Seq[NamedParameter] = fromId match {
        case None => Seq()
        case Some(articleId) => Seq("fromId" -> articleId.value)
      }

      val count = SQL(
        "select count(*) from article a0" + fromId.map(articleId => " where").getOrElse("") + where
      ).on(
        (
          Seq[NamedParameter](
            "now" -> Instant.ofEpochMilli(now),
            "tagName" -> tagName.getOrElse("")
          )
          ++ fromIdParm
        ) *
      ).as(SqlParser.scalar[Long].single)

      PagedRecords(page, pageSize, (count + pageSize - 1) / pageSize, orderBy, accum(recs, recs.head._1, Vector()))
    }
  }

  def remove(id: ArticleId)(implicit conn: Connection): Int = {
    import scala.language.postfixOps

    SQL(
      """
      delete from article where article_id = {id}
      """
    ).on(
      "id" -> id.value
    ).executeUpdate()
  }
}
