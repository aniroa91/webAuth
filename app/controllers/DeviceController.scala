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
import play.api.libs.json.Json
import services.domain.CommonService.formatYYYYmmddHHmmss


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */

@Singleton
class DeviceController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured{

  def index =  withAuth { username => implicit request =>
    try {
      // get data for map chart
      val mapNoc = Await.result(BrasService.listNocOutlier, Duration.Inf)
      Ok(views.html.device.index(username,mapNoc))
    }
    catch{
      case e: Exception => Ok(views.html.device.index(username,null))
    }
  }

  def getHostJson(id: String) = Action { implicit request =>
    try{
      val rsHost = Await.result(BrasService.getHostBras(id), Duration.Inf)
      val re = rsHost.map(
        iter =>
                Json.obj(
                  "host" -> iter._1,
                  "module" -> iter._2
                ) ->
                  Json.obj(
                    "cpe" -> iter._3,
                    "lostip" -> iter._4
                  )
      )
      val idBras = id.split('/')(0)
      val time = id.split('/')(1)
      val brasChart = Await.result(BrasService.getJsonBrasChart(idBras,time),Duration.Inf)
      val jsBras = Json.obj(
        "host" -> re,
        "time" -> brasChart.map({ t => formatYYYYmmddHHmmss(t._1.toString)}),
        "logoff" -> brasChart.map({ t =>t._2}),
        "signin" -> brasChart.map({ t => t._3}),
        "users" -> brasChart.map({ t => t._4})
      )
      Ok(Json.toJson(jsBras))
    }
    catch{
      case e: Exception => Ok("error")
    }
  }

  def getBrasJson(id: String) = Action { implicit request =>
    try{
      val lstBras = Await.result(BrasService.listBrasById(id), Duration.Inf)
      var mapBras = collection.mutable.Map[String, Seq[(String,String,String,String)]]()
      val arrOutlier = lstBras.map(x => (x._1->x._2)).toList.distinct
      for(outlier <- arrOutlier){
        val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS")
        val dateTime = DateTime.parse(outlier._2, formatter)
        val oldTime  = dateTime.minusMinutes(5).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))
        val brasKey = Await.result(BrasService.opViewKibana(outlier._1,dateTime.toString,oldTime), Duration.Inf)
        mapBras += (outlier._1+"/"+outlier._2-> brasKey)
      }
      val arrLine = lstBras.map(x => (x._1, x._2) -> x._3).groupBy(x => x._1).mapValues(x => x.map(y => y._2).mkString("|"))
      val arrCard = lstBras.map(x => (x._1, x._2, x._3) -> x._4).groupBy(x => x._1).mapValues(x => x.map(y => y._2).mkString("|"))
      val arrHost = lstBras.map(x => (x._1, x._2, x._3,x._4) -> x._5).groupBy(x => x._1).mapValues(x => x.map(y => y._2).mkString("|"))
      val jsBras = Json.obj(
        "bras" -> arrOutlier,
        "linecard" -> arrLine,
        "card" -> arrCard,
        "host" -> arrHost,
        "mapBras" -> mapBras
      )
      Ok(Json.toJson(jsBras))
    }
    catch{
      case e: Exception => Ok("error")
    }
  }

}


