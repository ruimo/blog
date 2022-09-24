package helpers

import play.api.Application
import scala.reflect.ClassTag
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._

trait InjectorSupport {
  Option(System.getenv("GECKO_DRIVER_PATH")) match {
    case None => println(
      "*** You do not set environment variable GECKO_DRIVER_PATH. You cannot use FIREFOX for test ***"
    )
    case Some(p) => System.setProperty("webdriver.gecko.driver", p)
  }

  Option(System.getenv("CHROME_DRIVER_PATH")) match {
    case None => println(
      "*** You do not set environment variable CHROME_DRIVER_PATH. You cannot use CHROME for test ***"
    )
    case Some(p) => System.setProperty("webdriver.chrome.driver", p)
  }

  def appl(conf: Map[String, Any] = inMemoryDatabase()): Application = GuiceApplicationBuilder().configure(conf).build()

  def inject[T](implicit app: Application, c: ClassTag[T]): T = app.injector.instanceOf[T]
}
