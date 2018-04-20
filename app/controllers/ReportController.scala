package controllers

import play.api.mvc._
import javax.inject.Inject
import javax.inject.Singleton
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import services.CacheService
import services.domain.CommonService

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class ReportController @Inject() (cc: ControllerComponents) extends AbstractController(cc) with Secured{
  /* Authentication action*/
  def Authenticated(f: AuthenticatedRequest => Result) = {
    Action { request =>
      val username = request.session.get("username").get.toString
      username match {
        case "btgd@ftel" =>
          f(AuthenticatedRequest(username, request))
        case none =>
          Redirect(routes.LoginController.index).withNewSession.flashing(
            "success" -> "You are now logged out."
          )
      }
    }
  }

  def index(day: String) = Authenticated { implicit request =>
    val key = if (CommonService.isDayValid(day)) {
      day
    } else {
      CommonService.getLatestDay()
    }
    val response = CacheService.getReport(key)
    Ok(views.html.dns_v2.report.index(key, response._1,request.session.get("username").get.toString))
  }
}