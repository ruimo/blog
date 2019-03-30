package controllers

import play.api.mvc.MultipartFormData.FilePart
import java.nio.file.{Files, Path}

import scala.sys.process.Process
import java.sql.Types
import java.io.InputStream

import play.api.libs.json.{util => _, _}
import java.sql.Connection

import scala.language.postfixOps
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import java.nio.channels.{Channels, WritableByteChannel}
import java.io.{PipedInputStream, PipedOutputStream}

import play.api.mvc.MultipartFormData
import play.api.libs.streams.Accumulator
import akka.stream.scaladsl.Sink
import play.core.parsers.Multipart
import play.api.http.HttpEntity
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, Lang, MessagesApi, Messages => msg}
import play.api.libs.json.{JsNumber, JsString, Json}
import javax.inject._
import play.api._
import play.api.db.DBApi
import play.api.mvc._
import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset}
import java.time.ZoneOffset.UTC

import models._
import helpers.Jdbc.{closeThrowingNothing, rollbackThrowing}

@Singleton
class FileController @Inject() (
  cc: ControllerComponents,
  implicit val settings: Settings,
  implicit val ec: ExecutionContext,
  dbApi: DBApi,
  parsers: PlayBodyParsers,
  val bloggerRepo: BloggerRepo
) extends AbstractController(cc) with I18nSupport with AuthenticatedSupport with TimeZoneSupport {
  val db = dbApi.database("default")
  val logger = Logger(getClass)

  def index(page: Int, pageSize: Int, orderBySpec: String) = authenticated(parsers.anyContent) { implicit req =>
    Ok(views.html.file(page, pageSize, orderBySpec))
  }

  def fileList(page: Int, pageSize: Int, orderBySpec: String) = authenticated(parsers.anyContent) { implicit req =>
    db.withConnection { implicit conn =>
      Ok(
        views.html.fileList(
          Image.list(page, pageSize, OrderBy(orderBySpec)),
          TimeZoneSupport.formatter(msg("imageDateFormatInImageList")),
          toLocalDateTime(_)(implicitly)
        )
      )
    }
  }

  def create() = authenticated(parse.multipartFormData) { implicit req =>
    db.withConnection { implicit conn =>
      req.body.files.foreach { filePart =>
        val thumbnail = if (isImageFile(filePart)) {
          createThumbnail(filePart.ref.path)
        } else None
        Image.create(filePart.filename, filePart.contentType, filePart.ref.path, thumbnail)
      }
    }
    Ok("")
  }

  def isImageFile(filePart: FilePart[_]): Boolean = filePart.contentType.map {
    case "image/gif" => true
    case "image/jpeg" => true
    case "image/jpg" => true
    case "image/pjpeg" => true
    case "image/x-png" => true
    case "image/png" => true
    case _ => false
  }.getOrElse(false)

  def createThumbnail(file: Path, maxWidth: Int = 120, maxHeight: Int = 120): Option[Path] = {
    val f: Path = Files.createTempFile(null, extention(file))
    val cmd = "convert -geometry " + maxWidth + "x" + maxHeight + " " + file.toAbsolutePath + " " + f.toAbsolutePath
    logger.info("Invoking [" + cmd + "]")
    val rc = (Process(cmd) run) exitValue()
    if (rc == 0) {
      logger.info("[" + cmd + "] success")
      Some(f)
    }
    else {
      logger.error("[" + cmd + "] failed. rc = " + rc)
      None
    }
  }

  def extention(file: Path): String = {
    val fileName = file.getFileName.toString
    val idx = fileName.lastIndexOf('.')
    if (idx == -1) "" else fileName.substring(idx)
  }

//  def create() = Action(parse.multipartFormData(handleFilePartAsFile)) { implicit req =>
//    Logger.info("FileController.create() called " + req.body.files)
//    req.body.files.foreach { filePart =>
//      Logger.info("Upload completed " + filePart)
//    }
//
//    Ok(
//      ""
//    )
//  }

  def getImage(id: Long, thumbnail: Boolean) = Action { implicit req =>
    logger.info("getImage(" + id + ", " + thumbnail + ") called")
    db.withConnection { implicit conn =>
      Image.getImageSize(ImageId(id), thumbnail)
    }.map { case (sizeOpt, fileName, contentType) =>
      sizeOpt.map { size =>
        logger.info("Size for image (id = " + id + ") = " + size)
        val lock = new AnyRef
        val buf = new Array[Byte](32 * 1024)
        val source = Source.unfoldResource[ByteString, (Connection, InputStream)](
          create = () => {
            val conn = db.getConnection()
            try {
              val col = if (thumbnail) "thumbnail" else "body"
              val pstmt = conn.prepareStatement(s"select $col from image where image_id = ?")
              pstmt.setLong(1, id)
              val rs = pstmt.executeQuery()
              if (! rs.next()) {
                logger.error("No image found for id=" + id)
                throw new Error("No image found for id=" + id)
              }
              val is = rs.getBinaryStream(1)
              lock.synchronized {
                (conn, is)
              }
            }
            catch {
              case t: Throwable =>
                closeThrowingNothing(conn)
                throw t
            }
          },
          read = {
            lock.synchronized {
              case (conn, is) =>
                val readSize = is.read(buf)
                if (readSize == -1) None else Some(ByteString.fromArray(buf, 0, readSize))
            }
          },
          close = {
            lock.synchronized {
              case (conn, is) =>
                closeThrowingNothing(conn)
            }
          }
        )
  
        Result(
          header = ResponseHeader(
            200,
            Map(
              "Content-Disposition" -> ("filename=\"" + fileName + "\""),
              "Cache-control" -> "max-age=31536000"
            )
          ),
          body = HttpEntity.Streamed(source, Some(size), contentType)
        )
      }.getOrElse {
        Redirect(routes.Assets.versioned("images/NotFound.png"))
      }
    }.getOrElse {
      NotFound
    }
  }

  def testGet = Action { implicit req =>
    Ok(views.html.test())
  }

  def handleFilePartAsFile: Multipart.FilePartHandler[Long] = {
    val lock = new AnyRef
    val conn: Connection = db.getConnection
    logger.info("Connection obtained. " + conn)

    fi => try {
      logger.info("File uploaded " + fi)
      val pstmt = conn.prepareStatement("""
insert into image(
  image_id, file_name, content_type, body
) values (
  (select nextval('image_seq')), ?, ?, ?
)
      """)
      val is = new PipedInputStream()
      val buf: WritableByteChannel = Channels.newChannel(new PipedOutputStream(is))
      val blobSetter = Future {
        logger.info("setBlob() start.")
        pstmt.setBinaryStream(3, is)
        logger.info("setBlob() end.")
      }

      val sink: Sink[ByteString, Future[Long]] = Sink.fold(0L) { (size, in) => 
        lock.synchronized {
          buf.write(in.asByteBuffer)
          size + in.size
        }
      }

      Accumulator(sink).map { size: Long =>
        try {
          logger.info("Finalizing transaction.")
          buf.close()
          Await.result(blobSetter, 30 seconds)
          lock.synchronized {
            pstmt.setString(1, fi.fileName)
            fi.contentType match {
              case Some(str) => pstmt.setString(2, str)
              case None => pstmt.setNull(2, Types.BLOB)
            }
            pstmt.executeUpdate()
            conn.commit()
            logger.info("Transaction committed.")
            MultipartFormData.FilePart(fi.partName, fi.fileName, fi.contentType, size)
          }
        }
        catch {
          case t: Throwable => throw rollbackThrowing(conn, t)
        }
        finally {
          closeThrowingNothing(conn)
        }
      }
    }
    catch {
      case t: Throwable =>
        logger.error("Error in persisting file.", t)
        lock.synchronized {
          try {
            throw rollbackThrowing(conn, t)
          }
          finally {
            closeThrowingNothing(conn)
          }
        }
    }
  }

  def testPost = Action(parse.multipartFormData(handleFilePartAsFile)) { implicit req =>
    val fileOption = req.body.file("file").map {
      case fp@MultipartFormData.FilePart(key, filename, contentType, ref, size, dispType) =>
        logger.info("Upload completed " + fp + " size = " + size)
    }

    Ok("")
  }

  def test() = Action { implicit req =>
    val res = Array(0L)
    val source = Source.unfoldResource[ByteString, Array[Long]](
      create = () => {
        println("create called.")
        res
      },
      read = r => {
        println("read called. r = " + r)
        if (r(0) < 6) {
          r(0) += 1
          Some(ByteString(r(0).toByte))
        }  else None
      },
      close = l => {
        println("close called")
      }
    )

    Result(
      header = ResponseHeader(200, Map.empty),
      body = HttpEntity.Streamed(source, Some(6), Some("application/octet-stream"))
    )
  }
}
