package controllers

import play.api.db.Database
import play.api.Logger
import play.api.mvc.Results._
import play.api.mvc._
import javax.inject._

import scala.concurrent.{ExecutionContext, Future}
import models._
import play.api.mvc.Security.AuthenticatedRequest

case class LoginBlogger(
  session: LoginSession,
  blogger: Blogger
)

class MyAuthenticatedBuilder[U, P](
  userinfo: RequestHeader => Option[U],
  defaultParser: BodyParser[P],
  onUnauthorized: RequestHeader => Result= implicit request =>
    Unauthorized(
      views.html.defaultpages.unauthorized()
    )
  )(
    implicit val executionContext: ExecutionContext
  ) extends ActionBuilder[({ type R[A] = AuthenticatedRequest[A, U] })#R, P] {

  lazy val parser = defaultParser

  def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A, U]) => Future[Result]) =
    authenticate(request, block)

  /**
    * Authenticate the given block.
    */
  def authenticate[A](request: Request[A], block: (AuthenticatedRequest[A, U]) => Future[Result]) = {
    userinfo(request).map { user =>
      block(new AuthenticatedRequest(user, request))
    } getOrElse {
      Future.successful(onUnauthorized(request))
    }
  }
}

trait AuthenticatedSupport {
  def db: Database
  def bloggerRepo: BloggerRepo
  implicit val ec: ExecutionContext

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

  def authenticated[P](parser: BodyParser[P]) = new MyAuthenticatedBuilder[LoginBlogger, P](
    req => loginBlogger(req),
    parser,
    onUnauthorized
  )
}

