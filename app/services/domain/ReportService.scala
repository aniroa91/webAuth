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
import model.StatsResponse
import com.ftel.bigdata.utils.DateTimeUtil
import scala.util.Try
import model.ReportResponse


class ReportService extends AbstractService {

  def get(day: String): ReportResponse = {
    val prev = getPreviousDay(day)

    val multiSearchResponse = client.execute(
      multi( // label:black AND _type:top
        search(s"dns-stats-${day}" / "count") query { not(termQuery(LABEL_FIELD, TOTAL_VALUE)) },
        search(s"dns-stats-${prev}" / "count") query { not(termQuery(LABEL_FIELD, TOTAL_VALUE)) },
        search(s"dns-stats-${day}" / "malware") query { must(termQuery(LABEL_FIELD, BLACK_VALUE)) } sortBy { fieldSort(NUM_QUERY_FIELD) order SortOrder.DESC } limit 100,
        search(s"dns-stats-${day}" / "top") query { must(termQuery(LABEL_FIELD, BLACK_VALUE)) } sortBy { fieldSort(NUM_QUERY_FIELD) order SortOrder.DESC } limit 1000
//        search(s"dns-overview-*" / "overview") sortBy { fieldSort(DAY_FIELD) order SortOrder.DESC } limit 30
        )).await
    val currentResponse = multiSearchResponse.responses(0)
    val previousResponse = multiSearchResponse.responses(1)
    val malwaresResponse = multiSearchResponse.responses(2)
    val mainDomainResponse = multiSearchResponse.responses(3)
//    val dailyResponse = multiSearchResponse.responses(4)

    if (currentResponse.hits == null) {
      //getDashboard(getLatestDay())
      null
    } else {
      val labelClassify = getTotalInfo(currentResponse)
      val current = sumTotalInfo(labelClassify.map(x => x._2))
      val previous = sumTotalInfo(getTotalInfo(previousResponse).map(x => x._2))
      val malwares = getMalwareInfo(malwaresResponse)
      val blacks = getTopBlackByNumOfQuery(day)
      val seconds = getTopRank(1, day)
//      val daily = getTotalInfoDaily(dailyResponse)

      ReportResponse(day, current, previous, labelClassify, malwares, blacks, seconds)
    }
  }
  
//  def get(day: String): ReportResponse = {
//    val prev = getPreviousDay(day)
//
//    val multiSearchResponse = client.execute(
//      multi( // label:black AND _type:top
//        search(s"dns-stats-${day}" / "count") query { not(termQuery("label", "total")) },
//        search(s"dns-stats-${day}" / "malware") query { must(termQuery("label", "black")) } sortBy { fieldSort("number_of_record") order SortOrder.DESC } limit 100,
//        search(s"dns-stats-${day}" / "top") query { must(termQuery("label", "black")) } sortBy { fieldSort("number_of_record") order SortOrder.DESC } limit 1000,
//        search(s"dns-stats-${prev}" / "count") query { not(termQuery("label", "total")) },
//        search(s"dns-overview-*" / "overview") sortBy { fieldSort("day") order SortOrder.DESC } limit 30
//
//        //search(ES_INDEX_ALL / "domain") query { must(termQuery("second", domain)) } aggregations (
//        //  cardinalityAgg("num_of_domain", "domain")),
//        //search((ES_INDEX + "whois") / "whois") query { must(termQuery("domain", domain)) }
//        )).await
//    val labelResponse = multiSearchResponse.responses(0)
//    val malwareResponse = multiSearchResponse.responses(1)
//    val domainResponse = multiSearchResponse.responses(2)
//    val labelResponsePrev = multiSearchResponse.responses(3)
//    val dailyResponse = multiSearchResponse.responses(4)
//    
////    println("=========")
////    println(labelResponse.hits)
////    println("=========")
//    if (labelResponse.hits == null) {
//      //getStatsByDay(getLatestDay())
//      null
//    } else {
//      val arrayLabelResponse = labelResponse.hits.hits.map(x => {
//        val map = x.sourceAsMap
//        val label = map.getOrElse("label", "").toString()
//        val malwares = if (label == BLACK_VALUE) map.getOrElse("number_of_malware", "0").toString().toInt else 0
//        LabelResponse(
//          label,
//          map.getOrElse("number_of_record", "0").toString().toInt,
//          map.getOrElse("number_of_domain", "0").toString().toInt,
//          map.getOrElse("number_of_ip", "0").toString().toInt,
//          malwares,
//          map.getOrElse("success", "0").toString().toInt,
//          map.getOrElse("failed", "0").toString().toInt,
//          map.getOrElse("number_of_second_domain", "0").toString().toInt)
//      })
//
//      val arrayMalwareResponse = malwareResponse.hits.hits.map(x => {
//        val map = x.sourceAsMap
//        MalwareResponse(
//          map.getOrElse("malware", "").toString(),
//          map.getOrElse("number_of_record", "0").toString().toInt,
//          map.getOrElse("number_of_domain", "0").toString().toInt,
//          map.getOrElse("number_of_ip", "0").toString().toInt)
//      })
//
//      val arrayDomainResponse = domainResponse.hits.hits.map(x => {
//        val map = x.sourceAsMap
//        //println(map)
//        DomainResponse(
//          map.getOrElse("domain", "").toString(),
//          map.getOrElse("malware", "0").toString(),
//          map.getOrElse("number_of_record", "0").toString().toInt,
//          map.getOrElse("number_of_ip", "0").toString().toInt)
//      })
//
//      val total = getTotalFrom(labelResponse)
//      val totalPrev = getTotalFrom(labelResponsePrev)
//
//      val daily = dailyResponse.hits.hits.reverse.map(x => {
//        val map = x.sourceAsMap
//         DailyResponse(
//          map.getOrElse("day", "").toString(),
//          map.getOrElse("number_of_record", "0").toString().toInt,
//          map.getOrElse("number_of_domain", "0").toString().toInt,
//          map.getOrElse("number_of_ip", "0").toString().toInt)
//      })
//      
//      val secondBlack = getTopBlackByNumOfQuery(day)
//      val arraySecondBlackResponse = secondBlack.map(x => SecondResponse(x._1, x._2.label, x._2.malware, x._2.numOfQuery, 0, x._2.numOfClient))
//      val second = getTopRank(1, day)
//      val arraySecondResponse = second.map(x => SecondResponse(x._1, x._2.label, x._2.malware, x._2.numOfQuery, 0, x._2.numOfClient))
//      StatsResponse(day, total, totalPrev, arrayLabelResponse, arrayMalwareResponse, arrayDomainResponse)
//    }
//  }
}

object ReportService {
  def get(day: String): ReportResponse = {
    val service = new ReportService();
    val latestDay = service.getLatestDay()
    val isValid = Try(DateTimeUtil.create(day, DateTimeUtil.YMD)).isSuccess
    if (isValid) service.get(day) else service.get(latestDay)
    //service.get(day)
    //CacheService.getDaskboard(service, latestDay)
  }
}