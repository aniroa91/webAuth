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
import service.{KpiService, ProblemService}
import services.domain.CommonService


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class KpiController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured{

  def index =  withAuth { username => implicit request =>
    try {
      val t0 = System.currentTimeMillis()

      val weekly = Await.result(ProblemService.listWeekly(), Duration.Inf)
      val lstProvince = Await.result(ProblemService.listProvinceByWeek(weekly(0)._2), Duration.Inf)
      val location = lstProvince.map(x=> LocationUtils.getRegionByProvWorld(x._1) -> LocationUtils.getNameProvWorld(x._1)).filter(x=> x._1 != "").distinct.sorted
      val kpi = Await.result(KpiService.listKpi(weekly(0)._2), Duration.Inf).map(x=> (x._1, CommonService.format2DecimalDouble(x._2),
        CommonService.format2DecimalDouble(x._3), CommonService.percentDouble(x._2, x._3)))

      println("time:"+(System.currentTimeMillis() -t0))
      Ok(device.views.html.kpi.index(KpiResponse(weekly, location, kpi), username, controllers.routes.KpiController.index()))
    }
    catch{
      case e: Exception => Ok(device.views.html.kpi.index(null, username, controllers.routes.KpiController.index()))
    }
  }

  def getJsonKpi() = withAuth {username => implicit request =>
    try{
      val time = System.currentTimeMillis()
      val date = request.body.asFormUrlEncoded.get("date").head
      var province = LocationUtils.getCodeProvWorld(request.body.asFormUrlEncoded.get("province").head)
      println(province)
      if(province == "BRU") province = "BRA"
      val kpi = Await.result(KpiService.listKpiJson(date, province), Duration.Inf).map(x=> (x._1, CommonService.format2DecimalDouble(x._2), CommonService.format2DecimalDouble(x._3),
            CommonService.percentDouble(x._2, x._3), CommonUtils.getTitleIndex(x._1), CommonUtils.getDescriptIndex(x._1))).toArray.sorted
      val rs = Json.obj(
        "kpi" -> kpi
      )
      println("timeJson:"+(System.currentTimeMillis() -time))
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
      var province = LocationUtils.getCodeProvWorld(request.body.asFormUrlEncoded.get("province").head)
      if(province == "BRU") province = "BRA"
      val kpiWeekly = Await.result(KpiService.listKpiTimeSeries(date, province, index), Duration.Inf)
      val rs = Json.obj(
        "cate" -> kpiWeekly.map(x=> CommonService.formatStringYYMMDD(x._1)),
        "data" -> kpiWeekly.map(x=> CommonService.format2Decimal(x._2))
      )
      println("timeJson:"+(System.currentTimeMillis() -time))
      Ok(Json.toJson(rs))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }
}