package models

import play.api._
import javax.inject._

@Singleton
class Settings @Inject() (
  conf: Configuration
) {
  val SiteTitle:String = conf.getString("site.title").getOrElse(
    throw new Error("No site.title found in application.conf")
  )
  val SiteSubTitle: Option[String] = conf.getString("site.subTitle")
  val SiteLink: String = conf.getString("site.link").getOrElse(
    throw new Error("No site.link found in application.conf")
  )
  val SiteLogo: Option[String] = conf.getString("site.logo")
  val Author: String = conf.getString("site.author").getOrElse(
    throw new Error("No site.author found in application.conf")
  )
}
