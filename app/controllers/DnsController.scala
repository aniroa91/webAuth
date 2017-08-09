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
import com.ftel.bigdata.dns.parameters.Label
import com.ftel.bigdata.utils.DateTimeUtil
import scala.util.Try

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class DnsController @Inject() (protected val dbConfigProvider: DatabaseConfigProvider,
    cc: ControllerComponents) extends AbstractController(cc) with HasDatabaseConfigProvider[JdbcProfile]  with I18nSupport {

  val form = Form(
    mapping(
      "value" -> text
    )(SearchData.apply)(SearchData.unapply)
  )


//  def index = Action { implicit request: Request[AnyContent] =>
//    val formValidationResult = form.bindFromRequest
//    if (!formValidationResult.hasErrors) {
//      val day = formValidationResult.get.value.trim()
//      val isValid = Try(DateTimeUtil.create(day, DateTimeUtil.YMD)).isSuccess
//      val response = if (isValid) DomainService.getStatsByDay(day) else DomainService.getStatsByDay(DomainService.getLatestDay())
//      Ok(views.html.ace.index(form, response))
//    } else {
//      val latestDay = DomainService.getLatestDay()
//      val response = DomainService.getStatsByDay(latestDay)
//      Ok(views.html.ace.index(form, response))
//    }
//  }
  
  def reportPage(day: String) = Action { implicit request: Request[AnyContent] =>
    val isValid = Try(DateTimeUtil.create(day, DateTimeUtil.YMD)).isSuccess
    val response = if (isValid) DomainService.getStatsByDay(day) else DomainService.getStatsByDay(DomainService.getLatestDay())
    Ok(views.html.ace.report(form, response))
  }
  
  def reportBody(day: String) = Action {
    val isValid = Try(DateTimeUtil.create(day, DateTimeUtil.YMD)).isSuccess
    val response = if (isValid) DomainService.getStatsByDay(day) else DomainService.getStatsByDay(DomainService.getLatestDay())
    Ok(views.html.ace.reportBody(response))
  }
  
  def dashboardPage = Action { implicit request: Request[AnyContent] =>
    val formValidationResult = form.bindFromRequest
    if (!formValidationResult.hasErrors) {
      val day = formValidationResult.get.value.trim()
      val isValid = Try(DateTimeUtil.create(day, DateTimeUtil.YMD)).isSuccess
      val response = if (isValid) DomainService.getDashboard(day) else DomainService.getDashboard(DomainService.getLatestDay())
      Ok(views.html.ace.dashboard(form, response))
    } else {
      val latestDay = DomainService.getLatestDay()
      val response = DomainService.getDashboard(latestDay)
      Ok(views.html.ace.dashboard(form, response))
    }

  }
  
  def getDomainDetail(domain: String) = Action {
    val secondDomain = DomainUtil.getSecondTopLevel(domain)
    val response = DomainService.getDomainInfo(secondDomain)
    if (response != null) {
      Ok(JsonUtil.prettyJson(response.toJsonObject))
    } else Ok("{}")
  }

//  def search = Action { implicit request: Request[AnyContent] =>
//    val formValidationResult = form.bindFromRequest
//    if (!formValidationResult.hasErrors) {
//      val domain = formValidationResult.get.domain
//      val secondDomain = DomainUtil.getSecondTopLevel(domain).toLowerCase()
//      val response = DomainService.getDomainInfo(secondDomain)
//      Ok(views.html.search(form, secondDomain.toUpperCase(), response, DomainService.getLogoPath(secondDomain)))
//    } else {
//      Ok(views.html.search(form, null, null, null))
//    }
//  }

  def profilePage = Action { implicit request: Request[AnyContent] =>
    val formValidationResult = form.bindFromRequest
    //println(formValidationResult.hasErrors)
    //println(formValidationResult.errors)
    if (!formValidationResult.hasErrors) {
      val domain = formValidationResult.get.value
      //println(domain)
      val secondDomain = DomainUtil.getSecondTopLevel(domain).toLowerCase()
      val response = DomainService.getDomainInfo(secondDomain)
      Ok(views.html.ace.profile(form, secondDomain.toLowerCase(), response, DomainService.getLogoPath(secondDomain)))
    } else {
      Ok(views.html.ace.profile(form, null, null, null))
    }
  }
  
  def rankPage = Action { implicit request: Request[AnyContent] =>
      val latestDay = DomainService.getLatestDay()
      val all = DomainService.getTopRank(1, latestDay)
      val white = DomainService.getTopByNumOfQuery(latestDay, Label.White)
      val black = DomainService.getTopBlackByNumOfQuery(latestDay)
      val unknow = DomainService.getTopByNumOfQuery(latestDay, Label.Unknow)
      Ok(views.html.ace.rank(all, white, black, unknow))
  }
  
  def getImage(file: String) = Action {
    val source = scala.io.Source.fromFile(DomainService.STORAGE_PATH + file)(scala.io.Codec.ISO8859)
    val byteArray = source.map(_.toByte).toArray
    source.close()
    Ok(byteArray).as("image/png")
  }
}

case class SearchData(value: String)
