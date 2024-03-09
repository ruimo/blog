package models

import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import play.api.mvc.RequestHeader
import play.api.mvc.Result

case class LoginSession(bloggerId: Long, expirationTime: Long) {
  val serialized = s"${bloggerId};${expirationTime}"
  def isExpired(now: Long = System.currentTimeMillis): Boolean = now > expirationTime
  def renewExpirationTime(now: Long = System.currentTimeMillis): LoginSession = copy(
    expirationTime = now + LoginSession.SessionTimeout.toMillis
  )
  def toLoginSessionString: (String, String) = LoginSession.LoginSessionKey -> serialized
}

object LoginSession {
  val LoginSessionKey: String = "login"
  val SessionTimeout: Duration = 6.hour

  def retrieveLogin(requestHeader: RequestHeader): Option[LoginSession] =
    requestHeader.session.get(LoginSessionKey).map(LoginSession.apply)

  def retrieveLogin(result: Result)(implicit requestHeader: RequestHeader): Option[LoginSession] =
    result.session.get(LoginSessionKey).map(LoginSession.apply)

  def loginSessionString(bloggerId: Long, now: Long = System.currentTimeMillis): (String, String) =
    LoginSession(bloggerId, 0).renewExpirationTime(now).toLoginSessionString

  def apply(sessionString: String): LoginSession = {
    val args = sessionString.split(';').map(_.toLong)
    LoginSession(args(0), args(1))
  }
}

