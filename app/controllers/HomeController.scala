package controllers

import scala.concurrent.{Await, ExecutionContext}
import play.api.libs.ws._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.actor.ActorSystem
import play.api.libs.mailer._
import java.io.File

import play.api.libs.json.{JsString, Json}
import play.api.data._
import play.api.data.Forms._
import javax.inject._

import scala.collection.{immutable => imm}
import play.api._
import play.api.i18n.{I18nSupport, Lang, Messages, MessagesApi}
import play.api.mvc._
import models._
import java.sql.Connection

import helpers.Ogp
import play.api.db.DBApi
import play.api.i18n.{I18nSupport, Messages => msg}

@Singleton
class HomeController @Inject()(
  implicit val ec: ExecutionContext,
  cc: ControllerComponents,
  dbApi: DBApi,
  val bloggerRepo: BloggerRepo,
  implicit val settings: Settings,
  actorSystem: ActorSystem,
  mailerClient: MailerClient,
  ws: WSClient
) extends AbstractController(cc) with I18nSupport with AuthenticatedSupport with TimeZoneSupport {
  val db = dbApi.database("default")
  val logger = Logger(getClass)

  val isRecaptchaValid: String => Boolean = recaptcha => {
    logger.info("recaptcha string: " + recaptcha)
    val req = ws.url(settings.recaptcha.url)
      .post(Map("secret" -> Seq(settings.recaptcha.secret), "response" -> Seq(recaptcha)))
    val jsonResp = Json.parse(Await.result(req, 10.seconds).body)
    logger.info("recaptcha response: " + jsonResp)
    val isSuccess: Boolean = (jsonResp \ "success").as[Boolean]
    logger.info("isSuccess: " + isSuccess)
    isSuccess
  }

  val commentForm = Form(
    mapping(
      "articleId" -> longNumber,
      "namae" -> optional(text(maxLength = 64)),
      "comment" -> nonEmptyText(1, 1024),
      "g-recaptcha-response" -> text.verifying(
        "checkCaptcha", isRecaptchaValid
      )
    )(
      (articleId, name, body, recaptcha) => Comment(None, ArticleId(articleId), name, body, false)
    )(
      (comment) => Some(
        (comment.articleId.value, comment.name, comment.body, "")
      )
    )
  )

  def index(page: Int, pageSize: Int, orderBySpec: String, now: Long) = Action { implicit req =>
    db.withConnection { implicit conn =>
      val recs: PagedRecords[(Article, imm.Seq[Comment])] = Article.listWithComment(page, pageSize, OrderBy(orderBySpec), now)

      Ok(
        views.html.index(
          recs,
          TimeZoneSupport.formatter(msg("publishDateFormatInArticleList")),
          toLocalDateTime(_)(implicitly),
          commentForm
        )
      )
    }
  }

  def fromId(fromId: Long, page: Int, pageSize: Int, orderBySpec: String, now: Long) = Action { implicit req =>
    db.withConnection { implicit conn =>
      val recs: PagedRecords[(Article, imm.Seq[Comment])] =
        Article.listWithCommentFrom(page, pageSize, OrderBy(orderBySpec), now, fromId = ArticleId(fromId))

      Ok(
        views.html.index(
          recs,
          TimeZoneSupport.formatter(msg("publishDateFormatInArticleList")),
          toLocalDateTime(_)(implicitly),
          commentForm
        )
      )
    }
  }

  def postComment(id: Long) = Action { implicit req =>
    val articleId = ArticleId(id)
    val form = commentForm.bindFromRequest
    form.fold(
      formWithError => {
        logger.error("HomeController.postComment(" + id + ") validation error " + formWithError)
        db.withConnection { implicit conn =>
          BadRequest(
            views.html.showArticleWithComment(
              Article.showWithComment(ArticleId(id)),
              TimeZoneSupport.formatter(msg("publishDateFormatInArticleList")),
              toLocalDateTime(_)(implicitly),
              formWithError,
              Ogp.thumbnail(ArticleId(id))
            )
          )
        }
      },
      newComment => {
        db.withConnection { implicit conn =>
          Comment.create(articleId, newComment.name, newComment.body)
        }

        actorSystem.scheduler.scheduleOnce(0.second) {
          val email = Email(
            Messages("postCommentNotification"),
            settings.EmailFrom,
            settings.EmailTo,
            bodyText = Some(views.html.mail.commentPosted(articleId, newComment).toString)
          )
          mailerClient.send(email)
        }(ec)

        Redirect(
          routes.HomeController.showArticleById(id)
        ).flashing("message" -> Messages("commentPosted"))
      }
    )
  }

  def postCommentJson(id: Long) = Action { implicit req =>
    val articleId = ArticleId(id)
    val form = commentForm.bindFromRequest
    form.fold(
      formWithError => {
        BadRequest(
          Json.obj(
            "status" -> JsString("ValidationError"),
            "form" -> JsString(views.html.commentEntry(articleId, formWithError).toString)
          )
        )
      },
      newComment => {
        db.withConnection { implicit conn =>
          Comment.create(articleId, newComment.name, newComment.body)
        }

        actorSystem.scheduler.scheduleOnce(0.second) {
          val email = Email(
            Messages("postCommentNotification"),
            settings.EmailFrom,
            settings.EmailTo,
            bodyText = Some(views.html.mail.commentPosted(articleId, newComment).toString)
          )
          mailerClient.send(email)
        }(ec)

        Ok(
          Json.obj(
            "status" -> JsString("Success")
          )
        )
      }
    )
  }

  def showArticle(id: Long) = Action { implicit req =>
    Redirect(
      routes.HomeController.showArticleById(id)
    )
  }

  def showArticleById(id: Long) = Action { implicit req =>
    db.withConnection { implicit conn =>
      Ok(
        views.html.showArticleWithComment(
          Article.showWithComment(ArticleId(id)),
          TimeZoneSupport.formatter(msg("publishDateFormatInArticleList")),
          toLocalDateTime(_)(implicitly),
          commentForm,
          Ogp.thumbnail(ArticleId(id))
        )
      )
    }
  }
}
