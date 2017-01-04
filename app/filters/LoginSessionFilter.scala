package filters

import akka.stream.Materializer
import javax.inject._
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import models.LoginSession

@Singleton
class LoginSessionFilter @Inject()(
  implicit override val mat: Materializer,
  exec: ExecutionContext
) extends Filter {
  override def apply(
    nextFilter: RequestHeader => Future[Result]
  )(
    requestHeader: RequestHeader
  ): Future[Result] = {
    nextFilter(requestHeader).map { result =>
      // If an blogger is logged in, postpone login expiration time.
      LoginSession.retrieveLogin(result)(requestHeader) match {
        case Some(loginSession) =>
          result.withSession(requestHeader.session + loginSession.renewExpirationTime().toLoginSessionString)
        case None => result
      }
    }
  }
}
