package services.domain

import org.elasticsearch.search.sort.SortOrder

import scala.concurrent.duration.{Duration, SECONDS}

//import com.sksamuel.elastic4s.http.ElasticDsl.MultiSearchHttpExecutable
//import com.sksamuel.elastic4s.http.ElasticDsl.RichFuture
//import com.sksamuel.elastic4s.http.ElasticDsl.RichString
//import com.sksamuel.elastic4s.http.ElasticDsl.fieldSort
import com.sksamuel.elastic4s.http.ElasticDsl._
//import com.sksamuel.elastic4s.http.ElasticDsl.search

import model.DashboardResponse
import model.TotalInfo
import com.ftel.bigdata.utils.DateTimeUtil
import scala.collection.immutable.HashSet
import org.joda.time.Days
import model.Label

//import com.ftel.bigdata.dns.parameters.Label

//import org.apache.lucene.index.Terms
//import org.elasticsearch.search.aggregations.bucket.terms.Terms

object DashboardService extends AbstractService {

  def get(day: String): DashboardResponse = {
    get2(day)
  }

  def get2(day: String): DashboardResponse = {
    
    val date = DateTimeUtil.create(day, DateTimeUtil.YMD)
    
       
       
//       .map(x => x -> ReportService.get(x))
//       
//       .toArray.sortBy(x => x._1)
       
    //DashboardResponse(daily.head._2, daily.map(x => x._1 -> x._2.current))
    val report = ReportService.get(day)

//    val response = client.execute(search(s"dns-daily-*" / "docs") query { not(termQuery(LABEL_FIELD, TOTAL_VALUE))} size 1000).await
    //val response = client.execute(search(s"dns-daily-*" / "docs") size 1000).await
       //        aggregations (
      //          termsAggregation("daily").field("day").subagg(sumAgg("sum", "queries")) //order(Terms.Order.aggregation("daily", false)) size 30
      //          //termsAggregation("topSecond").field("second").subagg(sumAgg("sum", "queries")) size 10,
      //          //termsAggregation("hourly").field("hour").subagg(sumAgg("sum", "queries")) size 24,
      //          //termsAggregation("daily").field("day").subagg(sumAgg("sum", "queries")) size 30
      //        )
    //println(response.totalHits)
    val response = client.execute(search(s"dns-daily-*" / "docs") size 1000).await(Duration(30, SECONDS))
    val daily = response.hits.hits.map(x => {
      val sourceAsMap = x.sourceAsMap
      val day = getValueAsString(sourceAsMap, "day")
      day -> getTotalInfo(sourceAsMap)._2
     }).groupBy(x => x._1)
       .map(x => x._1 -> x._2.map(y => y._2).reduce((a,b) => a.plus(b)))
       .toArray
       .sortBy(x => x._1)
       
    val begin = daily.head._1
    //println(Days.daysBetween(date.toLocalDate(), DateTimeUtil.create(begin, DateTimeUtil.YMD).toLocalDate()).getDays())
    val seq = 0 until Days.daysBetween(DateTimeUtil.create(begin, DateTimeUtil.YMD).toLocalDate(), date.toLocalDate()).getDays() + 1
    val arrDays = seq.map(x => date.minusDays(x))
       .map(x => x.toString(DateTimeUtil.YMD))
       .toArray.reverse
    //println("hoang"+arrDays.length)
    val days = arrDays.slice(arrDays.length-30,arrDays.length)
    val map = daily.toMap
    if (report != null) {
      //val daily = getTotalInfoDaily(response)
      //if (daily.exists(x => x._1 == report.day)) {
      val black = CommonService.getTopByNumOfQuery(day, Label.Black)
      val white = CommonService.getTopByNumOfQuery(day, Label.White)
      val unknow = CommonService.getTopByNumOfQuery(day, Label.Unknow)
      
      DashboardResponse(report, days.map(x => if (map.contains(x)) x -> map.getOrElse(x, null) else x -> new TotalInfo()), black, white, unknow)
      //} else {
      //  DashboardResponse(report, daily :+ (report.day -> report.current))
      //}
    } else null
  }

  
  def getDiffSecond() {
    val now = DateTimeUtil.now.minusDays(4)//.toString(DateTimeUtil.YMD)
    val seq = 1 until 2
    val s = search(s"dns-days-second" / "docs") query {
         must(
           termQuery("year", now.toString("yyyy").toInt),
           termQuery("month", now.toString("MM").toInt),
           termQuery("value", now.toString("dd").toInt)
         )}
    
    val searchMap = seq.map(x => now.minus(x))
       .map(x => search(s"dns-days-second" / "docs") query {
         must(
           termQuery("year", x.toString("yyyy").toInt),
           termQuery("month", x.toString("MM").toInt),
           termQuery("value", x.toString("dd").toInt)
         )})
    //val multiSearchResponse = client.execute(multi(searchMap)).await
    //multiSearchResponse.responses.foreach(x => x.totalHits)
    println("Finished Search")
    println(client.show(s))
    client.close()
  }
  
//  private def getNumDay(num: Int): Array[Int] = {
//    
//  }
  
  @deprecated("This method will be removed in next release, please using get() method", "bigdata-play 0.0.1")
  def get1(day: String): DashboardResponse = {
    val report = ReportService.get(day)

    val multiSearchResponse = client.execute(multi(search(s"dns-overview-*" / "overview") sortBy { fieldSort(DAY_FIELD) order SortOrder.DESC } limit 30)).await(Duration(30, SECONDS))
    val dailyResponse = multiSearchResponse.responses(0)

    println(dailyResponse.hits.hits)
    if (report != null) {
      val daily = getTotalInfoDaily(dailyResponse)
      if (daily.exists(x => x._1 == report.day)) {
        DashboardResponse(report, daily, null, null, null)
      } else {
        DashboardResponse(report, daily :+ (report.day -> report.current), null, null, null)
      }
    } else null
  }
}