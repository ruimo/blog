package models

import anorm._
import anorm.SqlParser
import java.sql.Connection
import javax.inject._
import helpers.PasswordHash

case class BloggerId(value: Long) extends AnyVal

case class Blogger(
  id: Option[BloggerId],
  name: String,
  firstName: String,
  middleName: Option[String],
  lastName: String,
  email: String,
  passwordHash: Long,
  salt: Long,
  deleted: Boolean
)

@Singleton
class BloggerRepo @Inject() (
  passwordHash: PasswordHash
) {
  val simple = {
    SqlParser.get[Option[Long]]("blogger.blogger_id") ~
    SqlParser.get[String]("blogger.blogger_name") ~
    SqlParser.get[String]("blogger.first_name") ~
    SqlParser.get[Option[String]]("blogger.middle_name") ~
    SqlParser.get[String]("blogger.last_name") ~
    SqlParser.get[String]("blogger.email") ~
    SqlParser.get[Long]("blogger.password_hash") ~
    SqlParser.get[Long]("blogger.salt") ~
    SqlParser.get[Boolean]("blogger.deleted") map {
      case id~name~firstName~middleName~lastName~email~passwordHash~salt~deleted => Blogger(
        id.map(BloggerId.apply), name, firstName, middleName, lastName, email, passwordHash, salt, deleted
      )
    }
  }

  val AdminName = "administrator"

  def create(
    name: String, firstName: String, middleName: Option[String] = None, lastName: String,
    email: String, passwordHash: Long, salt: Long
  )(implicit conn: Connection): Blogger = {
    SQL(
      """
      insert into blogger(
        blogger_id, blogger_name, first_name, middle_name, last_name, email, password_hash, salt, deleted
      ) values (
        (select nextval('blogger_seq')),
        {name}, {firstName}, {middleName}, {lastName}, {email}, {passwordHash}, {salt}, false
      )
      """
    ).on(
      "name" -> name,
      "firstName" -> firstName,
      "middleName" -> middleName,
      "lastName" -> lastName,
      "email" -> email,
      "passwordHash" -> passwordHash,
      "salt" -> salt
    ).executeUpdate()

    val id = SQL("select currval('blogger_seq')").as(SqlParser.scalar[Long].single)
    Blogger(
      Some(BloggerId(id)), name, firstName, middleName, lastName, email, passwordHash, salt, false
    )
  }

  def list(
    page: Int = 0, pageSize: Int = 10, orderBy: OrderBy = OrderBy("blogger_name", Asc)
  )(
    implicit conn: Connection
  ): PagedRecords[Blogger] = {
    import scala.language.postfixOps

    val offset: Int = pageSize * page
    val records: Seq[Blogger] = SQL(
      s"select * from blogger where deleted = false order by $orderBy limit {pageSize} offset {offset}"
    ).on(
      "pageSize" -> pageSize,
      "offset" -> offset
    ).as(
      simple.*
    )

    val count = SQL(
      "select count(*) from blogger where deleted = false"
    ).as(SqlParser.scalar[Long].single)
      
    PagedRecords(page, pageSize, (count + pageSize - 1) / pageSize, orderBy, records)
  }

  def count()(implicit conn: Connection): Long = SQL(
    "select count(*) from blogger"
  ).as(
    SqlParser.scalar[Long].single
  )

  def get(id: BloggerId)(implicit conn: Connection): Option[Blogger] = SQL(
    "select * from blogger where blogger_id = {id}"
  ).on(
    "id" -> id.value
  ).as(
    simple.singleOpt
  )

  def login(name: String, password: String)(implicit conn: Connection): Option[Blogger] = SQL(
    "select * from blogger where blogger_name = {name}"
  ).on(
    "name" -> name
  ).as(
    simple.singleOpt
  ).flatMap { rec =>
    if (passwordHash.generate(password, rec.salt) == rec.passwordHash) Some(rec) else None
  }

  def changePassword(
    id: BloggerId, currentPassword: String, newPassword: String
  )(
    implicit conn: Connection
  ): Boolean = {
    get(id) match {
      case None => false
      case Some(blogger) =>
        if (passwordHash.generate(currentPassword, blogger.salt) == blogger.passwordHash) {
          val (salt, hash) = passwordHash.generateWithSalt(newPassword)
          SQL(
            """
            update blogger set
              password_hash = {hash},
              salt = {salt}
            where blogger_id = {id}
            """
          ).on(
            "hash" -> hash,
            "salt" -> salt,
            "id" -> blogger.id.get.value
          ).executeUpdate()
          true
        }
        else false
    }
  }
}
