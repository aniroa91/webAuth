package controllers

import javax.inject.Inject
import javax.inject.Singleton

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.text
import service.{BrasService, HostService}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import play.api.libs.json.Json
import services.domain.CommonService
import play.api.Logger
import device.utils.LocationUtils
import model.device._

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
case class BrasOutlier(csrfToken: String,_typeS: String,bras: String,date: String)

@Singleton
class SearchController @Inject()(cc: MessagesControllerComponents) extends MessagesAbstractController(cc) with Secured{

  val logger: Logger = Logger(this.getClass())
  val form = Form(
    mapping(
      "csrfToken" -> text,
      "_typeS" -> text,
      "bras" -> text,
      "date" -> text
    )(BrasOutlier.apply)(BrasOutlier.unapply)
  )

  // This will be the action that handles our form post
  def getFormBras = withAuth {username => implicit request: Request[AnyContent] =>

    val errorFunction = { formWithErrors: Form[controllers.BrasOutlier] =>
      println("error")
      Ok(device.views.html.search(form,username,"",null,null,CommonService.getCurrentDay()+"/"+CommonService.getCurrentDay(),null,"B",routes.SearchController.search))
    }

    val successFunction = { data: controllers.BrasOutlier =>
      println("done")
      Redirect(routes.SearchController.search).flashing("bras" -> data.bras.toUpperCase, "_typeS" -> data._typeS, "date" -> data.date)
    }

    val formValidationResult = form.bindFromRequest
    formValidationResult.fold(errorFunction, successFunction)
  }

  def fwdRequest(bras: String) = withAuth {username => implicit request: Request[AnyContent] =>
    Redirect(routes.SearchController.search).flashing( "bras" -> bras.toUpperCase, "_typeS" -> "B", "date" -> "")
  }

  def suggest(ct: String, _type: String) = withAuth {username => implicit request: Request[AnyContent] =>
    val bras = BrasService.getSuggestions(ct.toUpperCase, _type)
    val rs = Json.obj(
      "data" -> bras
    )
    Ok(Json.toJson(rs))
  }

  def callAjaxByTab() = withAuth {username => implicit request: Request[AnyContent] =>
    val province = if(request.session.get("verifiedLocation").get.equals("1")){
      request.session.get("location").get.split(",").map(x=> LocationUtils.getCodeProvincebyName(x)).mkString("|")
    } else ""
    try {
      val t0 = System.currentTimeMillis()
      val day = request.body.asFormUrlEncoded.get("date").head
      val brasId = request.body.asFormUrlEncoded.get("bras").head
      val tabName = request.body.asFormUrlEncoded.get("tabName").head
      if (!province.equals("") && !brasId.substring(0, 3).equals(province)) {
        logger.info(s"Page Search-401 Permission for user: when search bras:$brasId")
        // author
        Ok("Permission")
      }
      else {
        tabName match {
          case "tab_opsview" => {
            //  Nerror By Status
            val opServiceName = Await.result(BrasService.getOpsviewServiceSttResponse(brasId, day), Duration.Inf)
            //  Nerror By Service And Status
            val opServByStt = BrasService.getOpServByStatusResponse(brasId, day)
            val servName = opServByStt.map(x => x._1).distinct
            val servStatus = opServByStt.map(x => x._2).distinct
            val objServByStt = Json.obj(
              "opServByStt" -> opServByStt,
              "servName" -> servName,
              "servStatus" -> servStatus
            )
            val opsview = Json.obj(
              "objServiceName" -> opServiceName,
              "objServByStt" -> objServByStt
            )
            val rs = Json.obj(
              "opsview" -> opsview
            )
            logger.info(s"Page: Search Bras($brasId)- Tab $tabName - User:  - Time Query:"+(System.currentTimeMillis() -t0))
            Ok(Json.toJson(rs))
          }
          case "tab_kibana" => {
            //  Nerror By Severity
            val kibanaSeverity = BrasService.getErrorSeverityES(brasId, day)
            //  Nerror By Error Type
            val kibanaErrorType = BrasService.getErrorTypeES(brasId, day)
            val objErrorType = Json.obj(
              "cate" -> kibanaErrorType.map(x => x._1),
              "data" -> kibanaErrorType.map(x => x._2)
            )
            //  Nerror By Facility
            val kibanaFacility = BrasService.getFacilityES(brasId, day)
            //  Ntime By Ddos
            val kibanaDdos = BrasService.getDdosES(brasId, day)
            val objDdos = Json.obj(
              "cate" -> kibanaDdos.map(x => x._1),
              "data" -> kibanaDdos.map(x => x._2)
            )
            //  Nerror By Error And Severity
            val severityValue = BrasService.getSeveValueES(brasId, day)
            val kibana = Json.obj(
              "severity" -> kibanaSeverity,
              "errorType" -> objErrorType,
              "facility" -> kibanaFacility,
              "ddos" -> objDdos,
              "severityValue" -> severityValue
            )
            val rs = Json.obj(
              "kibana" -> kibana
            )
            logger.info(s"Page: Search Bras($brasId)- Tab $tabName - User:$username  - Time Query:"+(System.currentTimeMillis() -t0))
            Ok(Json.toJson(rs))
          }
          case "tab_inf" => {
            //  Nerror By Time
            val infErrorBytime = BrasService.getInfErrorBytimeResponse(brasId, day, 0)
            //  Signin and Logoff By Host
            val siglogByhost = BrasService.getSigLogByHost(brasId, day)
            val objSiglogHost = Json.obj(
              "cate" -> siglogByhost.arrCates,
              "signin" -> siglogByhost.sumSig,
              "logoff" -> siglogByhost.sumLog
            )
            //  No. Of Alert By Switch And Status
            val serviceByTime = Await.result(BrasService.getServiceNameStt(brasId.substring(0, brasId.indexOf("-")), day), Duration.Inf).map(x => (x._1, x._2.toLowerCase(), x._3))
            val objSerByTime = Json.obj(
              "cate" -> serviceByTime.map(x => x._1).distinct,
              "key" -> serviceByTime.map(x => x._2).distinct,
              "data" -> serviceByTime
            )
            //  No. Of Alert By Switch And Monitoring Service
            val sankeyService = Await.result(BrasService.getSankeyService(brasId.substring(0, brasId.indexOf("-")), day), Duration.Inf)
            val objSankey = Json.obj(
              "size" -> sankeyService.map(x => x._1).distinct.length,
              "data" -> sankeyService
            )
            //  Nerror Of Power Devices By Service And Status
            val devServByStt = Await.result(BrasService.getDeviceServStatus(brasId.substring(0, brasId.indexOf("-")), day), Duration.Inf)
            val devName = devServByStt.map(x => x._2).distinct
            val devStatus = devServByStt.map(x => x._3).distinct
            val devBras = devServByStt.map(x => x._1).distinct
            val objDevServStt = Json.obj(
              "arrName" -> devName,
              "arrStatus" -> devStatus,
              "arrBras" -> devBras,
              "data" -> devServByStt
            )
            //  Nerror By Host, Module And Error
            val infModuleHost = BrasService.getInfModuleResponse(brasId, day)
            val objInfMudule = Json.obj(
              "sumSf" -> infModuleHost.map(x => x._3).sum,
              "sumLofi" -> infModuleHost.map(x => x._4).sum,
              "sumTotal" -> infModuleHost.map(x => x._5).sum,
              "data" -> infModuleHost
            )
            val inf = Json.obj(
              "infErrorBytime" -> infErrorBytime,
              "objSankey" -> objSankey,
              "objSerByTime" -> objSerByTime,
              "objSiglogHost" -> objSiglogHost,
              "objDevServStt" -> objDevServStt,
              "objInfMudule" -> objInfMudule
            )
            val rs = Json.obj(
              "inf" -> inf
            )
            logger.info(s"Page: Search Bras($brasId)- Tab $tabName - User:  - Time Query:"+(System.currentTimeMillis() -t0))
            Ok(Json.toJson(rs))
          }
        }
      }
    }
    catch {
      case e: Exception => Ok("Error")
    }
  }

  def search =  withAuth {username => implicit request: Request[AnyContent] =>
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

            logger.info(s"Page: Search Host($brasId) - User: - Time Query:"+(System.currentTimeMillis() -t00))
            Ok(device.views.html.search(form,username,province,HostResponse(ticketOutlier,noOutlierModule,errHost,errorHourly,sigLogModule,arrSiglogModuleIndex,suyhaoModule,sigLogByHourly,
              splitterByHost,ErrModuleIndex(arrModule,arrIndex,errModuleIndex),sfContract),null,day,brasId,"I",routes.SearchController.search))
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
              // number outlier
              val noOutlier = Await.result(BrasService.getNoOutlierResponse(brasId, day), Duration.Inf)
              numOutlier += noOutlier.sum.toInt
              logger.info("tNumber: " + (System.currentTimeMillis() - timeStart))
            }
            // number outlier of host
            val noOutlierByhost = HostService.getNoOutlierInfByBras(brasId,day)
            // number sigin and logoff
            val t0 = System.currentTimeMillis()
            val sigLog = BrasService.getSigLogCurrent(brasId, day)
            val sigLogClients = BrasService.getSiglogClients(brasId,day)
            logger.info("tSigLog: " + (System.currentTimeMillis() - t0))
            //  Signin & Logoff By Time
            val t1 = System.currentTimeMillis()
            val rsLogsigBytime = BrasService.getSigLogBytimeCurrent(brasId, day)
            siginBytime = rsLogsigBytime.sumSig
            logoffBytime = rsLogsigBytime.sumLog
            logger.info("tSigLogBytime: " + (System.currentTimeMillis() - t1))
            //  Signin & Logoff By Linecard-card-port
            val t2 = System.currentTimeMillis()
            val linecardhost = BrasService.getLinecardhostCurrent(brasId, day)
            logger.info("tLinecardhost: " + (System.currentTimeMillis() - t2))
            //  Nerror (kibana & opview) By Time
            val t3 = System.currentTimeMillis()
            val arrOpsview = BrasService.getOpviewBytimeResponse(brasId, day, 0)
            val opviewBytime = (0 until 24).map(x => x -> CommonService.getIntValueByKey(arrOpsview, x)).toArray
            logger.info("tOpsviewBytime: " + (System.currentTimeMillis() - t3))
            val t4 = System.currentTimeMillis()
            val arrKibana = BrasService.getKibanaBytimeES(brasId, day).groupBy(_._1).mapValues(_.map(_._2).sum).toArray
            val kibanaBytime = (0 until 24).map(x => x -> CommonService.getIntValueByKey(arrKibana, x)).toArray
            logger.info("tKibanaBytime: " + (System.currentTimeMillis() - t4))
            // Ticket
            val t5 = System.currentTimeMillis()
            val ticketOutlier = Await.result(BrasService.getTicketOutlierByBrasId(brasId, day), Duration.Inf)
            logger.info("tTicket: " + (System.currentTimeMillis() - t5))

            logger.info(s"Page: Search Bras($brasId) - User:  - Time Query:"+(System.currentTimeMillis() -timeStart))
            Ok(device.views.html.search(form, username, province,null ,BrasResponse(ticketOutlier, BrasInfor(noOutlierByhost, numOutlier, (sigLog._1, sigLog._2),(sigLogClients._1,sigLogClients._2)),
              KibanaOpviewByTime(kibanaBytime, opviewBytime), SigLogByTime(siginBytime, logoffBytime), linecardhost), day, brasId,_typeS,routes.SearchController.search))
          }
        }
      }
      else{
        logger.info("Empty data")
        Ok(device.views.html.search(form,username,province,null,null,CommonService.getCurrentDay()+"/"+CommonService.getCurrentDay(),null,if(province.equals("")) "B" else "I",routes.SearchController.search))
      }
    }
    catch{
      case e: Exception => Ok(device.views.html.search(form,username,province,null,null,CommonService.getCurrentDay()+"/"+CommonService.getCurrentDay(),null,
        if(province.equals("")) "B" else "I",routes.SearchController.search))
    }
  }
}