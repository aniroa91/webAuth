package profile.controllers.internet

import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.text
import javax.inject.Inject
import javax.inject.Singleton
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import controllers.SearchContract
import controllers.Secured
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.ElasticsearchClientUri
import services.Configure
import views.html.ace.client
import com.sksamuel.elastic4s.http.ElasticDsl._
import profile.services.internet.HistoryService
import com.ftel.bigdata.utils.StringUtil
import profile.services.internet.response.History
import profile.services.internet.response.Hourly


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HistoryController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured{

  
  val client = Configure.client
  val form = Form(
    mapping(
      "ct" -> text)(SearchContract.apply)(SearchContract.unapply))

  def index(date: String) =  withAuth { username => implicit request =>
    //println("DATE: " + date)
    
//    try {
      val formValidationResult = form.bindFromRequest
      if (!formValidationResult.hasErrors) {
        val contract = formValidationResult.get.q.trim()
        if (StringUtil.isNullOrEmpty(contract)) {
          println(contract)
          //Ok(views.html.profile.internet.history.index(form, username, HistoryService.get("day", date), date, "day"))
          //Ok(views.html.profile.internet.history.index(form, username, HistoryService.get("week", "2018-02-08"), date, "week"))
          Ok(views.html.profile.internet.history.index(form, username, HistoryService.getAll("month", "2018-02-01"), date, "month"))
        } else {
          Ok(views.html.profile.internet.history.indexContract(form, username, HistoryService.getContract("day", date, contract.toLowerCase()), contract, "day"))
          //Ok(views.html.profile.internet.history.indexContract(form, username, HistoryService.getContract("week", "2018-02-08", contract.toLowerCase()), contract, "week"))
          //Ok(views.html.profile.internet.history.indexContract(form, username, HistoryService.getContract("month", "2018-02-01", contract.toLowerCase()), contract, "month"))
        }
      } else {
        Ok(views.html.profile.internet.history.index(form, username, HistoryService.getAll("month", "2018-02-01"), "2018-02", "month"))
      }
//    } catch {
//      case e: Exception => Ok("Message: " + e.getMessage)
//    }
  }
  
//  private def getResponse(): InternetReponse = {
//    
//    HistoryService.get(date)
//    //HistoryService.get("bpfdl-150820-434")
////    getResponseTest()
//  }
  
  
  
//  private def getResponseTest(): History = {
//    val numberOfContract = 2089675
//    val numberOfSession = 9581974
//    val contractHourly = Array(
//        0 -> 10L,
//        1 -> 40L,
//        2 -> 30L,
//        3 -> 80L,
//        4 -> 60L,
//        5 -> 90L,
//        6 -> 30L,
//        7 -> 80L,
//        8 -> 100L,
//        9 -> 120L,
//        10 -> 150L,
//        11 -> 400L,
//        12 -> 800L,
//        13 -> 500L,
//        14 -> 200L,
//        15 -> 100L,
//        16 -> 250L,
//        17 -> 360L,
//        18 -> 240L,
//        19 -> 800L,
//        20 -> 1120L,
//        21 -> 1200L,
//        22 -> 900L,
//        23 -> 200L).map(x => x._1.toLong -> x._2)
//        
//    val sessionHourly = contractHourly.map(x => x._1.toInt -> x._2)
//    val downloadHourly = contractHourly.map(x => x._1.toInt -> x._2)
//    val uploadHourly = contractHourly.map(x => x._1.toInt -> x._2)
//    val status = Array(
//        "ACTLIVE" -> 80,
//        "ACTLOFF" -> 20)
//    val topContract = Array(
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L))
//    val topProvince = Array(
//        ("A", 100L, 200L, 100L, 50L),
//        ("B", 100L, 200L, 100L, 50L),
//        ("C", 100L, 200L, 100L, 50L),
//        ("D", 100L, 200L, 100L, 50L),
//        ("E", 100L, 200L, 100L, 50L),
//        ("F", 100L, 200L, 100L, 50L),
//        ("G", 100L, 200L, 100L, 50L),
//        ("E", 100L, 200L, 100L, 50L),
//        ("F", 100L, 200L, 100L, 50L),
//        ("G", 100L, 200L, 100L, 50L))
//        
//    val topRegion = Array(
//        ("A", 100L, 200L, 100L, 50L),
//        ("B", 100L, 200L, 100L, 50L),
//        ("C", 100L, 200L, 100L, 50L),
//        ("D", 100L, 200L, 100L, 50L),
//        ("E", 100L, 200L, 100L, 50L),
//        ("F", 100L, 200L, 100L, 50L),
//        ("G", 100L, 200L, 100L, 50L))
//        
//    History(
//        "contract",
//        numberOfContract,
//        numberOfSession,
//        null,
//        null,
//        null,
//        status,
//        topContract,
//        topProvince,
//        topRegion)
//  }
}



