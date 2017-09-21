package services.domain

import model.DashboardResponse

import org.elasticsearch.search.sort.SortOrder
import com.ftel.bigdata.utils.HttpUtil
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.search.SearchHit
import com.sksamuel.elastic4s.http.search.SearchResponse
import services.CacheService
import com.ftel.bigdata.utils.DateTimeUtil
import scala.util.Try
import model.ReportResponse
import model.MalwareInfo
import model.Label
import utils.Sort
import model.DayHourly

//import com.ftel.bigdata.dns.parameters.Label

object ReportService extends AbstractService {

  def get(day: String): ReportResponse = {
    get2(day)
  }
  
  def get2(day: String): ReportResponse = {
    val prev = CommonService.getPreviousDay(day)
    
    val time0 = System.currentTimeMillis()
    val multiSearchResponse = client.execute(
      multi(
        search(s"dns-daily-${day}" / "docs"),
        search(s"dns-daily-${prev}" / "docs"),
        search(s"dns-malware-${day}" / "docs")// query { must(termQuery(LABEL_FIELD, BLACK_VALUE)) }
          aggregations (
            termsAggregation("top")
              .field("malware")
              .subaggs(
                  sumAgg("sum", "queries"),
                  cardinalityAgg("unique-second", "second"),
                  cardinalityAgg("unique-client", "client")
              ) size 100
          ),
        search(s"dns-client-${day}" / "docs"),
        search(s"dns-client-${prev}" / "docs"),
        search(s"dns-org-${day}" / "docs") sortBy (fieldSort(NUM_QUERY_FIELD) order SortOrder.DESC) size 10, // org
        search(s"dns-country-${day}" / "docs") sortBy (fieldSort(NUM_QUERY_FIELD) order SortOrder.DESC) size 10, // countr
        search(s"dns-hourly-day-${day}" / "docs") size 24 // hourly
        )).await
    val time1 = System.currentTimeMillis()
    
    val currentResponse = multiSearchResponse.responses(0)
    val previousResponse = multiSearchResponse.responses(1)
    val malwaresResponse = multiSearchResponse.responses(2)
    val currentClientUnique = multiSearchResponse.responses(3)
    val previousClientUnique = multiSearchResponse.responses(4)
    
    val orgResponse = multiSearchResponse.responses(5)
    val countryResponse = multiSearchResponse.responses(6)
    val hourlyResponse = multiSearchResponse.responses(7)

    if (currentResponse.hits != null) {
      //println(currentResponse.hits.hits)
      val time2 = System.currentTimeMillis()
      val labelClassify = getTotalInfo(currentResponse)
      val current = sumTotalInfo(labelClassify.map(x => x._2)).clone(currentClientUnique.totalHits)
      val previous = if (previousResponse.hits != null) {
        sumTotalInfo(getTotalInfo(previousResponse).map(x => x._2)).clone(previousClientUnique.totalHits)
      } else current
      val malwares = getMalwareInfo2(malwaresResponse) // Array(1,2,3,4,5,6,7,8,9,10).reverse.map(x => MalwareInfo("test", x, 1, 1))//
      val blacks = CommonService.getTopByNumOfQuery(day, Label.Black)
      val seconds = CommonService.getTopRank(1, day)
      
      val org = orgResponse.hits.hits.map(x => x.sourceAsMap).map(x => getValueAsString(x, "org") -> getValueAsInt(x, NUM_QUERY_FIELD))
      val country = countryResponse.hits.hits.map(x => x.sourceAsMap).map(x => getValueAsString(x, "country") -> getValueAsInt(x, NUM_QUERY_FIELD))
      val hourly = hourlyResponse.hits.hits.map(x => x.sourceAsMap)
        .map(x => DayHourly(getValueAsInt(x, "hour"), getValueAsInt(x, NUM_QUERY_FIELD), getValueAsInt(x, NUM_IP_FIELD), getValueAsInt(x, NUM_SECOND_FIELD)))

      //def sort(arr: Array[(String, Int)]): Array[(String, Int)] = Sort[(String, Int)](arr, (x: (String, Int)) => x._2, false)
      def sort(arr: Array[DayHourly]): Array[DayHourly] = Sort[DayHourly](arr, (x: DayHourly) => x.hour, true)
      //def sort(arr: Array[(String, Int)]): Array[(String, Int)] = Sort[(String, Int)](arr, (x: (String, Int)) => x._2, false)
        
      val time3 = System.currentTimeMillis()
      ReportResponse(day, current, previous, labelClassify, malwares, blacks, seconds, Sort(org, false), Sort(country, false), sort(hourly))
      //null
    } else null
  }
  
  @deprecated("", "Using get2 method instead of")
  def get1(day: String): ReportResponse = {
    val prev = CommonService.getPreviousDay(day)
    
    val time0 = System.currentTimeMillis()
    val multiSearchResponse = client.execute(
      multi(
        search(s"dns-daily-${day}" / "docs"),
        search(s"dns-daily-${prev}" / "docs"),
        search(s"dns-statslog-2017-08-21" / "docs") query { must(termQuery(LABEL_FIELD, BLACK_VALUE)) } sortBy { fieldSort(NUM_QUERY_FIELD) order SortOrder.DESC } limit 100
        //search(s"dns-stats-${day}" / "top") query { must(termQuery(LABEL_FIELD, BLACK_VALUE)) } sortBy { fieldSort(NUM_QUERY_FIELD) order SortOrder.DESC } limit 1000
        )).await
    val time1 = System.currentTimeMillis()
    
    val currentResponse = multiSearchResponse.responses(0)
    val previousResponse = multiSearchResponse.responses(1)
    val malwaresResponse = multiSearchResponse.responses(2)
    //val mainDomainResponse = multiSearchResponse.responses(3)

    if (currentResponse.hits != null) {
      //println(currentResponse.hits.hits)
      val time2 = System.currentTimeMillis()
      val labelClassify = getTotalInfo(currentResponse)
      val current = sumTotalInfo(labelClassify.map(x => x._2))
      val previous = if (previousResponse.hits != null) {
        sumTotalInfo(getTotalInfo(previousResponse).map(x => x._2))
      } else current
      val malwares = getMalwareInfo(malwaresResponse) // Array(1,2,3,4,5,6,7,8,9,10).reverse.map(x => MalwareInfo("test", x, 1, 1))//
      val blacks = CommonService.getTopByNumOfQuery(day, Label.Black)
      val seconds = CommonService.getTopRank(1, day)
      val time3 = System.currentTimeMillis()
      ReportResponse(day, current, previous, labelClassify, malwares, blacks, seconds, null, null, null)
    } else null
  }
}
