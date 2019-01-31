package controllers

import javax.inject.Inject
import javax.inject.Singleton

import play.api.mvc._
import com.google.common.util.concurrent.AbstractService
import model.device.{NocCount, _}
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.text
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.{BrasService, HostService}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import controllers.Secured

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import play.api.libs.json.Json
import services.domain.CommonService
import services.domain.CommonService.formatYYYYmmddHHmmss

import scala.util.control.Breaks._
import play.api.Logger
import device.utils.{CommonUtils, LocationUtils}
import com.ftel.bigdata.utils.FileUtil
import javax.swing.JFileChooser
import javax.swing.filechooser.FileSystemView
import java.io._

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
case class BrasOutlier(csrfToken: String,_typeS: String,bras: String,date: String)
case class Test(group: String, message: String, age: Int)
case class MonthPicker(csrfToken: String,startMonth: String,endMonth: String)
case class DayPicker( csrfToken: String, day: String)

@Singleton
class DeviceController @Inject()(cc: MessagesControllerComponents) extends MessagesAbstractController(cc) with Secured{

  val logger: Logger = Logger(this.getClass())
  val formOverview = Form(
    mapping(
      "csrfToken" -> text,
      "startMonth" -> text,
      "endMonth" -> text
    )(MonthPicker.apply)(MonthPicker.unapply)
  )
  val formDaily = Form(
    mapping(
      "csrfToken" -> text,
      "day" -> text
    )(DayPicker.apply)(DayPicker.unapply)
  )
  val form = Form(
    mapping(
      "csrfToken" -> text,
      "_typeS" -> text,
      "bras" -> text,
      "date" -> text
    )(BrasOutlier.apply)(BrasOutlier.unapply)
  )

  // This will be the action that handles our form post
  def getFormMonthPicker = withAuth { username => implicit request: Request[AnyContent] =>

    val errorFunction = { formWithErrors: Form[controllers.MonthPicker] =>
      println("error")
      Ok(device.views.html.monthly.overview(username,null))
    }

    val successFunction = { data: controllers.MonthPicker =>
      println("done")
      Redirect(routes.DeviceController.overview).flashing("startMonth" -> data.startMonth, "endMonth" -> data.endMonth)
    }

    val formValidationResult = formOverview.bindFromRequest
    formValidationResult.fold(errorFunction, successFunction)
  }

  // index page Dashboard Device
  def overview =  withAuth { username => implicit request =>
    try {
      //val minMaxMonth = CommonService.getRangeCurrentMonth()
      val minMaxMonth = Await.result(BrasService.getMinMaxMonth(), Duration.Inf)
      val threeMonth = Await.result(BrasService.get3MonthLastest(),Duration.Inf)
      val fromMonth = if(request.flash.get("startMonth").toString != "None") request.flash.get("startMonth").get+"-01" else threeMonth(threeMonth.length-1)
      val toMonth = if(request.flash.get("endMonth").toString != "None") request.flash.get("endMonth").get+"-01" else threeMonth(0)
      val rangeMonth = CommonService.getAllMonthfromRange(fromMonth,toMonth)

      // get Total INF Errors By Months
      val mapProvinceTotalInf = Await.result(BrasService.getProvinceTotalInf(fromMonth,toMonth), Duration.Inf)
      val totalInf = mapProvinceTotalInf.map(x=> (x._1,LocationUtils.getRegion(x._2.trim),x._3)).groupBy(x=> (x._1,x._2)).mapValues(_.map(_._3).sum).toSeq.sorted.toArray.map(x=> (x._1._1,x._1._2,x._2))
      // get Total Service Monitor Notices By Months
      val mapProvinceOpsview = Await.result(BrasService.getProvinceOpsview(fromMonth,toMonth), Duration.Inf)
      val opsview = mapProvinceOpsview.map(x=> (x._1,LocationUtils.getNameProvincebyCode(x._2),LocationUtils.getRegion(x._2.trim), x._4,x._3)).toArray
      // get Total Device Errors By Months
      val mapProvinceKibana = Await.result(BrasService.getProvinceKibana(fromMonth,toMonth), Duration.Inf)
      val kibana = mapProvinceKibana.map(x=> (x._1,LocationUtils.getNameProvincebyCode(x._2),LocationUtils.getRegion(x._2.trim), x._4,x._3)).toArray
      // get Total SuyHao Not Pass By Months
      val mapSuyhao = Await.result(BrasService.getProvinceSuyhao(fromMonth,toMonth), Duration.Inf)
      val suyhao = mapSuyhao.map(x=> (x._1,LocationUtils.getNameProvincebyCode(x._2),LocationUtils.getRegion(x._2.trim), x._4,x._3,x._5)).toArray
      // get Total SignIn & LogOff By Months
      val mapSiglog = rangeMonth.map(x => x -> Await.result(BrasService.getSigLogByRegion(x+"-01"), Duration.Inf).map(x=> (LocationUtils.getRegion(x._1.trim),x._2,x._3,x._4,x._5)))
      val signIn = mapSiglog.map(x=>x._1-> x._2.groupBy(_._1).mapValues(_.map(_._2).sum).toSeq.sorted.map(x=> -x._2).toArray)
      val logoff = mapSiglog.map(x=>x._1-> x._2.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sorted.map(x=> x._2).toArray)
      val signIn_clients = mapSiglog.map(x=>x._1-> x._2.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sorted.map(x=> -x._2).toArray)
      val logoff_clients = mapSiglog.map(x=>x._1-> x._2.groupBy(_._1).mapValues(_.map(_._5).sum).toSeq.sorted.map(x=> x._2).toArray)
      // get Total Device Errors By Severity
      val mapCount = Await.result(BrasService.getProvinceCount(fromMonth+"/"+toMonth), Duration.Inf)
      val alertCount = mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._3)).toArray
      val critCount = mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._4)).toArray
      val warningCount = mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._5)).toArray
      val noticeCount = mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._6)).toArray
      val errCount = mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._7)).toArray
      val emergCount = mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._8)).toArray
      val nocCount = NocCount(alertCount,critCount,warningCount,noticeCount,errCount,emergCount)
      // get Statistic Of Poor Connections By Months
      val mapContract = Await.result(BrasService.getProvinceContract(fromMonth,toMonth), Duration.Inf)
      val contracts = mapContract.map(x=> (x._1,LocationUtils.getNameProvincebyCode(x._2),LocationUtils.getRegion(x._2.trim),x._3,x._4,x._5,x._6)).toArray
      // get Total Notices By Status
      val mapOpsviewType = Await.result(BrasService.getProvinceOpsviewType(fromMonth+"/"+toMonth), Duration.Inf)
      val opsviewType = mapOpsviewType.map(x=> (LocationUtils.getRegion(x._1),x._2,x._3,x._4,x._5)).toArray
      val ok_opsview = opsviewType.groupBy(_._1).mapValues(_.map(_._2).sum).toSeq.sorted.toArray
      val warning_opsview = opsviewType.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sorted.toArray
      val unknown_opsview = opsviewType.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sorted.toArray
      val crit_opsview = opsviewType.groupBy(_._1).mapValues(_.map(_._5).sum).toSeq.sorted.toArray
      val heatmapOpsview = (ok_opsview++warning_opsview++unknown_opsview++crit_opsview).groupBy(_._1).map{case (k,v) => k -> v.map(x=> x._2.toString).mkString("_")}.toSeq.sorted.toArray
      // get Total INF Errors By Type
      val mapInfType = Await.result(BrasService.getProvinceInfDownError(fromMonth+"/"+toMonth), Duration.Inf)
      val infDown = mapInfType.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._3)).toArray
      val userDown = mapInfType.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._4)).toArray
      val rougeError = mapInfType.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._5)).toArray
      val lostSignal = mapInfType.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._6)).toArray
      val infTypeError = InfTypeError(infDown,userDown,rougeError,lostSignal)

      Ok(device.views.html.monthly.overview(username,RegionOverview(TimePicker(minMaxMonth(0)._1.substring(0,minMaxMonth(0)._1.lastIndexOf("-")),minMaxMonth(0)._2.substring(0,minMaxMonth(0)._1.lastIndexOf("-")),fromMonth.substring(0,fromMonth.lastIndexOf("-")),toMonth.substring(0,toMonth.lastIndexOf("-")),rangeMonth),opsview,kibana,suyhao,SigLogRegion(signIn,logoff,signIn_clients,logoff_clients),nocCount,contracts,heatmapOpsview,infTypeError,totalInf)))
    }
    catch{
      case e: Exception => Ok(device.views.html.monthly.overview(username,null))
    }
  }

  // This will be the action that handles our form post
  def getFormDayPicker = withAuth { username => implicit request: Request[AnyContent] =>

    val errorFunction = { formWithErrors: Form[controllers.DayPicker] =>
      println("error")
      Ok(device.views.html.daily(null,CommonService.getCurrentDay(), username))
    }

    val successFunction = { data: controllers.DayPicker =>
      println("done")
      Redirect(routes.DeviceController.getDaily).flashing("day" -> data.day)
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
      logger.info("t566:"+(System.currentTimeMillis() -t5))
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
      logger.info("tAll:"+(System.currentTimeMillis() -t0))
      logger.info("======END SERVICE DAILY PAGE======")
      Ok(device.views.html.daily(DailyResponse((rsSiglog._1, rsSiglog._2), rsErrorsDevice, rsNoticeOpsview,(kibanaBytime, opviewBytime), rsLogsigBytime, rsErrorsInf, rsErrorHostDaily, brasOutlier, infOutlier), day, username))
    }
    catch{
      case e : Exception => Ok(device.views.html.daily(null, CommonService.getCurrentDay(), username))
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

  def getcountType(id: String,month: String) = Action { implicit request =>
    try{
      val monthRange = if(month.indexOf("/")>=0) month.split("/")(0)+"-01/"+month.split("/")(1)+"-01" else month
      // get NOC count by Region
      val mapCount = Await.result(BrasService.getProvinceCount(monthRange), Duration.Inf)
      val alertCount = mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._3)).toArray
      val critCount = mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._4)).toArray
      val warningCount = mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._5)).toArray
      val noticeCount = mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._6)).toArray
      val errCount = mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._7)).toArray
      val emergCount = mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._8)).toArray
      val res = NocCount(alertCount,critCount,warningCount,noticeCount,errCount,emergCount)
      val regionType = id match {
        case "AlertCount" => {
          Json.obj(
            "dtaByRegion" -> res.alertCount.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sorted,
            "data" -> res.alertCount,
            "arrRegion" -> res.alertCount.map(x=> x._1).asInstanceOf[Array[(String)]].toSeq.distinct.sorted
          )
        }
        case "CritCount" =>{
          Json.obj(
            "dtaByRegion" -> res.critCount.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sorted,
            "data" -> res.critCount,
            "arrRegion" -> res.critCount.map(x=> x._1).asInstanceOf[Array[(String)]].toSeq.distinct.sorted
          )
        }
        case "WarningCount" => {
          Json.obj(
            "dtaByRegion" -> res.warningCount.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sorted,
            "data" -> res.warningCount,
            "arrRegion" -> res.warningCount.map(x=> x._1).asInstanceOf[Array[(String)]].toSeq.distinct.sorted
          )
        }
        case "NoticeCount" => {
          Json.obj(
            "dtaByRegion" -> res.noticeCount.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sorted,
            "data" -> res.noticeCount,
            "arrRegion" -> res.noticeCount.map(x=> x._1).asInstanceOf[Array[(String)]].toSeq.distinct.sorted
          )
        }
        case "ErrCount" => {
          Json.obj(
            "dtaByRegion" -> res.errCount.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sorted,
            "data" -> res.errCount,
            "arrRegion" -> res.errCount.map(x=> x._1).asInstanceOf[Array[(String)]].toSeq.distinct.sorted
          )
        }
        case "EmergCount" => {
          Json.obj(
            "dtaByRegion" -> res.emergCount.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sorted,
            "data" -> res.emergCount,
            "arrRegion" -> res.emergCount.map(x=> x._1).asInstanceOf[Array[(String)]].toSeq.distinct.sorted
          )
        }
      }
      Ok(Json.toJson(regionType))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  def geterrorType(id: String,month: String) = Action { implicit request =>
    try{
      val monthRange = if(month.indexOf("/")>=0) month.split("/")(0)+"-01/"+month.split("/")(1)+"-01" else month
      val mapInfType = Await.result(BrasService.getProvinceInfDownError(monthRange), Duration.Inf)
      val infDown = mapInfType.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._3)).toArray
      val userDown = mapInfType.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._4)).toArray
      val rougeError = mapInfType.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._5)).toArray
      val lostSignal = mapInfType.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._6)).toArray
      val res = InfTypeError(infDown,userDown,rougeError,lostSignal)

      val regionType = id match {
        case "InfDown" => {
          Json.obj(
            "dtaByRegion" -> res.infDown.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sorted,
            "data" -> res.infDown,
            "arrRegion" -> res.infDown.map(x=> x._1).asInstanceOf[Array[(String)]].toSeq.distinct.sorted
          )
        }
        case "UseDown" => {
          Json.obj(
            "dtaByRegion" -> res.userDown.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sorted,
            "data" -> res.userDown,
            "arrRegion" -> res.userDown.map(x=> x._1).asInstanceOf[Array[(String)]].toSeq.distinct.sorted
          )
        }
        case "RougeError" => {
          Json.obj(
            "dtaByRegion" -> res.rougeError.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sorted,
            "data" -> res.rougeError,
            "arrRegion" -> res.rougeError.map(x=> x._1).asInstanceOf[Array[(String)]].toSeq.distinct.sorted
          )
        }
        case "LostSignal" => {
          Json.obj(
            "dtaByRegion" -> res.lostSignal.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sorted,
            "data" -> res.lostSignal,
            "arrRegion" -> res.lostSignal.map(x=> x._1).asInstanceOf[Array[(String)]].toSeq.distinct.sorted
          )
        }
      }
      Ok(Json.toJson(regionType))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  def getErrorKibana(_typeError: String,month: String) = Action { implicit request =>
    try{
      // get Top total kibana
      val topKibana = Await.result(BrasService.getTopKibana(month,_typeError), Duration.Inf).map(x=> (LocationUtils.getNameProvincebyCode(x._1), x._2, x._3))
      val kibanaObj = Json.obj(
        "data" -> topKibana,
        "dataProvince" -> topKibana.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sortWith(_._2 > _._2),
        "categories" -> topKibana.map(x=>x._1).asInstanceOf[Seq[String]].distinct
      )
      Ok(Json.toJson(kibanaObj))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  def getserviceStatus(_typeService: String,month: String) = Action { implicit request =>
    try{
      // get Top opsview status
      val topOpsview = Await.result(BrasService.getTopOpsview(month,_typeService), Duration.Inf).map(x=> (LocationUtils.getNameProvincebyCode(x._1), x._2, x._3))
      val rs = Json.obj(
        "data" -> topOpsview,
        "dataProvince" -> topOpsview.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sortWith(_._2 > _._2),
        "categories" -> topOpsview.map(x=>x._1).asInstanceOf[Seq[String]].distinct
      )
      Ok(Json.toJson(rs))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  def getOltPoorconn(_typeOlt: String,month: String) = Action { implicit request =>
    try{
      // get Top opsview status
      val topOLT = Await.result(BrasService.getTopPoorconn(month,_typeOlt), Duration.Inf)
      val rs = Json.obj(
        "month" -> month,
        "data" -> topOLT.map(x=>x._2),
        "categories" -> topOLT.map(x=>x._1)
      )
      Ok(Json.toJson(rs))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  def getInfTopErr(_typeInf: String,month: String) = Action { implicit request =>
    try{
      // get Top total Inf
      val topInf = Await.result(BrasService.getTopInf(month,_typeInf), Duration.Inf)
      val rs = Json.obj(
        "data" -> topInf,
        "dataBras" -> topInf.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sortWith(_._2 > _._2),
        "categories" -> topInf.map(x=>x._1).asInstanceOf[Seq[String]].distinct
      )
      Ok(Json.toJson(rs))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  // Tab Compare
  def compareByMonth(month: String) = Action{ implicit request =>
    try{
      val currMonth = if(month.equals("")) CommonService.getPreviousMonth() else month
      val prevMonth = CommonService.getPreviousMonth(currMonth)
      /* Box Signin & Logoff */
      val sigLog = Await.result(BrasService.getSigLogByMonth(currMonth), Duration.Inf)
      val signinObj = Json.obj(
        "currSignin" -> sigLog.filter(x=> x._1 == currMonth+"-01").map(x=> x._2).sum,
        "prevSignin" -> sigLog.filter(x=> x._1 == prevMonth+"-01").map(x=> x._2).sum,
        "percent"    -> CommonService.percent(sigLog.filter(x=> x._1 == currMonth+"-01").map(x=> x._2).sum.toLong, sigLog.filter(x=> x._1 == prevMonth+"-01").map(x=> x._2).sum.toLong)
      )
      val logoffObj = Json.obj(
        "currLogoff" -> sigLog.filter(x=> x._1 == currMonth+"-01").map(x=> x._3).sum,
        "prevLogoff" -> sigLog.filter(x=> x._1 == prevMonth+"-01").map(x=> x._3).sum,
        "percent"    -> CommonService.percent(sigLog.filter(x=> x._1 == currMonth+"-01").map(x=> x._3).sum.toLong, sigLog.filter(x=> x._1 == prevMonth+"-01").map(x=> x._3).sum.toLong)
      )
      /* Box Suyhao */
      val suyhao = Await.result(BrasService.getSuyhaoByMonth(currMonth), Duration.Inf)
      val suyhaoObj = Json.obj(
        "currSuyhao" -> suyhao.filter(x=> x._1 == currMonth+"-01").map(x=> x._2).sum,
        "prevSuyhao" -> suyhao.filter(x=> x._1 == prevMonth+"-01").map(x=> x._2).sum,
        "percent"    -> CommonService.percent(suyhao.filter(x=> x._1 == currMonth+"-01").map(x=> x._2).sum.toLong, suyhao.filter(x=> x._1 == prevMonth+"-01").map(x=> x._2).sum.toLong)
      )
      val notSuyhaoObj = Json.obj(
        "currNotsuyhao" -> suyhao.filter(x=> x._1 == currMonth+"-01").map(x=> x._3).sum,
        "prevNotsuyhao" -> suyhao.filter(x=> x._1 == prevMonth+"-01").map(x=> x._3).sum,
        "percent"       -> CommonService.percent(suyhao.filter(x=> x._1 == currMonth+"-01").map(x=> x._3).sum.toLong, suyhao.filter(x=> x._1 == prevMonth+"-01").map(x=> x._3).sum.toLong)
      )
      /* Box Bras Outlier & affected_clients */
      val brasOutlier = Await.result(BrasService.getBrasOutlierByMonth(currMonth), Duration.Inf)
      val brasObj = Json.obj(
        "currBras" -> brasOutlier.filter(x=> x._1 == currMonth+"-01").map(x=> x._2).sum,
        "prevBras" -> brasOutlier.filter(x=> x._1 == prevMonth+"-01").map(x=> x._2).sum,
        "percent"  -> CommonService.percent(brasOutlier.filter(x=> x._1 == currMonth+"-01").map(x=> x._2).sum.toLong, brasOutlier.filter(x=> x._1 == prevMonth+"-01").map(x=> x._2).sum.toLong)
      )
      val affectObj = Json.obj(
        "currAffect" -> brasOutlier.filter(x=> x._1 == currMonth+"-01").map(x=> x._3).sum,
        "prevAffect" -> brasOutlier.filter(x=> x._1 == prevMonth+"-01").map(x=> x._3).sum,
        "percent"  -> CommonService.percent(brasOutlier.filter(x=> x._1 == currMonth+"-01").map(x=> x._3).sum.toLong, brasOutlier.filter(x=> x._1 == prevMonth+"-01").map(x=> x._3).sum.toLong)
      )
      /* Box INF Outlier */
      val infOutlier = Await.result(BrasService.getInfOutlierByMonth(currMonth), Duration.Inf)
      val outliInfObj = Json.obj(
        "currOut" -> infOutlier.filter(x=> x._1 == currMonth+"-01").map(x=> x._2).sum,
        "prevOut" -> infOutlier.filter(x=> x._1 == prevMonth+"-01").map(x=> x._2).sum,
        "percent"  -> CommonService.percent(infOutlier.filter(x=> x._1 == currMonth+"-01").map(x=> x._2).sum.toLong, infOutlier.filter(x=> x._1 == prevMonth+"-01").map(x=> x._2).sum.toLong)
      )
      val clientInfObj = Json.obj(
        "currClient" -> infOutlier.filter(x=> x._1 == currMonth+"-01").map(x=> x._3).sum,
        "prevClient" -> infOutlier.filter(x=> x._1 == prevMonth+"-01").map(x=> x._3).sum,
        "percent"    -> CommonService.percent(infOutlier.filter(x=> x._1 == currMonth+"-01").map(x=> x._3).sum.toLong, infOutlier.filter(x=> x._1 == prevMonth+"-01").map(x=> x._3).sum.toLong)
      )
      val infDownCliObj = Json.obj(
        "currInfCli" -> infOutlier.filter(x=> x._1 == currMonth+"-01").map(x=> x._4).sum,
        "prevInfCli" -> infOutlier.filter(x=> x._1 == prevMonth+"-01").map(x=> x._4).sum,
        "percent"    -> CommonService.percent(infOutlier.filter(x=> x._1 == currMonth+"-01").map(x=> x._4).sum.toLong, infOutlier.filter(x=> x._1 == prevMonth+"-01").map(x=> x._4).sum.toLong)
      )
      val infDownOutObj = Json.obj(
        "currInfOut" -> infOutlier.filter(x=> x._1 == currMonth+"-01").map(x=> x._5).sum,
        "prevInfOut" -> infOutlier.filter(x=> x._1 == prevMonth+"-01").map(x=> x._5).sum,
        "percent"    -> CommonService.percent(infOutlier.filter(x=> x._1 == currMonth+"-01").map(x=> x._5).sum.toLong, infOutlier.filter(x=> x._1 == prevMonth+"-01").map(x=> x._5).sum.toLong)
      )
      val userDownCliObj = Json.obj(
        "currUserCli" -> infOutlier.filter(x=> x._1 == currMonth+"-01").map(x=> x._6).sum,
        "prevUserCli" -> infOutlier.filter(x=> x._1 == prevMonth+"-01").map(x=> x._6).sum,
        "percent"    -> CommonService.percent(infOutlier.filter(x=> x._1 == currMonth+"-01").map(x=> x._6).sum.toLong, infOutlier.filter(x=> x._1 == prevMonth+"-01").map(x=> x._6).sum.toLong)
      )
      val userDownOutObj = Json.obj(
        "currUserOut" -> infOutlier.filter(x=> x._1 == currMonth+"-01").map(x=> x._7).sum,
        "prevUserOut" -> infOutlier.filter(x=> x._1 == prevMonth+"-01").map(x=> x._7).sum,
        "percent"     -> CommonService.percent(infOutlier.filter(x=> x._1 == currMonth+"-01").map(x=> x._7).sum.toLong, infOutlier.filter(x=> x._1 == prevMonth+"-01").map(x=> x._7).sum.toLong)
      )
      val sfCliObj = Json.obj(
        "currSfCli" -> infOutlier.filter(x=> x._1 == currMonth+"-01").map(x=> x._8).sum,
        "prevSfCli" -> infOutlier.filter(x=> x._1 == prevMonth+"-01").map(x=> x._8).sum,
        "percent"    -> CommonService.percent(infOutlier.filter(x=> x._1 == currMonth+"-01").map(x=> x._8).sum.toLong, infOutlier.filter(x=> x._1 == prevMonth+"-01").map(x=> x._8).sum.toLong)
      )
      val sfOutObj = Json.obj(
        "currSfOut" -> infOutlier.filter(x=> x._1 == currMonth+"-01").map(x=> x._9).sum,
        "prevSfOut" -> infOutlier.filter(x=> x._1 == prevMonth+"-01").map(x=> x._9).sum,
        "percent"     -> CommonService.percent(infOutlier.filter(x=> x._1 == currMonth+"-01").map(x=> x._9).sum.toLong, infOutlier.filter(x=> x._1 == prevMonth+"-01").map(x=> x._9).sum.toLong)
      )
      val signalCliObj = Json.obj(
        "currSigCli" -> infOutlier.filter(x=> x._1 == currMonth+"-01").map(x=> x._10).sum,
        "prevSigCli" -> infOutlier.filter(x=> x._1 == prevMonth+"-01").map(x=> x._10).sum,
        "percent"    -> CommonService.percent(infOutlier.filter(x=> x._1 == currMonth+"-01").map(x=> x._10).sum.toLong, infOutlier.filter(x=> x._1 == prevMonth+"-01").map(x=> x._10).sum.toLong)
      )
      val signalOutObj = Json.obj(
        "currSigOut" -> infOutlier.filter(x=> x._1 == currMonth+"-01").map(x=> x._11).sum,
        "prevSigOut" -> infOutlier.filter(x=> x._1 == prevMonth+"-01").map(x=> x._11).sum,
        "percent"     -> CommonService.percent(infOutlier.filter(x=> x._1 == currMonth+"-01").map(x=> x._11).sum.toLong, infOutlier.filter(x=> x._1 == prevMonth+"-01").map(x=> x._11).sum.toLong)
      )
      /* Box Contract & Device & Connections */
      val device = Await.result(BrasService.getDeviceByMonth(currMonth), Duration.Inf)
      val noContractObj = Json.obj(
        "currNoct" -> device.filter(x=> x._1 == currMonth+"-01").map(x=> x._2).sum,
        "prevNoct" -> device.filter(x=> x._1 == prevMonth+"-01").map(x=> x._2).sum,
        "percent"  -> CommonService.percent(device.filter(x=> x._1 == currMonth+"-01").map(x=> x._2).sum.toLong, device.filter(x=> x._1 == prevMonth+"-01").map(x=> x._2).sum.toLong)
      )
      val noDeviceObj = Json.obj(
        "currNodev" -> device.filter(x=> x._1 == currMonth+"-01").map(x=> x._3).sum,
        "prevNodev" -> device.filter(x=> x._1 == prevMonth+"-01").map(x=> x._3).sum,
        "percent"  -> CommonService.percent(device.filter(x=> x._1 == currMonth+"-01").map(x=> x._3).sum.toLong, device.filter(x=> x._1 == prevMonth+"-01").map(x=> x._3).sum.toLong)
      )
      val notPoorObj = Json.obj(
        "currNotpoor" -> device.filter(x=> x._1 == currMonth+"-01").map(x=> x._4).sum,
        "prevNotpoor" -> device.filter(x=> x._1 == prevMonth+"-01").map(x=> x._4).sum,
        "percent"  -> CommonService.percent(device.filter(x=> x._1 == currMonth+"-01").map(x=> x._4).sum.toLong, device.filter(x=> x._1 == prevMonth+"-01").map(x=> x._4).sum.toLong)
      )
      val poorObj = Json.obj(
        "currPoor" -> device.filter(x=> x._1 == currMonth+"-01").map(x=> x._5).sum,
        "prevPoor" -> device.filter(x=> x._1 == prevMonth+"-01").map(x=> x._5).sum,
        "percent"  -> CommonService.percent(device.filter(x=> x._1 == currMonth+"-01").map(x=> x._5).sum.toLong, device.filter(x=> x._1 == prevMonth+"-01").map(x=> x._5).sum.toLong)
      )
      /* Box Kibana & Opsview */
      val kibaOps = Await.result(BrasService.getKibaOpsByMonth(currMonth), Duration.Inf)
      val kibanaObj = Json.obj(
        "currKiba" -> kibaOps.filter(x=> x._1 == currMonth+"-01").map(x=> x._2).sum,
        "prevKiba" -> kibaOps.filter(x=> x._1 == prevMonth+"-01").map(x=> x._2).sum,
        "percent"  -> CommonService.percent(kibaOps.filter(x=> x._1 == currMonth+"-01").map(x=> x._2).sum.toLong, kibaOps.filter(x=> x._1 == prevMonth+"-01").map(x=> x._2).sum.toLong)
      )
      val opsObj = Json.obj(
        "currOps" -> kibaOps.filter(x=> x._1 == currMonth+"-01").map(x=> x._3).sum,
        "prevOps" -> kibaOps.filter(x=> x._1 == prevMonth+"-01").map(x=> x._3).sum,
        "percent"  -> CommonService.percent(kibaOps.filter(x=> x._1 == currMonth+"-01").map(x=> x._3).sum.toLong, kibaOps.filter(x=> x._1 == prevMonth+"-01").map(x=> x._3).sum.toLong)
      )
      /* Box INF Error */
      val infError = Await.result(BrasService.getInfErrorByMonth(currMonth), Duration.Inf)
      val totalInfObj = Json.obj(
        "currTotal" -> infError.filter(x=> x._1 == currMonth+"-01").map(x=> x._2).sum,
        "prevTotal" -> infError.filter(x=> x._1 == prevMonth+"-01").map(x=> x._2).sum,
        "percent"  -> CommonService.percent(infError.filter(x=> x._1 == currMonth+"-01").map(x=> x._2).sum.toLong, infError.filter(x=> x._1 == prevMonth+"-01").map(x=> x._2).sum.toLong)
      )
      val infDownObj = Json.obj(
        "currInfD" -> infError.filter(x=> x._1 == currMonth+"-01").map(x=> x._3).sum,
        "prevInfD" -> infError.filter(x=> x._1 == prevMonth+"-01").map(x=> x._3).sum,
        "percent"  -> CommonService.percent(infError.filter(x=> x._1 == currMonth+"-01").map(x=> x._3).sum.toLong, infError.filter(x=> x._1 == prevMonth+"-01").map(x=> x._3).sum.toLong)
      )
      val userDownObj = Json.obj(
        "currUserD" -> infError.filter(x=> x._1 == currMonth+"-01").map(x=> x._4).sum,
        "prevUserD" -> infError.filter(x=> x._1 == prevMonth+"-01").map(x=> x._4).sum,
        "percent"  -> CommonService.percent(infError.filter(x=> x._1 == currMonth+"-01").map(x=> x._4).sum.toLong, infError.filter(x=> x._1 == prevMonth+"-01").map(x=> x._4).sum.toLong)
      )
      val signalObj = Json.obj(
        "currSignal" -> infError.filter(x=> x._1 == currMonth+"-01").map(x=> x._5).sum,
        "prevSignal" -> infError.filter(x=> x._1 == prevMonth+"-01").map(x=> x._5).sum,
        "percent"  -> CommonService.percent(infError.filter(x=> x._1 == currMonth+"-01").map(x=> x._5).sum.toLong, infError.filter(x=> x._1 == prevMonth+"-01").map(x=> x._5).sum.toLong)
      )
      val sfObj = Json.obj(
        "currSf" -> infError.filter(x=> x._1 == currMonth+"-01").map(x=> x._6).sum,
        "prevSf" -> infError.filter(x=> x._1 == prevMonth+"-01").map(x=> x._6).sum,
        "percent"  -> CommonService.percent(infError.filter(x=> x._1 == currMonth+"-01").map(x=> x._6).sum.toLong, infError.filter(x=> x._1 == prevMonth+"-01").map(x=> x._6).sum.toLong)
      )
      val lofiObj = Json.obj(
        "currLofi" -> infError.filter(x=> x._1 == currMonth+"-01").map(x=> x._7).sum,
        "prevLofi" -> infError.filter(x=> x._1 == prevMonth+"-01").map(x=> x._7).sum,
        "percent"  -> CommonService.percent(infError.filter(x=> x._1 == currMonth+"-01").map(x=> x._7).sum.toLong, infError.filter(x=> x._1 == prevMonth+"-01").map(x=> x._7).sum.toLong)
      )
      val rougeObj = Json.obj(
        "currRouge" -> infError.filter(x=> x._1 == currMonth+"-01").map(x=> x._8).sum,
        "prevRouge" -> infError.filter(x=> x._1 == prevMonth+"-01").map(x=> x._8).sum,
        "percent"  -> CommonService.percent(infError.filter(x=> x._1 == currMonth+"-01").map(x=> x._8).sum.toLong, infError.filter(x=> x._1 == prevMonth+"-01").map(x=> x._8).sum.toLong)
      )
      val rs = Json.obj(
        "currMonth"  -> currMonth,
        "prevMonth"  -> prevMonth,
        "signin"     -> signinObj,
        "logoff"     -> logoffObj,
        "suyhao"     -> suyhaoObj,
        "notSuyhao"  -> notSuyhaoObj,
        "bras"       -> brasObj,
        "affected"   -> affectObj,
        "outliInf"   -> outliInfObj,
        "clientInf"  -> clientInfObj,
        "infDownout" -> infDownOutObj,
        "infDowncli" -> infDownCliObj,
        "userDownCli" -> userDownCliObj,
        "userDownout" -> userDownOutObj,
        "sfCli"       -> sfCliObj,
        "sfOut"       -> sfOutObj,
        "signalOut"   -> signalOutObj,
        "signalCli"   -> signalCliObj,
        "noContract" -> noContractObj,
        "noDevice"   -> noDeviceObj,
        "poor"       -> poorObj,
        "notPoor"    -> notPoorObj,
        "kibana"     -> kibanaObj,
        "opsview"    -> opsObj,
        "totalInf"   -> totalInfObj,
        "infDown"    -> infDownObj,
        "userDown"   -> userDownObj,
        "signal"     -> signalObj,
        "sf"         -> sfObj,
        "lofi"       -> lofiObj,
        "rouge"      -> rougeObj
      )
      Ok(Json.toJson(rs))
    }
    catch {
      case e: Exception => Ok("Error")
    }
  }

  // Tab TOP N
  def topNJson(monthStr: String,_typeError: String,_typeService: String,_typeOLTpoor: String,_typeInferr: String) = Action { implicit request =>
    try{
      val month = if(monthStr.equals("")) CommonService.getPreviousMonth() else monthStr
      // get Top Sigin
      val topSignin = Await.result(BrasService.getTopSignin(month), Duration.Inf).map(x=> (LocationUtils.getNameProvincebyCode(x._1), x._2, x._3, x._4))
      val siginObj = Json.obj(
        "data" -> topSignin,
        "dataProvince" -> topSignin.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sortWith(_._2 > _._2),
        "dataClients" -> topSignin.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sortWith(_._2 > _._2),
        "categories" -> topSignin.map(x=>x._1).asInstanceOf[Seq[String]].distinct
      )
      // get Top Logoff
      val topLogoff = Await.result(BrasService.getTopLogoff(month), Duration.Inf).map(x=> (LocationUtils.getNameProvincebyCode(x._1), x._2, x._3, x._4))
      val logoffObj = Json.obj(
        "data" -> topLogoff,
        "dataProvince" -> topLogoff.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sortWith(_._2 > _._2),
        "dataClients" -> topLogoff.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sortWith(_._2 > _._2),
        "categories" -> topLogoff.map(x=>x._1).asInstanceOf[Seq[String]].distinct
      )
      // get Top total kibana
      val topKibana = Await.result(BrasService.getTopKibana(month,_typeError), Duration.Inf).map(x=> (LocationUtils.getNameProvincebyCode(x._1), x._2, x._3))
      val kibanaObj = Json.obj(
        "data" -> topKibana,
        "dataProvince" -> topKibana.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sortWith(_._2 > _._2),
        "categories" -> topKibana.map(x=>x._1).asInstanceOf[Seq[String]].distinct
      )
      // get Top total opsview
      val topOpsview = Await.result(BrasService.getTopOpsview(month,_typeService), Duration.Inf).map(x=> (LocationUtils.getNameProvincebyCode(x._1), x._2, x._3))
      val opsviewObj = Json.obj(
        "data" -> topOpsview,
        "dataProvince" -> topOpsview.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sortWith(_._2 > _._2),
        "categories" -> topOpsview.map(x=>x._1).asInstanceOf[Seq[String]].distinct
      )
      // get Top total Inf
      val topInf = Await.result(BrasService.getTopInf(month,_typeInferr), Duration.Inf)
      val infObj = Json.obj(
        "data" -> topInf,
        "dataBras" -> topInf.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sortWith(_._2 > _._2),
        "categories" -> topInf.map(x=>x._1).asInstanceOf[Seq[String]].distinct
      )
      // get Top Not Passed Suyhao
      val topSuyhao = Await.result(BrasService.getTopnotSuyhao(month), Duration.Inf).map(x=> (LocationUtils.getNameProvincebyCode(x._1), x._2, x._3, x._4))
      val suyhaoObj = Json.obj(
        "data" -> topSuyhao,
        "dataProvince" -> topSuyhao.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sortWith(_._2 > _._2),
        "dataClients" -> topSuyhao.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sortWith(_._2 > _._2),
        "categories" -> topSuyhao.map(x=>x._1).asInstanceOf[Seq[String]].distinct
      )
      // get Top 10 OLT By Poor Connections
      val topPoor = Await.result(BrasService.getTopPoorconn(month,_typeOLTpoor), Duration.Inf)
      val poorObj = Json.obj(
        "data" -> topPoor.map(x=>x._2),
        "categories" -> topPoor.map(x=>x._1)
      )

      val rs = Json.obj(
        "month"      -> month,
        "topSignin"  -> siginObj,
        "topLogoff"  -> logoffObj,
        "topKibana"  -> kibanaObj,
        "topOpsview" -> opsviewObj,
        "topInf"     -> infObj,
        "topSuyhao"  ->suyhaoObj,
        "topPoor"    -> poorObj
      )
      Ok(Json.toJson(rs))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  // page NOC
  def dashboard =  withAuth { username => implicit request =>
    try {
      val mapBrasOutlier = Await.result(BrasService.getBrasOutlierCurrent(CommonService.getCurrentDay()),Duration.Inf)
      Ok(device.views.html.dashboard(username,mapBrasOutlier))
    }
    catch{
      case e: Exception => Ok(device.views.html.dashboard(username,null))
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

  def groupRegionByMonth(month: String,_typeNoc: String,_typeError: String) = Action { implicit request =>
    try{
      val monthRange = if(month.indexOf("/")>=0) month.split("/")(0)+"-01/"+month.split("/")(1)+"-01" else month
      // get Opsview Types
      val mapOpsviewType = Await.result(BrasService.getProvinceOpsviewType(monthRange), Duration.Inf)
      val opsviewType = mapOpsviewType.map(x=> (LocationUtils.getRegion(x._1.trim),x._2,x._3,x._4,x._5)).toArray
      val ok_opsview = opsviewType.groupBy(_._1).mapValues(_.map(_._2).sum).toSeq.sorted.toArray
      val warning_opsview = opsviewType.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sorted.toArray
      val unknown_opsview = opsviewType.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sorted.toArray
      val crit_opsview = opsviewType.groupBy(_._1).mapValues(_.map(_._5).sum).toSeq.sorted.toArray
      val heatmapOpsview = (ok_opsview++warning_opsview++unknown_opsview++crit_opsview).groupBy(_._1).map{case (k,v) => k -> v.map(x=> x._2.toString).mkString("_")}.toSeq.sorted
      val heatmapOpsObj = Json.obj(
        "data" -> heatmapOpsview,
        "categories" -> heatmapOpsview.map(x=>x._1)
      )
      // get NOC count by Region
      val mapCount = Await.result(BrasService.getProvinceCount(monthRange), Duration.Inf)

      val typeCount = _typeNoc match {
        case "AlertCount" => mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._3)).toArray
        case "CritCount" => mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._4)).toArray
        case "WarningCount" => mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._5)).toArray
        case "NoticeCount" => mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._6)).toArray
        case "ErrCount" => mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._7)).toArray
        case "EmergCount" => mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._8)).toArray
      }

      val nocCountObj = Json.obj(
        "dataSeries" -> typeCount.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sorted,
        "data" -> typeCount,
        "categories" -> typeCount.map(x=> x._1).toSeq.distinct.sorted
      )
      // get Inf down, user down,lost signal,rouge error by Region
      val mapInfType = Await.result(BrasService.getProvinceInfDownError(monthRange), Duration.Inf)
      val typeInferr = _typeError match {
        case "InfDown" => mapInfType.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._3)).toArray
        case "UseDown" => mapInfType.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._4)).toArray
        case "RougeError" => mapInfType.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._5)).toArray
        case "LostSignal" => mapInfType.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._6)).toArray
      }

      val infTypeObj = Json.obj(
        "dataSeries" -> typeInferr.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sorted,
        "data" -> typeInferr,
        "categories" -> typeInferr.map(x=> x._1).toSeq.distinct.sorted
      )

      val rs = Json.obj(
        "heatmapOpsview" -> heatmapOpsObj,
        "nocCountObj" -> nocCountObj,
        "infTypeObj" -> infTypeObj
/*        "siglogObj" -> siglogObj*/
      )

      Ok(Json.toJson(rs))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  def drilldownSigLogMonth(id: String,month: String) = Action {implicit  request =>
    try{
      val rangeMonth = CommonService.getAllMonthfromRange(month.split("/")(0)+"-01",month.split("/")(1)+"-01")
      val rs = id match {
        // get inf by Region
        case id if(id.substring(id.indexOf(" ")+1).matches("^\\d+$")) => {
          val sigLogRegion = rangeMonth.map(x => x -> Await.result(BrasService.getSigLogByRegion(x+"-01"), Duration.Inf).map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._3,x._4,x._5)).asInstanceOf[Seq[(String,String,Int,Int,Int,Int)]].filter(x=> x._1 == id))
          Json.obj(
            "categories" -> sigLogRegion.map(x=>x._1-> x._2.groupBy(_._2).mapValues(_.map(_._3).sum).toSeq.sorted.map(x=> x._1)).filter(x=>x._1 == rangeMonth(rangeMonth.length-1)).map(x=>x._2),
            "signin" -> sigLogRegion.map(x=>x._1-> x._2.groupBy(_._2).mapValues(_.map(_._3).sum).toSeq.sorted.map(x=> x._1 -> -x._2)),
            "logoff" -> sigLogRegion.map(x=>x._1-> x._2.groupBy(_._2).mapValues(_.map(_._4).sum).toSeq.sorted.map(x=> x._1 -> x._2)),
            "signin_clients" -> sigLogRegion.map(x=>x._1-> x._2.groupBy(_._2).mapValues(_.map(_._5).sum).toSeq.sorted.map(x=> x._1 -> -x._2)),
            "logoff_clients" -> sigLogRegion.map(x=>x._1-> x._2.groupBy(_._2).mapValues(_.map(_._6).sum).toSeq.sorted.map(x=> x._1 -> x._2))
          )
        }
        // get inf by Province
        case id if(!id.substring(id.indexOf(" ")+1).matches("^\\d+$") && id.indexOf("-")<0) => {
          val sigLogProvince = rangeMonth.map(x => x -> Await.result(BrasService.getSigLogByProvince(x+"-01",LocationUtils.getCodeProvincebyName(id),rangeMonth(rangeMonth.length-1)+"-01"), Duration.Inf).map(x=> (x._1,x._2,x._3,x._4,x._5)).asInstanceOf[Seq[(String,Int,Int,Int,Int)]])
          Json.obj(
            "categories" -> sigLogProvince.map(x=>x._1-> x._2.groupBy(_._1).mapValues(_.map(_._2).sum).toSeq.sorted.map(x=> x._1)).filter(x=>x._1 == rangeMonth(rangeMonth.length-1)).map(x=>x._2),
            "signin" -> sigLogProvince.map(x=>x._1-> x._2.groupBy(_._1).mapValues(_.map(_._2).sum).toSeq.sorted.map(x=> x._1 -> -x._2)),
            "logoff" -> sigLogProvince.map(x=>x._1-> x._2.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sorted.map(x=> x._1 -> x._2)),
            "signin_clients" -> sigLogProvince.map(x=>x._1-> x._2.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sorted.map(x=> x._1 -> -x._2)),
            "logoff_clients" -> sigLogProvince.map(x=>x._1-> x._2.groupBy(_._1).mapValues(_.map(_._5).sum).toSeq.sorted.map(x=> x._1 -> x._2))
          )
        }
        // get inf by Bras
        case id if(!id.substring(id.indexOf(" ")+1).matches("^\\d+$") && id.indexOf("-")>=0) =>{
          val sigLogBras = rangeMonth.map(x => x -> Await.result(BrasService.getSigLogByBras(x+"-01",id,rangeMonth(rangeMonth.length-1)+"-01"), Duration.Inf).map(x=> (x._1,x._2,x._3,x._4,x._5)).asInstanceOf[Seq[(String,Int,Int,Int,Int)]])
          Json.obj(
            "categories" -> sigLogBras.map(x=>x._1-> x._2.groupBy(_._1).mapValues(_.map(_._2).sum).toSeq.sorted.map(x=> x._1)).filter(x=>x._1 == rangeMonth(rangeMonth.length-1)).map(x=>x._2),
            "signin" -> sigLogBras.map(x=>x._1-> x._2.groupBy(_._1).mapValues(_.map(_._2).sum).toSeq.sorted.map(x=> x._1 -> -x._2)),
            "logoff" -> sigLogBras.map(x=>x._1-> x._2.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sorted.map(x=> x._1 -> x._2)),
            "signin_clients" -> sigLogBras.map(x=>x._1-> x._2.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sorted.map(x=> x._1 -> -x._2)),
            "logoff_clients" -> sigLogBras.map(x=>x._1-> x._2.groupBy(_._1).mapValues(_.map(_._5).sum).toSeq.sorted.map(x=> x._1 ->x._2))
          )
        }
      }
      Ok(Json.toJson(rs))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  def drillUpSigLogmonth(id: String,month: String) = Action { implicit request  =>
    try{
      val rangeMonth = CommonService.getAllMonthfromRange(month.split("/")(0)+"-01",month.split("/")(1)+"-01")
      val rs = id match {
        case id if(id.equals("*")) => {
          val mapSiglog = rangeMonth.map(x => x -> Await.result(BrasService.getSigLogByRegion(x+"-01"), Duration.Inf).map(x=> (LocationUtils.getRegion(x._1.trim),x._2,x._3,x._4,x._5)))
           Json.obj(
            "categories" -> mapSiglog.map(x=>x._1-> x._2.groupBy(_._1).map(x=>x._1).toSeq.sorted).filter(x=>x._1 == rangeMonth(rangeMonth.length-1)).map(x=>x._2),
            "signin" -> mapSiglog.map(x=>x._1-> x._2.groupBy(_._1).mapValues(_.map(_._2).sum).toSeq.sorted.map(x=>x._1 -> -x._2).toArray),
            "logoff" -> mapSiglog.map(x=>x._1-> x._2.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sorted.map(x=>x._1 -> x._2).toArray),
            "signin_clients" -> mapSiglog.map(x=>x._1-> x._2.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sorted.map(x=>x._1 -> -x._2).toArray),
            "logoff_clients" -> mapSiglog.map(x=>x._1-> x._2.groupBy(_._1).mapValues(_.map(_._5).sum).toSeq.sorted.map(x=>x._1 -> x._2).toArray)
          )
        }
        case id if(id.indexOf("Region")>=0) => {
          val sigLogRegion = rangeMonth.map(x => x -> Await.result(BrasService.getSigLogByRegion(x+"-01"), Duration.Inf).map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._3,x._4,x._5)).asInstanceOf[Seq[(String,String,Int,Int,Int,Int)]].filter(x=> x._1 == id.split(":")(1)))
          Json.obj(
            "categories" -> sigLogRegion.map(x=>x._1-> x._2.groupBy(_._2).mapValues(_.map(_._3).sum).toSeq.sorted.map(x=> x._1)).filter(x=>x._1 == rangeMonth(rangeMonth.length-1)).map(x=>x._2),
            "signin" -> sigLogRegion.map(x=>x._1-> x._2.groupBy(_._2).mapValues(_.map(_._3).sum).toSeq.sorted.map(x=>x._1 -> -x._2)),
            "logoff" -> sigLogRegion.map(x=>x._1-> x._2.groupBy(_._2).mapValues(_.map(_._4).sum).toSeq.sorted.map(x=>x._1 -> x._2)),
            "signin_clients" -> sigLogRegion.map(x=>x._1-> x._2.groupBy(_._2).mapValues(_.map(_._5).sum).toSeq.sorted.map(x=>x._1 -> -x._2)),
            "logoff_clients" -> sigLogRegion.map(x=>x._1-> x._2.groupBy(_._2).mapValues(_.map(_._6).sum).toSeq.sorted.map(x=>x._1 -> x._2))
          )
        }
        case id if(id.indexOf("Province")>=0) => {
          val sigLogProvince = rangeMonth.map(x => x -> Await.result(BrasService.getSigLogByProvince(x+"-01",LocationUtils.getCodeProvincebyName(id.split(":")(1)),rangeMonth(rangeMonth.length-1)+"-01"), Duration.Inf).map(x=> (x._1,x._2,x._3,x._4,x._5)).asInstanceOf[Seq[(String,Int,Int,Int,Int)]])
          Json.obj(
            "categories" -> sigLogProvince.map(x=>x._1-> x._2.groupBy(_._1).mapValues(_.map(_._2).sum).toSeq.sorted.map(x=> x._1)).filter(x=>x._1 == rangeMonth(rangeMonth.length-1)).map(x=>x._2),
            "signin" -> sigLogProvince.map(x=>x._1-> x._2.groupBy(_._1).mapValues(_.map(_._2).sum).toSeq.sorted.map(x=>x._1 -> -x._2)),
            "logoff" -> sigLogProvince.map(x=>x._1-> x._2.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sorted.map(x=>x._1 -> x._2)),
            "signin_clients" -> sigLogProvince.map(x=>x._1-> x._2.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sorted.map(x=>x._1 -> -x._2)),
            "logoff_clients" -> sigLogProvince.map(x=>x._1-> x._2.groupBy(_._1).mapValues(_.map(_._5).sum).toSeq.sorted.map(x=>x._1 -> x._2))
          )
        }
      }
      Ok(Json.toJson(rs))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  def drilldownTotalInf(id: String,month: String) = Action {implicit  request =>
      try{
      val rangeMonth = CommonService.getAllMonthfromRange(month.split("/")(0)+"-01",month.split("/")(1)+"-01")
      val jsInf = id match {
          // get inf by Region
        case id if(id.substring(id.indexOf(" ")+1).matches("^\\d+$")) => {
          val rs = Await.result(BrasService.getProvinceTotalInf(month.split("/")(0)+"-01",month.split("/")(1)+"-01"), Duration.Inf).map(x=> (x._1,x._2,LocationUtils.getRegion(x._2.trim),x._3)).asInstanceOf[Seq[(String,String,String,Double)]].filter(x=> x._3==id).groupBy(x=> (x._1,x._2)).mapValues(_.map(_._4).sum).toSeq.sorted.map(x=> (x._1._1,LocationUtils.getNameProvincebyCode(x._1._2),x._2))
          Json.obj(
            "data" -> rs,
            "categories" -> rs.map(x=>x._2).distinct.sorted,
            "month" -> rs.map(x=>x._1).distinct.sorted
          )
        }
        // get inf by Province
        case id if(!id.substring(id.indexOf(" ")+1).matches("^\\d+$") && id.indexOf("-")<0) => {
          // get distinct bras by province
          val mapBras = rangeMonth.map(x=>Await.result(BrasService.getDistinctBrasbyProvince(x+"-01",LocationUtils.getCodeProvincebyName(id)), Duration.Inf).toArray).filter(x=> x.length>0)
          var arrId = mapBras(0)
          for(i <- 1 until mapBras.length){
            arrId = arrId ++ mapBras(i)
            arrId = arrId.diff(arrId.distinct).distinct
          }
          val rs = Await.result(BrasService.getTotalInfbyProvince(month,LocationUtils.getCodeProvincebyName(id),arrId), Duration.Inf).map(x=>(x._1,x._2,x._3))
          Json.obj(
            "data" -> rs,
            "categories" -> rs.map(x=>x._2).distinct.sorted,
            "month" -> rs.map(x=>x._1).distinct.sorted
          )
        }
        // get inf by Bras
        case id if(!id.substring(id.indexOf(" ")+1).matches("^\\d+$") && id.indexOf("-")>=0) => {
          // get distinct HOST by BRAS
          val mapHost = rangeMonth.map(x=>Await.result(BrasService.getDistinctHostbyBras(x+"-01",id), Duration.Inf).toArray).filter(x=> x.length>0)
          var arrId = mapHost(0)
          for(i <- 1 until mapHost.length){
            arrId = arrId ++ mapHost(i)
            arrId = arrId.diff(arrId.distinct).distinct
          }

          val top10HostId = Await.result(BrasService.getTop10HostId(month.split("/")(1)+"-01",arrId), Duration.Inf).toArray
          val rs = Await.result(BrasService.getTotalInfbyBras(month,id,top10HostId), Duration.Inf).map(x=>(x._1,x._2,x._3))
          Json.obj(
            "data" -> rs,
            "categories" -> rs.map(x=>x._2).slice(0,10),
            "month" -> rs.map(x=>x._1).distinct.sorted
          )
        }
      }
      Ok(Json.toJson(jsInf))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  def drillUpTotalInf(id: String,month: String) = Action { implicit request  =>
    try{
      val rangeMonth = CommonService.getAllMonthfromRange(month.split("/")(0)+"-01",month.split("/")(1)+"-01")
      val rs = id match {
        case id if(id.equals("*")) => Await.result(BrasService.getProvinceTotalInf(month.split("/")(0)+"-01",month.split("/")(1)+"-01"), Duration.Inf).map(x=> (x._1,LocationUtils.getRegion(x._2.trim),x._3)).groupBy(x=> (x._1,x._2)).mapValues(_.map(_._3).sum).toSeq.sorted.map(x=> (x._1._1,x._1._2,x._2))
        case id if(id.indexOf("Region")>=0) => Await.result(BrasService.getProvinceTotalInf(month.split("/")(0)+"-01",month.split("/")(1)+"-01"), Duration.Inf).map(x=> (x._1,x._2,LocationUtils.getRegion(x._2.trim),x._3)).asInstanceOf[Seq[(String,String,String,Double)]].filter(x=> x._3==id.split(":")(1)).groupBy(x=> (x._1,x._2)).mapValues(_.map(_._4).sum).toSeq.sorted.map(x=> (x._1._1,LocationUtils.getNameProvincebyCode(x._1._2),x._2))
        case id if(id.indexOf("Province")>=0) => {
          // get distinct bras by province
          val mapBras = rangeMonth.map(x=>Await.result(BrasService.getDistinctBrasbyProvince(x+"-01",LocationUtils.getCodeProvincebyName(id.split(":")(1))), Duration.Inf).toArray).filter(x=> x.length>0)
          var arrId = mapBras(0)
          for(i <- 1 until mapBras.length){
            arrId = arrId ++ mapBras(i)
            arrId = arrId.diff(arrId.distinct).distinct
          }
          Await.result(BrasService.getTotalInfbyProvince(month,LocationUtils.getCodeProvincebyName(id.split(":")(1)),arrId), Duration.Inf).map(x=>(x._1,x._2,x._3))
        }
      }
      val jsInf = Json.obj(
        "data" -> rs,
        "categories" -> rs.map(x=>x._2).asInstanceOf[Seq[(String)]].distinct.sorted,
        "month" -> rs.map(x=>x._1).asInstanceOf[Seq[(String)]].distinct.sorted
      )
      Ok(Json.toJson(jsInf))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  def drillUpNoticeByStatus(id: String,month: String) = Action { implicit request  =>
    try{
      val monthRange = if(month.indexOf("/")>=0) month.split("/")(0)+"-01/"+month.split("/")(1)+"-01" else month
      val rs = id match {
        case id if(id.equals("*")) => {
          val mapOpsviewType = Await.result(BrasService.getProvinceOpsviewType(monthRange), Duration.Inf)
          val opsviewType = mapOpsviewType.map(x=> (LocationUtils.getRegion(x._1),x._2,x._3,x._4,x._5)).toArray
          val ok_opsview = opsviewType.groupBy(_._1).mapValues(_.map(_._2).sum).toSeq.sorted.toArray
          val warning_opsview = opsviewType.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sorted.toArray
          val unknown_opsview = opsviewType.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sorted.toArray
          val crit_opsview = opsviewType.groupBy(_._1).mapValues(_.map(_._5).sum).toSeq.sorted.toArray
          (ok_opsview++warning_opsview++unknown_opsview++crit_opsview).groupBy(_._1).map{case (k,v) => k -> v.map(x=> x._2.toString).mkString("_")}.toSeq.sorted
        }
        case id if(id.indexOf("Region")>=0) => {
          val mapOpsviewType = Await.result(BrasService.getProvinceOpsviewType(monthRange), Duration.Inf).map(x=> (LocationUtils.getNameProvincebyCode(x._1),LocationUtils.getRegion(x._1.trim),x._2,x._3,x._4,x._5)).asInstanceOf[Seq[(String,String,Int,Int,Int,Int)]].filter(x=> x._2==id.split(":")(1))
          val opsviewType = mapOpsviewType.map(x=> (x._1,x._3,x._4,x._5,x._6)).toArray
          val ok_opsview = opsviewType.groupBy(_._1).mapValues(_.map(_._2).sum).toSeq.sorted.toArray
          val warning_opsview = opsviewType.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sorted.toArray
          val unknown_opsview = opsviewType.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sorted.toArray
          val crit_opsview = opsviewType.groupBy(_._1).mapValues(_.map(_._5).sum).toSeq.sorted.toArray
          (ok_opsview++warning_opsview++unknown_opsview++crit_opsview).groupBy(_._1).map{case (k,v) => k -> v.map(x=> x._2.toString).mkString("_")}.toSeq.sorted
        }
      }
      val jsInf = Json.obj(
        "data" -> rs,
        "categories" -> rs.map(x=>x._1)
      )
      Ok(Json.toJson(jsInf))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  def drilldownNoticeByStatus(id: String,month: String) = Action {implicit  request =>
    try{
      val monthRange = if(month.indexOf("/")>=0) month.split("/")(0)+"-01/"+month.split("/")(1)+"-01" else month
      val rs = id match {
        // get status notice by Region
        case id if(id.substring(id.indexOf(" ")+1).matches("^\\d+$")) => {
          val mapOpsviewType = Await.result(BrasService.getProvinceOpsviewType(monthRange), Duration.Inf).map(x=> (LocationUtils.getNameProvincebyCode(x._1),LocationUtils.getRegion(x._1.trim),x._2,x._3,x._4,x._5)).asInstanceOf[Seq[(String,String,Int,Int,Int,Int)]].filter(x=> x._2==id)
          val opsviewType = mapOpsviewType.map(x=> (x._1,x._3,x._4,x._5,x._6)).toArray
          val ok_opsview = opsviewType.groupBy(_._1).mapValues(_.map(_._2).sum).toSeq.sorted.toArray
          val warning_opsview = opsviewType.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sorted.toArray
          val unknown_opsview = opsviewType.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sorted.toArray
          val crit_opsview = opsviewType.groupBy(_._1).mapValues(_.map(_._5).sum).toSeq.sorted.toArray
          (ok_opsview++warning_opsview++unknown_opsview++crit_opsview).groupBy(_._1).map{case (k,v) => k -> v.map(x=> x._2.toString).mkString("_")}.toSeq.sorted

        }
        // get status notice by Province
        case id if(!id.substring(id.indexOf(" ")+1).matches("^\\d+$") && id.indexOf("-")<0) => {
          val opsviewType = Await.result(BrasService.getBrasOpsviewType(monthRange,LocationUtils.getCodeProvincebyName(id)), Duration.Inf).toArray
          val ok_opsview = opsviewType.groupBy(_._1).mapValues(_.map(_._2).sum).toSeq.sorted.toArray
          val warning_opsview = opsviewType.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sorted.toArray
          val unknown_opsview = opsviewType.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sorted.toArray
          val crit_opsview = opsviewType.groupBy(_._1).mapValues(_.map(_._5).sum).toSeq.sorted.toArray
          (ok_opsview++warning_opsview++unknown_opsview++crit_opsview).groupBy(_._1).map{case (k,v) => k -> v.map(x=> x._2.toString).mkString("_")}.toSeq.sorted

        }
      }
      val jsInf = Json.obj(
        "data" -> rs,
        "categories" -> rs.map(x=>x._1)
      )
      Ok(Json.toJson(jsInf))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

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

  def realtimeBras() =  withAuth { username => implicit request =>
    try {
      val bras = Await.result(BrasService.getBrasOutlierCurrent(CommonService.getCurrentDay()), Duration.Inf)
      val jsBras = Json.obj(
        "bras" -> bras
      )
      Ok(Json.toJson(jsBras))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  // This will be the action that handles our form post
  def getFormBras = withAuth { username => implicit request: Request[AnyContent] =>

    val errorFunction = { formWithErrors: Form[controllers.BrasOutlier] =>
      println("error")
      Ok(device.views.html.search(form,username,null,null,CommonService.getCurrentDay()+"/"+CommonService.getCurrentDay(),null,"B",routes.DeviceController.search))
    }

    val successFunction = { data: controllers.BrasOutlier =>
      println("done")
      Redirect(routes.DeviceController.search).flashing("bras" -> data.bras, "_typeS" -> data._typeS, "date" -> data.date)
    }

    val formValidationResult = form.bindFromRequest
    formValidationResult.fold(errorFunction, successFunction)
  }

  def search =  withAuth { username => implicit request: Request[AnyContent] =>
    try {
      logger.info("======Start Service Search======")
      if (request.flash.get("bras").toString != "None") {
        logger.info(request.flash.get("bras").get.trim)
        val _typeS = request.flash.get("_typeS").get
        val time = request.flash.get("date").get
        var day = ""
        val brasId = request.flash.get("bras").get.trim()
        if (time == null || time == ""){
          day = CommonService.getCurrentDay()+"/"+CommonService.getCurrentDay()
        }
        else{ day =  time}

        var numOutlier = 0
        val fromDay = day.split("/")(0)
        var siginBytime = new Array[Long](0)
        var logoffBytime = new Array[Long](0)
        var arrSiglogModuleIndex = new Array[(String,String,Int,Int)](0)
        val timeStart= System.currentTimeMillis()
        // for result INF-HOST
        if(_typeS.equals("I")){
          logger.info("Search Host")
          val t00 = System.currentTimeMillis()
          // get errors by host tableIndex
          val errHost = HostService.getInfHostDailyResponse(brasId,day)
          logger.info("t00:"+ (System.currentTimeMillis() - t00))
          val t01 = System.currentTimeMillis()
          /* get bubble chart sigin and logoff by host */
          val sigLogbyModuleIndex = HostService.getSigLogbyModuleIndex(brasId,day)
          logger.info("t010:"+ (System.currentTimeMillis() - t01))
          for(i <- 0 until sigLogbyModuleIndex.length){
            // check group both signin and logoff
            if(sigLogbyModuleIndex(i)._2.indexOf("_") >= 0){
              arrSiglogModuleIndex +:= (sigLogbyModuleIndex(i)._1._1,sigLogbyModuleIndex(i)._1._2,sigLogbyModuleIndex(i)._2.split("_")(0).toInt,sigLogbyModuleIndex(i)._2.split("_")(1).toInt)
            }
          }
          val siginByModule = arrSiglogModuleIndex.groupBy(_._1).mapValues(_.map(_._3).sum).toArray
          val logoffByModule = arrSiglogModuleIndex.groupBy(_._1).mapValues(_.map(_._4).sum).toArray
          val sigLogModule = (siginByModule++logoffByModule).groupBy(_._1).map{case (k,v) => k -> v.map(x=> x._2.toString).mkString("_")}.toArray
          logger.info("t01:"+ (System.currentTimeMillis() - t01))
          val t0 = System.currentTimeMillis()
          val noOutlierModule = HostService.getNoOutlierInfByHost(brasId,day)
          logger.info("t0:"+ (System.currentTimeMillis() - t0))
          val t1 = System.currentTimeMillis()
          /* get total error by hourly*/
          val errorHourly = HostService.getErrorHostbyHourly(brasId,day)
          logger.info("t1:"+ (System.currentTimeMillis() - t1))
          val t2 = System.currentTimeMillis()
          /* get suyhao by module*/
          val suyhaoModule = Await.result(HostService.getSuyhaobyModule(brasId,day), Duration.Inf)
          logger.info("t2:"+ (System.currentTimeMillis() - t2))
          val t3 = System.currentTimeMillis()
          /* get sigin and logoff by hourly */
          val sigLogByHourly = HostService.getSiglogByHourly(brasId,day)
          logger.info("t3:"+ (System.currentTimeMillis() - t3))
          val t4 = System.currentTimeMillis()
          /* get splitter by host*/
          val splitterByHost = Await.result(HostService.getSplitterByHost(brasId,day), Duration.Inf)
          logger.info("t4:"+ (System.currentTimeMillis() - t4))
          val t5 = System.currentTimeMillis()
          /* get tableIndex error by module and index */
          val errModuleIndex = HostService.getErrorTableModuleIndex(brasId,day)
          val arrModule = errModuleIndex.map(x=>x._1).distinct
          val arrIndex = errModuleIndex.map(x=>x._2).distinct
          logger.info("t5:"+ (System.currentTimeMillis() - t5))
          val t6 = System.currentTimeMillis()
          // get table contract with sf>300
          val sfContract = Await.result(HostService.getPortPonDown(brasId,day), Duration.Inf)
          logger.info("t6:"+ (System.currentTimeMillis() - t6))
          logger.info("timeHost:"+ (System.currentTimeMillis() - t00))
          Ok(device.views.html.search(form,username,HostResponse(noOutlierModule,errHost,errorHourly,sigLogModule,arrSiglogModuleIndex,suyhaoModule,sigLogByHourly,splitterByHost,ErrModuleIndex(arrModule,arrIndex,errModuleIndex),sfContract),null,day,brasId,"I",routes.DeviceController.search))
        }
        // for result BRAS
        else {
          logger.info("Search Bras")
          /* GET ES CURRENT */
          if (day.split("/")(1).equals(CommonService.getCurrentDay())) {
            // number outlier
            numOutlier = BrasService.getNoOutlierCurrent(brasId, CommonService.getCurrentDay())
          }
          /* GET HISTORY DATA */
          if (!fromDay.equals(CommonService.getCurrentDay())) {
            val t00 = System.currentTimeMillis()
            // number outlier
            val noOutlier = Await.result(BrasService.getNoOutlierResponse(brasId, day), Duration.Inf)
            numOutlier += noOutlier.sum.toInt
            logger.info("t00: " + (System.currentTimeMillis() - t00))
          }
          // number outlier of host
          val noOutlierByhost = HostService.getNoOutlierInfByBras(brasId,day)
          // number sigin and logoff
          val t01 = System.currentTimeMillis()
          val sigLog = BrasService.getSigLogCurrent(brasId, day)
          val sigLogClients = BrasService.getSiglogClients(brasId,day)
          logger.info("tSigLog: " + (System.currentTimeMillis() - t01))
          // SIGNIN LOGOFF BY TIME
          val t03 = System.currentTimeMillis()
          val rsLogsigBytime = BrasService.getSigLogBytimeCurrent(brasId, day)
          siginBytime = rsLogsigBytime.sumSig
          logoffBytime = rsLogsigBytime.sumLog
          logger.info("tSigLogBytime: " + (System.currentTimeMillis() - t03))

          val t02 = System.currentTimeMillis()
          // line-card-port
          val linecardhost = BrasService.getLinecardhostCurrent(brasId, day)
          logger.info("tLinecardhost: " + (System.currentTimeMillis() - t02))

          val t1 = System.currentTimeMillis()
          // Nerror (kibana & opview) By Time
          val arrOpsview = BrasService.getOpviewBytimeResponse(brasId, day, 0)
          val opviewBytime = (0 until 24).map(x => x -> CommonService.getIntValueByKey(arrOpsview, x)).toArray
          logger.info("tOpsviewBytime: " + (System.currentTimeMillis() - t1))
          //val arrKibana = Await.result(BrasService.getKibanaBytimeResponse(brasId,day,0), Duration.Inf).toArray
          val t20 = System.currentTimeMillis()
          val arrKibana = BrasService.getKibanaBytimeES(brasId, day).groupBy(_._1).mapValues(_.map(_._2).sum).toArray
          val kibanaBytime = (0 until 24).map(x => x -> CommonService.getIntValueByKey(arrKibana, x)).toArray
          logger.info("tKibanaBytime: " + (System.currentTimeMillis() - t20))
          // INF ERROR
          val t3 = System.currentTimeMillis()
          val infErrorBytime = BrasService.getInfErrorBytimeResponse(brasId, day, 0)
          //val infErrorBytime = (0 until 24).map(x => x -> CommonService.getIntValueByKey(arrInferror, x)).toArray
          logger.info("tInfErrorBytime: " + (System.currentTimeMillis() - t3))
          val t30 = System.currentTimeMillis()
          //val infErrorBytime = null
          // INF HOST
          val infHostBytime = BrasService.getInfhostResponse(brasId, day).filter(x=> (x._2 !=0 && x._3 !=0))
          logger.info("tInfHostBytime: " + (System.currentTimeMillis() - t30))
          // val infHostBytime = null
          val t4 = System.currentTimeMillis()
          // INF MODULE
          val infModuleBytime = BrasService.getInfModuleResponse(brasId, day)
          logger.info("tInfModuleBytime: " + (System.currentTimeMillis() - t4))
          //val infModuleBytime = null
          val t5 = System.currentTimeMillis()
          // OPVIEW TREE MAP
          val opServiceName = Await.result(BrasService.getOpsviewServiceSttResponse(brasId, day), Duration.Inf)
          //val opServiceName = null
          // OPVIEW SERVICE BY STATUS
          val opServByStt = BrasService.getOpServByStatusResponse(brasId, day)
          val servName = opServByStt.map(x => x._1).distinct
          val servStatus = opServByStt.map(x => x._2).distinct
          logger.info("tOpsviewTreeMap: " + (System.currentTimeMillis() - t5))

          //val t6= System.currentTimeMillis()
          //val kibanaSeverity = Await.result(BrasService.getErrorSeverityResponse(brasId,day), Duration.Inf)
          // println("t6: "+(System.currentTimeMillis() - t6))
          val t60 = System.currentTimeMillis()
          // KIBANA Severity
          val kibanaSeverity = BrasService.getErrorSeverityES(brasId, day)

          logger.info("tKibanaSeverity: " + (System.currentTimeMillis() - t60))
          val t7 = System.currentTimeMillis()
          // KIBANA Error type
          //val kibanaErrorType = Await.result(BrasService.getErrorTypeResponse(brasId,day), Duration.Inf)
          val kibanaErrorType = BrasService.getErrorTypeES(brasId, day)
          logger.info("tKibanaErrorType: " + (System.currentTimeMillis() - t7))
          // KIBANA Facility
          val t8 = System.currentTimeMillis()
          //val kibanaFacility = Await.result(BrasService.getFacilityResponse(brasId,day), Duration.Inf)
          val kibanaFacility = BrasService.getFacilityES(brasId, day)
          logger.info("tKibanaFacility: " + (System.currentTimeMillis() - t8))
          // KIBANA DDos
          val t9 = System.currentTimeMillis()
          // val kibanaDdos = Await.result(BrasService.getDdosResponse(brasId,day), Duration.Inf)
          val kibanaDdos = BrasService.getDdosES(brasId, day)
          logger.info("tKibanaDdos: " + (System.currentTimeMillis() - t9))
          // KIBANA Severity value
          val t10 = System.currentTimeMillis()
          //val severityValue = Await.result(BrasService.getSeveValueResponse(brasId,day), Duration.Inf)
          val severityValue = BrasService.getSeveValueES(brasId, day)
          logger.info("tSeverityValue: " + (System.currentTimeMillis() - t10))
          // SIGNIN LOGOFF BY HOST
          val siglogByhost = BrasService.getSigLogByHost(brasId, day)
          logger.info("tSiglogByhost: " + (System.currentTimeMillis() - t10))

          logger.info("timeAll: " + (System.currentTimeMillis() - timeStart))
          Ok(device.views.html.search(form, username,null ,BrasResponse(BrasInfor(noOutlierByhost,numOutlier, (sigLog._1, sigLog._2),(sigLogClients._1,sigLogClients._2)), KibanaOpviewByTime(kibanaBytime, opviewBytime), SigLogByTime(siginBytime, logoffBytime),
            infErrorBytime, infHostBytime, infModuleBytime, opServiceName, ServiceNameStatus(servName, servStatus, opServByStt), linecardhost, KibanaOverview(kibanaSeverity, kibanaErrorType, kibanaFacility, kibanaDdos, severityValue), siglogByhost), day, brasId,_typeS,routes.DeviceController.search))
        }
      }
      else{
        logger.info("Empty data")
        Ok(device.views.html.search(form,username,null,null,CommonService.getCurrentDay()+"/"+CommonService.getCurrentDay(),null,"B",routes.DeviceController.search))
      }
    }
    catch{
      case e: Exception => Ok(device.views.html.search(form,username,null,null,CommonService.getCurrentDay(),null,"B",routes.DeviceController.search))
    }
  }

  def getHostJson(id: String) = Action { implicit request =>
    try{
      val t0 = System.currentTimeMillis()
      val rsHost = Await.result(BrasService.getHostBras(id), Duration.Inf)
      val re = rsHost.map(
        iter =>
          Json.obj(
            "host" -> iter._1,
            "module" -> iter._2
          ) ->
            Json.obj(
              "signin" -> iter._3,
              "logoff" -> iter._4,
              "sf" -> iter._5,
              "lofi" -> iter._6,
              "user_down" -> iter._7,
              "inf_down" -> iter._8,
              "rouge_error" -> iter._9,
              "lost_signal" -> iter._10,
              "label" -> iter._11
            )
      )

      logger.info("tRsHost: " + (System.currentTimeMillis() - t0))
      val t1 = System.currentTimeMillis()
      val idBras = id.split('/')(0)
      val time = id.split('/')(1)
      val brasChart = BrasService.getJsonESBrasChart(idBras,time)
      //val listCard = Await.result(BrasService.getBrasCard(idBras,time,"",""),Duration.Inf)
      // get data heatmap chart
      val sigLog = brasChart.map({ t => (t._1,t._2,t._3)}).filter(t => CommonService.formatUTC(t._1) == time)
      val numLog = if(sigLog.asInstanceOf[Array[(String,Int,Int)]].length >0) sigLog.asInstanceOf[Array[(String,Int,Int)]](0)._2 else 0
      val numSig = if(sigLog.asInstanceOf[Array[(String,Int,Int)]].length > 0) sigLog.asInstanceOf[Array[(String,Int,Int)]](0)._3 else 0
      logger.info("tBrasChart: " + (System.currentTimeMillis() - t1))
      val t2 = System.currentTimeMillis()

      val _type = if(numLog>numSig) "LogOff" else "SignIn"
      // get logoff user
      val userLogoff = BrasService.getUserLogOff(idBras,time,_type)
      logger.info("tUserLogoff: " + (System.currentTimeMillis() - t2))
      val t3 = System.currentTimeMillis()

      // get list card
      val listCard = BrasService.getJsonBrasCard(idBras,time,_type)
      val heatCard = listCard.map(x=> x._1._2)
      val heatLinecard = listCard.map(x=> x._1._1)
      logger.info("tCard: " + (System.currentTimeMillis() - t3))
      val t4 = System.currentTimeMillis()

      // get tableIndex kibana and opview
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
      val dateTime = DateTime.parse(time, formatter)
      val oldTime  = dateTime.minusMinutes(30).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))
      val brasOpview = Await.result(BrasService.getOpsview(idBras,time,oldTime), Duration.Inf)
      val brasKibana = Await.result(BrasService.getKibana(idBras,time,oldTime), Duration.Inf)
      logger.info("tOpKiba: " + (System.currentTimeMillis() - t4))

      val t5 = System.currentTimeMillis()
      val signinUnique = if(_type == "LogOff") Await.result(BrasService.getSigninUnique(idBras, time), Duration.Inf).map(x=> x._2).sum
                         else Await.result(BrasService.getSigninUnique(idBras, time), Duration.Inf).map(x=> x._1).sum
      val trackingUser = Await.result(BrasService.getTrackingUser(idBras, time), Duration.Inf).toArray
      val users = if(trackingUser.length > 0) trackingUser(0)._1 else 0
      val perct = if(trackingUser.length > 0) trackingUser(0)._2 else 0
      val metricErr = Await.result(BrasService.getErrorMetric(idBras, time), Duration.Inf).sum
      val metrics = Json.obj(
        "signin" -> signinUnique,
        "label"  -> s"${_type} Unique",
        "error"  -> metricErr,
        "users"  -> users,
        "perct"  -> perct
      )
      logger.info("metrics: " + (System.currentTimeMillis() - t5))

      val jsBras = Json.obj(
        "metrics"      -> metrics,
        "host"         -> re,
        "sigLog"       -> sigLog,
        "time"         -> brasChart.map({ t => CommonService.formatUTC(t._1.toString)}),
        "logoff"       -> brasChart.map({ t => (CommonService.formatUTC(t._1),t._2)}),
        "signin"       -> brasChart.map({ t => (CommonService.formatUTC(t._1),t._3)}),
        "users"        -> brasChart.map({ t => t._4}),
        "heatCard"     -> heatCard,
        "heatLinecard" -> heatLinecard,
        "dtaCard"      -> listCard,
        "brasKibana"   -> brasKibana,
        "brasOpview"   -> brasOpview,
        "userLogoff"   -> userLogoff
      )
      println("time:"+ (System.currentTimeMillis() -t0))
      Ok(Json.toJson(jsBras))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

}