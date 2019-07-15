package controllers

import javax.inject.Inject
import javax.inject.Singleton

import device.models.KpiResponse
import device.utils.{CommonUtils, LocationUtils}
import play.api.Logger
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import play.api.libs.json.Json
import service.{KpiService, ProblemService}
import services.domain.CommonService


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class KpiController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured{
  val logger: Logger = Logger(this.getClass())

  def index = withAuth {username => implicit request =>
    val province = if(request.session.get("verifiedLocation").get.equals("1")){
      // only show 5 chart: Devices Get Problem With Critical Alert, Devices Get Problem With Warn Alert, Devices Get Problem With Broken Cable,
      // Devices Get Problem With Suy Hao Index, Devices Get Problem With OLT Error
      request.session.get("location").get.split(",").map(x=> LocationUtils.getCodeProvincebyName(x)).mkString(",")
    } else "All"
    try {
      val t0 = System.currentTimeMillis()
      val weekly = Await.result(ProblemService.listWeekly(), Duration.Inf)
      val lstProvince = Await.result(ProblemService.listProvinceByWeek(weekly(0)._2, if(province.equals("All")) "" else province), Duration.Inf)
      val location = lstProvince.map(x=> LocationUtils.getRegion(x._1) -> LocationUtils.getNameProvincebyCode(x._1)).filter(x=> x._1 != "").distinct.sorted
      val kpi = Await.result(KpiService.listKpi(weekly(0)._2, province), Duration.Inf).map(x=> (x._1, CommonService.format2DecimalDouble(x._2),
        CommonService.format2DecimalDouble(x._3), CommonService.percentDouble(x._2, x._3)))
      val rs = if(!province.equals("All")) kpi.filter(x=> CommonUtils.checkExistIndex(x._1) != "") else kpi

      logger.info(s"Page: KPI - User: ${username} - Time Query:"+(System.currentTimeMillis() -t0))
      Ok(device.views.html.weekly.kpi(KpiResponse(weekly, location,
        (rs.filter(x=> CommonUtils.getGroupIndex(x._1) == "Connectivity").sortWith((x, y) => CommonUtils.getSequenceIndex(x._1) < CommonUtils.getSequenceIndex(y._1)),
        rs.filter(x=> CommonUtils.getGroupIndex(x._1) == "Device Error").sortWith((x, y) => CommonUtils.getSequenceIndex(x._1) < CommonUtils.getSequenceIndex(y._1)),
        rs.filter(x=> CommonUtils.getGroupIndex(x._1) == "Service Monitor").sortWith((x, y) => CommonUtils.getSequenceIndex(x._1) < CommonUtils.getSequenceIndex(y._1)),
        rs.filter(x=> CommonUtils.getGroupIndex(x._1) == "Client Error").sortWith((x, y) => CommonUtils.getSequenceIndex(x._1) < CommonUtils.getSequenceIndex(y._1)))),
        username, if(province == "All") province else request.session.get("location").getOrElse("All"), controllers.routes.KpiController.index()))
    }
    catch{
      case e: Exception => Ok(device.views.html.weekly.kpi(null, username, if(province == "All") province else request.session.get("location").getOrElse("All"), controllers.routes.KpiController.index()))
    }
  }

  def getJsonKpi() = withAuth {username => implicit request =>
    try{
      val time = System.currentTimeMillis()
      val date = request.body.asFormUrlEncoded.get("date").head
      val prov = request.body.asFormUrlEncoded.get("province").head
      val province = prov match {
        case prov if(prov == "All" || prov == "International" || prov.split(",").length>1 || (prov.split(" ").length>1 && prov.split(" ")(1).matches("^\\d+$"))) => prov
        case _ => LocationUtils.getCodeProvincebyName(prov)
      }

      val kpi = Await.result(KpiService.listKpi(date, province), Duration.Inf).map(x=> (x._1, CommonService.format2DecimalDouble(x._2), CommonService.format2DecimalDouble(x._3),
            CommonService.percentDouble(x._2, x._3), CommonUtils.getTitleIndex(x._1), CommonUtils.getDescriptIndex(x._1))).toArray.sorted
      val rsKpi = if(request.body.asFormUrlEncoded.get("isAuthor").head == "1") kpi.filter(x=> CommonUtils.checkExistIndex(x._1) != "") else kpi
      val connect = rsKpi.filter(x=> CommonUtils.getGroupIndex(x._1) == "Connectivity").sortWith((x, y) => CommonUtils.getSequenceIndex(x._1) < CommonUtils.getSequenceIndex(y._1))
      val device = rsKpi.filter(x=> CommonUtils.getGroupIndex(x._1) == "Device Error").sortWith((x, y) => CommonUtils.getSequenceIndex(x._1) < CommonUtils.getSequenceIndex(y._1))
      val service = rsKpi.filter(x=> CommonUtils.getGroupIndex(x._1) == "Service Monitor").sortWith((x, y) => CommonUtils.getSequenceIndex(x._1) < CommonUtils.getSequenceIndex(y._1))
      val client = rsKpi.filter(x=> CommonUtils.getGroupIndex(x._1) == "Client Error").sortWith((x, y) => CommonUtils.getSequenceIndex(x._1) < CommonUtils.getSequenceIndex(y._1))

      val rs = Json.obj(
        "kpi" -> rsKpi,
        "connect"  -> connect,
        "device"  -> device,
        "service"  -> service,
        "client"  -> client
      )
      logger.info(s"Page: KPI - User: ${username} - Choose Province: ${LocationUtils.getNameProvincebyCode(province)}, Date: $date - Time Query:"+(System.currentTimeMillis() -time))
      Ok(Json.toJson(rs))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  def getKpiTimeSeries() = withAuth {username => implicit request =>
    try{
      val time = System.currentTimeMillis()
      val date = request.body.asFormUrlEncoded.get("date").head
      val index = request.body.asFormUrlEncoded.get("index").head
      val prov = request.body.asFormUrlEncoded.get("province").head
      val province = prov match {
        case prov if(prov == "All" || prov == "International" || prov.split(",").length>1 || (prov.split(" ").length>1 && prov.split(" ")(1).matches("^\\d+$"))) => prov
        case _ => LocationUtils.getCodeProvincebyName(prov)
      }
      val kpiWeekly = Await.result(KpiService.listKpiTimeSeries(date, province, index), Duration.Inf)
      val rs = Json.obj(
        "cate" -> kpiWeekly.map(x=> CommonService.formatStringYYMMDD(x._1)),
        "data" -> kpiWeekly.map(x=> CommonService.format2Decimal(x._2))
      )
      logger.info(s"Page: KPI - User: ${username} - Choose Province: ${LocationUtils.getNameProvincebyCode(province)}, Date: $date - Time Query:"+(System.currentTimeMillis() -time))
      Ok(Json.toJson(rs))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }
}