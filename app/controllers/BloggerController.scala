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
import play.api.db.Database

case class Login(userName: String, password: String)
object Login {
  def unapply(u: Login): Option[(String, String)] = Some((u.userName, u.password))
}
case class ChangePassword(currentPassword: String, newPasswords: (String, String))
object ChangePassword {
  def unapply(u: ChangePassword): Option[(String, (String, String))] = Some((u.currentPassword, u.newPasswords))
}

@Singleton
class BloggerController @Inject() (
  dbApi: DBApi,
  passwordHash: PasswordHash,
  cc: ControllerComponents,
  parsers: PlayBodyParsers,
  implicit val bloggerRepo: BloggerRepo,
  implicit val settings: Settings,
  implicit val ec: ExecutionContext,
) extends AbstractController(cc) with I18nSupport {
  implicit val db: Database = dbApi.database("default")
  val logger = Logger(getClass)
  val authenticated = new AuthenticatedActionBuilder(AuthenticatedActionBuilder.loginBlogger, parsers.anyContent)

  val loginForm = Form[Login](
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

  def startLogin(url: String) = Action { implicit req: Request[AnyContent] =>
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

  def login(url: String) = Action { implicit req: Request[AnyContent] =>
    loginForm.bindFromRequest().fold(
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

  def logoff = authenticated(parsers.anyContent) { implicit req: UserRequest[AnyContent] =>
    Redirect(routes.HomeController.index()).withSession(req.session - LoginSession.LoginSessionKey)
  }

  def changePasswordStart = authenticated(parsers.anyContent) { implicit req: UserRequest[AnyContent] =>
    implicit val loggedInBlogger = Some(req.user)
    Ok(views.html.changePassword(changePasswordForm))
  }

  def changePassword = authenticated(parsers.anyContent) { implicit req: UserRequest[AnyContent] =>
    implicit val loggedInBlogger = Some(req.user)

    changePasswordForm.bindFromRequest().fold(
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
