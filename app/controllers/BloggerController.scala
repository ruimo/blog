package controllers

import play.api.i18n.Messages
import java.net.URLDecoder

import play.api.data._
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.libs.json.{JsString, Json}
import javax.inject._
import play.api._
import play.api.db.DBApi
import play.api.mvc._
import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset}
import java.time.ZoneOffset.UTC

import helpers.PasswordHash
import helpers.Sanitizer
import models._

import scala.concurrent.ExecutionContext

case class Login(userName: String, password: String)
case class ChangePassword(currentPassword: String, newPasswords: (String, String))

@Singleton
class BloggerController @Inject() (
  parsers: PlayBodyParsers,
  implicit val settings: Settings,
  implicit val ec: ExecutionContext,
  dbApi: DBApi,
  val bloggerRepo: BloggerRepo,
  passwordHash: PasswordHash,
  cc: ControllerComponents
) extends AbstractController(cc) with I18nSupport with AuthenticatedSupport {
  val db = dbApi.database("default")
  val logger = Logger(getClass)

  val loginForm = Form(
    mapping(
      "bloggerName" -> text(minLength = 8, maxLength = 24),
      "password" -> text(minLength = 8, maxLength = 24)
    )(Login.apply)(Login.unapply)
  )

  def changePasswordForm = Form(
    mapping(
      "currentPassword" -> text(minLength = 8, maxLength = 24),
      "newPasswords" -> tuple(
        "main" -> text(minLength = 8, maxLength = 24),
        "confirm" -> text
      ).verifying(
        "confirmPasswordDoesNotMatch", passwords => passwords._1 == passwords._2
      )
    )(ChangePassword.apply)(ChangePassword.unapply)
  )

  def startLogin(url: String) = Action { implicit req =>
    logger.info("StartLogin(" + url + ")")
    db.withConnection { implicit conn =>
      val count = bloggerRepo.count()
      if (count == 0) {
        logger.info("No bloggers found. Creating first blogger.")
        val password = passwordHash.password()
        val (salt, hash) = passwordHash.generateWithSalt(password)
        val blogger = bloggerRepo.create(
          name = bloggerRepo.AdminName, "admin", None, "manager", "set@your.email", hash, salt
        )

        logger.info("--------------------")
        logger.info("Your first blogger '" + blogger.name + "' has password '" + password + "'")
        logger.info("--------------------")

        Ok(
          views.html.login(
            loginForm.fill(
              Login(bloggerRepo.AdminName, "")
            ).discardingErrors.withGlobalError(Messages("checkLogFileForAdminPassword")),
            url
          )
        )
      }
      else {
        Ok(views.html.login(loginForm, url))
      }
    }
  }

 def login(url: String) = Action { implicit req =>
    loginForm.bindFromRequest.fold(
      formWithError => {
        logger.error("UserController.login validation error " + formWithError)
        BadRequest(views.html.login(formWithError, url))
      },
      login => db.withConnection { implicit conn =>
        bloggerRepo.login(login.userName, login.password) match {
          case None => BadRequest(
            views.html.login(loginForm.fill(login).withGlobalError(Messages("nameAndPasswordNotMatched")), url)
          )
          case Some(blogger) =>
            Redirect(Sanitizer.forUrl(url)).withSession(req.session + LoginSession.loginSessionString(blogger.id.get.value))
        }
      }
    )
  }

  def logoff = authenticated(parsers.anyContent) { implicit req =>
    Redirect(routes.HomeController.index()).withSession(req.session - LoginSession.LoginSessionKey)
  }

  def changePasswordStart = authenticated(parsers.anyContent) { implicit req =>
    Ok(views.html.changePassword(changePasswordForm))
  }

  def changePassword = authenticated(parsers.anyContent) { implicit req =>
    changePasswordForm.bindFromRequest.fold(
      formWithError => {
        logger.error("BloggerController.changePassword validation error: " + formWithError)
        BadRequest(
          views.html.changePassword(formWithError)
        )
      },
      newPassword => db.withConnection { implicit conn =>
        if (
          bloggerRepo.changePassword(
            BloggerId(req.user.session.bloggerId),
            newPassword.currentPassword, newPassword.newPasswords._1
          )
        ) Redirect(routes.HomeController.index()).flashing("message" -> Messages("paswordChanged"))
        else BadRequest(
          views.html.changePassword(
            changePasswordForm.withGlobalError("passwordDoesnotMatch")
          )
        )
      }
    )
  }
}
