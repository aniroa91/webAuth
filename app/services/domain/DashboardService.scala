package services.domain

import model.DashboardResponse

import org.elasticsearch.search.sort.SortOrder
import com.ftel.bigdata.utils.HttpUtil
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.search.SearchHit
import com.sksamuel.elastic4s.http.search.SearchResponse
//import model.MalwareResponse
//import model.DailyResponse
//import model.DomainResponse
//import model.LabelResponse
//import model.SecondResponse
import services.CacheService


class DashboardService extends AbstractService {

  def get(day: String): DashboardResponse = {
    val service = new ReportService();
    val report = service.get(day)
    val prev = getPreviousDay(day)

    val multiSearchResponse = client.execute(
      multi( // label:black AND _type:top
        //search(s"dns-stats-${day}" / "count") query { not(termQuery(LABEL_FIELD, TOTAL_VALUE)) },
        //search(s"dns-stats-${prev}" / "count") query { not(termQuery(LABEL_FIELD, TOTAL_VALUE)) },
        //search(s"dns-stats-${day}" / "malware") query { must(termQuery(LABEL_FIELD, BLACK_VALUE)) } sortBy { fieldSort(NUM_QUERY_FIELD) order SortOrder.DESC } limit 100,
        //search(s"dns-stats-${day}" / "top") query { must(termQuery(LABEL_FIELD, BLACK_VALUE)) } sortBy { fieldSort(NUM_QUERY_FIELD) order SortOrder.DESC } limit 1000,
        search(s"dns-overview-*" / "overview") sortBy { fieldSort(DAY_FIELD) order SortOrder.DESC } limit 30
        )).await
    //val currentResponse = multiSearchResponse.responses(0)
    //val previousResponse = multiSearchResponse.responses(1)
    //val malwaresResponse = multiSearchResponse.responses(2)
    //val mainDomainResponse = multiSearchResponse.responses(3)
    val dailyResponse = multiSearchResponse.responses(0)

    if (report == null) {
      //getDashboard(getLatestDay())
      null
    } else {
      //val labelClassify = getTotalInfo(currentResponse)
      //val current = sumTotalInfo(labelClassify.map(x => x._2))
      //val previous = sumTotalInfo(getTotalInfo(previousResponse).map(x => x._2))
      //val malwares = getMalwareInfo(malwaresResponse)
      //val blacks = getMainDomainInfo(mainDomainResponse)
      //val seconds = getTopRank(1, day)
      val daily = getTotalInfoDaily(dailyResponse)

      DashboardResponse(report, daily)
    }
  }
}

object DashboardService {
  def get(): DashboardResponse = {
    val service = new DashboardService();
    val latestDay = service.getLatestDay()
    CacheService.getDaskboard(service, latestDay)
  }
}