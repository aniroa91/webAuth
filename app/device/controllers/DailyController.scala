package controllers

import play.api.mvc._
import controllers.Secured
import javax.inject.Inject
import javax.inject.Singleton

import device.utils.{CommonUtils, LocationUtils}
import model.device.{DailyResponse, InfResponse}
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.text
import play.api.libs.json.Json
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.{BrasService, HostService}
import services.domain.CommonService

import scala.concurrent.Await
import scala.concurrent.duration.Duration


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class DailyController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured{
  val logger: Logger = Logger(this.getClass())
  val formDaily = Form(
    mapping(
      "csrfToken" -> text,
      "day" -> text
    )(DayPicker.apply)(DayPicker.unapply)
  )

  // This will be the action that handles our form post
  def getFormDayPicker = withAuth { username => implicit request: Request[AnyContent] =>

    val errorFunction = { formWithErrors: Form[controllers.DayPicker] =>
      println("error")
      Ok(device.views.html.daily(null,CommonService.getCurrentDay(), username))
    }

    val successFunction = { data: controllers.DayPicker =>
      println("done")
      Redirect(routes.DailyController.getDaily).flashing("day" -> data.day)
    }

    val formValidationResult = formDaily.bindFromRequest
    formValidationResult.fold(errorFunction, successFunction)
  }

  def getDaily =  withAuth { username => implicit request =>
    try{
      logger.info("======START SERVICE DAILY PAGE======")
      val t0 = System.currentTimeMillis()
      val day = request.flash.get("day").getOrElse(CommonService.getCurrentDay())
      val rsSiglog        = BrasService.getSigLogRegionDaily(day, "")
      logger.info("t0:"+(System.currentTimeMillis() -t0))
      val t1 = System.currentTimeMillis()
      val rsErrorsDevice  = BrasService.getDeviceErrorsRegionDaily(day).map(x=> x._1 -> (x._2+x._3+x._4+x._5+x._6+x._7))
      logger.info("t1:"+(System.currentTimeMillis() -t1))
      val t2 = System.currentTimeMillis()
      val rsNoticeOpsview = BrasService.getServiceNoticeRegionDaily(day, "*")
      logger.info("t2:"+(System.currentTimeMillis() -t2))
      val t3 = System.currentTimeMillis()
      val rsErrorsInf     = BrasService.getInfErrorsDaily(day, "*").groupBy(x=> x._1).map(x=> x._1 -> x._2.map(x=> x._5).sum).toArray.sorted
      logger.info("t3:"+(System.currentTimeMillis() -t3))
      val t4 = System.currentTimeMillis()
      val arrOpsview      = BrasService.getOpviewBytimeResponse("*", day, 0)
      val opviewBytime    = (0 until 24).map(x => x -> CommonService.getIntValueByKey(arrOpsview, x)).toArray
      val arrKibana       = BrasService.getKibanaBytimeES("*", day).groupBy(_._1).mapValues(_.map(_._2).sum).toArray
      val kibanaBytime    = (0 until 24).map(x => x -> CommonService.getIntValueByKey(arrKibana, x)).toArray
      logger.info("t4:"+(System.currentTimeMillis() -t4))
      val t5 = System.currentTimeMillis()
      val rsLogsigBytime  = BrasService.getSigLogByDaily("*", day)
      logger.info("t5:"+(System.currentTimeMillis() -t5))
      val t6 = System.currentTimeMillis()
      val rsErrorHostDaily = Await.result(BrasService.getErrorHostdaily("*",day), Duration.Inf).toArray
      logger.info("t6:"+(System.currentTimeMillis() -t6))
      val t7 = System.currentTimeMillis()
      val brasOutlier = Await.result(BrasService.getBrasOutlierDaily(day), Duration.Inf).filter(x=> x._1 != "" && x._1.split("-").length == 4 && x._1.split("-")(0).length == 3)
        .map(x=> (LocationUtils.getRegion(x._1.split("-")(0)), x._2)).toArray.sorted
      logger.info("t7:"+(System.currentTimeMillis() -t7))
      val t8 = System.currentTimeMillis()
      val infOutlier = Await.result(BrasService.getInfAccessOutlierDaily(day), Duration.Inf).filter(x=> x._1 != "" && x._1.split("-").length == 4 && x._1.split("-")(0).length == 3)
        .map(x=> (LocationUtils.getRegion(x._1.split("-")(0)), x._3)).toArray.sorted
      logger.info("t8:"+(System.currentTimeMillis() -t8))
      val t9 = System.currentTimeMillis()
      val ticketIssues = Await.result(BrasService.getTicketIssue(day), Duration.Inf)
      val coreIssue = ticketIssues.filter(x=> x._1 == "Hệ thống Core IP").map(x=> (LocationUtils.getRegionByProvWorld(x._2),LocationUtils.getNameProvWorld(x._2), x._3, x._4)).sorted
      val noneCoreIssue = ticketIssues.filter(x=> x._1 == "Hệ Thống Access" || x._1== "Hệ thống Ngoại vi").map(x=> (LocationUtils.getRegionByProvWorld(x._2),LocationUtils.getNameProvWorld(x._2), x._3, x._4)).sorted

      logger.info("t9:"+(System.currentTimeMillis() -t9))
      logger.info("tAll:"+(System.currentTimeMillis() -t0))
      logger.info("======END SERVICE DAILY PAGE======")
      Ok(device.views.html.daily(DailyResponse((rsSiglog._1, rsSiglog._2), rsErrorsDevice, rsNoticeOpsview,(kibanaBytime, opviewBytime), rsLogsigBytime, rsErrorsInf, rsErrorHostDaily, brasOutlier, infOutlier, (coreIssue, noneCoreIssue)), day, username))
    }
    catch{
      case e : Exception => Ok(device.views.html.daily(null, CommonService.getCurrentDay(), username))
    }
  }

  def drilldownBrasOutlier(id: String, day: String) = Action { implicit request =>
    try{
      val rs = id match {
        // get inf by All
        case id if(id.equals("*")) => {
          val brasOutlier = Await.result(BrasService.getBrasOutlierDaily(day), Duration.Inf).filter(x=> x._1 != "" && x._1.split("-").length == 4 && x._1.split("-")(0).length == 3)
            .map(x=> (LocationUtils.getRegion(x._1.split("-")(0)), x._2)).toArray.sorted
          Json.obj(
            "data" -> brasOutlier,
            "key"   -> brasOutlier.map(x=> x._1).distinct.sorted,
            "location" -> "region"
          )
        }
        // get inf by Region
        case id if(id.substring(id.indexOf(" ")+1).matches("^\\d+$")) => {
          val brasOutlier = Await.result(BrasService.getBrasOutlierDaily(day), Duration.Inf).filter(x=> x._1 != "" && x._1.split("-").length == 4 && x._1.split("-")(0).length == 3)
            .map(x=> (LocationUtils.getRegion(x._1.split("-")(0)),LocationUtils.getNameProvincebyCode(x._1.split("-")(0)) ,x._2)).filter(x=> x._1 == id)
            .map(x=> (x._2, x._3)).toArray.sorted
          Json.obj(
            "data" -> brasOutlier,
            "key"   -> brasOutlier.map(x=> x._1).distinct.sorted,
            "location" -> "region"
          )
        }
        // get inf by Province
        case id if(!id.substring(id.indexOf(" ")+1).matches("^\\d+$") && id.indexOf("-")<0) => {
          val brasOutlier = Await.result(BrasService.getBrasOutlierDaily(day), Duration.Inf).filter(x=> x._1 != "" && x._1.split("-").length == 4 && x._1.split("-")(0).length == 3)
            .map(x=> (LocationUtils.getNameProvincebyCode(x._1.split("-")(0)), x._1, x._2)).filter(x=> x._1 == id)
            .map(x=> (x._2, x._3)).toArray.sorted
          Json.obj(
            "data" -> brasOutlier,
            "key"   -> brasOutlier.map(x=> x._1).distinct.sorted,
            "location" -> "province"
          )
        }
      }
      Ok(Json.toJson(rs))
    }
    catch {
      case e: Exception => Ok("Error")
    }
  }

  def drilldownSigLogDaily(id: String,day: String) = Action {implicit  request =>
    try{
      val rs = id match {
        // get inf by Region
        case id if(id.equals("*")) => {
          val sigLog = BrasService.getSigLogRegionDaily(day, "")
          Json.obj(
            "cates"   -> sigLog._1.map(x=> x._1),
            "logoff"  -> sigLog._1,
            "signin"  -> sigLog._2
          )
        }
        // get inf by Region
        case id if(id.substring(id.indexOf(" ")+1).matches("^\\d+$")) => {
          val sigLog = BrasService.getSigLogProvinceDaily(day, id)
          Json.obj(
            "cates"   -> sigLog._1.map(x=> x._1),
            "logoff"  -> sigLog._1,
            "signin"  -> sigLog._2
          )
        }
        // get inf by Province
        case id if(!id.substring(id.indexOf(" ")+1).matches("^\\d+$") && id.indexOf("-")<0) => {
          val provinceName = LocationUtils.getCodeProvincebyName(id)
          val sigLog = BrasService.getSigLogBrasDaily(day, provinceName)
          Json.obj(
            "cates"   -> sigLog._1.map(x=> x._1),
            "logoff"  -> sigLog._1,
            "signin"  -> sigLog._2
          )
        }
        // get inf by Bras
        case id if(!id.substring(id.indexOf(" ")+1).matches("^\\d+$") && id.indexOf("-")>=0) =>{
          val sigLog = BrasService.getSigLogHostDaily(day, id)
          Json.obj(
            "cates"   -> sigLog._1.map(x=> x._1),
            "logoff"  -> sigLog._1,
            "signin"  -> sigLog._2
          )
        }
      }
      Ok(Json.toJson(rs))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  def drilldownErrorDevice(id: String,day: String, err: String) = Action {implicit  request =>
    try{
      val rs = id match {
        // get inf by Region
        case id if(id.equals("*")) => {
          val errors = BrasService.getDeviceErrorsRegionDaily(day)
          val data = if(err.equals("*")) errors.map(x=> x._1 -> (x._2+x._3+x._4+x._5+x._6+x._7)) else if(err.equals("alert")) errors.map(x=> x._1 -> x._2) else if(err.equals("crit")) errors.map(x=> x._1 -> x._3)
          else if(err.equals("emerg")) errors.map(x=> x._1 -> x._4) else if(err.equals("err")) errors.map(x=> x._1 -> x._5) else if(err.equals("notice")) errors.map(x=> x._1 -> x._6)
          else if(err.equals("warning")) errors.map(x=> x._1 -> x._7)
          Json.obj(
            "data"   -> data.asInstanceOf[Array[(String, Double)]]
          )
        }
        // get inf by Region
        case id if(id.substring(id.indexOf(" ")+1).matches("^\\d+$")) => {
          val errors = BrasService.getDeviceErrorsProvinceDaily(day, id)
          val data = if(err.equals("*")) errors.map(x=> x._1 -> (x._2+x._3+x._4+x._5+x._6+x._7)) else if(err.equals("alert")) errors.map(x=> x._1 -> x._2) else if(err.equals("crit")) errors.map(x=> x._1 -> x._3)
          else if(err.equals("emerg")) errors.map(x=> x._1 -> x._4) else if(err.equals("err")) errors.map(x=> x._1 -> x._5) else if(err.equals("notice")) errors.map(x=> x._1 -> x._6)
          else if(err.equals("warning")) errors.map(x=> x._1 -> x._7)

          Json.obj(
            "data"   -> data.asInstanceOf[Array[(String, Double)]]
          )
        }
        // get inf by Province
        case id if(!id.substring(id.indexOf(" ")+1).matches("^\\d+$") && id.indexOf("-")<0) => {
          val errors = BrasService.getDeviceErrorsBrasDaily(day, id)
          val data = if(err.equals("*")) errors.map(x=> x._1 -> (x._2+x._3+x._4+x._5+x._6+x._7)) else if(err.equals("alert")) errors.map(x=> x._1 -> x._2) else if(err.equals("crit")) errors.map(x=> x._1 -> x._3)
          else if(err.equals("emerg")) errors.map(x=> x._1 -> x._4) else if(err.equals("err")) errors.map(x=> x._1 -> x._5) else if(err.equals("notice")) errors.map(x=> x._1 -> x._6)
          else if(err.equals("warning")) errors.map(x=> x._1 -> x._7)

          Json.obj(
            "data"   -> data.asInstanceOf[Array[(String, Double)]]
          )
        }
      }
      Ok(Json.toJson(rs))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  def drilldownInfOutlier(id: String, day: String) = Action {implicit  request =>
    try{
      val rs = id match {
        // get inf by All
        case id if(id.equals("*")) => {
          val outliers = Await.result(BrasService.getInfAccessOutlierDaily(day), Duration.Inf).filter(x=> x._1 != "" && x._1.split("-").length == 4 && x._1.split("-")(0).length == 3)
            .map(x=> (LocationUtils.getRegion(x._1.split("-")(0)), x._3)).toArray.sorted
          Json.obj(
            "data" -> outliers,
            "key"   -> outliers.map(x=> x._1).distinct.sorted,
            "location" -> "region"
          )
        }
        // get inf by Region
        case id if(id.substring(id.indexOf(" ")+1).matches("^\\d+$")) => {
          val outliers = Await.result(BrasService.getInfAccessOutlierDaily(day), Duration.Inf).filter(x=> x._1 != "" && x._1.split("-").length == 4 && x._1.split("-")(0).length == 3)
            .map(x=> (LocationUtils.getRegion(x._1.split("-")(0)), LocationUtils.getNameProvincebyCode(x._1.split("-")(0)),x._1, x._3)).filter(x=> x._1 == id)
            .map(x=> (x._2, x._4)).toArray.sorted
          Json.obj(
            "data" -> outliers,
            "key"   -> outliers.map(x=> x._1).distinct.sorted,
            "location" -> "region"
          )
        }
        // get inf by Province
        case id if(!id.substring(id.indexOf(" ")+1).matches("^\\d+$") && id.indexOf("-")<0) => {
          val outliers = Await.result(BrasService.getInfAccessOutlierDaily(day), Duration.Inf).filter(x=> x._1 != "" && x._1.split("-").length == 4 && x._1.split("-")(0).length == 3)
            .map(x=> ( LocationUtils.getNameProvincebyCode(x._1.split("-")(0)),x._1, x._3)).filter(x=> x._1 == id).map(x=> (x._2, x._3)).toArray.sorted
          Json.obj(
            "data"     -> outliers,
            "key"      -> outliers.map(x=> x._1).distinct.sorted,
            "location" -> "province"
          )
        }
        // get inf by Bras
        case id if(!id.substring(id.indexOf(" ")+1).matches("^\\d+$") && id.indexOf("-")>=0) =>{
          val outliers = Await.result(BrasService.getInfAccessOutlierDaily(day), Duration.Inf).filter(x=> x._1 != "" && x._1.split("-").length == 4 && x._1.split("-")(0).length == 3)
            .filter(x=> x._1 == id).map(x=> (x._2, x._3)).toArray.sorted
          Json.obj(
            "data" -> outliers,
            "key"   -> outliers.map(x=> x._1).distinct.sorted,
            "location" -> "bras"
          )
        }
      }
      Ok(Json.toJson(rs))
    }
    catch {
      case e: Exception => Ok("Error")
    }
  }

  def drilldownInfErrorsDaily(id: String,day: String, err: String) = Action {implicit  request =>
    try{
      val rs = id match {
        // get inf by Region
        case id if(id.equals("*")) => {
          BrasService.getInfErrorsDaily(day, err).groupBy(x=> x._1).map(x=> x._1 -> x._2.map(x=> x._5).sum).toArray.sorted
        }
        // get inf by Region
        case id if(id.substring(id.indexOf(" ")+1).matches("^\\d+$")) => {
          BrasService.getInfErrorsDaily(day, err).filter(x=> x._1 == id).groupBy(x=> x._2).map(x=> x._1 -> x._2.map(x=> x._5).sum).toArray.sorted
        }
        // get inf by Province
        case id if(!id.substring(id.indexOf(" ")+1).matches("^\\d+$") && id.indexOf("-")<0) => {
          BrasService.getInfErrorsDaily(day, err).filter(x=> x._2 == id).groupBy(x=> x._3).map(x=> x._1 -> x._2.map(x=> x._5).sum).toArray.sortWith((x, y)=> x._2>y._2).slice(0,10)
        }
        // get inf by Bras
        case id if(!id.substring(id.indexOf(" ")+1).matches("^\\d+$") && id.indexOf("-")>=0) =>{
          BrasService.getInfErrorsDaily(day, err).filter(x=> x._3 == id).groupBy(x=> x._4).map(x=> x._1 -> x._2.map(x=> x._5).sum).toArray.sortWith((x, y)=> x._2>y._2).slice(0,10)
        }
      }
      Ok(Json.toJson(Json.obj("data" -> rs)))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  def getErrInfHourly(_type: String, id: String, day: String, bras: String) = Action {implicit request =>
    try{
      // _type: 0 hourly time
      // _type: 1 frame time
      val jsError = if(_type == "0"){
        val rs = Await.result(BrasService.getErrorHostdaily(bras, day), Duration.Inf).toArray
        id match {
          case id if(id == "0") => {
            Json.obj(
              "cates" -> (0 until 24).map(x=> x+"h"),
              "data"  -> rs
            )
          }
          case id if(id == "1") => {
            val rsErrorHost = rs.map(x=> (CommonUtils.getRangeTime(x._1.toDouble), x._2, x._3, x._4, x._5, x._6, x._7,x._8))
              .groupBy(x=> x._1).map(x=> (x._1, x._2.map(y=> y._2).sum, x._2.map(y=> y._3).sum, x._2.map(y=> y._4).sum, x._2.map(y=> y._5).sum, x._2.map(y=> y._6).sum, x._2.map(y=> y._7).sum, x._2.map(y=> y._8).sum)).toArray
            Json.obj(
              "cates" -> CommonUtils.rangeTime.map(x=> x._2).toArray.sorted,
              "data"  -> rsErrorHost
            )
          }
        }
      }
      else{
        val rs = HostService.getErrorHostbyHourly(bras, day)
        id match {
          case id if(id == "0") => {
            Json.obj(
              "cates" -> (0 until 24).map(x=> x+"h"),
              "data"  -> rs
            )
          }
          case id if(id == "1") => {
            val rsErrorHost = rs.map(x=> (CommonUtils.getRangeTime(x._1.toDouble), x._2, x._3, x._4, x._5, x._6, x._7, x._8))
              .groupBy(x=> x._1).map(x=> (x._1, x._2.map(y=> y._2).sum, x._2.map(y=> y._3).sum, x._2.map(y=> y._4).sum, x._2.map(y=> y._5).sum, x._2.map(y=> y._6).sum, x._2.map(y=> y._7).sum, x._2.map(y=> y._8).sum)).toArray
            Json.obj(
              "cates" -> CommonUtils.rangeTime.map(x=> x._2).toArray.sorted,
              "data"  -> rsErrorHost
            )
          }
        }
      }
      Ok(Json.toJson(jsError))
    }
    catch {
      case e: Exception => Ok("Error")
    }
  }

  def getServNoticeByType(noti: String,day: String) = Action { implicit request =>
    try{
      val rs = BrasService.getServiceNoticeRegionDaily(day, noti)
      val json = Json.obj(
        "data"  -> rs,
        "cates" -> rs.map(x=> x._1).distinct.toSeq.sorted
      )
      Ok(Json.toJson(json))
    }
    catch {
      case e: Exception => Ok("Error")
    }
  }

  def getDailyByBrasLocation(id: String, day: String) = Action { implicit request =>
    try{
      val bras_id = if(id.equals("All")) "*" else id
      val daystr  = if(id.equals("All")) day else day+"/"+day
      // SigLog by time
      val rsLogsigBytime  = BrasService.getSigLogByDaily(bras_id, day)
      val jsonSigLog = Json.obj(
        "signin"  -> rsLogsigBytime.signin.map(x=> x._2),
        "logoff" -> rsLogsigBytime.logoff.map(x=> x._2),
        "signin_clients"  -> rsLogsigBytime.signin.map(x=> x._3),
        "logoff_clients" -> rsLogsigBytime.logoff.map(x=> x._3)
      )
      // Kibana opsview by time
      val arrOpsview      = BrasService.getOpviewBytimeResponse(bras_id, daystr, 0)
      val opviewBytime    = (0 until 24).map(x => x -> CommonService.getIntValueByKey(arrOpsview, x)).toArray
      val arrKibana       = BrasService.getKibanaBytimeES(bras_id, daystr).groupBy(_._1).mapValues(_.map(_._2).sum).toArray
      val kibanaBytime    = (0 until 24).map(x => x -> CommonService.getIntValueByKey(arrKibana, x)).toArray
      val kibaOps = Json.obj(
        "kibana"  -> kibanaBytime,
        "opsview" -> opviewBytime
      )
      // Total errors hourly
      val rsErrorHost = Await.result(BrasService.getErrorHostdaily(bras_id,day), Duration.Inf).toArray
      val rs = Json.obj(
        "kibanaOpsview" -> kibaOps,
        "errorHourly"   -> rsErrorHost,
        "sigLogBytime"  -> jsonSigLog
      )
      Ok(Json.toJson(rs))
    }
    catch {
      case e: Exception => Ok("Error")
    }
  }

}