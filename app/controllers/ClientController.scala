package controllers

import javax.inject.Inject
import javax.inject.Singleton
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.text
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import play.api.mvc.AnyContent
import play.api.mvc.Request
import services.CacheService
import play.api.i18n.I18nSupport
import services.domain.ClientService
import services.domain.CommonService

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class ClientController @Inject() (cc: ControllerComponents) extends AbstractController(cc) with I18nSupport {

  
  val form = Form(
    mapping(
      "q" -> text
    )(SearchData.apply)(SearchData.unapply)
  )

  def index = Action { implicit request: Request[AnyContent] =>
    val formValidationResult = form.bindFromRequest
    if (!formValidationResult.hasErrors) {
      val ip = formValidationResult.get.q
      val response = CacheService.getClient(ip)
      Ok(views.html.dns.profile.client.index(form, response._1, null, ip))
    } else {
      Ok(views.html.dns.profile.client.index(form, null, ClientService.getTop(), ""))
    }
  }

  
  def history(ip: String, offset: Int) = Action {
      if (ip == "") {
        Ok("")
      } else {
        val headers = Array[String]("Day", "Time", "Domain", "Second", "Malware", "RCode", "Answers")
        
        val history = ClientService.historyBlack2(ip, offset, CommonService.SIZE_DEFAULT).asInstanceOf[Array[Array[Any]]]
         Ok(views.html.dns.profile.client.tablescroll(null, history))
      }
//      println(history.size)
//      val html = views.html.ace.timeline("", history)
//      val html2 = html + views.html.ace.timeline("", history).body
      
    
  }
}


