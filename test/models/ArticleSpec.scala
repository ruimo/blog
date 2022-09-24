package models

import java.util.concurrent.TimeUnit

import helpers.InjectorSupport
import play.api.Application
import play.api.db.Database
import play.api.db.DBApi
import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable._
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import models._

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import com.ruimo.scoins.Scoping._

class ArticleSpec extends Specification with InjectorSupport {
  "Article" should {
    "Can create articles" in {
      implicit val app: Application = GuiceApplicationBuilder().configure(inMemoryDatabase()).build()
      val bloggerRepo = inject[BloggerRepo]

      inject[Database].withConnection { implicit conn =>
        val blogger = bloggerRepo.create(
          "name", "firstName", None, "lastName", "email", 1L, 2L
        )
        val article0 = Article(Article.createId(), blogger.id.get, "title0", "body0", createdTime = 10L, publishTime = 11L)
        Article.create(article0)

        val article1 = Article(Article.createId(), blogger.id.get, "title1", "body1", createdTime = 20L, publishTime = 5L)
        Article.create(article1)

        val article2 = Article(Article.createId(), blogger.id.get, "title2", "body2", createdTime = 30L, publishTime = 20L)
        Article.create(article2)

        val comment0 = Comment.create(article0.id, Some("name0"), "comment0", createdTime = 100L)
        val comment1 = Comment.create(article0.id, Some("name1"), "comment1", createdTime = 10L)
        val comment2 = Comment.create(article0.id, Some("name2"), "comment2", createdTime = 0L)

        val comment3 = Comment.create(article2.id, Some("name3"), "comment3", createdTime = 20L)

        val recs = Article.listWithComment().records
        recs.size === 3
        doWith(recs(0)) { case (a, c) =>
          a.id === article2.id
          c.size === 1
          c(0) === comment3
        }
        doWith(recs(1)) { case (a, c) =>
          a.id === article0.id
          c.size === 3
          c(0) === comment0
          c(1) === comment1
          c(2) === comment2
        }
        doWith(recs(2)) { case (a, c) =>
          a.id === article1.id
          c.size === 0
        }
      }
    }
  }
}

