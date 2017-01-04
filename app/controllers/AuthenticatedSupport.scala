package controllers

import play.api.mvc.Security.AuthenticatedBuilder
import play.api.db.Database
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results._

import play.api.mvc._
import javax.inject._

import scala.concurrent.{ExecutionContext, Future}
import models._

case class LoginBlogger(
  session: LoginSession,
  blogger: Blogger
)

trait AuthenticatedSupport {
  def db: Database
  def bloggerRepo: BloggerRepo

  def onUnauthorized(request: RequestHeader) = Redirect(
    routes.BloggerController.startLogin(if (request.method.equalsIgnoreCase("get")) request.uri else "/")
  )

  implicit def loginBlogger(implicit request: RequestHeader): Option[LoginBlogger] =
    LoginSession.retrieveLogin(request) match {
      case None => None
      case Some(loginSession) => {
        if (loginSession.isExpired()) None
        else db.withConnection { implicit conn =>
          bloggerRepo.get(BloggerId(loginSession.bloggerId)).map { blogger =>
            LoginBlogger(loginSession, blogger)
          }
        }
      }
    }

  def authenticated = new AuthenticatedBuilder[LoginBlogger](
    req => loginBlogger(req),
    onUnauthorized
  )
}

