package models

import com.typesafe.config.Config
import play.api._
import javax.inject._

import scala.collection.{immutable => imm}

case class ExternalLink(name: String, link: String)
case class Recaptcha(url: String, secret: String)

@Singleton
class Settings @Inject() (
  conf: Configuration
) {
  val SiteTitle:String = conf.getOptional[String]("site.title").getOrElse(
    throw new Error("No site.title found in application.conf")
  )
  val SiteSubTitle: Option[String] = conf.getOptional[String]("site.subTitle")
  val SiteLink: String = conf.getOptional[String]("site.link").getOrElse(
    throw new Error("No site.link found in application.conf")
  )
  val SiteLogo: Option[String] = conf.getOptional[String]("site.logo")
  val Author: String = conf.getOptional[String]("site.author").getOrElse(
    throw new Error("No site.author found in application.conf")
  )
  val ExternalLinks: imm.Seq[ExternalLink] = conf.getOptional[Seq[Configuration]]("site.externalLinks").map { el =>
    el.map { e =>
      ExternalLink(
        e.getOptional[String]("name").getOrElse("No site.externalLinks[].name found in application.conf"),
        e.getOptional[String]("link").getOrElse("No site.externalLinks[].name found in application.conf")
      )
    }
  }.getOrElse {
    throw new Error("No site.externalLinks found in application.conf")
  }.toList
  val EmailFrom = conf.getOptional[String]("email.from").getOrElse(
    throw new Error("No email.from.author found in application.conf")
  )
  val EmailTo: imm.Seq[String] = conf.getOptional[Seq[String]]("email.to").map { _.toList }.getOrElse {
    throw new Error("No email.to found in application.conf")
  }
  val TweetScreenNameForArticle: Option[String] = conf.getOptional[String]("sns.twitter.tweetArticle")
  val FbLikeArticle: Boolean = conf.getOptional[Boolean]("sns.facebook.likeArticle").getOrElse(false)
  val HatenaArticle: Boolean = conf.getOptional[Boolean]("sns.hatena.article").getOrElse(false)
  val PocketArticle: Boolean = conf.getOptional[Boolean]("sns.pocket.article").getOrElse(false)
  val GooglePlusArticle: Boolean = conf.getOptional[Boolean]("sns.googlePlus.article").getOrElse(false)
  val recaptcha = Recaptcha(
    url = conf.getOptional[String]("recaptcha.url").getOrElse(throw new Error("No recaptcha.url specified in application.conf")),
    secret = conf.getOptional[String]("recaptcha.secret").getOrElse(throw new Error("No recaptcha.secret specified in application.conf"))
  )
}
