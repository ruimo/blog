package models

import java.util.concurrent.TimeUnit

import helpers.InjectorSupport
import play.api.db.DBApi
import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable._
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import models._
import org.specs2.specification.BeforeAfterEach
import play.api.db.Database

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class BloggerSpec extends Specification with InjectorSupport {
  "Blogger" should {
    "Can create blogger" in {
      implicit val app: Application = GuiceApplicationBuilder().configure(inMemoryDatabase()).build()
      val bloggerRepo = inject[BloggerRepo]

      inject[Database].withConnection { implicit conn =>
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
