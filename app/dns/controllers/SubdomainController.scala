package controllers

import javax.inject.Inject
import javax.inject.Singleton
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import services.CacheService
import java.text.NumberFormat
import com.ftel.bigdata.utils.NumberUtil
import com.ftel.bigdata.utils.DomainUtil
import services.domain.CommonService
import com.ftel.bigdata.utils.StringUtil
import play.api.mvc._
/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class SubdomainController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured {

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

  def index(domain: String) =  Authenticated { implicit request =>
    if (StringUtil.isNullOrEmpty(domain)) {
      Ok(dns.views.html.search.subdomain(null, null, null,request.session.get("username").get.toString))
    } else {
      val second = DomainUtil.extract(domain).second
      val logo = CommonService.getLogo(second, false)
      val response = CacheService.getDomain(second)
      Ok(dns.views.html.search.subdomain(domain, response._1, logo, request.session.get("username").get.toString))
    }
  }
}

