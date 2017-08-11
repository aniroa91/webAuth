package controllers

import javax.inject._
import play.api.mvc._
import play.api._
//import play.api.mvc._
//import services.DomainService
import utils.JsonUtil
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
//import model.Response
import com.ftel.bigdata.utils.HttpUtil
import com.ftel.bigdata.utils.FileUtil
import com.ftel.bigdata.utils.DomainUtil
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import com.typesafe.config.ConfigFactory
import play.api.data._
import play.api.i18n._
import play.api.mvc._
import com.ftel.bigdata.dns.parameters.Label
import com.ftel.bigdata.utils.DateTimeUtil
import scala.util.Try
import services.domain.ProfileService
import services.domain.AbstractService
import services.AppGlobal

case class SearchData(value: String)

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class ProfileController @Inject() (protected val dbConfigProvider: DatabaseConfigProvider, cc: ControllerComponents)
      extends AbstractController(cc) with HasDatabaseConfigProvider[JdbcProfile]  with I18nSupport {

  val form = Form(
    mapping(
      "value" -> text
    )(SearchData.apply)(SearchData.unapply)
  )

  def index = Action { implicit request: Request[AnyContent] =>
    val formValidationResult = form.bindFromRequest
    if (!formValidationResult.hasErrors) {
      val domain = formValidationResult.get.value
      val secondDomain = DomainUtil.getSecondTopLevel(domain).toLowerCase()
      val response = ProfileService.get(secondDomain)
      Ok(views.html.ace.profile(form, secondDomain.toLowerCase(), response, AppGlobal.getLogoPath(secondDomain)))
    } else {
      Ok(views.html.ace.profile(form, null, null, null))
    }
  }
}


