package controllers

import javax.inject._
import play.api.mvc._
import services.DomainService
import com.typesafe.config.ConfigFactory
import com.ftel.bigdata.utils.FileUtil
import com.ftel.bigdata.utils.HttpUtil
import com.ftel.bigdata.dns.parameters.Label

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  /*
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }
  */
  
  val URL_DOMAIN_DEFAULT = "../assets/images/logo/domain.png"
  val STORAGE_PATH = ConfigFactory.load().getString("storage") + "/"
  val LOGO_URL = "https://logo.clearbit.com/"
  def index = Action {
    val latestDay = DomainService.getLatestDay()
    val all = DomainService.getTopRank(1, latestDay)
    val white = DomainService.getTopByNumOfQuery(latestDay, Label.White)
    val black = DomainService.getTopBlackByNumOfQuery(latestDay)
    val unknow = DomainService.getTopByNumOfQuery(latestDay, Label.Unknow)
    Ok(views.html.home(all, white, black, unknow))
  }
}
