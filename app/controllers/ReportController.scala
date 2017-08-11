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
import services.domain.ReportService

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class ReportController @Inject() (cc: ControllerComponents) extends AbstractController(cc) {

  def index(day: String) = Action {
//    val isValid = Try(DateTimeUtil.create(day, DateTimeUtil.YMD)).isSuccess
//    val response = if (isValid) DomainService.getStatsByDay(day) else DomainService.getStatsByDay(DomainService.getLatestDay())
    Ok(views.html.ace.report(ReportService.get(day)))
  }
}