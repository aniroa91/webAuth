package profile.controllers.internet

import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.text
import javax.inject.Inject
import javax.inject.Singleton
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import controllers.SearchContract
import controllers.Secured
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.ElasticsearchClientUri
import services.Configure
import views.html.ace.client
import com.sksamuel.elastic4s.http.ElasticDsl._
import profile.services.internet.HistoryService
import com.ftel.bigdata.utils.StringUtil
import profile.services.internet.response.History
import profile.services.internet.response.Hourly
import profile.services.internet.CompareService


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class CompareController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured{

  
  val client = Configure.client
  val form = Form(
    mapping(
      "ct" -> text)(SearchContract.apply)(SearchContract.unapply))

  def index =  withAuth { username => implicit request =>
    val formValidationResult = form.bindFromRequest
    try {
      if (!formValidationResult.hasErrors) {
        val contract = formValidationResult.get.q.trim()
        
        Ok(views.html.profile.internet.compare.date.index(form, username, contract, CompareService.getDay(Array("2018-02-01", "2018-02-03"))))
      } else {
        Ok(views.html.profile.internet.compare.date.index(form, username, null, CompareService.getDay(Array("2018-02-01", "2018-02-03"))))
      }
    } catch {
      case e: Exception => Ok(views.html.profile.internet.compare.date.index(form, username, null, CompareService.getDay(Array("2018-02-01", "2018-02-03"))))
    }
  }
}



