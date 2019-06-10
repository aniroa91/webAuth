package controllers

import javax.inject.Inject
import javax.inject.Singleton

import play.api.mvc._
import model.device.{NocCount, _}
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.text
import service.{BrasService, HostService}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import play.api.libs.json.Json
import services.domain.CommonService
import play.api.Logger
import device.utils.{CommonUtils, LocationUtils}

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
      Ok(device.views.html.monthly.overview(username,"",null))
    }

    val successFunction = { data: controllers.MonthPicker =>
      println("done")
      Redirect(routes.DeviceController.overview).flashing("startMonth" -> data.startMonth, "endMonth" -> data.endMonth)
    }

    val formValidationResult = formOverview.bindFromRequest
    formValidationResult.fold(errorFunction, successFunction)
  }

  // index page Dashboard Device Monthly
  def overview =  withAuth { username => implicit request =>
    val province = if(request.session.get("verifiedLocation").get.equals("1")){
      request.session.get("location").get.split(",").map(x=> LocationUtils.getCodeProvincebyName(x)).mkString("|")
    } else ""
    try {
      //val minMaxMonth = CommonService.getRangeCurrentMonth()
      val minMaxMonth = Await.result(BrasService.getMinMaxMonth(), Duration.Inf)
      val threeMonth = Await.result(BrasService.get3MonthLastest(),Duration.Inf)
      val fromMonth = if(request.flash.get("startMonth").toString != "None") request.flash.get("startMonth").get+"-01" else threeMonth(threeMonth.length-1)
      val toMonth = if(request.flash.get("endMonth").toString != "None") request.flash.get("endMonth").get+"-01" else threeMonth(0)
      val rangeMonth = CommonService.getAllMonthfromRange(fromMonth,toMonth)

      // get Total INF Errors By Months
      val mapProvinceTotalInf = Await.result(BrasService.getProvinceTotalInf(fromMonth,toMonth, province), Duration.Inf)
      val totalInf = mapProvinceTotalInf.map(x=> (x._1,LocationUtils.getRegion(x._2.trim),x._3)).groupBy(x=> (x._1,x._2)).mapValues(_.map(_._3).sum).toSeq.sorted.toArray.map(x=> (x._1._1,x._1._2,x._2))
      // get Total Service Monitor Notices By Months
      val mapProvinceOpsview = if(province.equals("")) Await.result(BrasService.getProvinceOpsview(fromMonth,toMonth), Duration.Inf) else Seq[(String,String,String,Int)]()
      val opsview = mapProvinceOpsview.map(x=> (x._1,LocationUtils.getNameProvincebyCode(x._2),LocationUtils.getRegion(x._2.trim), x._4,x._3)).toArray
      // get Total Device Errors By Months
      val mapProvinceKibana = if(province.equals("")) Await.result(BrasService.getProvinceKibana(fromMonth,toMonth), Duration.Inf) else Seq[(String,String,String,Int)]()
      val kibana = mapProvinceKibana.map(x=> (x._1,LocationUtils.getNameProvincebyCode(x._2),LocationUtils.getRegion(x._2.trim), x._4,x._3)).toArray
      // get Total SuyHao Not Pass By Months
      val mapSuyhao = Await.result(BrasService.getProvinceSuyhao(fromMonth,toMonth,province), Duration.Inf)
      val suyhao = mapSuyhao.map(x=> (x._1,LocationUtils.getNameProvincebyCode(x._2),LocationUtils.getRegion(x._2.trim), x._4,x._3,x._5)).toArray
      // get Total SignIn & LogOff By Months
      val mapSiglog = rangeMonth.map(x => if(province.equals("")) x -> Await.result(BrasService.getSigLogByRegion(x+"-01"), Duration.Inf).map(x=> (LocationUtils.getRegion(x._1.trim),x._2,x._3,x._4,x._5))
                      else x -> Seq[(String, Int, Int, Int, Int)]())
      val signIn = mapSiglog.map(x=>x._1-> x._2.groupBy(_._1).mapValues(_.map(_._2).sum).toSeq.sorted.map(x=> -x._2).toArray)
      val logoff = mapSiglog.map(x=>x._1-> x._2.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sorted.map(x=> x._2).toArray)
      val signIn_clients = mapSiglog.map(x=>x._1-> x._2.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sorted.map(x=> -x._2).toArray)
      val logoff_clients = mapSiglog.map(x=>x._1-> x._2.groupBy(_._1).mapValues(_.map(_._5).sum).toSeq.sorted.map(x=> x._2).toArray)
      // get Total Device Errors By Severity
      val mapCount = if(province.equals("")) Await.result(BrasService.getProvinceCount(fromMonth+"/"+toMonth), Duration.Inf) else Seq[(String,String,Int,Int,Int,Int,Int,Int)]()
      val alertCount = mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._3)).toArray
      val critCount = mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._4)).toArray
      val warningCount = mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._5)).toArray
      val noticeCount = mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._6)).toArray
      val errCount = mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._7)).toArray
      val emergCount = mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._8)).toArray
      val nocCount = NocCount(alertCount,critCount,warningCount,noticeCount,errCount,emergCount)
      // get Statistic Of Connections By CPE Signal (Access)
      val mapContract = Await.result(BrasService.getProvinceContract(fromMonth,toMonth,province), Duration.Inf)
      val contracts = mapContract.map(x=> (x._1,LocationUtils.getNameProvincebyCode(x._2),LocationUtils.getRegion(x._2.trim),x._3,x._4,x._5,x._6)).toArray
      // get Total Notices By Status
      val mapOpsviewType = if(province.equals("")) Await.result(BrasService.getProvinceOpsviewType(fromMonth+"/"+toMonth), Duration.Inf) else Seq[(String,Int,Int,Int,Int)]()
      val opsviewType = mapOpsviewType.map(x=> (LocationUtils.getRegion(x._1),x._2,x._3,x._4,x._5)).toArray
      val ok_opsview = opsviewType.groupBy(_._1).mapValues(_.map(_._2).sum).toSeq.sorted.toArray
      val warning_opsview = opsviewType.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sorted.toArray
      val unknown_opsview = opsviewType.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sorted.toArray
      val crit_opsview = opsviewType.groupBy(_._1).mapValues(_.map(_._5).sum).toSeq.sorted.toArray
      val heatmapOpsview = (ok_opsview++warning_opsview++unknown_opsview++crit_opsview).groupBy(_._1).map{case (k,v) => k -> v.map(x=> x._2.toString).mkString("_")}.toSeq.sorted.toArray
      // get Total INF Errors By Type
      val mapInfType = Await.result(BrasService.getProvinceInfDownError(fromMonth+"/"+toMonth, province), Duration.Inf)
      val infDown = mapInfType.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._3)).toArray
      val userDown = mapInfType.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._4)).toArray
      val rougeError = mapInfType.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._5)).toArray
      val lostSignal = mapInfType.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._6)).toArray
      val infTypeError = InfTypeError(infDown,userDown,rougeError,lostSignal)
      //  Total Outliers Bras Device
      val brasOutlier = Await.result(BrasService.getOutlierMonthly(fromMonth, toMonth,"bras", province), Duration.Inf).filter(x=> x._1 != "" && x._1.split("-").length == 4 && x._1.split("-")(0).length == 3)
        .map(x=> (LocationUtils.getRegion(x._1.split("-")(0)), x._2)).toArray.sorted
      //  Total Outliers Inf Device
      val infOutlier = Await.result(BrasService.getOutlierMonthly(fromMonth, toMonth,"inf", province), Duration.Inf).filter(x=> x._1 != "" && x._1.split("-").length == 4 && x._1.split("-")(0).length == 3)
        .map(x=> (LocationUtils.getRegion(x._1.split("-")(0)), x._2)).toArray.sorted

      Ok(device.views.html.monthly.overview(username,province,RegionOverview(TimePicker(minMaxMonth(0)._1.substring(0,minMaxMonth(0)._1.lastIndexOf("-")),minMaxMonth(0)._2.substring(0,minMaxMonth(0)._1.lastIndexOf("-")),fromMonth.substring(0,fromMonth.lastIndexOf("-")),
        toMonth.substring(0,toMonth.lastIndexOf("-")),rangeMonth),opsview,kibana,suyhao,SigLogRegion(signIn,logoff,signIn_clients,logoff_clients),
        nocCount,contracts,heatmapOpsview,infTypeError,totalInf,brasOutlier,infOutlier)))
    }
    catch{
      case e: Exception => Ok(device.views.html.monthly.overview(username,province,null))
    }
  }

  def drilldownOutlierMonth(id: String, fromMonth: String, toMonth: String, db: String) = withAuth {username => implicit  request =>
    val province = if(request.session.get("verifiedLocation").get.equals("1")){
      request.session.get("location").get.split(",").map(x=> LocationUtils.getCodeProvincebyName(x)).mkString("|")
    } else ""
    try{
      val rs = id match {
        // get inf by All
        case id if(id.equals("*")) => {
          val brasOutlier = Await.result(BrasService.getOutlierMonthly(fromMonth+"-01", toMonth+"-01", db, province), Duration.Inf).filter(x=> x._1 != "" && x._1.split("-").length == 4 && x._1.split("-")(0).length == 3)
            .map(x=> (LocationUtils.getRegion(x._1.split("-")(0)), x._2)).toArray.sorted
          Json.obj(
            "data" -> brasOutlier,
            "key"   -> brasOutlier.map(x=> x._1).distinct.sorted,
            "location" -> "region"
          )
        }
        // get inf by Region
        case id if(id.substring(id.indexOf(" ")+1).matches("^\\d+$")) => {
          val brasOutlier = Await.result(BrasService.getOutlierMonthly(fromMonth+"-01", toMonth+"-01", db, province), Duration.Inf).filter(x=> x._1 != "" && x._1.split("-").length == 4 && x._1.split("-")(0).length == 3)
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
          val brasOutlier = Await.result(BrasService.getOutlierMonthly(fromMonth+"-01", toMonth+"-01", db, province), Duration.Inf).filter(x=> x._1 != "" && x._1.split("-").length == 4 && x._1.split("-")(0).length == 3)
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

  def getcountType(id: String,month: String) = withAuth {username => implicit request =>
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

  def geterrorType(id: String,month: String) = withAuth {username => implicit request =>
    val province = if(request.session.get("verifiedLocation").get.equals("1")){
      request.session.get("location").get.split(",").map(x=> LocationUtils.getCodeProvincebyName(x)).mkString("|")
    } else ""
    try{
      val monthRange = if(month.indexOf("/")>=0) month.split("/")(0)+"-01/"+month.split("/")(1)+"-01" else month
      val mapInfType = Await.result(BrasService.getProvinceInfDownError(monthRange,province), Duration.Inf)
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

  def getErrorKibana(_typeError: String,month: String) = withAuth {username => implicit request =>
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

  def getserviceStatus(_typeService: String,month: String) = withAuth {username => implicit request =>
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

  def getOltPoorconn(_typeOlt: String,month: String) = withAuth {username => implicit request =>
    val province = if(request.session.get("verifiedLocation").get.equals("1")){
      request.session.get("location").get.split(",").map(x=> LocationUtils.getCodeProvincebyName(x)).mkString("|")
    } else ""
    try{
      // get Top opsview status
      val topOLT = Await.result(BrasService.getTopPoorconn(month,_typeOlt, province), Duration.Inf).map(x=> (LocationUtils.getNameProvincebyCode(x._1), x._2, x._3))
      val rs = Json.obj(
        "data" -> topOLT,
        "dataProvince" -> topOLT.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sortWith(_._2 > _._2),
        "categories" -> topOLT.map(x=>x._1).asInstanceOf[Seq[String]].distinct
      )
      Ok(Json.toJson(rs))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  def getInfTopErr(_typeInf: String,month: String) = withAuth {username => implicit request =>
    val province = if(request.session.get("verifiedLocation").get.equals("1")){
      request.session.get("location").get.split(",").map(x=> LocationUtils.getCodeProvincebyName(x)).mkString("|")
    } else ""
    try{
      // get Top total Inf
      val topInf = Await.result(BrasService.getTopInf(month,_typeInf, province), Duration.Inf).map(x=> (LocationUtils.getNameProvincebyCode(x._1), x._2, x._3, x._4))
      val rs = Json.obj(
        "data" -> topInf,
        "cates" -> topInf.map(x=> x._1).toSeq.distinct.sorted
      )
      Ok(Json.toJson(rs))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  // tab trending
  def trendByMonth() = withAuth {username => implicit request =>
    val province = if(request.session.get("verifiedLocation").get.equals("1")){
      request.session.get("location").get.split(",").map(x=> LocationUtils.getCodeProvincebyName(x)).mkString("|")
    } else ""
    try{
      val month = CommonService.getPreviousMonth()
      // get Outliers Of BRAS By Month
      val topBrasOutlier = if(province.equals("")) Await.result(BrasService.getTopBrasOutMonthly(), Duration.Inf).map(x=> x._1.substring(0, x._1.lastIndexOf("-")) -> x._2)
                           else Seq[(String, Long)]()
      val brasObj = Json.obj(
        "data"       -> topBrasOutlier.map(x=> x._2),
        "categories" -> topBrasOutlier.map(x=> x._1)
      )
      // get Connectivity By Month
      val topConnect =  if(province.equals("")) Await.result(BrasService.getTopConnectMonthly(), Duration.Inf).map(x=> (x._1.substring(0, x._1.lastIndexOf("-")), x._2, x._3))
      else Seq[(String, Long, Long)]()
      val connectObj = Json.obj(
        "categories" -> topConnect.map(x=> x._1),
        "signin" -> topConnect.map(x=> x._2),
        "logoff" -> topConnect.map(x=> x._3),
        "dataAvg" -> topConnect.map(x=> (x._2+x._3) / 2)
      )
      // get Outliers Of OLT By Month
      val topOlt = Await.result(BrasService.getTopOltMonthly(province), Duration.Inf).map(x=> x._1.substring(0, x._1.lastIndexOf("-")) -> x._2)
      val oltObj = Json.obj(
        "data"       -> topOlt.map(x=> x._2),
        "categories" -> topOlt.map(x=> x._1)
      )
      // get INF Error By Month
      val topInfErr = Await.result(BrasService.getTopInfErrMonthly(province), Duration.Inf).map(x=> x._1.substring(0, x._1.lastIndexOf("-")) -> x._2)
      val infObj = Json.obj(
        "data"       -> topInfErr.map(x=> x._2),
        "categories" -> topInfErr.map(x=> x._1)
      )
      // get BRAS Error By Month
      val topBrasErr = if(province.equals("")) Await.result(BrasService.getTopOverviewNocMonthly("total_kibana"), Duration.Inf).map(x=> x._1.substring(0, x._1.lastIndexOf("-")) -> x._2)
                       else Seq[(String, Long)]()
      val brasErrObj = Json.obj(
        "data"       -> topBrasErr.map(x=> x._2),
        "categories" -> topBrasErr.map(x=> x._1)
      )
      // get Service Monitoring By Month
      val topSevice = if(province.equals("")) Await.result(BrasService.getTopOverviewNocMonthly("total_opsview"), Duration.Inf).map(x=> x._1.substring(0, x._1.lastIndexOf("-")) -> x._2)
                      else Seq[(String, Long)]()
      val serviceObj = Json.obj(
        "data"       -> topSevice.map(x=> x._2),
        "categories" -> topSevice.map(x=> x._1)
      )
      // get Severity Of BRAS Error By Month
      val topServBrasErr = if(province.equals("")) Await.result(BrasService.getTopServBrasErrMonthly(), Duration.Inf).map(x=> (x._1.substring(0, x._1.lastIndexOf("-")), x._2, x._3, x._4, x._5, x._6, x._7))
                           else Seq[(String, Long,Long,Long,Long,Long,Long)]()
      val maxSparkline1 = if(province.equals("")) (CommonService.formatPattern(topServBrasErr.map(x=> x._2).last.toInt), CommonService.formatPattern(topServBrasErr.map(x=> x._3).last.toInt),
        CommonService.formatPattern(topServBrasErr.map(x=> x._4).last.toInt), CommonService.formatPattern(topServBrasErr.map(x=> x._5).last.toInt),
        CommonService.formatPattern(topServBrasErr.map(x=> x._6).last.toInt),CommonService.formatPattern(topServBrasErr.map(x=> x._7).last.toInt)) else ("","","","","","")
      val servBrasErrObj = Json.obj(
        "alert" -> topServBrasErr.map(x=> x._1 -> x._2),
        "crit" -> topServBrasErr.map(x=> x._1 -> x._3),
        "emerg" -> topServBrasErr.map(x=> x._1 -> x._4),
        "err" -> topServBrasErr.map(x=> x._1 -> x._5),
        "notice" -> topServBrasErr.map(x=> x._1 -> x._6),
        "warn" -> topServBrasErr.map(x=> x._1 -> x._7),
        "maxSparkline" -> maxSparkline1
      )
      // get Severity Of BRAS Error By Month
      val topServOpsview = if(province.equals("")) Await.result(BrasService.getTopServOpsviewMonthly(), Duration.Inf).map(x=> (x._1.substring(0, x._1.lastIndexOf("-")), x._2, x._3, x._4, x._5))
                           else Seq[(String, Long,Long,Long,Long)]()
      val maxSparkline2 = if(province.equals("")) (CommonService.formatNumber(topServOpsview.map(x=> x._2).last.toInt), CommonService.formatNumber(topServOpsview.map(x=> x._3).last.toInt), CommonService.formatNumber(topServOpsview.map(x=> x._4).last.toInt),
           CommonService.formatNumber(topServOpsview.map(x=> x._5).last.toInt)) else ("","","","")
      val servOpsviewObj = Json.obj(
        "crit" -> topServOpsview.map(x=> x._1 -> x._2),
        "ok" -> topServOpsview.map(x=> x._1 -> x._3),
        "warn" -> topServOpsview.map(x=> x._1 -> x._4),
        "unknown" -> topServOpsview.map(x=> x._1 -> x._5),
        "maxSparkline" -> maxSparkline2
      )
      // get Detail Of INF Error By Month
      val topServInfErr = Await.result(BrasService.getTopServInfErrMonthly(province), Duration.Inf).map(x=> (x._1.substring(0, x._1.lastIndexOf("-")), x._2, x._3, x._4, x._5, x._6, x._7))
      val maxSparkline3 = if(province.equals("")) (CommonService.formatNumber(topServInfErr.map(x=> x._2).last.toInt), CommonService.formatNumber(topServInfErr.map(x=> x._3).last.toInt),
        CommonService.formatNumber(topServInfErr.map(x=> x._4).last.toInt), CommonService.formatNumber(topServInfErr.map(x=> x._5).last.toInt), CommonService.formatNumber(topServInfErr.map(x=> x._6).last.toInt),CommonService.formatNumber(topServInfErr.map(x=> x._7).last.toInt))
                          else ("","","","","","")
      val servInfErrObj = Json.obj(
        "infDown" -> topServInfErr.map(x=> x._1 -> x._2),
        "userDown" -> topServInfErr.map(x=> x._1 -> x._3),
        "sf" -> topServInfErr.map(x=> x._1 -> x._4),
        "lofi" -> topServInfErr.map(x=> x._1 -> x._5),
        "lost" -> topServInfErr.map(x=> x._1 -> x._6),
        "rouge" -> topServInfErr.map(x=> x._1 -> x._7),
        "maxSparkline" -> maxSparkline3
      )

      val rs = Json.obj(
        "month"      -> month,
        "topConnect" -> connectObj,
        "topBras"    -> brasObj,
        "topInf"     -> infObj,
        "topOlt"     -> oltObj,
        "topBrasErr" -> brasErrObj,
        "topService" -> serviceObj,
        "servBrasErr" -> servBrasErrObj,
        "servOpsview" -> servOpsviewObj,
        "servInfError"  -> servInfErrObj
      )
      Ok(Json.toJson(rs))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  // Tab Compare
  def compareByMonth(month: String) = withAuth{username => implicit request =>
    val province = if(request.session.get("verifiedLocation").get.equals("1")){
      request.session.get("location").get.split(",").map(x=> LocationUtils.getCodeProvincebyName(x)).mkString("|")
    } else ""
    try{
      val currMonth = if(month.equals("")) CommonService.getPreviousMonth() else month
      val prevMonth = CommonService.getPreviousMonth(currMonth)
      /* Box Signin & Logoff */
      val sigLog = if(province.equals("")) Await.result(BrasService.getSigLogByMonth(currMonth), Duration.Inf) else Seq[(String, Int, Int)]()
      var percent = if(province.equals("")) CommonService.percent(sigLog.filter(x=> x._1 == currMonth+"-01").map(x=> x._2).sum.toLong, sigLog.filter(x=> x._1 == prevMonth+"-01").map(x=> x._2).sum.toLong)
                    else 0.0
      val signinObj = Json.obj(
        "currSignin" -> sigLog.filter(x=> x._1 == currMonth+"-01").map(x=> x._2).sum,
        "prevSignin" -> sigLog.filter(x=> x._1 == prevMonth+"-01").map(x=> x._2).sum,
        "percent"    -> percent
      )
      percent = if(province.equals("")) CommonService.percent(sigLog.filter(x=> x._1 == currMonth+"-01").map(x=> x._3).sum.toLong, sigLog.filter(x=> x._1 == prevMonth+"-01").map(x=> x._3).sum.toLong)
                else 0.0
      val logoffObj = Json.obj(
        "currLogoff" -> sigLog.filter(x=> x._1 == currMonth+"-01").map(x=> x._3).sum,
        "prevLogoff" -> sigLog.filter(x=> x._1 == prevMonth+"-01").map(x=> x._3).sum,
        "percent"    -> percent
      )
      /* Box Suyhao */
      val suyhao = Await.result(BrasService.getSuyhaoByMonth(currMonth, province), Duration.Inf)
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
      percent = if(province.equals("")) CommonService.percent(brasOutlier.filter(x=> x._1 == currMonth+"-01").map(x=> x._2).sum.toLong, brasOutlier.filter(x=> x._1 == prevMonth+"-01").map(x=> x._2).sum.toLong)
                else 0.0
      val brasObj = Json.obj(
        "currBras" -> brasOutlier.filter(x=> x._1 == currMonth+"-01").map(x=> x._2).sum,
        "prevBras" -> brasOutlier.filter(x=> x._1 == prevMonth+"-01").map(x=> x._2).sum,
        "percent"  -> percent
      )
      percent = if(province.equals("")) CommonService.percent(brasOutlier.filter(x=> x._1 == currMonth+"-01").map(x=> x._3).sum.toLong, brasOutlier.filter(x=> x._1 == prevMonth+"-01").map(x=> x._3).sum.toLong)
                else 0.0
      val affectObj = Json.obj(
        "currAffect" -> brasOutlier.filter(x=> x._1 == currMonth+"-01").map(x=> x._3).sum,
        "prevAffect" -> brasOutlier.filter(x=> x._1 == prevMonth+"-01").map(x=> x._3).sum,
        "percent"  -> percent
      )
      /* Box INF Outlier */
      val infOutlier = Await.result(BrasService.getInfOutlierByMonth(currMonth, province), Duration.Inf)
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
      val device = Await.result(BrasService.getDeviceByMonth(currMonth, province), Duration.Inf)
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
      val kibaOps = if(province.equals("")) Await.result(BrasService.getKibaOpsByMonth(currMonth), Duration.Inf) else Seq[(String, Int, Int)]()
      percent = if(province.equals("")) CommonService.percent(kibaOps.filter(x=> x._1 == currMonth+"-01").map(x=> x._2).sum.toLong, kibaOps.filter(x=> x._1 == prevMonth+"-01").map(x=> x._2).sum.toLong)
                else 0.0
      val kibanaObj = Json.obj(
        "currKiba" -> kibaOps.filter(x=> x._1 == currMonth+"-01").map(x=> x._2).sum,
        "prevKiba" -> kibaOps.filter(x=> x._1 == prevMonth+"-01").map(x=> x._2).sum,
        "percent"  -> percent
      )
      percent = if(province.equals("")) CommonService.percent(kibaOps.filter(x=> x._1 == currMonth+"-01").map(x=> x._3).sum.toLong, kibaOps.filter(x=> x._1 == prevMonth+"-01").map(x=> x._3).sum.toLong)
                else 0.0
      val opsObj = Json.obj(
        "currOps" -> kibaOps.filter(x=> x._1 == currMonth+"-01").map(x=> x._3).sum,
        "prevOps" -> kibaOps.filter(x=> x._1 == prevMonth+"-01").map(x=> x._3).sum,
        "percent"  -> percent
      )
      /* Box INF Error */
      val infError = Await.result(BrasService.getInfErrorByMonth(currMonth,province), Duration.Inf)
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
        "province"   -> province,
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
  def topNJson(monthStr: String,_typeError: String,_typeService: String,_typeOLTpoor: String,_typeInferr: String) = withAuth {username => implicit request =>
    val province = if(request.session.get("verifiedLocation").get.equals("1")){
      request.session.get("location").get.split(",").map(x=> LocationUtils.getCodeProvincebyName(x)).mkString("|")
    } else ""
    try{
      val month = if(monthStr.equals("")) CommonService.getPreviousMonth() else monthStr
      // get Top Sigin
      val topSignin = if(province.equals("")) Await.result(BrasService.getTopSignin(month), Duration.Inf).map(x=> (LocationUtils.getNameProvincebyCode(x._1), x._2, x._3, x._4))
                      else Seq[(String, String, Int, Int)]()
      val siginObj = Json.obj(
        "data" -> topSignin,
        "dataProvince" -> topSignin.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sortWith(_._2 > _._2),
        "dataClients" -> topSignin.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sortWith(_._2 > _._2),
        "categories" -> topSignin.map(x=>x._1).asInstanceOf[Seq[String]].distinct
      )
      // get Top Logoff
      val topLogoff = if(province.equals("")) Await.result(BrasService.getTopLogoff(month), Duration.Inf).map(x=> (LocationUtils.getNameProvincebyCode(x._1), x._2, x._3, x._4))
                      else Seq[(String,String,Int,Int)]()
      val logoffObj = Json.obj(
        "data" -> topLogoff,
        "dataProvince" -> topLogoff.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sortWith(_._2 > _._2),
        "dataClients" -> topLogoff.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sortWith(_._2 > _._2),
        "categories" -> topLogoff.map(x=>x._1).asInstanceOf[Seq[String]].distinct
      )
      // get top province Bras outliers
      val topBrasOut = if(province.equals("")) Await.result(BrasService.topBrasOut(month), Duration.Inf).map(x=> (LocationUtils.getNameProvincebyCode(x._1), x._2, x._3))
                       else Seq[(String, String, Int)]()
      val brasOutObj = Json.obj(
        "data" -> topBrasOut,
        "dataProvince" -> topBrasOut.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sortWith(_._2 > _._2),
        "dataClients" -> "",
        "categories" -> topBrasOut.map(x=>x._1).asInstanceOf[Seq[String]].distinct
      )
      // get top province Inf outliers
      val topInfOut = Await.result(BrasService.topInfOut(month, province), Duration.Inf).map(x=> (LocationUtils.getNameProvincebyCode(x._1), x._2, x._3))
      val infOutObj = Json.obj(
        "data" -> topInfOut,
        "dataProvince" -> topInfOut.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sortWith(_._2 > _._2),
        "dataClients" -> "",
        "categories" -> topInfOut.map(x=>x._1).asInstanceOf[Seq[String]].distinct
      )
      // get Top total kibana
      val topKibana = if(province.equals("")) Await.result(BrasService.getTopKibana(month,_typeError), Duration.Inf).map(x=> (LocationUtils.getNameProvincebyCode(x._1), x._2, x._3))
                      else Seq[(String, String, Int)]()
      val kibanaObj = Json.obj(
        "data" -> topKibana,
        "dataProvince" -> topKibana.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sortWith(_._2 > _._2),
        "categories" -> topKibana.map(x=>x._1).asInstanceOf[Seq[String]].distinct
      )
      // get Top total opsview
      val topOpsview = if(province.equals("")) Await.result(BrasService.getTopOpsview(month,_typeService), Duration.Inf).map(x=> (LocationUtils.getNameProvincebyCode(x._1), x._2, x._3))
                       else Seq[(String, String, Int)]()
      val opsviewObj = Json.obj(
        "data" -> topOpsview,
        "dataProvince" -> topOpsview.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sortWith(_._2 > _._2),
        "categories" -> topOpsview.map(x=>x._1).asInstanceOf[Seq[String]].distinct
      )
      // get Top total Inf
      val topInf = Await.result(BrasService.getTopInf(month,_typeInferr, province), Duration.Inf).map(x=> (LocationUtils.getNameProvincebyCode(x._1), x._2, x._3, x._4))
      val infObj = Json.obj(
        "data" -> topInf,
        "cates" -> topInf.map(x=> x._1).toSeq.distinct.sorted
      )
      // get Top Not Passed Suyhao
      val topSuyhao = Await.result(BrasService.getTopnotSuyhao(month,province), Duration.Inf).map(x=> (LocationUtils.getNameProvincebyCode(x._1), x._2, x._3, x._4))
      val suyhaoObj = Json.obj(
        "data" -> topSuyhao,
        "dataProvince" -> topSuyhao.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sortWith(_._2 > _._2),
        "dataClients" -> topSuyhao.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sortWith(_._2 > _._2),
        "categories" -> topSuyhao.map(x=>x._1).asInstanceOf[Seq[String]].distinct
      )
      // get Top 10 OLT By Poor Connections
      val topPoor = Await.result(BrasService.getTopPoorconn(month,_typeOLTpoor,province), Duration.Inf).map(x=> (LocationUtils.getNameProvincebyCode(x._1), x._2, x._3))
      val poorObj = Json.obj(
        "data" -> topPoor,
        "dataProvince" -> topPoor.groupBy(_._1).mapValues(_.map(_._3).sum).toSeq.sortWith(_._2 > _._2),
        "categories" -> topPoor.map(x=>x._1).asInstanceOf[Seq[String]].distinct
      )

      val rs = Json.obj(
        "month"      -> month,
        "province"   -> province,
        "topSignin"  -> siginObj,
        "topLogoff"  -> logoffObj,
        "topBrasOut" -> brasOutObj,
        "topInfOut"  -> infOutObj,
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

  def groupRegionByMonth(month: String,_typeNoc: String,_typeError: String) = withAuth {username => implicit request =>
    val province = if(request.session.get("verifiedLocation").get.equals("1")){
      request.session.get("location").get.split(",").map(x=> LocationUtils.getCodeProvincebyName(x)).mkString("|")
    } else ""
    try{
      val monthRange = if(month.indexOf("/")>=0) month.split("/")(0)+"-01/"+month.split("/")(1)+"-01" else month
      // get Opsview Types
      val mapOpsviewType = if(province.equals("")) Await.result(BrasService.getProvinceOpsviewType(monthRange), Duration.Inf) else Seq[(String,Int,Int,Int,Int)]()
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
      val mapCount = if(province.equals("")) Await.result(BrasService.getProvinceCount(monthRange), Duration.Inf) else Seq[(String,String,Int,Int,Int,Int,Int,Int)]()
      val typeCount = if(province.equals("")) _typeNoc match {
        case "AlertCount" => mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._3)).toArray
        case "CritCount" => mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._4)).toArray
        case "WarningCount" => mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._5)).toArray
        case "NoticeCount" => mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._6)).toArray
        case "ErrCount" => mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._7)).toArray
        case "EmergCount" => mapCount.map(x=> (LocationUtils.getRegion(x._1.trim),LocationUtils.getNameProvincebyCode(x._1),x._2,x._8)).toArray
      } else Array[(String, String, String, Int)]()

      val nocCountObj = Json.obj(
        "dataSeries" -> typeCount.groupBy(_._1).mapValues(_.map(_._4).sum).toSeq.sorted,
        "data" -> typeCount,
        "categories" -> typeCount.map(x=> x._1).toSeq.distinct.sorted
      )
      // get Inf down, user down,lost signal,rouge error by Region
      val mapInfType = Await.result(BrasService.getProvinceInfDownError(monthRange,province), Duration.Inf)
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
      )

      Ok(Json.toJson(rs))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  def drilldownSigLogMonth(id: String,month: String) = withAuth {username => implicit  request =>
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

  def drillUpSigLogmonth(id: String,month: String) = withAuth {username => implicit request  =>
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

  def drilldownTotalInf(id: String,month: String) = withAuth { username => implicit  request =>
    val province = if(request.session.get("verifiedLocation").get.equals("1")){
      request.session.get("location").get.split(",").map(x=> LocationUtils.getCodeProvincebyName(x)).mkString("|")
    } else ""
      try{
      val rangeMonth = CommonService.getAllMonthfromRange(month.split("/")(0)+"-01",month.split("/")(1)+"-01")
      val jsInf = id match {
          // get inf by Region
        case id if(id.substring(id.indexOf(" ")+1).matches("^\\d+$")) => {
          val rs = Await.result(BrasService.getProvinceTotalInf(month.split("/")(0)+"-01",month.split("/")(1)+"-01", province), Duration.Inf).map(x=> (x._1,x._2,LocationUtils.getRegion(x._2.trim),x._3)).asInstanceOf[Seq[(String,String,String,Double)]].filter(x=> x._3==id).groupBy(x=> (x._1,x._2)).mapValues(_.map(_._4).sum).toSeq.sorted.map(x=> (x._1._1,LocationUtils.getNameProvincebyCode(x._1._2),x._2))
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

  def drillUpTotalInf(id: String,month: String) = withAuth {username => implicit request  =>
    val province = if(request.session.get("verifiedLocation").get.equals("1")){
      request.session.get("location").get.split(",").map(x=> LocationUtils.getCodeProvincebyName(x)).mkString("|")
    } else ""
    try{
      val rangeMonth = CommonService.getAllMonthfromRange(month.split("/")(0)+"-01",month.split("/")(1)+"-01")
      val rs = id match {
        case id if(id.equals("*")) => Await.result(BrasService.getProvinceTotalInf(month.split("/")(0)+"-01",month.split("/")(1)+"-01", province), Duration.Inf).map(x=> (x._1,LocationUtils.getRegion(x._2.trim),x._3)).groupBy(x=> (x._1,x._2)).mapValues(_.map(_._3).sum).toSeq.sorted.map(x=> (x._1._1,x._1._2,x._2))
        case id if(id.indexOf("Region")>=0) => Await.result(BrasService.getProvinceTotalInf(month.split("/")(0)+"-01",month.split("/")(1)+"-01", province), Duration.Inf).map(x=> (x._1,x._2,LocationUtils.getRegion(x._2.trim),x._3)).asInstanceOf[Seq[(String,String,String,Double)]].filter(x=> x._3==id.split(":")(1)).groupBy(x=> (x._1,x._2)).mapValues(_.map(_._4).sum).toSeq.sorted.map(x=> (x._1._1,LocationUtils.getNameProvincebyCode(x._1._2),x._2))
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

  def drillUpNoticeByStatus(id: String,month: String) = withAuth {username => implicit request  =>
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

  def drilldownNoticeByStatus(id: String,month: String) = withAuth {username => implicit  request =>
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

  // This will be the action that handles our form post
  def getFormBras = withAuth { username => implicit request: Request[AnyContent] =>

    val errorFunction = { formWithErrors: Form[controllers.BrasOutlier] =>
      println("error")
      Ok(device.views.html.search(form,username,"",null,null,CommonService.getCurrentDay()+"/"+CommonService.getCurrentDay(),null,"B",routes.DeviceController.search))
    }

    val successFunction = { data: controllers.BrasOutlier =>
      println("done")
      Redirect(routes.DeviceController.search).flashing("bras" -> data.bras.toUpperCase, "_typeS" -> data._typeS, "date" -> data.date)
    }

    val formValidationResult = form.bindFromRequest
    formValidationResult.fold(errorFunction, successFunction)
  }

  def fwdRequest(bras: String) = withAuth {username => implicit request: Request[AnyContent] =>
    Redirect(routes.DeviceController.search).flashing( "bras" -> bras.toUpperCase, "_typeS" -> "B", "date" -> "")
  }

  def suggest(ct: String, _type: String) = withAuth { username =>  implicit request: Request[AnyContent] =>
    val bras = BrasService.getSuggestions(ct.toUpperCase, _type)
    val rs = Json.obj(
      "data" -> bras
    )
    Ok(Json.toJson(rs))
  }

  def search =  withAuth { username => implicit request: Request[AnyContent] =>
    val province = if(request.session.get("verifiedLocation").get.equals("1")){
      request.session.get("location").get.split(",").map(x=> LocationUtils.getCodeProvincebyName(x)).mkString("|")
    } else ""
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
        // author
        if(!province.equals("") && !brasId.substring(0,3).equals(province)){
          Redirect(controllers.routes.ErrorController.index)
        }
        else{
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
            // get ticket timeline
            val t7 = System.currentTimeMillis()
            val ticketOutlier = Await.result(HostService.getTicketOutlierByHost(brasId, day), Duration.Inf)
            logger.info("tTicket: " + (System.currentTimeMillis() - t7))

            logger.info("timeHost:"+ (System.currentTimeMillis() - t00))
            Ok(device.views.html.search(form,username,province,HostResponse(ticketOutlier,noOutlierModule,errHost,errorHourly,sigLogModule,arrSiglogModuleIndex,suyhaoModule,sigLogByHourly,splitterByHost,ErrModuleIndex(arrModule,arrIndex,errModuleIndex),sfContract),null,day,brasId,"I",routes.DeviceController.search))
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
            // INF SERVICE
            val serviceByTime = Await.result(BrasService.getServiceNameStt(brasId.substring(0, brasId.indexOf("-")), day), Duration.Inf).map(x=> (x._1, x._2.toLowerCase(), x._3))
            logger.info("serviceByTime: " + (System.currentTimeMillis() - t30))
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
            // SERVICE SANKEY
            val t11 = System.currentTimeMillis()
            val sankeyService = Await.result(BrasService.getSankeyService(brasId.substring(0, brasId.indexOf("-")), day), Duration.Inf)
            logger.info("tSankeyService: " + (System.currentTimeMillis() - t11))
            // table Nerror Of Power Devices By Service And Status
            val t12 = System.currentTimeMillis()
            val devServByStt = Await.result(BrasService.getDeviceServStatus(brasId.substring(0, brasId.indexOf("-")), day), Duration.Inf)
            val devName = devServByStt.map(x => x._2).distinct
            val devStatus = devServByStt.map(x => x._3).distinct
            val devBras = devServByStt.map(x => x._1).distinct
            logger.info("tDeviceStatus: " + (System.currentTimeMillis() - t12))
            val t13 = System.currentTimeMillis()
            val ticketOutlier = Await.result(BrasService.getTicketOutlierByBrasId(brasId, day), Duration.Inf)
            logger.info("tTicket: " + (System.currentTimeMillis() - t13))

            logger.info("timeAll: " + (System.currentTimeMillis() - timeStart))
            Ok(device.views.html.search(form, username,province,null ,BrasResponse(ticketOutlier, BrasInfor(noOutlierByhost,numOutlier, (sigLog._1, sigLog._2),(sigLogClients._1,sigLogClients._2)), KibanaOpviewByTime(kibanaBytime, opviewBytime), SigLogByTime(siginBytime, logoffBytime),
              infErrorBytime, serviceByTime, infModuleBytime, opServiceName, ServiceNameStatus(servName, servStatus, opServByStt), linecardhost,
              KibanaOverview(kibanaSeverity, kibanaErrorType, kibanaFacility, kibanaDdos, severityValue), siglogByhost, sankeyService,
              DeviceNameStatus(devBras, devName, devStatus, devServByStt)), day, brasId,_typeS,routes.DeviceController.search))
         }
        }
      }
      else{
        logger.info("Empty data")
        Ok(device.views.html.search(form,username,province,null,null,CommonService.getCurrentDay()+"/"+CommonService.getCurrentDay(),null,if(province.equals("")) "B" else "I",routes.DeviceController.search))
      }
    }
    catch{
      case e: Exception => Ok(device.views.html.search(form,username,province,null,null,CommonService.getCurrentDay()+"/"+CommonService.getCurrentDay(),null,
        if(province.equals("")) "B" else "I",routes.DeviceController.search))
    }
  }

}