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
  
  //val URL_LOGO = ConfigFactory.load().getString("URL_LOGO")
  
//  def index(domain: String) = Action {
//    val secondDomain = DomainUtil.getSecondTopLevel(domain).toLowerCase()
//    val logoUrl = "https://logo.clearbit.com/" + secondDomain
//    val path = "public/images/logo/" + secondDomain + ".png"
//    val assetsPath = "../assets/images/logo/" + secondDomain + ".png"
//    
////    if (!FileUtil.isExist(path)) {
////      println("Don't exist " + path)
////      HttpUtil.download(logoUrl, path, "172.30.45.220", 80)
////    }
//    val response: Response = DomainService.getDomainInfo(secondDomain)
//    //println(response, URL_LOGO + "/vnexpress.net.png")
//    Ok(views.html.search(secondDomain.toUpperCase(), response, "../assets/images/logo/domain.png"))
//  }

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
    val response = DomainService.getDomainInfo(secondDomain)
    if (response != null) {
      Ok(JsonUtil.prettyJson(response.toJsonObject))
    } else Ok("{}")
  }
  
  
  
//  def formview = Action { implicit request: Request[AnyContent] =>
//    Ok(views.html.form(form))
//  }

  val URL_DOMAIN_DEFAULT = "../assets/images/logo/domain.png"
  val STORAGE_PATH = ConfigFactory.load().getString("storage") + "/"
  val LOGO_URL = "https://logo.clearbit.com/"
  def search = Action { implicit request: Request[AnyContent] =>
    val formValidationResult = form.bindFromRequest
    if (!formValidationResult.hasErrors) {
      val domain = formValidationResult.get.domain
      val secondDomain = DomainUtil.getSecondTopLevel(domain).toLowerCase()
      val logoUrl = LOGO_URL + secondDomain
      // Can't save outside path in application, this path must be in assets application
      // When download, it will storage at local, don't append to assets in *.jar
      //val pathSave = "public/images/logo/" + secondDomain + ".png"
      val path = STORAGE_PATH + secondDomain + ".png"
      val logo = "../extassets/" + secondDomain + ".png"
      
      if (!FileUtil.isExist(path)) {
        println("Don't exist " + path)
        //println("LOGO URL: " + logoUrl)
        //val content = HttpUtil.getContent(logoUrl, "172.30.45.220", 80)
        //println("CONTENT: " + content)
        HttpUtil.download(logoUrl, path, "172.30.45.220", 80)
        
      }
      
      //FileUtil.readLines("/home/dungvc/Desktop/top-10.csv").foreach(println)
      val logoSrc = if (FileUtil.isExist(path)) {
        logo
      } else URL_DOMAIN_DEFAULT
      val response = DomainService.getDomainInfo(secondDomain)
      Ok(views.html.search(form, secondDomain.toUpperCase(), response, logoSrc))
    } else {
//    println(formValidationResult.value)
//    println(formValidationResult.data)
//    println(formValidationResult.errors)
//    println(formValidationResult.mapping.format)
    //println(formValidationResult.hasErrors)
//    if (formValidationResult.hasErrors()) {
//      formValidationResult.
//    }
//    if (formValidationResult.errors != None) {
//      //val searchForm = formValidationResult.get
//      println(formValidationResult)
//    }
    
    //formValidationResult.errors.foreach(x => println(x.key -> x.message))
      Ok(views.html.search(form, null, null, null))
    }
  }
  
//  private def getResponse(domain: String): Response = {
//    val secondDomain = DomainUtil.getSecondTopLevel(domain).toLowerCase()
//    val logoUrl = "https://logo.clearbit.com/" + secondDomain
//    val path = "public/images/logo/" + secondDomain + ".png"
//    val assetsPath = "../assets/images/logo/" + secondDomain + ".png"
//    
////    if (!FileUtil.isExist(path)) {
////      println("Don't exist " + path)
////      HttpUtil.download(logoUrl, path, "172.30.45.220", 80)
////    }
//    DomainService.getDomainInfo(secondDomain)
//  }
  
//  val STORAGE_PATH = ConfigFactory.load().getString("storage")
//  
//  def getFile(file: String) = Action {
//    val result = new java.io.File(file)
//    //Ok(result)
//    Assets.versioned(STORAGE_PATH, file).parser
//  }
  
//  def downloadLogo(domain: String) = Action {
//    val logoUrl = LOGO_URL + domain
//    HttpUtil.download(logoUrl, "./", "172.30.45.220", 80)
//    Ok("")
//  }
  
  def getImage(file: String) = Action {
    val source = scala.io.Source.fromFile(STORAGE_PATH + file)(scala.io.Codec.ISO8859)
    val byteArray = source.map(_.toByte).toArray
    source.close()
    Ok(byteArray).as("image/png")
  }
}

case class SearchData(domain: String)
