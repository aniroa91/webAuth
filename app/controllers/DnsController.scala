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

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class DnsController @Inject() (protected val dbConfigProvider: DatabaseConfigProvider,
    cc: ControllerComponents) extends AbstractController(cc) with HasDatabaseConfigProvider[JdbcProfile] {

  //val URL_LOGO = ConfigFactory.load().getString("URL_LOGO")
  
  def index(domain: String) = Action {
    val secondDomain = DomainUtil.getSecondTopLevel(domain).toLowerCase()
    val logoUrl = "https://logo.clearbit.com/" + secondDomain
    val path = "public/images/logo/" + secondDomain + ".png"
    val assetsPath = "../assets/images/logo/" + secondDomain + ".png"
    
//    if (!FileUtil.isExist(path)) {
//      println("Don't exist " + path)
//      HttpUtil.download(logoUrl, path, "172.30.45.220", 80)
//    }
    val response: Response = DomainService.getDomainInfo(secondDomain)
    //println(response, URL_LOGO + "/vnexpress.net.png")
    Ok(views.html.search(secondDomain.toUpperCase(), response, "../assets/images/logo/domain.png"))
  }

//  def search(domain: String) = Action {
//    val form = Form((
//      "domain" -> of[String]))
//      form.bindFromRequest()
//      
//  // .form().bindFromRequest();
//    Logger.info("Username is: " + dynamicForm.get("username"));
//    Logger.info("Password is: " + dynamicForm.get("password"));
//    return ok("ok, I recived POST data. That's all...");
//  }
  
  def getDomainDetail(domain: String) = Action {
    val secondDomain = DomainUtil.getSecondTopLevel(domain)
    val jsonObject = DomainService.getDomainInfo(secondDomain)
    Ok(JsonUtil.prettyJson(jsonObject.toJsonObject))
  }
  
  
}
