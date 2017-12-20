package controllers

import javax.inject.Inject
import javax.inject.Singleton
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import services.CacheService
import java.text.NumberFormat
import com.ftel.bigdata.utils.NumberUtil
import dns.utils.DataSampleUtil

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class CompareController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured {

  def index(domains: String) =  Action {
//    Ok(views.html.compare.index())
//    Ok(domains)
    val responses = domains.split(",").map(x => x -> CacheService.getDomain(x)._1).toMap
    
//    responses.foreach(x => {
//      println(x._1)
//      println(x._2.hourly.map(y => y._1 + ":" + y._2).mkString("\t"))
//    })
    Ok(views.html.compare.index(responses.filter(x => x._2 != null)))
  }

}

