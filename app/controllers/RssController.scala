package controllers

import akka.util.ByteString
import play.twirl.api.Xml
import play.api.libs.json.{Json, JsString}
import play.api.data._
import play.api.data.Forms._
import javax.inject._
import scala.collection.{immutable => imm}
import play.api._
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.mvc._
import models._
import java.sql.Connection
import play.api.db.DBApi
import play.api.i18n.{I18nSupport, Messages => msg}
import play.api.http.Writeable

@Singleton
class RssController @Inject()(
  val messagesApi: MessagesApi,
  dbApi: DBApi,
  val bloggerRepo: BloggerRepo,
  settings: Settings
) extends Controller with I18nSupport with AuthenticatedSupport with TimeZoneSupport {
  val db = dbApi.database("default")

  val xmlWriteable = new Writeable[Xml](
    xml => ByteString("""<?xml version="1.0" encoding="UTF-8"?>""" + xml.toString, ByteString.UTF_8),
    Some("text/xml")
  )

  def atom(page: Int, pageSize: Int, orderBySpec: String, now: Long) = Action { implicit req =>
    db.withConnection { implicit conn =>
      val recs: PagedRecords[Article] = Article.list(page, pageSize, OrderBy(orderBySpec), now)
      Ok(
        views.xml.atom(recs, settings)
      )(xmlWriteable)
    }
  }
}
