package controllers

import play.api.mvc._
import controllers.Secured
import javax.inject.Inject
import javax.inject.Singleton

import device.utils.{CommonUtils, LocationUtils}
import model.device.{DailyResponse, InfResponse, SigLogClientsDaily}
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
      Ok(device.views.html.daily(null,CommonService.getCurrentDay(), username, ""))
    }

    val successFunction = { data: controllers.DayPicker =>
      println("done")
      Redirect(routes.DailyController.getDaily).flashing("day" -> data.day)
    }

    val formValidationResult = formDaily.bindFromRequest
    formValidationResult.fold(errorFunction, successFunction)
  }

  def getDaily =  withAuth {username => implicit request =>
    val province = if(request.session.get("verifiedLocation").get.equals("1")){
      // only show 3 chart: Total Outliers Access Device, Inf Errors By Location, Total Error By Time
      request.session.get("location").get.split(",").map(x=> LocationUtils.getCodeProvincebyName(x)).mkString(",")
    } else ""
    try{
      logger.info("======START SERVICE DAILY PAGE======")
      val t0 = System.currentTimeMillis()
      val day = request.flash.get("day").getOrElse(CommonService.getCurrentDay())
      //  Total SignIn & LogOff Daily
      val rsSiglog        = if(province.equals("")) BrasService.getSigLogRegionDaily(day, "") else ( Array[(String, Long, Long)](), Array[(String, Long, Long)]())
      logger.info("t0:"+(System.currentTimeMillis() -t0))
      val t1 = System.currentTimeMillis()
      //  Device Errors By Location
      val rsErrorsDevice  = if(province.equals("")) BrasService.getDeviceErrorsRegionDaily(day).map(x=> x._1 -> (x._2+x._3+x._4+x._5+x._6+x._7)) else Array[(String, Double)]()
      logger.info("t1:"+(System.currentTimeMillis() -t1))
      val t2 = System.currentTimeMillis()
      //  Total Outliers Bras Device
      val brasOutlier = if(province.equals("")) Await.result(BrasService.getBrasOutlierDaily(day), Duration.Inf).filter(x=> x._1 != "" && x._1.split("-").length == 4 && x._1.split("-")(0).length == 3)
        .map(x=> (LocationUtils.getRegion(x._1.split("-")(0)), x._2)).toArray.sorted
      else Array[(String, Int)]()
      logger.info("t2:"+(System.currentTimeMillis() -t2))
      val t3 = System.currentTimeMillis()
      // Ticket at Core & Access Group
      val ticketIssues = Await.result(BrasService.getTicketIssue(day, province), Duration.Inf)
      val coreIssue = if(province.equals("")) ticketIssues.filter(x=> x._1 == "Hệ thống Core IP").map(x=> (LocationUtils.getRegion(x._2),LocationUtils.getNameProvincebyCode(x._2), x._3, x._4, x._5, x._6)).sorted
                      else Seq[(String, String, String, Int, Int, Int)]()
      val noneCoreIssue = ticketIssues.filter(x=> x._1 == "Hệ Thống Access" || x._1== "Hệ thống Ngoại vi").map(x=> (LocationUtils.getRegion(x._2),LocationUtils.getNameProvincebyCode(x._2), x._3, x._4, x._5, x._6)).sorted

      // combobox Bras Province
      val lstProvBras = if(province.equals("")) BrasService.getServiceNoticeRegionDaily(day, "*").map(x=> (x._1, x._2, x._3))
                        else BrasService.getServiceNoticeRegionDaily(day, "*").filter(x=> "Lam Dong".indexOf(x._2) >=0).map(x=> (x._1, x._2, x._3))

      logger.info("t3:"+(System.currentTimeMillis() -t3))
      logger.info(s"Page: DAILY - User:$username  - Time Query:"+(System.currentTimeMillis() -t0))
      logger.info("======END SERVICE DAILY PAGE======")
      Ok(device.views.html.daily(DailyResponse((rsSiglog._1, rsSiglog._2), rsErrorsDevice, brasOutlier, (coreIssue, noneCoreIssue), lstProvBras), day, username, province))
    }
    catch{
      case e : Exception => Ok(device.views.html.daily(null, CommonService.getCurrentDay(), username, province))
    }
  }

  def loadJsonDaily(day: String) = withAuth {username => implicit request =>
    val province = if(request.session.get("verifiedLocation").get.equals("1")){
      // only show 3 chart: Total Outliers Access Device, Inf Errors By Location, Total Error By Time
      request.session.get("location").get.split(",").map(x=> LocationUtils.getCodeProvincebyName(x)).mkString(",")
    } else ""
    try{
      val t0 = System.currentTimeMillis()
      // Total Outliers Access Device
      val infOutlier = Await.result(BrasService.getInfAccessOutlierDaily(day, province), Duration.Inf).filter(x=> x._1 != "" && x._1.split("-").length == 4 && x._1.split("-")(0).length == 3)
        .map(x=> (LocationUtils.getRegion(x._1.split("-")(0)), x._3)).toArray.sorted
      val objInfOutlier = Json.obj(
        "cate" -> infOutlier.map(x=> x._1).distinct.sorted,
        "data" -> infOutlier
      )
      logger.info("t0:"+(System.currentTimeMillis() -t0))
      val t1 = System.currentTimeMillis()
      // Service Monitor Notices
      val rsNoticeOpsview = if(province.equals("")) BrasService.getServiceNoticeRegionDaily(day, "*") else Array[(String, String,String, Double)]()
      val objNotice = Json.obj(
        "cate" -> rsNoticeOpsview.map(x=> x._1).toSeq.distinct.sorted,
        "data" -> rsNoticeOpsview
      )
      logger.info("t1:"+(System.currentTimeMillis() -t1))
      val t2 = System.currentTimeMillis()
      // Inf Errors By Location
      val rsErrorsInf     = BrasService.getInfErrorsDaily(day, "*", province).groupBy(x=> x._1).map(x=> x._1 -> x._2.map(x=> x._5).sum).toArray.sorted
      logger.info("t2:"+(System.currentTimeMillis() -t2))
      val t3 = System.currentTimeMillis()
      // Nerror (kibana & opview) By Time
      val arrOpsview      = BrasService.getOpviewBytimeResponse("*", day, 0)
      val opviewBytime    = if(province.equals(""))  (0 until 24).map(x => x -> CommonService.getIntValueByKey(arrOpsview, x)).toArray else Array[(Int, Int)]()
      val arrKibana       = BrasService.getKibanaBytimeES("*", day).groupBy(_._1).mapValues(_.map(_._2).sum).toArray
      val kibanaBytime    = if(province.equals("")) (0 until 24).map(x => x -> CommonService.getIntValueByKey(arrKibana, x)).toArray else Array[(Int, Int)]()
      val nErrorTime = Json.obj(
        "kibana" -> kibanaBytime,
        "opsview" -> opviewBytime
      )
      logger.info("t3:"+(System.currentTimeMillis() -t3))
      val t4 = System.currentTimeMillis()
      // Signin & Logoff By Time
      val rsLogsigBytime  = if(province.equals("")) BrasService.getSigLogByDaily("*", day) else SigLogClientsDaily(Array[(Int, Long, Long)](), Array[(Int, Long, Long)]())
      val objSigLogTime = Json.obj(
        "signin" -> rsLogsigBytime.signin.map(x=> x._2),
        "logoff" -> rsLogsigBytime.logoff.map(x=> x._2),
        "signinCli" -> rsLogsigBytime.signin.map(x=> x._3),
        "logoffCli" -> rsLogsigBytime.logoff.map(x=> x._3)
      )
      logger.info("t4:"+(System.currentTimeMillis() -t4))
      val t5 = System.currentTimeMillis()
      // Total Error By Time
      val rsErrorHostDaily = Await.result(BrasService.getErrorHostdaily("*", day, province), Duration.Inf).toArray
      logger.info("t5:"+(System.currentTimeMillis() -t5))
      logger.info(s"Page: DAILY - User:$username  - Time Query:"+(System.currentTimeMillis() -t0))

      val rs = Json.obj(
        "infOutlier" -> objInfOutlier,
        "objNotice"  -> objNotice,
        "objErrorInf" -> rsErrorsInf,
        "objSigLogTime" -> objSigLogTime,
        "errHostByTime" -> rsErrorHostDaily,
        "nErrorTime" -> nErrorTime,
        "province" -> province
      )
      Ok(Json.toJson(rs))
    }
    catch {
      case e: Exception => Ok("Error")
    }
  }

  def drilldownBrasOutlier(id: String, day: String) = withAuth {username => implicit request =>
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
        case _ => {
          Json.obj(
            "data" -> "",
            "key"   -> "empty",
            "location" -> ""
          )
        }
      }
      Ok(Json.toJson(rs))
    }
    catch {
      case e: Exception => Ok("Error")
    }
  }

  def drilldownSigLogDaily(id: String,day: String) = withAuth {username => implicit request =>
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

  def drilldownErrorDevice(id: String,day: String, err: String) = withAuth {username => implicit request =>
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
        case _ => {
          Json.obj(
            "data" -> "empty"
          )
        }
      }
      Ok(Json.toJson(rs))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  def drilldownInfOutlier(id: String, day: String, province: String) = withAuth {username => implicit request =>
    try{
      val rs = id match {
        // get inf by All
        case id if(id.equals("*")) => {
          val outliers = Await.result(BrasService.getInfAccessOutlierDaily(day, province), Duration.Inf).filter(x=> x._1 != "" && x._1.split("-").length == 4 && x._1.split("-")(0).length == 3)
            .map(x=> (LocationUtils.getRegion(x._1.split("-")(0)), x._3)).toArray.sorted
          Json.obj(
            "data" -> outliers,
            "key"   -> outliers.map(x=> x._1).distinct.sorted,
            "location" -> "region"
          )
        }
        // get inf by Region
        case id if(id.substring(id.indexOf(" ")+1).matches("^\\d+$")) => {
          val outliers = Await.result(BrasService.getInfAccessOutlierDaily(day, province), Duration.Inf).filter(x=> x._1 != "" && x._1.split("-").length == 4 && x._1.split("-")(0).length == 3)
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
          val outliers = Await.result(BrasService.getInfAccessOutlierDaily(day, province), Duration.Inf).filter(x=> x._1 != "" && x._1.split("-").length == 4 && x._1.split("-")(0).length == 3)
            .map(x=> ( LocationUtils.getNameProvincebyCode(x._1.split("-")(0)),x._1, x._3)).filter(x=> x._1 == id).map(x=> (x._2, x._3)).toArray.sorted
          Json.obj(
            "data"     -> outliers,
            "key"      -> outliers.map(x=> x._1).distinct.sorted,
            "location" -> "province"
          )
        }
        // get inf by Bras
        case id if(!id.substring(id.indexOf(" ")+1).matches("^\\d+$") && id.indexOf("-")>=0) =>{
          val outliers = Await.result(BrasService.getInfAccessOutlierDaily(day, province), Duration.Inf).filter(x=> x._1 != "" && x._1.split("-").length == 4 && x._1.split("-")(0).length == 3)
            .filter(x=> x._1 == id).map(x=> (x._2, x._3)).toArray.sorted
          Json.obj(
            "data" -> outliers,
            "key"   -> outliers.map(x=> x._1).distinct.sorted,
            "location" -> "bras"
          )
        }
        case _ => {
          Json.obj(
            "data" -> "",
            "key"   -> "empty",
            "location" -> ""
          )
        }
      }
      Ok(Json.toJson(rs))
    }
    catch {
      case e: Exception => Ok("Error")
    }
  }

  def drilldownInfErrorsDaily(id: String,day: String, err: String, province: String) = withAuth {username => implicit request =>
    try{
      val rs = id match {
        // get inf by Region
        case id if(id.equals("*")) => {
          BrasService.getInfErrorsDaily(day, err, province).groupBy(x=> x._1).map(x=> x._1 -> x._2.map(x=> x._5).sum).toArray.sorted
        }
        // get inf by Region
        case id if(id.substring(id.indexOf(" ")+1).matches("^\\d+$")) => {
          BrasService.getInfErrorsDaily(day, err, province).filter(x=> x._1 == id).groupBy(x=> x._2).map(x=> x._1 -> x._2.map(x=> x._5).sum).toArray.sorted
        }
        // get inf by Province
        case id if(!id.substring(id.indexOf(" ")+1).matches("^\\d+$") && id.indexOf("-")<0) => {
          BrasService.getInfErrorsDaily(day, err, province).filter(x=> x._2 == id).groupBy(x=> x._3).map(x=> x._1 -> x._2.map(x=> x._5).sum).toArray.sortWith((x, y)=> x._2>y._2).slice(0,10)
        }
        // get inf by Bras
        case id if(!id.substring(id.indexOf(" ")+1).matches("^\\d+$") && id.indexOf("-")>=0) =>{
          BrasService.getInfErrorsDaily(day, err, province).filter(x=> x._3 == id).groupBy(x=> x._4).map(x=> x._1 -> x._2.map(x=> x._5).sum).toArray.sortWith((x, y)=> x._2>y._2).slice(0,10)
        }
        case _ => {
          Array[(String, Double)]()
        }
      }
      Ok(Json.toJson(Json.obj("data" -> rs)))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  def getErrInfHourly(_type: String, id: String, day: String, bras: String, province: String) = withAuth {username => implicit request =>
    try{
      // _type: 0 hourly time
      // _type: 1 frame time
      val jsError = if(_type == "0"){
        val rs = Await.result(BrasService.getErrorHostdaily(bras, day, province), Duration.Inf).toArray
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

  def getServNoticeByType(noti: String,day: String) = withAuth {username => implicit request =>
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

  def getDailyByBrasLocation(id: String, day: String, province: String, _type: String) = withAuth {username => implicit request =>
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
      val rsErr = Await.result(BrasService.getErrorHostdaily(bras_id,day,province), Duration.Inf).toArray
      val jsonErr = _type match {
        case "0" => {
          Json.obj(
            "cates" -> (0 until 24).map(x=> x+"h"),
            "data"  -> rsErr
          )
        }
        case "1" => {
          val rsErrGroup = rsErr.map(x=> (CommonUtils.getRangeTime(x._1.toDouble), x._2, x._3, x._4, x._5, x._6, x._7,x._8))
            .groupBy(x=> x._1).map(x=> (x._1, x._2.map(y=> y._2).sum, x._2.map(y=> y._3).sum, x._2.map(y=> y._4).sum, x._2.map(y=> y._5).sum, x._2.map(y=> y._6).sum, x._2.map(y=> y._7).sum, x._2.map(y=> y._8).sum)).toArray
          Json.obj(
            "cates" -> CommonUtils.rangeTime.map(x=> x._2).toArray.sorted,
            "data"  -> rsErrGroup
          )
        }
      }

      val rs = Json.obj(
        "kibanaOpsview" -> kibaOps,
        "errorHourly"   -> jsonErr,
        "sigLogBytime"  -> jsonSigLog
      )
      Ok(Json.toJson(rs))
    }
    catch {
      case e: Exception => Ok("Error")
    }
  }

}