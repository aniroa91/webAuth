package services

import model.DashboardResponse
import services.domain.DashboardService

object CacheService {
  private val DASH_BOARD_MAP = scala.collection.mutable.Map[String, DashboardResponse]()

  def getDaskboard(service: DashboardService, day: String): DashboardResponse = {
    if (DASH_BOARD_MAP.contains(day)) {
      DASH_BOARD_MAP.get(day).get
    } else {
      DASH_BOARD_MAP.clear()
      val response = service.get(day)
      DASH_BOARD_MAP.put(day, response)
      response
    }
  }
}