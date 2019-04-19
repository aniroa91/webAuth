package controllers

import javax.inject.Inject
import javax.inject.Singleton

import device.models.ProblemResponse
import device.utils.LocationUtils
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import play.api.libs.json.Json
import service.ProblemService
import services.domain.CommonService


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class ProblemController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured{

  def index =  withAuth { username => implicit request =>
    try {
      val weekly = Await.result(ProblemService.listWeekly(), Duration.Inf)
      val lstProvince = Await.result(ProblemService.listProvinceByWeek(weekly(0)._2), Duration.Inf)
      val location = lstProvince.map(x=> LocationUtils.getRegionByProvWorld(x._1) -> LocationUtils.getNameProvWorld(x._1)).filter(x=> x._1 != "").distinct.sorted
      val deviceType = lstProvince.map(x=> x._2 -> x._3).groupBy(x=> x._1).map(y=> y._1 -> y._2.map(x=> x._2).sum).toArray
      val probConnectivity = Await.result(ProblemService.listProbconnectivity(weekly(0)._2), Duration.Inf)
      val probError = Await.result(ProblemService.listProbError(weekly(0)._2), Duration.Inf)
      val probWarn = Await.result(ProblemService.listProbWarning(weekly(0)._2), Duration.Inf)
      val critAlert = Await.result(ProblemService.listCritAlerts(weekly(0)._2), Duration.Inf)
      val warnAlert = Await.result(ProblemService.listWarnAlerts(weekly(0)._2), Duration.Inf)
      val suyhao = Await.result(ProblemService.listSuyhao(weekly(0)._2), Duration.Inf).map(x=> (x._1, x._2, x._3, CommonService.format2Decimal(x._4)))
      val broken = Await.result(ProblemService.listBroken(weekly(0)._2), Duration.Inf)
      val olts = Await.result(ProblemService.listOLT(weekly(0)._2), Duration.Inf)
                .map(x=> (x._1, x._2, CommonService.formatPattern(x._3), CommonService.formatPattern(x._4), CommonService.formatPattern(x._5),
                  CommonService.formatPattern(x._6), CommonService.formatPattern(x._7), CommonService.formatPattern(x._8)))
      Ok(device.views.html.problem.index(ProblemResponse(weekly, location, deviceType, probConnectivity, probError, probWarn, critAlert, warnAlert, suyhao, broken, olts), username, controllers.routes.ProblemController.index()))
    }
    catch{
      case e: Exception => Ok(device.views.html.problem.index(null, username, controllers.routes.ProblemController.index()))
    }
  }

  def getJsonProblem() = Action { implicit request =>
    try{
      val date = request.body.asFormUrlEncoded.get("date").head
      val province = request.body.asFormUrlEncoded.get("province").head

      var arrProv = province.split(",").filter(x=> x != "All").map(x=> LocationUtils.getCodeProvWorld(x))
      if(arrProv.indexOf("BRU") >= 0) arrProv :+= "BRA"
      val lstProvince = Await.result(ProblemService.listProvinceByWeek(date), Duration.Inf).filter(x=> arrProv.indexOf(x._1) >=0)
      val location = lstProvince.map(x=> LocationUtils.getRegionByProvWorld(x._1) -> LocationUtils.getNameProvWorld(x._1)).filter(x=> x._1 != "").distinct.sorted
      val deviceType = lstProvince.map(x=> x._2 -> x._3).groupBy(x=> x._1).map(y=> y._1 -> y._2.map(x=> x._2).sum).toArray
      val probConnect = Await.result(ProblemService.listProbconnectivity(date), Duration.Inf).filter(x=> arrProv.indexOf(x._1.split("-")(0)) >=0)
      val probError = Await.result(ProblemService.listProbError(date), Duration.Inf).filter(x=> arrProv.indexOf(x._1.split("-")(0)) >=0)
      val probWarn = Await.result(ProblemService.listProbWarning(date), Duration.Inf).filter(x=> arrProv.indexOf(x._1.split("-")(0)) >=0)
      val critAlert = Await.result(ProblemService.listCritAlerts(date), Duration.Inf).filter(x=> arrProv.indexOf(x._1.split("-")(0)) >=0)
      val warnAlert = Await.result(ProblemService.listWarnAlerts(date), Duration.Inf).filter(x=> arrProv.indexOf(x._1.split("-")(0)) >=0)
      val suyhao = Await.result(ProblemService.listSuyhao(date), Duration.Inf).filter(x=> arrProv.indexOf(x._1.substring(0, 3)) >=0).map(x=> (x._1, x._2, x._3, CommonService.format2Decimal(x._4)))
      val broken = Await.result(ProblemService.listBroken(date), Duration.Inf).filter(x=> arrProv.indexOf(x._1.substring(0, 3)) >=0)
      val olts = Await.result(ProblemService.listOLT(date), Duration.Inf).filter(x=> arrProv.indexOf(x._1.substring(0, 3)) >=0)
        .map(x=> (x._1, x._2, CommonService.formatPattern(x._3), CommonService.formatPattern(x._4), CommonService.formatPattern(x._5),
          CommonService.formatPattern(x._6), CommonService.formatPattern(x._7), CommonService.formatPattern(x._8)))
    
      val objLocation = Json.obj(
        "key"  -> location.map(x=> x._1).distinct.sorted,
        "data" -> location
      )
      val objDevType = Json.obj(
        "data" -> deviceType,
        "sum"  -> deviceType.map(x=> x._2).sum
      )
      val objConn = Json.obj(
        "cates"  -> probConnect.map(x=> x._1),
        "signin" -> probConnect.map(x=> x._2),
        "logoff" -> probConnect.map(x=> x._3)
      )
      val objErr = Json.obj(
        "cates"  -> probError.map(x=> x._1),
        "data" -> probError.map(x=> x._2),
        "name" -> "Error"
      )
      val objWarn = Json.obj(
        "cates"  -> probWarn.map(x=> x._1),
        "data" -> probWarn.map(x=> x._2),
        "name" -> "Warn"
      )
      val objCrit = Json.obj(
        "cates"  -> critAlert.map(x=> x._1),
        "data" -> critAlert.map(x=> x._2),
        "name" -> "Crit"
      )
      val objWarnAlert = Json.obj(
        "cates"  -> warnAlert.map(x=> x._1),
        "data" -> warnAlert.map(x=> x._2),
        "name" -> "Warn"
      )
      val rs = Json.obj(
        "location"   -> objLocation,
        "deviceType" -> objDevType,
        "probConn"   -> objConn,
        "probErr"    -> objErr,
        "probWarn"   -> objWarn,
        "probCrit"   -> objCrit,
        "probWarnAlert"   -> objWarnAlert,
        "suyhao"     -> suyhao,
        "broken"     -> broken,
        "olt"        -> olts
      )

      Ok(Json.toJson(rs))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }
}


