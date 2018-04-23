package controllers

import play.api.mvc._
import javax.inject.Inject
import javax.inject.Singleton
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import services.CacheService
import java.text.NumberFormat
import com.ftel.bigdata.utils.NumberUtil
import com.ftel.bigdata.utils.DateTimeUtil

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class CompareController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured {
  /* Authentication action*/
  def Authenticated(f: AuthenticatedRequest => Result) = {
    Action { request =>
      val username = request.session.get("username").get.toString
      username match {
        case "btgd@ftel" =>
          f(AuthenticatedRequest(username, request))
        case none =>
          Redirect(routes.LoginController.index).withNewSession.flashing(
            "success" -> "You are now logged out."
          )
      }
    }
  }

  def index(domains: String) =  Authenticated { implicit request =>
//    Ok(views.html.compareDate.index())
//    Ok(domains)
    val responses = domains.split(",").map(x => x -> CacheService.getDomain(x)._1).toMap.filter(x => x._2 != null)
    val now = DateTimeUtil.now
    
    val dailyCate = (1 until 30).toArray.map(x => now.minusDays(x).toString("yyyy-MM-dd"))
    
    val dailyQueries = responses.map(x => {
      val second = x._1
      val history = x._2.history.map(y => y.day -> y.queries).toMap
      second -> dailyCate.map(y => history.getOrElse(y, 0L))
      }).toArray
      
    val dailyClients = responses.map(x => {
      val second = x._1
      val history = x._2.history.map(y => y.day -> y.clients).toMap
      second -> dailyCate.map(y => history.getOrElse(y, 0L))
      }).toArray
      
        //var clients = @Html(Json.stringify(Json.toJson(responses.map(x => x._1 -> x._2.history.map(y => y.clients)).toArray)));
        
//    responses.foreach(x => {
//      println(x._1)
//      println(x._2.hourly.map(y => y._1 + ":" + y._2).mkString("\t"))
//    })

    Ok(dns.views.html.compare.index(responses, dailyCate, dailyQueries, dailyClients))
  }

}

