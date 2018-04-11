package controllers

import com.ftel.bigdata.utils.DomainUtil
import play.api.mvc._
import javax.inject.Inject
import javax.inject.Singleton
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.text
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.i18n.I18nSupport
import play.api.mvc.AbstractController
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.mvc.Request
import services.CacheService
import services.domain.CommonService
import slick.jdbc.JdbcProfile

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class ProfileController @Inject() (protected val dbConfigProvider: DatabaseConfigProvider, cc: ControllerComponents)
      extends AbstractController(cc) with HasDatabaseConfigProvider[JdbcProfile] with Secured {

  val form = Form(
    mapping(
      "ct" -> text
    )(SearchContract.apply)(SearchContract.unapply)
  )

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
    val formValidationResult = form.bindFromRequest
    try {
      if (!formValidationResult.hasErrors) {
        val domain = formValidationResult.get.q.trim()
        val second = DomainUtil.extract(domain).second
        val logo = CommonService.getLogo(second, false)
        val response = CacheService.getDomain(second)
        Ok(views.html.dns_v2.search.index(form, second, response._1, logo, request.session.get("username").get.toString))
      } else {
        Ok(views.html.dns_v2.search.index(form, null, null, null,request.session.get("username").get.toString))
      }
    }
    catch{
      case e: Exception => Ok(views.html.dns_v2.search.index(form, null, null, null,request.session.get("username").get.toString))  }
  }
}


