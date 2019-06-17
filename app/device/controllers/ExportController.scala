package controllers

import javax.inject.Inject
import javax.inject.Singleton

import device.utils.LocationUtils
import play.api.Logger
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

  val logger: Logger = Logger(this.getClass())

  def index =  withAuth { username => implicit request =>
    val province = if(request.session.get("verifiedLocation").get.equals("1")){
      // only show 5 chart: Devices Get Problem With Critical Alert, Devices Get Problem With Warn Alert, Devices Get Problem With Broken Cable,
      // Devices Get Problem With Suy Hao Index, Devices Get Problem With OLT Error
      request.session.get("location").get.split(",").map(x=> LocationUtils.getCodeProvincebyName(x)).mkString("|")
    } else ""
    Ok(device.views.html.export.index(username, province, controllers.routes.ExportController.index()))
  }

  def exportData() = withAuth {username => implicit request =>
    val province = if(request.session.get("verifiedLocation").get.equals("1")){
      // only show 5 chart: Devices Get Problem With Critical Alert, Devices Get Problem With Warn Alert, Devices Get Problem With Broken Cable,
      // Devices Get Problem With Suy Hao Index, Devices Get Problem With OLT Error
      request.session.get("location").get.split(",").map(x=> LocationUtils.getCodeProvincebyName(x)).mkString("|")
    } else ""
    try{
      val time = System.currentTimeMillis()
      println("Start export data....")
      val fromDate = request.body.asFormUrlEncoded.get("fromDate").head
      val toDate = request.body.asFormUrlEncoded.get("toDate").head
      val dataSource = request.body.asFormUrlEncoded.get("dataSource").head
      val module = request.body.asFormUrlEncoded.get("module").head.toInt
      var device = request.body.asFormUrlEncoded.get("device").head.trim().toUpperCase
      val colRow = request.body.asFormUrlEncoded.get("colRow").head.toInt
      val nextDate = if(colRow == 5) CommonService.getNextDay(fromDate) else CommonService.getNextDay(toDate)
      var isMatch = 0
      if(province.length>device.length){
        if(province.indexOf(device) >=0) {
          isMatch = 1
          device = province
        }
      }
      else{
        if(device.indexOf(province) >=0) {
          isMatch = 1
          device = device
        }
      }
      if(province.equals("") ||isMatch == 1){
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
          case "PortPonDown" => {
            implicit val contractFormat = Json.format[JsonPortPonDown]
            val portPon = Await.result(ExportService.exportByportPon(fromDate, nextDate, device, module), Duration.Inf).slice(0,colRow)
            Json.obj(
              "fileName" -> fileName,
              "data" -> portPon
            )
          }
        }
        logger.info(s"Page: KPI - User: ${username} - Time Query:"+(System.currentTimeMillis() -time))
        Ok(Json.toJson(rs))
      }
      else{
        Ok("permission")
      }
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }
}