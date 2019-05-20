package controllers

import javax.inject.Inject
import javax.inject.Singleton

import device.models.KpiResponse
import device.utils.{CommonUtils, LocationUtils}
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import play.api.libs.json.Json
import service._
import services.domain.CommonService


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class ExportController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured{

  def index =  withAuth { username => implicit request =>
    Ok(device.views.html.export.index(username, controllers.routes.ExportController.index()))
  }

  def exportData() = withAuth {username => implicit request =>
    try{
      val time = System.currentTimeMillis()
      println("Start export data....")
      val fromDate = request.body.asFormUrlEncoded.get("fromDate").head
      val toDate = request.body.asFormUrlEncoded.get("toDate").head
      val dataSource = request.body.asFormUrlEncoded.get("dataSource").head
      val device = request.body.asFormUrlEncoded.get("device").head.trim().toUpperCase
      val colRow = request.body.asFormUrlEncoded.get("colRow").head.toInt
      val nextDate = if(colRow == 5) CommonService.getNextDay(fromDate) else CommonService.getNextDay(toDate)

      val fileName = if(fromDate.equals(toDate)) dataSource.toUpperCase()+"_"+CommonService.formatDateYYMMDD(fromDate)+".csv" else dataSource.toUpperCase()+"_"+CommonService.formatDateYYMMDD(fromDate)+"_"+CommonService.formatDateYYMMDD(toDate)+".csv"
      val rs = dataSource match {
        case "Kibana" => {
          implicit val contractFormat = Json.format[JsonKibana]
          val kibana = Await.result(ExportService.exportByKibana(fromDate, nextDate, device), Duration.Inf).slice(0,colRow)
          Json.obj(
            "fileName" -> fileName,
            "data" -> kibana
          )
        }
        case "Opsview" => {
          implicit val contractFormat = Json.format[JsonOpsview]
          val opsview = Await.result(ExportService.exportByOpsview(fromDate, nextDate, device), Duration.Inf).slice(0,colRow)
          Json.obj(
            "fileName" -> fileName,
            "data" -> opsview
          )
        }
        case "Inf" => {
          implicit val contractFormat = Json.format[JsonInf]
          val inf = Await.result(ExportService.exportByInf(fromDate, nextDate, device), Duration.Inf).slice(0,colRow)
          Json.obj(
            "fileName" -> fileName,
            "data" -> inf
          )
        }
        case "Suyhao" => {
          implicit val contractFormat = Json.format[JsonSuyhao]
          val suyhao = Await.result(ExportService.exportBySuyhao(fromDate, nextDate, device), Duration.Inf).slice(0,colRow)
          Json.obj(
            "fileName" -> fileName,
            "data" -> suyhao
          )
        }
      }
      println("End export data:"+(System.currentTimeMillis() -time))
      Ok(Json.toJson(rs))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }
}