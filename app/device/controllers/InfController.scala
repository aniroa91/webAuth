package controllers

import play.api.mvc._
import controllers.Secured
import javax.inject.Inject
import javax.inject.Singleton

import model.device.InfResponse
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.BrasService
import services.domain.CommonService

import scala.concurrent.Await
import scala.concurrent.duration.Duration


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class InfController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured{
  val logger: Logger = Logger(this.getClass())

  def inf(id: String) =  withAuth { username => implicit request =>
    try {
      val t01 = System.currentTimeMillis()
      val infDown = Await.result(BrasService.getInfDownMudule("*"),Duration.Inf)
      logger.info("t01: " + (System.currentTimeMillis() - t01))
      val t02 = System.currentTimeMillis()
      val userDown = Await.result(BrasService.getUserDownMudule("*"),Duration.Inf)
      logger.info("t02: " + (System.currentTimeMillis() - t02))
      val t03 = System.currentTimeMillis()
      val spliter = Await.result(BrasService.getSpliterMudule("*"),Duration.Inf)
      logger.info("t03: " + (System.currentTimeMillis() - t03))
      val t04 = System.currentTimeMillis()
      val sfLofi = Await.result(BrasService.getSflofiMudule("*"),Duration.Inf)
      logger.info("t04: " + (System.currentTimeMillis() - t04))
      val t05 = System.currentTimeMillis()
      val indexRouge = BrasService.getIndexRougeMudule("*")
      logger.info("t05: " + (System.currentTimeMillis() - t05))
      val t06 = System.currentTimeMillis()
      val totalOutlier = Await.result(BrasService.getTotalOutlier(), Duration.Inf).sum
      logger.info("t06: " + (System.currentTimeMillis() - t06))
      logger.info("time: " + (System.currentTimeMillis() - t01))
      Ok(device.views.html.inf(username,InfResponse(userDown,infDown,spliter,sfLofi,indexRouge,totalOutlier),id))
    }
    catch{
      case e: Exception => Ok(device.views.html.inf(username,null,id))
    }
  }

  def getSigLogInfjson(id: String) = Action { implicit request =>
    try{
      val resSiglog = BrasService.getSigLogInfjson(id.trim())
      val resError =  Await.result(BrasService.getErrorHistory(id.trim()),Duration.Inf)
      val jsError = Json.obj(
        "time" -> resError.map(x=>x._1.substring(0,x._1.indexOf("."))),
        "error" -> resError.map({ x =>x._2})
      )
      val jsSiglog = Json.obj(
        "time" -> resSiglog._2.map(x=>x._1),
        "logoff" -> resSiglog._2.map({ t =>t._2}),
        "signin" -> resSiglog._1.map({ t => t._2})
      )

      val jsInf = Json.obj(
        "jsSiglog" -> jsSiglog,
        "jsError" -> jsError
      )
      Ok(Json.toJson(jsInf))
    }
    catch{
      case e: Exception => Ok("error")
    }
  }

  def exportCSV(date: String) = Action { implicit request =>
    try{
      //println(date)
      var status = "Ok"
      val t01 = System.currentTimeMillis()
      val sfLofi = Await.result(BrasService.getSflofiMudule(date), Duration.Inf)
        .map(x => (x._1, x._2, x._3, x._7, x._8, x._4, x._5, x._9, x._10)).toArray
      logger.info("timSf: " + (System.currentTimeMillis() - t01))
      //val data = Array(("Date Time", "Module", "Host", "User Down", "Inf Down", "Sf Error", "Lofi Error", "Rouge Error", "Lost Signal")) ++: sfLofi
      val rs = Json.obj(
        "data" -> sfLofi
      )
      Ok(Json.toJson(rs))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  def getHostMonitor(host: String) = Action { implicit request =>
    try{
      //val t02 = System.currentTimeMillis()
      val rsHost =  Await.result(BrasService.getHostMonitor(host),Duration.Inf)
      val rsGraph = BrasService.getSiglogContract(host)
      val sigLog = rsHost.map(x=> (x._1,x._2,x._3,CommonService.getSigLogByNameContract(x._3,rsGraph)))
      val jsInf = Json.obj(
        "host" -> sigLog,
        "module" -> rsHost.map(x=> x._1).distinct,
        "totalClient"-> rsHost.map(x=> x._3).distinct.length,
        "totalSpliter"-> rsHost.map(x=> x._2).distinct.length,
        "totalModule"-> rsHost.map(x=> x._1).distinct.length,
        "totalSignin" -> sigLog.filter(x=> x._4 == "SignIn").length,
        "totalLogoff" -> sigLog.filter(x=> x._4 == "LogOff").length
      )
      //println("time: "+(System.currentTimeMillis() - t02))
      Ok(Json.toJson(jsInf))
    }
    catch{
      case e: Exception => Ok("error")
    }
  }

  def confirmLabel(host: String,module: String,time: String) = Action { implicit request =>
    try{
      val res =  Await.result(BrasService.confirmLabelInf(host,module,time), Duration.Inf)
      Ok(Json.toJson(res))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  def rejectLabel(host: String,module: String,time: String) = Action { implicit request =>
    try{
      val res =  Await.result(BrasService.rejectLabelInf(host,module,time), Duration.Inf)
      Ok(Json.toJson(res))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

}