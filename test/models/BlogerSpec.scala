package models

import java.util.concurrent.TimeUnit

import play.api.db.DBApi
import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable._
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import models._
import org.specs2.specification.BeforeAfterEach

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class BloggerSpec extends Specification {
  "Blogger" should {
    "Can create blogger" in {
      val app = new GuiceApplicationBuilder().configure(
        "db.default.url" -> "jdbc:h2:mem:test;DATABASE_TO_UPPER=false;TRACE_LEVEL_SYSTEM_OUT=2"
      ).build()
      val dbApi  = app.injector.instanceOf[DBApi]
      val db = dbApi.database("default")
      val bloggerRepo = app.injector.instanceOf[BloggerRepo]

      db.withConnection { implicit conn =>
        val blogger = bloggerRepo.create(
          "name", "firstName", None, "lastName", "email", 1L, 2L
        )
        val list = bloggerRepo.list()
        list.records.size === 1
        list.records(0) === blogger
      }
    }
  }
}
