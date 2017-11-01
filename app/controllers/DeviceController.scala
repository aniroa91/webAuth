package controllers

import javax.inject.Inject
import javax.inject.Singleton

import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.BrasService
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class DeviceController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured{

  def index =  withAuth { username => implicit request =>
    try {
      val lstBras = Await.result(BrasService.listBrasOutlier, Duration.Inf)
      var mapBras = collection.mutable.Map[String, Seq[(String,String,String,String)]]()
      val arrOutlier = lstBras.map(x => (x._1->x._2)).toList.distinct
      for(outlier <- arrOutlier){
        val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS")
        val dateTime = DateTime.parse(outlier._2, formatter)
        val oldTime  = dateTime.minusMinutes(5).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))
        val brasKey = Await.result(BrasService.opViewKibana(outlier._1,dateTime.toString,oldTime), Duration.Inf)
        mapBras += (outlier._1+",Time: "+outlier._2-> brasKey)
      }
      Ok(views.html.device.index(username, lstBras,mapBras))
    }
    catch{
      case e: Exception => Ok(views.html.device.index(username,null,null))
    }
  }

  def ajaxCall = Action { implicit request =>
    Ok("Ajax Call!")
  }
}


