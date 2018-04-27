package controllers

//import com.ftel.bigdata.dns.parameters.Label
import play.api.mvc._
import javax.inject.Inject
import javax.inject.Singleton
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import services.domain.CommonService
import services.CacheService

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class RankController @Inject() (cc: ControllerComponents) extends AbstractController(cc) with Secured{
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

  def index = Authenticated { implicit request =>
      Ok(dns.views.html.rank.index(CacheService.getRank()._1,request.session.get("username").get.toString))
  }
}
