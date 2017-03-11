package controllers

import scala.concurrent.Await
import play.api.libs.ws._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.actor.ActorSystem
import play.api.libs.mailer._
import java.io.File
import play.api.libs.json.{Json, JsString}
import play.api.data._
import play.api.data.Forms._
import javax.inject._
import scala.collection.{immutable => imm}
import play.api._
import play.api.i18n.{I18nSupport, Lang, MessagesApi, Messages}
import play.api.mvc._
import models._
import java.sql.Connection
import play.api.db.DBApi
import play.api.i18n.{I18nSupport, Messages => msg}

@Singleton
class HomeController @Inject()(
  val messagesApi: MessagesApi,
  dbApi: DBApi,
  val bloggerRepo: BloggerRepo,
  implicit val settings: Settings,
  actorSystem: ActorSystem,
  mailerClient: MailerClient,
  ws: WSClient
) extends Controller with I18nSupport with AuthenticatedSupport with TimeZoneSupport {
  val db = dbApi.database("default")

  val isRecaptchaValid: String => Boolean = recaptcha => {
    val req = ws.url(settings.recaptcha.url)
      .withQueryString("secret" -> settings.recaptcha.secret, "response" -> recaptcha)
      .get
    (Json.parse(Await.result(req, 10.seconds).body) \ "success").get == "true"
  }

  val commentForm = Form(
    mapping(
      "articleId" -> longNumber,
      "namae" -> optional(text(maxLength = 64)),
      "comment" -> nonEmptyText(1, 1024),
      "g-recaptcha-response" -> nonEmptyText.verifying(
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
        }

        Ok(
          Json.obj(
            "status" -> JsString("Success")
          )
        )
      }
    )
  }

  def showArticle(id: Long) = Action { implicit req =>
    db.withConnection { implicit conn =>
      Ok(
        views.html.showArticleWithComment(
          Article.showWithComment(ArticleId(id)),
          TimeZoneSupport.formatter(msg("publishDateFormatInArticleList")),
          toLocalDateTime(_)(implicitly),
          commentForm
        )
      )
    }
  }
}
