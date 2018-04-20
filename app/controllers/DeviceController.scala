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
import service.BrasService
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

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
case class BrasOutlier(bras: String,date: String)

@Singleton
class DeviceController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured{

  val logger: Logger = Logger(this.getClass())
  val form = Form(
    mapping(
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
      Ok(views.html.device.dashboard(username,mapBrasOutlier))
    }
    catch{
      case e: Exception => Ok(views.html.device.dashboard(username,null))
    }
  }

  def realtimeBras() =  withAuth { username => implicit request =>
    val bras = Await.result(BrasService.getBrasOutlierCurrent(CommonService.getCurrentDay()),Duration.Inf)
    val jsBras = Json.obj(
      "bras" -> bras
    )
    Ok(Json.toJson(jsBras))
  }

  def search =  withAuth { username => implicit request =>
    val formValidationResult = form.bindFromRequest
    try {
      if (!formValidationResult.hasErrors) {
        val time = formValidationResult.get.date.trim()
        var day = ""
        val brasId = formValidationResult.get.bras.trim()
        if (time == null || time == ""){
          day = CommonService.getCurrentDay()+"/"+CommonService.getCurrentDay()
        }
        else{ day =  time}

        val hourlyStr = CommonService.RANK_HOURLY
        var numOutlier = 0
        var sigin = 0
        var logoff = 0
        val fromDay = day.split("/")(0)
        var nextDay = CommonService.getNextDay(day.split("/")(1))
        var siginBytime = new Array[Int](0)
        var logoffBytime = new Array[Int](0)
        val timeStart= System.currentTimeMillis()
        /* GET ES CURRENT */
        if(day.split("/")(1).equals(CommonService.getCurrentDay())){
          nextDay = day.split("/")(1)
          // number sigin and logoff
          val sigLog = BrasService.getSigLogCurrent(brasId,CommonService.getCurrentDay())
          sigin = sigLog._1
          logoff = sigLog._2
          // number outlier
          numOutlier = BrasService.getNoOutlierCurrent(brasId,CommonService.getCurrentDay())
        }
        val t0 = System.currentTimeMillis()
        /* GET HISTORY DATA */
        if(!fromDay.equals(CommonService.getCurrentDay())) {
          // number sigin and logoff
          val siginLogoff = Await.result(BrasService.getSigLogResponse(brasId, fromDay, nextDay), Duration.Inf)
          sigin = if (siginLogoff.length > 0) {
            siginLogoff(0)._1 + sigin
          } else sigin
          logoff = if (siginLogoff.length > 0) {
            siginLogoff(0)._2 + logoff
          } else logoff
          logger.info("t00: "+(System.currentTimeMillis() - t0))
          val t01 = System.currentTimeMillis()
          // number outlier
          val noOutlier = Await.result(BrasService.getNoOutlierResponse(brasId, day), Duration.Inf)
          numOutlier += noOutlier.sum.toInt
          logger.info("t01: "+(System.currentTimeMillis() - t01))
         /* val t02 = System.currentTimeMillis()
           LINECARD, CARD, PORT
          val seqLinecard = Await.result(BrasService.getLinecardhostResponse(brasId,day), Duration.Inf)
          val linecardhistory = seqLinecard.map(x=>x._1-> (x._2+"-"+x._3)).toArray
          linecardhistory.foreach(println)
          linecardhost = (linecardhost++linecardhistory).groupBy(_._1).map{case (k,v) => k -> v.map(x=> x._2.toString).mkString("_")}.toArray
          println("t02: "+(System.currentTimeMillis() - t02))*/

         /* val t03 = System.currentTimeMillis()
          // SIGNIN LOGOFF BY TIME
          val rsSigtime = hourlyStr.split(",").map(x=> x-> Await.result(BrasService.getSigLogBytimeResponse(brasId,day,x.toInt), Duration.Inf).map(x=> x._2)).map(x=> x._2.sum)
          println("====")
          rsSigtime.foreach(println)
          siginBytime = siginBytime.zipAll(rsSigtime,0,0).map { case (x, y) => x + y }
          println("====")
          siginBytime.foreach(println)
          val rsLogtime = hourlyStr.split(",").map(x=> x-> Await.result(BrasService.getSigLogBytimeResponse(brasId,day,x.toInt), Duration.Inf).map(x=> x._3)).map(x=> x._2.sum)
          logoffBytime = logoffBytime.zipAll(rsLogtime,0,0).map { case (x, y) => x + y }
          println("t03: "+(System.currentTimeMillis() - t03))*/

        }
        // SIGNIN LOGOFF BY TIME
        val t03 = System.currentTimeMillis()
        val rsLogsigBytime = BrasService.getSigLogBytimeCurrent(brasId,day)
        siginBytime = rsLogsigBytime.sumSig
        siginBytime.foreach(println)
        logoffBytime = rsLogsigBytime.sumLog
        logger.info("tSigLogBytime: "+(System.currentTimeMillis() - t03))

        val t02= System.currentTimeMillis()
        // line-card-port
        val linecardhost = BrasService.getLinecardhostCurrent(brasId,day)
        logger.info("tLinecardhost: "+(System.currentTimeMillis() - t02))

        val t1 = System.currentTimeMillis()
        // Nerror (kibana & opview) By Time
        val arrOpsview = Await.result(BrasService.getOpviewBytimeResponse(brasId,day,0), Duration.Inf).toArray
        val opviewBytime = (0 until 24).map(x => x -> CommonService.getIntValueByKey(arrOpsview,x)).toArray
        logger.info("tOpsviewBytime: "+(System.currentTimeMillis() - t1))
        //val arrKibana = Await.result(BrasService.getKibanaBytimeResponse(brasId,day,0), Duration.Inf).toArray
        val t20= System.currentTimeMillis()
        val arrKibana = BrasService.getKibanaBytimeES(brasId,day)
        val kibanaBytime = (0 until 24).map(x => x -> CommonService.getIntValueByKey(arrKibana,x)).toArray
        logger.info("tKibanaBytime: "+(System.currentTimeMillis() - t20))
        // INF ERROR
        val t3= System.currentTimeMillis()
        val arrInferror = Await.result(BrasService.getInfErrorBytimeResponse(brasId,day,0), Duration.Inf).toArray
        val infErrorBytime = (0 until 24).map(x => x -> CommonService.getIntValueByKey(arrInferror,x)).toArray
        //val infErrorBytime = null
        // INF HOST
        val infHostBytime = Await.result(BrasService.getInfhostResponse(brasId,day), Duration.Inf).map(x=> x._1->(x._2->x._3))
        logger.info("tInfHostBytime: "+(System.currentTimeMillis() - t3))
        // val infHostBytime = null
        val t4= System.currentTimeMillis()
        // INF MODULE
        val infModuleBytime = Await.result(BrasService.getInfModuleResponse(brasId,day), Duration.Inf)
        logger.info("tInfModuleBytime: "+(System.currentTimeMillis() - t4))
        //val infModuleBytime = null
        val t5= System.currentTimeMillis()
        // OPVIEW TREE MAP
        val opServiceName = Await.result(BrasService.getOpsviewServiceSttResponse(brasId,day), Duration.Inf)
       //val opServiceName = null
        // OPVIEW SERVICE BY STATUS
        val opServByStt = Await.result(BrasService.getOpServByStatusResponse(brasId,day), Duration.Inf)
        val servName = opServByStt.map(x=> x._1).distinct
        val servStatus = opServByStt.map(x=> x._2).distinct
        logger.info("tOpsviewTreeMap: "+(System.currentTimeMillis() - t5))

        //val t6= System.currentTimeMillis()
        //val kibanaSeverity = Await.result(BrasService.getErrorSeverityResponse(brasId,day), Duration.Inf)
       // println("t6: "+(System.currentTimeMillis() - t6))
        val t60= System.currentTimeMillis()
        // KIBANA Severity
        val kibanaSeverity = BrasService.getErrorSeverityES(brasId,day)

        logger.info("tKibanaSeverity: "+(System.currentTimeMillis() - t60))
        val t7 = System.currentTimeMillis()
        // KIBANA Error type
        //val kibanaErrorType = Await.result(BrasService.getErrorTypeResponse(brasId,day), Duration.Inf)
        val kibanaErrorType = BrasService.getErrorTypeES(brasId,day)
        logger.info("tKibanaErrorType: "+(System.currentTimeMillis() - t7))
        // KIBANA Facility
        val t8 = System.currentTimeMillis()
        //val kibanaFacility = Await.result(BrasService.getFacilityResponse(brasId,day), Duration.Inf)
        val kibanaFacility = BrasService.getFacilityES(brasId,day)
        logger.info("tKibanaFacility: "+(System.currentTimeMillis() - t8))
        // KIBANA DDos
        val t9 = System.currentTimeMillis()
       // val kibanaDdos = Await.result(BrasService.getDdosResponse(brasId,day), Duration.Inf)
        val kibanaDdos = BrasService.getDdosES(brasId,day)
        logger.info("tKibanaDdos: "+(System.currentTimeMillis() - t9))
        // KIBANA Severity value
        val t10 = System.currentTimeMillis()
        //val severityValue = Await.result(BrasService.getSeveValueResponse(brasId,day), Duration.Inf)
        val severityValue = BrasService.getSeveValueES(brasId,day)
        logger.info("tSeverityValue: "+(System.currentTimeMillis() - t10))
        // SIGNIN LOGOFF BY HOST
        val siglogByhost =  BrasService.getSigLogByHost(brasId,day)
        logger.info("tSiglogByhost: "+(System.currentTimeMillis() - t10))

        logger.info("timeAll: "+(System.currentTimeMillis() - timeStart))
        Ok(views.html.device.search(form,username,BrasResponse(BrasInfor(numOutlier,(sigin,logoff)),KibanaOpviewByTime(kibanaBytime,opviewBytime),SigLogByTime(siginBytime,logoffBytime),
          infErrorBytime,infHostBytime,infModuleBytime,opServiceName,ServiceNameStatus(servName,servStatus,opServByStt),linecardhost,KibanaOverview(kibanaSeverity,kibanaErrorType,kibanaFacility,kibanaDdos,severityValue),siglogByhost), day,brasId))
      }
      else
        Ok(views.html.device.search(form,username,null,CommonService.getCurrentDay()+"/"+CommonService.getCurrentDay(),null))
    }
    catch{
      case e: Exception => Ok(views.html.device.search(form,username,null,CommonService.getCurrentDay(),null))
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
      // get table kibana and opview
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