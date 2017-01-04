import play.filters.csrf.CSRFFilter
import javax.inject._
import play.api._
import play.api.http.DefaultHttpFilters
import play.api.mvc._

import filters.LoginSessionFilter

@Singleton
class Filters @Inject() (
  env: Environment,
  csrfFilter: CSRFFilter,
loginSessionFilter: LoginSessionFilter
) extends DefaultHttpFilters(csrfFilter, loginSessionFilter)
