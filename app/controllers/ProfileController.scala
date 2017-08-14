package controllers

import com.ftel.bigdata.utils.DomainUtil

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

case class SearchData(q: String)

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class ProfileController @Inject() (protected val dbConfigProvider: DatabaseConfigProvider, cc: ControllerComponents)
      extends AbstractController(cc) with HasDatabaseConfigProvider[JdbcProfile]  with I18nSupport {

  val form = Form(
    mapping(
      "q" -> text
    )(SearchData.apply)(SearchData.unapply)
  )

  def index = Action { implicit request: Request[AnyContent] =>
    val formValidationResult = form.bindFromRequest
    if (!formValidationResult.hasErrors) {
      val domain = formValidationResult.get.q
      val secondDomain = DomainUtil.getSecondTopLevel(domain).toLowerCase()
      val logo = CommonService.getImageTag(secondDomain)
      val response = CacheService.getDomain(secondDomain)
      Ok(views.html.ace.profile(form, secondDomain, response, logo))
    } else {
      Ok(views.html.ace.profile(form, null, null, null))
    }
  }
}


