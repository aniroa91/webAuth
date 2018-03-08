package controllers

import javax.inject.Inject
import javax.inject.Singleton

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


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
case class BrasOutlier(bras: String,date: String)

@Singleton
class DeviceController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured{

  val form = Form(
    mapping(
      "bras" -> text,
      "date" -> text
    )(BrasOutlier.apply)(BrasOutlier.unapply)
  )

  def index =  withAuth { username => implicit request =>
    try {
      // get data for map chart
      val mapNoc = Await.result(BrasService.listNocOutlier, Duration.Inf)
      Ok(views.html.device.index(username,mapNoc))
    }
    catch{
      case e: Exception => Ok(views.html.device.index(username,null))
    }
  }

  def search =  withAuth { username => implicit request =>
    val formValidationResult = form.bindFromRequest
    //try {
      if (!formValidationResult.hasErrors) {
        var day = formValidationResult.get.date.trim()
        val brasId = formValidationResult.get.bras.trim()
        if (day == null || day == ""){
          day = CommonService.getCurrentDay()+"/"+CommonService.getCurrentDay()
        }

        // OVERVIEW
        val siginLogoff = Await.result(BrasService.getSigLogResponse(brasId,day), Duration.Inf)
        val noOutlier = Await.result(BrasService.getNoOutlierResponse(brasId,day), Duration.Inf)
        val hourlyStr = CommonService.RANK_HOURLY
        val opviewBytime = hourlyStr.split(",").map(x=> x-> Await.result(BrasService.getOpviewBytimeResponse(brasId,day,x.toInt), Duration.Inf).map(x=> x._2)).map(x=> x._2.sum)
        val kibanaBytime = hourlyStr.split(",").map(x=> x-> Await.result(BrasService.getKibanaBytimeResponse(brasId,day,x.toInt), Duration.Inf).map(x=> x._2)).map(x=> x._2.sum)

        val siginBytime = hourlyStr.split(",").map(x=> x-> Await.result(BrasService.getSigLogBytimeResponse(brasId,day,x.toInt), Duration.Inf).map(x=> x._2)).map(x=> x._2.sum)
        val logoffBytime = hourlyStr.split(",").map(x=> x-> Await.result(BrasService.getSigLogBytimeResponse(brasId,day,x.toInt), Duration.Inf).map(x=> x._3)).map(x=> x._2.sum)

        // INF ERROR
        val infErrorBytime = hourlyStr.split(",").map(x=> x-> Await.result(BrasService.getInfErrorBytimeResponse(brasId,day,x.toInt), Duration.Inf).map(x=> x._2)).map(x=> x._1->x._2.sum)
        //val infErrorBytime = null
        // INF HOST
        val infHostBytime = Await.result(BrasService.getInfhostResponse(brasId,day), Duration.Inf).map(x=> x._1->(x._2->x._3))
       // val infHostBytime = null
        // INF MODULE
        val infModuleBytime = Await.result(BrasService.getInfModuleResponse(brasId,day), Duration.Inf)
        //val infModuleBytime = null
        // OPVIEW TREE MAP
        val opServiceName = Await.result(BrasService.getOpsviewServiceSttResponse(brasId,day), Duration.Inf)
       //val opServiceName = null
        // OPVIEW SERVICE BY STATUS
        val opServByStt = Await.result(BrasService.getOpServByStatusResponse(brasId,day), Duration.Inf)
        val servName = opServByStt.map(x=> x._1).distinct
        val servStatus = opServByStt.map(x=> x._2).distinct
        // LINE CARD, CARD, PORT
        val linecardhost = Await.result(BrasService.getLinecardhostResponse(brasId,day), Duration.Inf)

        Ok(views.html.device.search(form,username,BrasResponse(BrasInfor(noOutlier,siginLogoff),KibanaOpviewByTime(kibanaBytime,opviewBytime),SigLogByTime(siginBytime,logoffBytime),
          infErrorBytime,infHostBytime,infModuleBytime,opServiceName,ServiceNameStatus(servName,servStatus,opServByStt),linecardhost), day,brasId))
      }
      else
        Ok(views.html.device.search(form,username,null,CommonService.getCurrentDay()+"/"+CommonService.getCurrentDay(),null))
    /*}
    catch{
      case e: Exception => Ok(views.html.device.search(form,username,null,CommonService.getCurrentDay(),null))
    }*/
  }

  def getHostJson(id: String) = Action { implicit request =>
    try{
      val rsHost = Await.result(BrasService.getHostBras(id), Duration.Inf)
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
      val brasChart = Await.result(BrasService.getJsonBrasChart(idBras,time),Duration.Inf)
      val listCard = Await.result(BrasService.getBrasCard(idBras,time,"",""),Duration.Inf)
      val sigLog = brasChart.map({ t => (t._1.substring(0,t._1.indexOf(".")+2),t._2,t._3)}).filter(t => t._1 == time.substring(0,time.indexOf(".")+2))
      //System.out.println(s"x $sigLog")
      val jsBras = Json.obj(
        "host" -> re,
        "sigLog" -> sigLog,
        "time" -> brasChart.map({ t => t._1.toString}),
        "logoff" -> brasChart.map({ t =>t._2}),
        "signin" -> brasChart.map({ t => t._3}),
        "users" -> brasChart.map({ t => t._4}),
        "heatCard" -> listCard.map { t => t._3},
        "heatLinecard" -> listCard.map { t => t._2},
        "dtaCard" -> listCard
      )
      Ok(Json.toJson(jsBras))
    }
    catch{
      case e: Exception => Ok("error")
    }
  }

  def getBrasJson(id: String) = Action { implicit request =>
    try{
      val lstBras = Await.result(BrasService.listBrasById(id), Duration.Inf)
      var mapBras = collection.mutable.Map[String, Seq[(String,String,String,String)]]()
      val arrOutlier = lstBras.map(x => (x._1->x._2)).toList.distinct
      var mapSigLog = new Array[String](arrOutlier.length)
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