package models

import helpers.InjectorSupport
import play.api.Application
import play.api.db.Database
import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable._
import play.api.inject.guice.GuiceApplicationBuilder

import models._

import scala.concurrent.duration._
import com.ruimo.scoins.Scoping._

class ArticleTagSpec extends Specification with InjectorSupport {
  "ArticleTag" should {
    "Can query empty" in {
      implicit val app: Application = GuiceApplicationBuilder().configure(inMemoryDatabase()).build()
      val bloggerRepo = inject[BloggerRepo]

      inject[Database].withConnection { implicit conn =>
        val blogger = bloggerRepo.create(
          "name", "firstName", None, "lastName", "email", 1L, 2L
        )
        val article0 = Article(Article.createId(), blogger.id.get, "title0", "body0", createdTime = 10L, publishTime = 11L)
        Article.create(article0)

        ArticleTag.list(article0.id).isEmpty === true
      }
    }

    "Can query single" in {
      implicit val app: Application = GuiceApplicationBuilder().configure(inMemoryDatabase()).build()
      val bloggerRepo = inject[BloggerRepo]

      inject[Database].withConnection { implicit conn =>
        val blogger = bloggerRepo.create(
          "name", "firstName", None, "lastName", "email", 1L, 2L
        )
        val article0 = Article(Article.createId(), blogger.id.get, "title0", "body0", createdTime = 10L, publishTime = 11L)
        Article.create(article0)

        ArticleTag.create(article0.id, "tag0")

        val tags = ArticleTag.list(article0.id)
        tags.size === 1
        tags(0).tagName === "tag0"
      }
    }

    "Can query tags" in {
      implicit val app: Application = GuiceApplicationBuilder().configure(inMemoryDatabase()).build()
      val bloggerRepo = inject[BloggerRepo]

      inject[Database].withConnection { implicit conn =>
        val blogger = bloggerRepo.create(
          "name", "firstName", None, "lastName", "email", 1L, 2L
        )
        val article0 = Article(Article.createId(), blogger.id.get, "title0", "body0", createdTime = 10L, publishTime = 11L)
        Article.create(article0)

        val article1 = Article(Article.createId(), blogger.id.get, "title1", "body1", createdTime = 10L, publishTime = 11L)
        Article.create(article1)

        val article2 = Article(Article.createId(), blogger.id.get, "title2", "body2", createdTime = 10L, publishTime = 11L)
        Article.create(article2)

        ArticleTag.create(article0.id, "tag0")
        ArticleTag.create(article1.id, "tag0")
        ArticleTag.create(article2.id, "tag1")

        val withoutFilter = Article.list()
        withoutFilter.records.size === 3

        doWith(Article.list(tagName = Some("tag0"))) { result =>
          result.records.size === 2
          result.records(0).id === article1.id
          result.records(1).id === article0.id
        }

        doWith(Article.list(tagName = Some("tag1"))) { result =>
          result.records.size === 1
          result.records(0).id === article2.id
        }

        doWith(Article.list(tagName = Some("tag2"))) { result =>
          result.records.size === 0
        }
      }
    }

    "Can query tags from" in {
      implicit val app: Application = GuiceApplicationBuilder().configure(inMemoryDatabase()).build()
      val bloggerRepo = inject[BloggerRepo]

      inject[Database].withConnection { implicit conn =>
        val blogger = bloggerRepo.create(
          "name", "firstName", None, "lastName", "email", 1L, 2L
        )
        val article0 = Article(Article.createId(), blogger.id.get, "title0", "body0", createdTime = 10L, publishTime = 11L)
        Article.create(article0)

        val article1 = Article(Article.createId(), blogger.id.get, "title1", "body1", createdTime = 10L, publishTime = 22L)
        Article.create(article1)

        val article2 = Article(Article.createId(), blogger.id.get, "title2", "body2", createdTime = 10L, publishTime = 33L)
        Article.create(article2)

        val article3 = Article(Article.createId(), blogger.id.get, "title3", "body3", createdTime = 10L, publishTime = 44L)
        Article.create(article3)

        ArticleTag.create(article0.id, "tag0")
        ArticleTag.create(article1.id, "tag0")
        ArticleTag.create(article2.id, "tag1")
        ArticleTag.create(article3.id, "tag0")

        doWith(Article.listFrom(fromId = article3.id, tagName = Some("tag0"))) { result =>
          result.records.size === 3
          result.records(0).id === article3.id
          result.records(1).id === article1.id
          result.records(2).id === article0.id
        }

        doWith(Article.listFrom(fromId = article2.id, tagName = Some("tag0"))) { result =>
          result.records.size === 2
          result.records(0).id === article1.id
          result.records(1).id === article0.id
        }
      
        doWith(Article.listFrom(fromId = article0.id, tagName = Some("tag0"))) { result =>
          result.records.size === 1
          result.records(0).id === article0.id
        }
      }
    }

    "Can query tags with comment" in {
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

        ArticleTag.create(article0.id, "tag0")
        ArticleTag.create(article1.id, "tag0")
        ArticleTag.create(article2.id, "tag1")

        val comment0 = Comment.create(article0.id, Some("name0"), "comment0", createdTime = 100L)
        val comment1 = Comment.create(article0.id, Some("name1"), "comment1", createdTime = 10L)
        val comment2 = Comment.create(article0.id, Some("name2"), "comment2", createdTime = 0L)

        val comment3 = Comment.create(article2.id, Some("name3"), "comment3", createdTime = 20L)

        doWith(Article.listWithComment(tagName = Some("tag0")).records) { recs =>
          recs.size === 2
          doWith(recs(0)) { case (a, c) =>
            a.id === article0.id
            c.size === 3
            c(0) === comment0
            c(1) === comment1
            c(2) === comment2
          }
          doWith(recs(1)) { case (a, c) =>
            a.id === article1.id
            c.size === 0
          }
        }
      }
    }
  }
}
