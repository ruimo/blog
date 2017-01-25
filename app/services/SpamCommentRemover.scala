package services

import javax.inject.Inject
import javax.inject.Singleton

import java.util.Calendar

import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.libs.mailer._
import play.api.db.DBApi
import play.api.Logger
import anorm._

@Singleton
class SpamCommentRemover @Inject() (
  dbApi: DBApi,
  system: ActorSystem
) {
  val db = dbApi.database("default")

  system.scheduler.schedule(0.microseconds, 60.minutes) {
    db.withConnection { implicit conn =>
      SQL(
        """
        delete from comment where body like '%[/url]%'
        """
      ).executeUpdate()
    }
  }
}
