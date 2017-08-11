package controllers

import javax.inject._
import play.api.mvc._
import play.api._
//import services.DomainService
import utils.JsonUtil
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
//import model.Response
import com.ftel.bigdata.utils.HttpUtil
import com.ftel.bigdata.utils.FileUtil
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import com.typesafe.config.ConfigFactory
import play.api.data._
import play.api.i18n._
import play.api.mvc._
import com.ftel.bigdata.utils.DateTimeUtil
import scala.util.Try
import services.domain.DashboardService
import services.CacheService

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class DashboardController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def index = Action {
    Ok(views.html.ace.dashboard(CacheService.getDaskboard()))
  }
}

