package controllers

import javax.inject.Inject
import javax.inject.Singleton
import play.api.mvc._
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import services.CacheService
import services.domain.CommonService

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */

case class AuthenticatedRequest (val username: String, request: Request[AnyContent])
  extends WrappedRequest(request)

@Singleton
class DashboardController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured{

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

  def index =  Authenticated { implicit request =>
    Ok(views.html.dns_v2.dashboard.index(CacheService.getDaskboard()._1,request.session.get("username").get.toString))
  }
}

