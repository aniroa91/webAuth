package controllers

import play.api.mvc._
import controllers.Secured
import javax.inject.Inject
import javax.inject.Singleton

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.Logger
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.BrasService

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import play.api.libs.json.Json
import services.domain.CommonService


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class BrasController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured{
  val logger: Logger = Logger(this.getClass())

  // page NOC
  def dashboard =  withAuth { username => implicit request =>
    try {
      val t0 = System.currentTimeMillis()
      val mapBrasOutlier = Await.result(BrasService.getBrasOutlierCurrent(CommonService.getCurrentDay()),Duration.Inf)
      logger.info(s"Page: NOC - User: ${username} - Time Query:"+(System.currentTimeMillis() -t0))
      Ok(device.views.html.noc(username,mapBrasOutlier))
    }
    catch{
      case e: Exception => Ok(device.views.html.noc(username,null))
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

  def getHostJson(id: String) = withAuth {username => implicit request =>
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
      val nameMetric = if(numLog>numSig) "Logged Off" else "Signed In"
      val metrics = Json.obj(
        "signin" -> signinUnique,
        "label"  -> s"Users $nameMetric",
        "error"  -> metricErr,
        "users"  -> users,
        "perct"  -> perct
      )
      logger.info("tMetrics: " + (System.currentTimeMillis() - t5))

      val t6 = System.currentTimeMillis()
      val devSwt = Await.result(BrasService.getDeviceSwitch(idBras.substring(0, idBras.indexOf("-"))+"%", time, oldTime), Duration.Inf)
      logger.info("TDevice type: " + (System.currentTimeMillis() - t6))

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
        "userLogoff"   -> userLogoff,
        "devSwitch"    -> devSwt
      )
      logger.info(s"Page: NOC(Click) - User: ${username} - Time Query:"+(System.currentTimeMillis() -t0))

      Ok(Json.toJson(jsBras))
    }
    catch{
      case e: Exception => Ok("Error")
    }
  }

  def confirmLabel(id: String,time: String) = withAuth {username => implicit request =>
    try{
      val res =  Await.result(BrasService.confirmLabel(id,time), Duration.Inf)
      Ok(Json.toJson(res))
    }
    catch{
      case e: Exception => Ok("error")
    }
  }

  def rejectLabel(id: String,time: String) = withAuth {username => implicit request =>
    try{
      val res =  Await.result(BrasService.rejectLabel(id,time), Duration.Inf)
      Ok(Json.toJson(res))
    }
    catch{
      case e: Exception => Ok("error")
    }
  }

}



