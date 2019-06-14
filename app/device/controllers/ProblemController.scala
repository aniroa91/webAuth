package controllers

import javax.inject.Inject
import javax.inject.Singleton

import device.models.ProblemResponse
import device.utils.LocationUtils
import play.api.Logger
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
  val logger: Logger = Logger(this.getClass())

  def index =  withAuth { username => implicit request =>
    val province = if(request.session.get("verifiedLocation").get.equals("1")){
      // only show 5 chart: Devices Get Problem With Critical Alert, Devices Get Problem With Warn Alert, Devices Get Problem With Broken Cable,
      // Devices Get Problem With Suy Hao Index, Devices Get Problem With OLT Error
      request.session.get("location").get.split(",").map(x=> LocationUtils.getCodeProvincebyName(x)).mkString("|")
    } else ""
    try {
      val t0 = System.currentTimeMillis()
      val weekly = Await.result(ProblemService.listWeekly(), Duration.Inf)
      val lstProvince = Await.result(ProblemService.listProvinceByWeek(weekly(0)._2, province), Duration.Inf)
      val location = lstProvince.map(x=> LocationUtils.getRegionByProvWorld(x._1) -> LocationUtils.getNameProvWorld(x._1)).filter(x=> x._1 != "").distinct.sorted
      var deviceType = lstProvince.map(x=> x._2 -> x._3).groupBy(x=> x._1).map(y=> y._1 -> y._2.map(x=> x._2).sum).toArray
      if(!province.equals("")) deviceType= deviceType.filter(x=> x._1 == "switch").filter(x=> x._1 == "host").filter(x=> x._1 == "power")

      val probConnectivity = if(province.equals("")) Await.result(ProblemService.listProbconnectivity(weekly(0)._2, lstProvince.map(x=> x._1).distinct.toArray), Duration.Inf) else Seq[(String, Long, Long)]()
      val probError = if(province.equals("")) Await.result(ProblemService.listProbError(weekly(0)._2, lstProvince.map(x=> x._1).distinct.toArray, "*"), Duration.Inf) else Seq[(String, Long)]()
      val probWarn = if(province.equals("")) Await.result(ProblemService.listProbWarning(weekly(0)._2, lstProvince.map(x=> x._1).distinct.toArray, "*"), Duration.Inf) else Seq[(String, Long)]()
      // Devices Get Problem With Critical Alert
      val critAlert = Await.result(ProblemService.listCritAlerts(weekly(0)._2, lstProvince.map(x=> x._1).distinct.toArray, "*"), Duration.Inf)
      // Devices Get Problem With Warn Alert
      val warnAlert = Await.result(ProblemService.listWarnAlerts(weekly(0)._2, lstProvince.map(x=> x._1).distinct.toArray, "*"), Duration.Inf)
      //  Devices Get Problem With Suy Hao Index
      val suyhao = Await.result(ProblemService.listSuyhao(weekly(0)._2, lstProvince.map(x=> x._1).distinct.toArray), Duration.Inf).map(x=> (x._1, x._2, x._3, CommonService.format2Decimal(x._4)))
      // Devices Get Problem With Broken Cable
      val broken = Await.result(ProblemService.listBroken(weekly(0)._2, lstProvince.map(x=> x._1).distinct.toArray), Duration.Inf)
      //  Devices Get Problem With OLT Error
      val olts = Await.result(ProblemService.listOLT(weekly(0)._2, lstProvince.map(x=> x._1).distinct.toArray), Duration.Inf)
                .map(x=> (x._1, x._2, CommonService.formatPattern(x._3), CommonService.formatPattern(x._4), CommonService.formatPattern(x._5),
                  CommonService.formatPattern(x._6), CommonService.formatPattern(x._7), CommonService.formatPattern(x._8)))
      logger.info("Time:"+(System.currentTimeMillis() -t0))
      Ok(device.views.html.weekly.problem(ProblemResponse(weekly, location, deviceType, probConnectivity, probError, probWarn, critAlert, warnAlert, suyhao, broken, olts), username,province, controllers.routes.ProblemController.index()))
    }
    catch{
      case e: Exception => Ok(device.views.html.weekly.problem(null, username,province, controllers.routes.ProblemController.index()))
    }
  }

  def getJsonProblem() = withAuth {username => implicit request =>
    try{
      val t0 = System.currentTimeMillis()
      val date = request.body.asFormUrlEncoded.get("date").head
      val province = request.body.asFormUrlEncoded.get("province").head

      var arrProv = province.split(",").filter(x=> x != "All").map(x=> LocationUtils.getCodeProvWorld(x))
      if(arrProv.indexOf("BRU") >= 0) arrProv :+= "BRA"
      val lstProvince = Await.result(ProblemService.listProvinceByWeek(date, ""), Duration.Inf).filter(x=> arrProv.indexOf(x._1) >=0)
      val location = lstProvince.map(x=> LocationUtils.getRegionByProvWorld(x._1) -> LocationUtils.getNameProvWorld(x._1)).filter(x=> x._1 != "").distinct.sorted
      var deviceType = lstProvince.map(x=> x._2 -> x._3).groupBy(x=> x._1).map(y=> y._1 -> y._2.map(x=> x._2).sum).toArray
      if(!province.equals("")) deviceType = deviceType.filter(x=> x._1 == "switch").filter(x=> x._1 == "host").filter(x=> x._1 == "power")

      val probConnect = Await.result(ProblemService.listProbconnectivity(date, arrProv), Duration.Inf)
      val probError = Await.result(ProblemService.listProbError(date, arrProv, "*"), Duration.Inf)
      val probWarn = Await.result(ProblemService.listProbWarning(date, arrProv, "*"), Duration.Inf)
      val critAlert = Await.result(ProblemService.listCritAlerts(date, arrProv, "*"), Duration.Inf)
      val warnAlert = Await.result(ProblemService.listWarnAlerts(date, arrProv, "*"), Duration.Inf)
      val suyhao = Await.result(ProblemService.listSuyhao(date, arrProv), Duration.Inf).map(x=> (x._1, x._2, x._3, CommonService.format2Decimal(x._4)))
      val broken = Await.result(ProblemService.listBroken(date, arrProv), Duration.Inf)
      val olts = Await.result(ProblemService.listOLT(date, arrProv), Duration.Inf)
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
      logger.info("TimeJson:"+(System.currentTimeMillis() -t0))
      Ok(Json.toJson(rs))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  def getJsonByDevType() = withAuth {username => implicit request =>
    try{
      val t0 = System.currentTimeMillis()
      val date = request.body.asFormUrlEncoded.get("date").head
      val province = request.body.asFormUrlEncoded.get("province").head
      val devType = request.body.asFormUrlEncoded.get("devType").head

      var arrProv = province.split(",").filter(x=> x != "All").map(x=> LocationUtils.getCodeProvWorld(x))
      if(arrProv.indexOf("BRU") >= 0) arrProv :+= "BRA"
      val probError = Await.result(ProblemService.listProbError(date, arrProv, devType), Duration.Inf)
      val probWarn = Await.result(ProblemService.listProbWarning(date, arrProv, devType), Duration.Inf)
      val critAlert = Await.result(ProblemService.listCritAlerts(date, arrProv, devType), Duration.Inf)
      val warnAlert = Await.result(ProblemService.listWarnAlerts(date, arrProv, devType), Duration.Inf)

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
        "probErr"    -> objErr,
        "probWarn"   -> objWarn,
        "probCrit"   -> objCrit,
        "probWarnAlert"   -> objWarnAlert
      )
      logger.info("TimeJson:"+(System.currentTimeMillis() -t0))
      Ok(Json.toJson(rs))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }
}



