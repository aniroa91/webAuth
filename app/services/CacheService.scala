package services

import com.ftel.bigdata.dns.parameters.Label

import model.DashboardResponse
import model.ProfileResponse
import model.RankResponse
import model.ReportResponse
import services.domain.CommonService
import services.domain.DashboardService
import services.domain.ProfileService
import services.domain.ReportService
import model.ClientResponse
import services.domain.ClientService
import com.ftel.bigdata.utils.StringUtil
import scala.concurrent.Future
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


object CacheService {
  private var latest: String = null
  private var dashboardCache: (String, DashboardResponse) = "" -> null
  private var rankCache: (String, RankResponse) = "" -> null
  private val reportCache = scala.collection.mutable.Map[String, ReportResponse]()
  private val domainCache = scala.collection.mutable.Map[String, ProfileResponse]()
  private val clientCache = scala.collection.mutable.Map[String, ClientResponse]()

  def refresh() {
    latest = null
    dashboardCache = "" -> null
    rankCache = "" -> null
    reportCache.clear()
    domainCache.clear()
    clientCache.clear()
  }
  
  def getDaskboard(): (DashboardResponse, Long) = {
    val time0 = System.currentTimeMillis()
    val day = CommonService.getLatestDay()
    val res = if (dashboardCache._1 == day) {
      dashboardCache._2
    } else {
      val response = DashboardService.get(day)
      dashboardCache = day -> response
      response
    }
    val time1 = System.currentTimeMillis()
    val elapse = time1 - time0
    res -> elapse
  }

  def getRank(): (RankResponse, Long) = {
    val time0 = System.currentTimeMillis()
    val day = CommonService.getLatestDay()
    val res = if (rankCache._1 == day) {
      rankCache._2
    } else {
      val all = CommonService.getTopRank(1, day)
      val black = CommonService.getTopByNumOfQuery(day, Label.Black)
      val white = CommonService.getTopByNumOfQuery(day, Label.White)
      val unknow = CommonService.getTopByNumOfQuery(day, Label.Unknow)
      
      val thread = new Thread {
        override def run {
            println("Start Download")
            all.map(x => x.name).map(x => CommonService.getLogo(x, true))
            println("Finish Download")
        }
      }
      thread.start()

      val last7Days = CommonService.getPreviousDay(day, 7)
      val last30Days = CommonService.getPreviousDay(day, 30)
      
      val topLast7Day = CommonService.getTopByNumOfQueryWithRange(last7Days, day)
      val topLast30Day = CommonService.getTopByNumOfQueryWithRange(last30Days, day)
      
      val response = RankResponse(all, black, white, unknow, topLast7Day, topLast30Day)
      rankCache = day -> response
      response
    }
    val time1 = System.currentTimeMillis()
    val elapse = time1 - time0
    res -> elapse
  }

  def getReport(day: String): (ReportResponse, Long) = {
    val time0 = System.currentTimeMillis()
    val res = if (reportCache.contains(day)) {
      reportCache.get(day).get
    } else {
      val response = ReportService.get(day)
      reportCache.put(day, response)
      response
    }
    val time1 = System.currentTimeMillis()
    val elapse = time1 - time0
    res -> elapse
  }

  def getDomain(domain: String): (ProfileResponse, Long) = {
    val time0 = System.currentTimeMillis()
    val day = CommonService.getLatestDay()
    if (latest != day) {
      domainCache.clear()
      latest = day
    }
    val res = if (domainCache.contains(domain)) {
      domainCache.get(domain).get
    } else {
      val response = ProfileService.get(domain)
      domainCache.put(domain, response)
      response
    }
    val time1 = System.currentTimeMillis()
    val elapse = time1 - time0
    res -> elapse
  }
  
  def getClient(ip: String): (ClientResponse, Long) = {
    val time0 = System.currentTimeMillis()
    val day = CommonService.getLatestDay()
    if (latest != day) {
      clientCache.clear()
      latest = day
    }
    val res = if (clientCache.contains(ip)) {
      clientCache.get(ip).get
    } else {
      val response = ClientService.get(ip, latest)
      clientCache.put(ip, response)
      response
    }
    val time1 = System.currentTimeMillis()
    val elapse = time1 - time0
    res -> elapse
  }
}