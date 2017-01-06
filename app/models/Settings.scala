package models

import play.api._
import javax.inject._
import scala.collection.{immutable => imm}

case class ExternalLink(name: String, link: String)

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
  val ExternalLinks: imm.Seq[ExternalLink] = conf.getConfigSeq("site.externalLinks").map { el =>
    el.map { e =>
      ExternalLink(
        e.getString("name").getOrElse("No site.externalLinks[].name found in application.conf"),
        e.getString("link").getOrElse("No site.externalLinks[].name found in application.conf")
      )
    }
  }.getOrElse {
    throw new Error("No site.externalLinks found in application.conf")
  }.toList
  val EmailFrom = conf.getString("email.from").getOrElse(
    throw new Error("No email.from.author found in application.conf")
  )
  val EmailTo: imm.Seq[String] = conf.getStringSeq("email.to").map { _.toList }.getOrElse {
    throw new Error("No email.to found in application.conf")
  }
  val TweetScreenNameForArticle: Option[String] = conf.getString("sns.twitter.tweetArticle")
  val FbLikeArticle: Boolean = conf.getBoolean("sns.facebook.likeArticle").getOrElse(false)
  val HatenaArticle: Boolean = conf.getBoolean("sns.hatena.article").getOrElse(false)
  val PocketArticle: Boolean = conf.getBoolean("sns.pocket.article").getOrElse(false)
  val GooglePlusArticle: Boolean = conf.getBoolean("sns.googlePlus.article").getOrElse(false)
}
