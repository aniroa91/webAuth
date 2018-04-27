package controllers

import javax.inject.Inject
import javax.inject.Singleton

import play.api.mvc._
import com.google.common.util.concurrent.AbstractService
import model.device._
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

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
case class BrasOutlier(csrfToken: String,_typeS: String,bras: String,date: String)

@Singleton
class DeviceController @Inject()(cc: MessagesControllerComponents) extends MessagesAbstractController(cc) with Secured{

  private var searching = controllers.BrasOutlier("","","","")
  val logger: Logger = Logger(this.getClass())
  val form = Form(
    mapping(
      "csrfToken" -> text,
      "_typeS" -> text,
      "bras" -> text,
      "date" -> text
    )(BrasOutlier.apply)(BrasOutlier.unapply)
  )

  /*def index =  withAuth { username => implicit request =>
    try {
      // get data for map chart
      val mapNoc = Await.result(BrasService.listNocOutlier, Duration.Inf)
      Ok(views.html.device.index(username,mapNoc))
    }
    catch{
      case e: Exception => Ok(views.html.device.index(username,null))
    }
  }*/

  def dashboard =  withAuth { username => implicit request =>
    try {
      val mapBrasOutlier = Await.result(BrasService.getBrasOutlierCurrent(CommonService.getCurrentDay()),Duration.Inf)
      Ok(device.views.html.dashboard(username,mapBrasOutlier))
    }
    catch{
      case e: Exception => Ok(device.views.html.dashboard(username,null))
    }
  }

  def realtimeBras() =  withAuth { username => implicit request =>
    val bras = Await.result(BrasService.getBrasOutlierCurrent(CommonService.getCurrentDay()),Duration.Inf)
    val jsBras = Json.obj(
      "bras" -> bras
    )
    Ok(Json.toJson(jsBras))
  }

  // This will be the action that handles our form post
  def getFormBras = withAuth { username => implicit request: Request[AnyContent] =>

    val errorFunction = { formWithErrors: Form[controllers.BrasOutlier] =>
      println("error")
      Ok(device.views.html.search(form,username,null,null,CommonService.getCurrentDay()+"/"+CommonService.getCurrentDay(),null,"B",routes.DeviceController.search))
    }

    val successFunction = { data: controllers.BrasOutlier =>
      println("done")
      searching = controllers.BrasOutlier(csrfToken = data.csrfToken, _typeS = data._typeS,bras = data.bras,date = data.date)
      Redirect(routes.DeviceController.search).flashing("info" -> "Bras searching!")
    }

    val formValidationResult = form.bindFromRequest
    formValidationResult.fold(errorFunction, successFunction)
  }

  def search =  withAuth { username => implicit request: Request[AnyContent] =>
    try {
      if (!searching.bras.equals("")) {
        val _typeS = searching._typeS
        val time = searching.date
        var day = ""
        val brasId = searching.bras
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
          val t0 = System.currentTimeMillis()
          // get errors by host tableIndex
          val errHost = Await.result(HostService.getInfHostDailyResponse(brasId,day), Duration.Inf)
          /* get bubble chart sigin and logoff by host */
          val sigLogbyModuleIndex = HostService.getSigLogbyModuleIndex(brasId,day)
          for(i <- 0 until sigLogbyModuleIndex.length){
            // check group both signin and logoff
            if(sigLogbyModuleIndex(i)._2.indexOf("_") >= 0){
              arrSiglogModuleIndex +:= (sigLogbyModuleIndex(i)._1._1,sigLogbyModuleIndex(i)._1._2,sigLogbyModuleIndex(i)._2.split("_")(0).toInt,sigLogbyModuleIndex(i)._2.split("_")(1).toInt)
            }
          }
          val siginByModule = arrSiglogModuleIndex.groupBy(_._1).mapValues(_.map(_._3).sum).toArray
          val logoffByModule = arrSiglogModuleIndex.groupBy(_._1).mapValues(_.map(_._4).sum).toArray
          val sigLogModule = (siginByModule++logoffByModule).groupBy(_._1).map{case (k,v) => k -> v.map(x=> x._2.toString).mkString("_")}.toArray
          /* get total error by hourly*/
          val errorHourly = Await.result(HostService.getErrorHostbyHourly(brasId,day), Duration.Inf)
          /* get suyhao by module*/
          val suyhaoModule = Await.result(HostService.getSuyhaobyModule(brasId,day), Duration.Inf)
          /* get sigin and logoff by hourly */
          val sigLogByHourly = HostService.getSiglogByHourly(brasId,day)
          /* get splitter by host*/
          val splitterByHost = Await.result(HostService.getSplitterByHost(brasId,day), Duration.Inf)
          /* get tableIndex error by module and index */
          val errModuleIndex = Await.result(HostService.getErrorTableModuleIndex(brasId,day), Duration.Inf)
          val arrModule = errModuleIndex.map(x=>x._1).distinct.toArray
          val arrIndex = errModuleIndex.map(x=>x._2).distinct.toArray
          // get table contract with sf>300
          val sfContract = Await.result(HostService.getContractwithSf(brasId,day), Duration.Inf)

          println("timeHost:"+ (System.currentTimeMillis() - t0))
          Ok(device.views.html.search(form,username,HostResponse(errHost,errorHourly,sigLogModule,arrSiglogModuleIndex,suyhaoModule,sigLogByHourly,splitterByHost,ErrModuleIndex(arrModule,arrIndex,errModuleIndex),sfContract),null,day,brasId,"I",routes.DeviceController.search))
        }
        // for result BRAS
        else {
          /* GET ES CURRENT */
          if (day.split("/")(1).equals(CommonService.getCurrentDay())) {
            // number outlier
            numOutlier = BrasService.getNoOutlierCurrent(brasId, CommonService.getCurrentDay())
          }
          /* GET HISTORY DATA */
          if (!fromDay.equals(CommonService.getCurrentDay())) {
            // number sigin and logoff
            /*val siginLogoff = Await.result(BrasService.getSigLogResponse(brasId, fromDay, nextDay), Duration.Inf)
            sigin = if (siginLogoff.length > 0) {
              siginLogoff(0)._1 + sigin
            } else sigin
            logoff = if (siginLogoff.length > 0) {
              siginLogoff(0)._2 + logoff
            } else logoff
            logger.info("t00: " + (System.currentTimeMillis() - t0))*/
            val t00 = System.currentTimeMillis()
            // number outlier
            val noOutlier = Await.result(BrasService.getNoOutlierResponse(brasId, day), Duration.Inf)
            numOutlier += noOutlier.sum.toInt
            logger.info("t00: " + (System.currentTimeMillis() - t00))
          }
          // number sigin and logoff
          val t01 = System.currentTimeMillis()
          val sigLog = BrasService.getSigLogCurrent(brasId, day)
          logger.info("tSigLogBytime: " + (System.currentTimeMillis() - t01))
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
          val arrOpsview = Await.result(BrasService.getOpviewBytimeResponse(brasId, day, 0), Duration.Inf).toArray
          val opviewBytime = (0 until 24).map(x => x -> CommonService.getIntValueByKey(arrOpsview, x)).toArray
          logger.info("tOpsviewBytime: " + (System.currentTimeMillis() - t1))
          //val arrKibana = Await.result(BrasService.getKibanaBytimeResponse(brasId,day,0), Duration.Inf).toArray
          val t20 = System.currentTimeMillis()
          val arrKibana = BrasService.getKibanaBytimeES(brasId, day).groupBy(_._1).mapValues(_.map(_._2).sum).toArray
          val kibanaBytime = (0 until 24).map(x => x -> CommonService.getIntValueByKey(arrKibana, x)).toArray
          logger.info("tKibanaBytime: " + (System.currentTimeMillis() - t20))
          // INF ERROR
          val t3 = System.currentTimeMillis()
          val arrInferror = Await.result(BrasService.getInfErrorBytimeResponse(brasId, day, 0), Duration.Inf).toArray
          val infErrorBytime = (0 until 24).map(x => x -> CommonService.getIntValueByKey(arrInferror, x)).toArray
          //val infErrorBytime = null
          // INF HOST
          val infHostBytime = Await.result(BrasService.getInfhostResponse(brasId, day), Duration.Inf).map(x => x._1 -> (x._2 -> x._3))
          logger.info("tInfHostBytime: " + (System.currentTimeMillis() - t3))
          // val infHostBytime = null
          val t4 = System.currentTimeMillis()
          // INF MODULE
          val infModuleBytime = Await.result(BrasService.getInfModuleResponse(brasId, day), Duration.Inf)
          logger.info("tInfModuleBytime: " + (System.currentTimeMillis() - t4))
          //val infModuleBytime = null
          val t5 = System.currentTimeMillis()
          // OPVIEW TREE MAP
          val opServiceName = Await.result(BrasService.getOpsviewServiceSttResponse(brasId, day), Duration.Inf)
          //val opServiceName = null
          // OPVIEW SERVICE BY STATUS
          val opServByStt = Await.result(BrasService.getOpServByStatusResponse(brasId, day), Duration.Inf)
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
          Ok(device.views.html.search(form, username,null ,BrasResponse(BrasInfor(numOutlier, (sigLog._1, sigLog._2)), KibanaOpviewByTime(kibanaBytime, opviewBytime), SigLogByTime(siginBytime, logoffBytime),
            infErrorBytime, infHostBytime, infModuleBytime, opServiceName, ServiceNameStatus(servName, servStatus, opServByStt), linecardhost, KibanaOverview(kibanaSeverity, kibanaErrorType, kibanaFacility, kibanaDdos, severityValue), siglogByhost), day, brasId,_typeS,routes.DeviceController.search))
        }
      }
      else
        Ok(device.views.html.search(form,username,null,null,CommonService.getCurrentDay()+"/"+CommonService.getCurrentDay(),null,"B",routes.DeviceController.search))
    }
    catch{
      case e: Exception => Ok(device.views.html.search(form,username,null,null,CommonService.getCurrentDay(),null,"B",routes.DeviceController.search))
    }
  }

  def getHostJson(id: String) = Action { implicit request =>
    try{
      val rsHost = Await.result(BrasService.getHostBras(id), Duration.Inf)
    //logger.info("success 0")
      val re = rsHost.map(
        iter =>
          Json.obj(
            "host" -> iter._1,
            "module" -> iter._2
          ) ->
            Json.obj(
              "cpe" -> iter._3,
              "lostip" -> iter._4
            )
      )
      val idBras = id.split('/')(0)
      val time = id.split('/')(1)
      val brasChart = BrasService.getJsonESBrasChart(idBras,time)
    //logger.info("success 1")
      //val listCard = Await.result(BrasService.getBrasCard(idBras,time,"",""),Duration.Inf)
      // get data heatmap chart
      val sigLog = brasChart.map({ t => (t._1,t._2,t._3)}).filter(t => CommonService.formatUTC(t._1) == time)
      val numLog = if(sigLog.asInstanceOf[Array[(String,Int,Int)]].length >0) sigLog.asInstanceOf[Array[(String,Int,Int)]](0)._2 else 0
      val numSig = if(sigLog.asInstanceOf[Array[(String,Int,Int)]].length > 0) sigLog.asInstanceOf[Array[(String,Int,Int)]](0)._3 else 0
      val _type = if(numLog>numSig) "LogOff" else "SignIn"
      // get logoff user
      val userLogoff = BrasService.getUserLogOff(idBras,time,_type)
      // get list card
      val listCard = BrasService.getJsonBrasCard(idBras,time,_type)
      val heatCard = listCard.map(x=> x._1._2)
      val heatLinecard = listCard.map(x=> x._1._1)
    //logger.info("success 3")
      // get tableIndex kibana and opview
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
      val dateTime = DateTime.parse(time, formatter)
      val oldTime  = dateTime.minusMinutes(30).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))
      val brasOpKiba = Await.result(BrasService.opViewKibana(idBras,time,oldTime), Duration.Inf)
    //logger.info("success 4")
      val jsBras = Json.obj(
        "host" -> re,
        "sigLog" -> sigLog,
        "time" -> brasChart.map({ t => CommonService.formatUTC(t._1.toString)}),
        "logoff" -> brasChart.map({ t =>t._2}),
        "signin" -> brasChart.map({ t => t._3}),
        "users" -> brasChart.map({ t => t._4}),
        "heatCard" -> heatCard,
        "heatLinecard" -> heatLinecard,
        "dtaCard" -> listCard,
        "mapBras" -> brasOpKiba,
        "userLogoff" -> userLogoff
      )
      Ok(Json.toJson(jsBras))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  def getBrasJson(id: String) = Action { implicit request =>
    try{
      val lstBras = Await.result(BrasService.listBrasById(id), Duration.Inf)
      var mapBras = collection.mutable.Map[String, Seq[(String,String,String,String)]]()
      val arrOutlier = lstBras.map(x => (x._1->x._2)).toList.distinct
      val mapSigLog = new Array[String](arrOutlier.length)
      var num =0;
      for(outlier <- arrOutlier){
        // get  map num of signin and logoff
        val objBras = Await.result(BrasService.getNumLogSiginById(outlier._1,outlier._2), Duration.Inf)
        mapSigLog(num)= objBras(0)._1.toString +"/" +objBras(0)._2.toString
        num = num +1;
        // get kibana and opview
        val tm = outlier._2.substring(0,outlier._2.indexOf(".")+3)
        val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS")
        val dateTime = DateTime.parse(tm, formatter)
        val oldTime  = dateTime.minusMinutes(30).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS"))
        val brasKey = Await.result(BrasService.opViewKibana(outlier._1,dateTime.toString,oldTime), Duration.Inf)
        mapBras += (outlier._1+"/"+outlier._2-> brasKey)
      }
      val arrLine = lstBras.map(x => (x._1, x._2) -> x._3).groupBy(x => x._1).mapValues(x => x.map(y => y._2).mkString("|"))
      val arrCard = lstBras.map(x => (x._1, x._2, x._3) -> x._4).groupBy(x => x._1).mapValues(x => x.map(y => y._2).mkString("|"))
      val arrHost = lstBras.map(x => (x._1, x._2, x._3,x._4) -> x._5).groupBy(x => x._1).mapValues(x => x.map(y => y._2).mkString("|"))
      val jsBras = Json.obj(
        "bras" -> arrOutlier,
        "logSig" ->mapSigLog,
        "linecard" -> arrLine,
        "card" -> arrCard,
        "host" -> arrHost,
        "mapBras" -> mapBras
      )
      Ok(Json.toJson(jsBras))
    }
    catch{
      case e: Exception => Ok("error")
    }
  }

}