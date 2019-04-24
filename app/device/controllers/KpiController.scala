package controllers

import javax.inject.Inject
import javax.inject.Singleton

import device.models.{KpiResponse, ProblemResponse}
import device.utils.LocationUtils
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
      val kpi = Await.result(KpiService.listKpi(weekly(0)._2), Duration.Inf).map(x=> (x._1, CommonService.formatNumberDouble(x._2.toLong), CommonService.formatNumberDouble(x._3.toLong), CommonService.percentDouble(x._3, x._2)))

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
      val province = request.body.asFormUrlEncoded.get("province").head

      var arrProv = province.split(",").filter(x=> x != "All").map(x=> LocationUtils.getCodeProvWorld(x))
      if(arrProv.indexOf("BRU") >= 0) arrProv :+= "BRA"
      val lstProvince = Await.result(ProblemService.listProvinceByWeek(date), Duration.Inf).filter(x=> arrProv.indexOf(x._1) >=0)
      val location = lstProvince.map(x=> LocationUtils.getRegionByProvWorld(x._1) -> LocationUtils.getNameProvWorld(x._1)).filter(x=> x._1 != "").distinct.sorted
      val kpi = Await.result(KpiService.listKpiJson(date), Duration.Inf).filter(x=> arrProv.indexOf(x._1) >= 0).
        map(x=> (x._2, x._3, x._4)).groupBy(x=> x._1).map(x=> (x._1, x._2.map(y=> y._2).sum, x._2.map(y=> y._3).sum))
          .map(x=> (x._1, CommonService.formatNumberDouble(x._2.toLong), CommonService.formatNumberDouble(x._3.toLong), CommonService.percentDouble(x._3, x._2)))

      val objLocation = Json.obj(
        "key"  -> location.map(x=> x._1).distinct.sorted,
        "data" -> location
      )
      val rs = Json.obj(
        "location"   -> objLocation,
        "kpi" -> kpi
      )
      println("timeJson:"+(System.currentTimeMillis() -time))
      Ok(Json.toJson(rs))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }
}