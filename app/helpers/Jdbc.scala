package helpers

import java.sql.{Connection, SQLException}
import play.api.Logger

object Jdbc {
  val logger = Logger(getClass)

  def closeThrowingNothing(conn: Connection) {
    try {
      conn.close()
      logger.info("Connection closed. " + conn)
    }
    catch {
      case e: SQLException =>
        logger.error("Connection close error. " + conn)
    }
  }

  def rollbackThrowing(conn: Connection, cause: Throwable): Throwable = try {
    conn.rollback()
    logger.info("Connection rollbacked. " + conn)
    cause
  }
  catch {
    case e: SQLException => e.addSuppressed(cause); e
  }
}

