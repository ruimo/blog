package controllers

import play.api.db.Database
import play.api.Logger
import play.api.mvc.Results._
import play.api.mvc._
import javax.inject._

import scala.concurrent.{ExecutionContext, Future}
import models._
import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc.Security.AuthenticatedBuilder
import play.mvc.Security.AuthenticatedAction

case class LoginBlogger(
  session: LoginSession,
  blogger: Blogger
)

// class MyAuthenticatedBuilder[U, P](
//   userinfo: RequestHeader => Option[U],
//   defaultParser: BodyParser[P],
//   onUnauthorized: RequestHeader => Result= implicit request =>
//     Unauthorized(
//       views.html.defaultpages.unauthorized()
//     )
//   )(
//     implicit val executionContext: ExecutionContext
//   ) extends ActionBuilder[({ type R[A] = AuthenticatedRequest[A, U] })#R, P] {

//   lazy val parser = defaultParser

//   def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A, U]) => Future[Result]) =
//     authenticate(request, block)

//   /**
//     * Authenticate the given block.
//     */
//   def authenticate[A](request: Request[A], block: (AuthenticatedRequest[A, U]) => Future[Result]) = {
//     userinfo(request).map { user =>
//       block(new AuthenticatedRequest(user, request))
//     } getOrElse {
//       Future.successful(onUnauthorized(request))
//     }
//   }
// }

class UserAuthenticatedBuilder @Inject (
  tryLogin: RequestHeader => Option[LoginBlogger],
  defaultParser: BodyParser[AnyContent]
)(implicit ec: ExecutionContext)
  extends AuthenticatedBuilder[LoginBlogger](
    tryLogin, defaultParser, req => Redirect(routes.BloggerController.startLogin(if (req.method.equalsIgnoreCase("get")) req.uri else "/"))
  )
{
}

class UserRequest[A](val user: LoginBlogger, val request: Request[A]) extends WrappedRequest[A](request)

object AuthenticatedActionBuilder {
  def loginBlogger(request: RequestHeader)(implicit db: Database, bloggerRepo: BloggerRepo): Option[LoginBlogger] =
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
}

class AuthenticatedActionBuilder(
  loginBlogger: RequestHeader => Option[LoginBlogger],
  override val parser: BodyParser[AnyContent]
)(
  override implicit val executionContext: ExecutionContext
) extends ActionBuilder[UserRequest, AnyContent] {

  type ResultBlock[A] = (UserRequest[A]) => Future[Result]
  val builder = new UserAuthenticatedBuilder(loginBlogger, parser)

  def invokeBlock[A](request: Request[A], block: ResultBlock[A]): Future[Result] = {
    builder.authenticate(request, { (authRequest: AuthenticatedRequest[A, LoginBlogger]) =>
      block(new UserRequest[A](authRequest.user, request))
    })
  }
}

// trait AuthenticatedSupport {
//   def db: Database
//   def bloggerRepo: BloggerRepo
//   implicit val ec: ExecutionContext

//   def onUnauthorized(request: RequestHeader) = Redirect(
//     routes.BloggerController.startLogin(if (request.method.equalsIgnoreCase("get")) request.uri else "/")
//   )

//   implicit def loginBlogger(implicit request: RequestHeader): Option[LoginBlogger] =
//     LoginSession.retrieveLogin(request) match {
//       case None => None
//       case Some(loginSession) => {
//         if (loginSession.isExpired()) None
//         else db.withConnection { implicit conn =>
//           bloggerRepo.get(BloggerId(loginSession.bloggerId)).map { blogger =>
//             LoginBlogger(loginSession, blogger)
//           }
//         }
//       }
//     }

//   def authenticated[P](parser: BodyParser[P]) = new MyAuthenticatedBuilder[LoginBlogger, P](
//     req => loginBlogger(req),
//     parser,
//     onUnauthorized
//   )
// }

