package models

import java.time.Instant
import java.io.{InputStream, FileInputStream}
import com.ruimo.scoins.LoanPattern._
import java.time.Instant
import java.nio.file.Path
import anorm._
import java.sql.Connection
import com.ruimo.scoins.ImmutableByteArray

case class ImageId(value: Long) extends AnyVal

case class Image(
  id: Option[ImageId] = None,
  fileName: String,
  contentType: Option[String],
  createdTime: Long = System.currentTimeMillis
)

object Image {
  val simple = {
    SqlParser.get[Option[Long]]("image.image_id") ~
    SqlParser.get[String]("image.file_name") ~
    SqlParser.get[Option[String]]("image.content_type") ~
    SqlParser.get[Instant]("image.created_time") map {
      case id~fileName~contentType~createdTime => Image(
        id.map(ImageId.apply), fileName, contentType, createdTime.toEpochMilli
      )
    }
  }

  def get(id: ImageId)(implicit conn: Connection): Option[Image] = SQL(
    "select * from image where image_id={id}"
  ).on(
    'id -> id.value
  ).as(
    simple.singleOpt
  )

  def list(page: Int, pageSize: Int, orderBy: OrderBy)(implicit conn: Connection): PagedRecords[Image] = {
    import scala.language.postfixOps
    val offset: Int = pageSize * page
    val records: Seq[Image] = SQL(
      s"""
      select * from image order by $orderBy limit {pageSize} offset {offset}
      """
    ).on(
      'pageSize -> pageSize,
      'offset -> offset
    ).as(
      simple *
    )

    val count = SQL(
      "select count(*) from image"
    ).as(SqlParser.scalar[Long].single)
      
    PagedRecords(page, pageSize, (count + pageSize - 1) / pageSize, orderBy, records)
  }

  def create(
    fileName: String, contentType: Option[String], body: Path, thumbnail: Option[Path], createdTime: Long = System.currentTimeMillis
  )(implicit conn: Connection): ImageId =
    using(new FileInputStream(body.toFile)) { bodyIs =>
      thumbnail match {
        case Some(tf) =>
          using(new FileInputStream(tf.toFile)) { thumIs =>
            create(fileName, contentType, bodyIs, Some(thumIs), createdTime)
          }.get
        case None =>
          create(fileName, contentType, bodyIs, None, createdTime)
      }
    }.get

  def create(
    fileName: String, contentType: Option[String], body: InputStream, thumbnail: Option[InputStream], createdTime: Long
  )(implicit conn: Connection): ImageId = {
    SQL(
      """
          insert into image(
            image_id, file_name, content_type, body, thumbnail, created_time
          ) values (
            (select nextval('image_seq')),
            {fileName}, {contentType}, {body}, {thumbnail}, {createdTime}
          )
          """
    ).on(
      'fileName -> fileName,
      'contentType -> contentType,
      'body -> body,
      'thumbnail -> thumbnail,
      'createdTime -> Instant.ofEpochMilli(createdTime)
    ).executeUpdate()

    ImageId(SQL("select currval('image_seq')").as(SqlParser.scalar[Long].single))
  }

  def getImageSize(id: ImageId, thumbnail: Boolean)(implicit conn: Connection): Option[(Option[Long], String, Option[String])] = {
    import scala.language.postfixOps
    val col = if (thumbnail) "thumbnail" else "body"
    SQL(
      s"""
      select file_name, content_type, LENGTH($col) len from image where image_id={id}
      """
    ).on(
      'id -> id.value
    ).as(
      SqlParser.get[Option[Long]]("len") ~ SqlParser.str("file_name") ~ SqlParser.get[Option[String]]("content_type") map (SqlParser.flatten) singleOpt
    )
  }
}
