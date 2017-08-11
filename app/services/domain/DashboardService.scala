package services.domain

import org.elasticsearch.search.sort.SortOrder

import com.sksamuel.elastic4s.http.ElasticDsl.MultiSearchHttpExecutable
import com.sksamuel.elastic4s.http.ElasticDsl.RichFuture
import com.sksamuel.elastic4s.http.ElasticDsl.RichString
import com.sksamuel.elastic4s.http.ElasticDsl.fieldSort
import com.sksamuel.elastic4s.http.ElasticDsl.multi
import com.sksamuel.elastic4s.http.ElasticDsl.search

import model.DashboardResponse

object DashboardService extends AbstractService {

  def get(day: String): DashboardResponse = {
    val report = ReportService.get(day)

    val multiSearchResponse = client.execute(
      multi(
        search(s"dns-overview-*" / "overview") sortBy { fieldSort(DAY_FIELD) order SortOrder.DESC } limit 30)).await
    val dailyResponse = multiSearchResponse.responses(0)

    if (report != null) {
      val daily = getTotalInfoDaily(dailyResponse)
      if (daily.exists(x => x._1 == report.day)) {
        DashboardResponse(report, daily)
      } else {
        DashboardResponse(report, daily :+ (report.day -> report.current))
      }
    } else null
  }
}