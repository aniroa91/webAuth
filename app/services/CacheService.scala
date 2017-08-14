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

object CacheService {
  private var latest: String = null
  private var dashboardCache: (String, DashboardResponse) = "" -> null
  private var rankCache: (String, RankResponse) = "" -> null
  private val reportCache = scala.collection.mutable.Map[String, ReportResponse]()
  private val domainCache = scala.collection.mutable.Map[String, ProfileResponse]()

  def getDaskboard(): DashboardResponse = {
    val day = CommonService.getLatestDay()
    if (dashboardCache._1 == day) {
      dashboardCache._2
    } else {
      val response = DashboardService.get(day)
      dashboardCache = day -> response
      response
    }
  }

  def getRank(): RankResponse = {
    val day = CommonService.getLatestDay()
    if (rankCache._1 == day) {
      rankCache._2
    } else {
      val all = CommonService.getTopRank(1, day)
      val black = CommonService.getTopBlackByNumOfQuery(day)
      val white = CommonService.getTopByNumOfQuery(day, Label.White)
      val unknow = CommonService.getTopByNumOfQuery(day, Label.Unknow)
      val response = RankResponse(all, black, white, unknow)
      rankCache = day -> response
      response
    }
  }

  def getReport(day: String): ReportResponse = {
    if (reportCache.contains(day)) {
      reportCache.get(day).get
    } else {
      val response = ReportService.get(day)
      reportCache.put(day, response)
      response
    }
  }

  def getDomain(domain: String): ProfileResponse = {
    val day = CommonService.getLatestDay()
    if (latest != day) {
      domainCache.clear()
    }
    if (domainCache.contains(domain)) {
      domainCache.get(domain).get
    } else {
      val response = ProfileService.get(domain)
      domainCache.put(domain, response)
      response
    }
  }
}