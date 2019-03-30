package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, Lang, MessagesApi, Messages => msg}
import play.api.libs.json.{JsString, Json}
import javax.inject._
import play.api._
import play.api.db.DBApi
import play.api.mvc._
import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset}
import java.time.ZoneOffset.UTC

import models._

import scala.concurrent.ExecutionContext

@Singleton
class ArticleController @Inject() (
  parsers: PlayBodyParsers,
  cc: ControllerComponents,
  implicit val ec: ExecutionContext,
  implicit val settings: Settings,
  dbApi: DBApi,
  val bloggerRepo: BloggerRepo
) extends AbstractController(cc) with I18nSupport with AuthenticatedSupport with TimeZoneSupport {
  val logger = Logger(getClass)
  val db = dbApi.database("default")

  def articleForm(id: ArticleId, bloggerId: BloggerId)(implicit req: RequestHeader) = {
    Form(
      mapping(
        "title" -> nonEmptyText(1, 256),
        "body" -> nonEmptyText(1, 64 * 1024),
        "publishTime" -> localDateTime("yyyy/MM/dd HH:mm")
      )(
        (title, body, publishTime) => Article(id, bloggerId, title, body, publishTime.toInstant(UTC).toEpochMilli)
      )(
        (article) => Some(
          (article.title, article.body, toLocalDateTime(article.publishTime))
        )
      )
    )
  }

  val removeForm = Form(
    single("id" -> longNumber)
  )

  val authorizeCommentForm = Form(
    single("id" -> longNumber)
  )

  def startCreate = authenticated(parsers.anyContent) { implicit req =>
    db.withConnection { implicit conn =>
      val id = Article.createId()
      val bloggerId = BloggerId(req.user.session.bloggerId)
      Ok(
        views.html.createArticle(
          id.value,
          articleForm(id, bloggerId).fill(Article(id, bloggerId)).discardingErrors
        )
      )
    }
  }

  def create(id: Long) = authenticated(parsers.anyContent) { implicit req =>
    val bloggerId = BloggerId(req.user.session.bloggerId)
    val form = articleForm(ArticleId(id), bloggerId).bindFromRequest

    form.fold(
      formWithError => {
        BadRequest(
          Json.obj(
            "status" -> JsString("ValidationError"),
            "form" -> JsString(views.html.articleForm(id, formWithError, routes.ArticleController.create).toString)
          )
        )
      },
      newArticle => {
        db.withConnection { implicit conn =>
          Article.create(newArticle)
        }
        Ok(
          Json.obj(
            "status" -> JsString("Success")
          )
        )
      }
    )
  }

  def updateStart(id: Long) = authenticated(parsers.anyContent) { implicit req =>
    val bloggerId = BloggerId(req.user.session.bloggerId)
    val articleId = ArticleId(id)

    db.withConnection { implicit conn =>
      Article.get(articleId) match {
        case None =>
          Redirect(routes.HomeController.index()).flashing("message" -> msg("unknownError"))
        case Some(article) =>
          Ok(views.html.updateArticle(id, articleForm(articleId, bloggerId).fill(article)))
      }
    }
  }

  def update(id: Long) = authenticated(parsers.anyContent) { implicit req =>
    val bloggerId = BloggerId(req.user.session.bloggerId)
    val form = articleForm(ArticleId(id), bloggerId).bindFromRequest

    form.fold(
      formWithError => {
        BadRequest(
          Json.obj(
            "status" -> JsString("ValidationError"),
            "form" -> JsString(views.html.articleForm(id, formWithError, routes.ArticleController.update).toString)
          )
        )
      },
      newArticle => {
        db.withConnection { implicit conn =>
          Article.update(newArticle)
        }
        Ok(
          Json.obj(
            "status" -> JsString("Success")
          )
        )
      }
    )
  }

  def remove() = authenticated(parsers.anyContent) { implicit req =>
    removeForm.bindFromRequest.fold(
      formWithError => {
        logger.error("ArticleController.remove() validation error " + formWithError)
        Redirect(routes.HomeController.index()).flashing("message" -> msg("unknownError"))
      },
      id => {
        db.withConnection { implicit conn =>
          Article.remove(ArticleId(id))
        }
        Redirect(routes.HomeController.index()).flashing("message" -> msg("removed", msg("article")))
      }
    )
  }

  def removeComment() = authenticated(parsers.anyContent) { implicit req =>
    removeForm.bindFromRequest.fold(
      formWithError => {
        logger.error("ArticleController.removeComment() validation error " + formWithError)
        Redirect(routes.HomeController.index()).flashing("message" -> msg("unknownError"))
      },
      id => {
        db.withConnection { implicit conn =>
          Comment.remove(CommentId(id))
        }
        Redirect(routes.HomeController.index()).flashing("message" -> msg("removed", msg("comment")))
      }
    )
  }

  def authorizeComment() = authenticated(parsers.anyContent) { implicit req =>
    authorizeCommentForm.bindFromRequest.fold(
      formWithError => {
        logger.error("ArticleController.authorizeComment() validation error " + formWithError)
        Redirect(routes.HomeController.index()).flashing("message" -> msg("unknownError"))
      },
      id => {
        db.withConnection { implicit conn =>
          Comment.authorizeComment(CommentId(id))
        }
        Redirect(routes.HomeController.index()).flashing("message" -> msg("commentAuthorized"))
      }
    )
  }
}
