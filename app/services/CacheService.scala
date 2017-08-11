package services

import model.DashboardResponse
import services.domain.DashboardService
import services.domain.CommonService
import model.RankResponse
import com.ftel.bigdata.dns.parameters.Label
import model.ReportResponse
import services.domain.ReportService

object CacheService {
  private var DASH_BOARD_CACHE: (String, DashboardResponse) = "" -> null
  private var RANK_CACHE: (String, RankResponse) = "" -> null
  private val REPORT_CACHE = scala.collection.mutable.Map[String, ReportResponse]()
  
  def getDaskboard(): DashboardResponse = {
    val day = CommonService.getLatestDay()
    if (DASH_BOARD_CACHE._1 == day) {
      DASH_BOARD_CACHE._2
    } else {
      val response = DashboardService.get(day)
      DASH_BOARD_CACHE = day -> response
      response
    }
  }
  
  def getRank(): RankResponse = {
    val day = CommonService.getLatestDay()
    if (RANK_CACHE._1 == day) {
      RANK_CACHE._2
    } else {
      val all = CommonService.getTopRank(1, day)
      val black = CommonService.getTopBlackByNumOfQuery(day)
      val white = CommonService.getTopByNumOfQuery(day, Label.White)
      val unknow = CommonService.getTopByNumOfQuery(day, Label.Unknow)
      val response = RankResponse(all, black, white, unknow)
      RANK_CACHE = day -> response
      response
    }
  }

  def getReport(day: String): ReportResponse = {
    if (REPORT_CACHE.contains(day)) {
      REPORT_CACHE.get(day).get
    } else {
      val response = ReportService.get(day)
      REPORT_CACHE.put(day, response)
      response
    }
  }
}