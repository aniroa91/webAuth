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

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class DnsController @Inject() (protected val dbConfigProvider: DatabaseConfigProvider,
    cc: ControllerComponents) extends AbstractController(cc) with HasDatabaseConfigProvider[JdbcProfile] {

  def index(domain: String) = Action {
    //val json = request.body.asJson.get
//    val second = DomainUtil
    
    HttpUtil.download("https://logo.clearbit.com/" + domain, "public/images/" + domain + ".png", "172.30.45.220", 80)
    
    val response: Response = DomainService.getDomainInfo(domain)
    Ok(views.html.search(domain.toUpperCase(), response, "../assets/images/" + domain + ".png"))
  }

  def getDomainDetail(domain: String) = Action {
    val jsonObject = DomainService.getDomainInfo(domain)
    Ok(JsonUtil.prettyJson(jsonObject.toJsonObject))
  }
  
  
}
