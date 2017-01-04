package helpers

import java.sql.{Connection, SQLException}
import play.api.Logger

object Jdbc {
  def closeThrowingNothing(conn: Connection) {
    try {
      conn.close()
      Logger.info("Connection closed. " + conn)
    }
    catch {
      case e: SQLException =>
        Logger.error("Connection close error. " + conn)
    }
  }

  def rollbackThrowing(conn: Connection, cause: Throwable): Throwable = try {
    conn.rollback()
    Logger.info("Connection rollbacked. " + conn)
    cause
  }
  catch {
    case e: SQLException => e.addSuppressed(cause); e
  }
}

