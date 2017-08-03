package controllers

import javax.inject._
import play.api.mvc._
import play.api._
//import play.api.mvc._
import services.DomainService
import utils.JsonUtil
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import model.Response
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

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class DnsController @Inject() (protected val dbConfigProvider: DatabaseConfigProvider,
    cc: ControllerComponents) extends AbstractController(cc) with HasDatabaseConfigProvider[JdbcProfile]  with I18nSupport {

  val form = Form(
    mapping(
      "domain" -> text
    )(SearchData.apply)(SearchData.unapply)
  )

  def getDomainDetail(domain: String) = Action {
    val secondDomain = DomainUtil.getSecondTopLevel(domain)
    val response = DomainService.getDomainInfo(secondDomain)
    if (response != null) {
      Ok(JsonUtil.prettyJson(response.toJsonObject))
    } else Ok("{}")
  }

  def search = Action { implicit request: Request[AnyContent] =>
    val formValidationResult = form.bindFromRequest
    if (!formValidationResult.hasErrors) {
      val domain = formValidationResult.get.domain
      val secondDomain = DomainUtil.getSecondTopLevel(domain).toLowerCase()
      val response = DomainService.getDomainInfo(secondDomain)
      Ok(views.html.search(form, secondDomain.toUpperCase(), response, DomainService.getLogoPath(secondDomain)))
    } else {
      Ok(views.html.search(form, null, null, null))
    }
  }

  
  def getImage(file: String) = Action {
    val source = scala.io.Source.fromFile(DomainService.STORAGE_PATH + file)(scala.io.Codec.ISO8859)
    val byteArray = source.map(_.toByte).toArray
    source.close()
    Ok(byteArray).as("image/png")
  }
}

case class SearchData(domain: String)
