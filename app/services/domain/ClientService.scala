package services.domain

import org.elasticsearch.search.sort.SortOrder

//import com.sksamuel.elastic4s.http.ElasticDsl.MultiSearchHttpExecutable
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.SearchResponse

import model.ClientResponse
import model.HistoryInfo
import model.MainDomainInfo
import services.ElasticUtil
import model.HistoryRow
import model.HistoryHour
import model.HistoryDay
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import com.ftel.bigdata.utils.DateTimeUtil
import com.ftel.bigdata.utils.Parameters

object ClientService extends AbstractService {

  def get(ip: String, day: String): ClientResponse = {
    val time0 = System.currentTimeMillis()
    
    val response = client.execute(search(s"dns-client-*" / "docs") query {boolQuery().must(termQuery("client", ip))} size 30).await
    val responseValid = client.execute(search(s"dns-history-client-${day}" / "docs")
          query { boolQuery().must(termQuery("client", ip)) }
          aggregations (
            termsAggregation("topDomain").field("domain").subagg(sumAgg("sum", "queries")) order(Terms.Order.aggregation("sum", false)) size 10,
            termsAggregation("topSecond").field("second").subagg(sumAgg("sum", "queries")) order(Terms.Order.aggregation("sum", false)) size 10
          ) sortBy (fieldSort(NUM_QUERY_FIELD) order SortOrder.DESC)
        ).await
        
//    val a = client.show(search(s"dns-history-client-${day}" / "docs")
//          query { boolQuery().must(termQuery("client", ip))}
//          aggregations (
//            (termsAggregation("topDomain")
//              .field("domain")
//              .subagg(sumAgg("sum", "queries")) order(Terms.Order.aggregation("sum", false)) size 10),
//            (termsAggregation("topSecond")
//              .field("second")
//              .subagg(sumAgg("sum", "queries")) order(Terms.Order.aggregation("sum", false)) size 10)
//          )
//            sortBy (fieldSort(NUM_QUERY_FIELD) order SortOrder.DESC)
//        )
        //println(a)
//        termsAggregation("topSecond").field("second").subagg(sumAgg("sum", "queries")) size 10,
//        termsAggregation("hourly").field("hour").subagg(sumAgg("sum", "queries")) size 24,
//        termsAggregation("daily").field("day").subagg(sumAgg("sum", "queries")) size 30
        //).await
    //val responseValid = client.execute(search(s"dns-history-client-${day}" / "docs") query {boolQuery().must(termQuery("client", ip))}).await
    
   
    val daily = getClientInfo(response, responseValid.totalHits).sortBy(x => x.day)
    val responseHourly = client.execute(search(s"dns-hourly-client-${day}" / "docs") query {boolQuery().must(termQuery("name", ip))} size 24).await
//    val response = client.execute(
//      search(s"dns-statslog-2017-08-20" / "docs") query {
//        boolQuery().must(termQuery("client", ip))
//        /*
//            boolQuery()
//              .must(termQuery("client", ip), termQuery("rCode", "0"))
//              .not(termQuery("tld", "null"))
//            */
//      } aggregations (
//        termsAggregation("topDomain").field("domain").subagg(sumAgg("sum", "queries")) size 10,
//        termsAggregation("topSecond").field("second").subagg(sumAgg("sum", "queries")) size 10,
//        termsAggregation("hourly").field("hour").subagg(sumAgg("sum", "queries")) size 24,
//        termsAggregation("daily").field("day").subagg(sumAgg("sum", "queries")) size 30
//      ) postFilter {
//          boolQuery().must(termQuery("rCode", "0")).not(termQuery("tld", "null"))
//      } sortBy (
//        fieldSort(DAY_FIELD) order SortOrder.DESC,
//        fieldSort("hour") order SortOrder.DESC) limit 10).await

//    val responseDaily = client.execute(
//      search(s"dns-statslog-2017-08-20" / "docs") query {
//        boolQuery().must(termQuery("client", ip))
//      } aggregations (
//        termsAggregation("daily").field("day").subagg(sumAgg("sum", "queries")) size 30)).await
//
//    val time1 = System.currentTimeMillis()
//
//    val topDomain = ElasticUtil.getBucketTerm(response, "topDomain", "sum").map(x => new MainDomainInfo(x.key, x.value)).sortBy(x => x.queries).reverse
//    val topSecond = ElasticUtil.getBucketTerm(response, "topSecond", "sum").map(x => new MainDomainInfo(x.key, x.value)).sortBy(x => x.queries).reverse
//    val hourly = ElasticUtil.getBucketTerm(response, "hourly", "sum").map(x => x.key.toInt -> x.value.toLong).sorted
//    val daily = ElasticUtil.getBucketTerm(responseDaily, "daily", "sum").map(x => x.key -> x.value.toLong).sorted
    //println(response)
    //println("Time: " + (time1 - time0))
    
    val topDomain = ElasticUtil.getBucketTerm(responseValid, "topDomain", "sum").map(x => new MainDomainInfo(x.key, x.value)).sortBy(x => x.queries).reverse
    val topSecond = ElasticUtil.getBucketTerm(responseValid, "topSecond", "sum").map(x => new MainDomainInfo(x.key, x.value)).sortBy(x => x.queries).reverse
    
    //daily.map(x => x.day -> x.queries).foreach(println)
    
    val history = getHistory(response)
    val current = daily.reverse.head
    val prev = if (daily.size >= 2) daily.reverse.tail.head else current
    
    val hourly = responseHourly.hits.hits.map(x => {
      val map = x.sourceAsMap
      val hour = map.getOrElse("hour", "0").toString.toInt
      val queries = map.getOrElse("queries", "0").toString.toLong
      hour -> queries
    }).toMap
    val hourInDay = (0 until 24).map(x => x -> hourly.getOrElse(x, 0L)).toArray
    //println(hourly.map(x => x._2).sum)
    //hourly.foreach(println)
    //println(hourly.size)
    ClientResponse(current, prev, topDomain, topSecond, hourInDay, daily, historyBlack(ip, 0, CommonService.SIZE_DEFAULT), historyBlack2(ip, 0, CommonService.SIZE_DEFAULT))
  }

  def getTop(): Array[(String, Int)] = {
    val latest = CommonService.getLatestDay()
//    val time0 = System.currentTimeMillis()
    val response = client.execute(search(s"dns-client-${latest}" / "docs") query {must(rangeQuery("rank").gt(0).lte(100))} limit 100).await
    val res = response.hits.hits.map(x => x.sourceAsMap)
      .map(x => x.getOrElse("client", "") -> x.getOrElse("queries", ""))
      .map(x => x._1.toString -> x._2.toString().toInt)
    res.sortBy(x => x._2).reverse
  }
  
  def historyJsonWithoutHour(ip: String, offset: Int, size: Int): HistoryInfo = {
    val latestDay = CommonService.getLatestDay()
    val time0 = System.currentTimeMillis()
    val response = client.execute(
      search(s"dns-history-client-*" / "docs") query {
        boolQuery()
          .must(termQuery("client", ip))
      } sortBy (
        fieldSort(DAY_FIELD) order SortOrder.DESC,
        fieldSort("queries") order SortOrder.DESC) from offset limit size).await
    getHistory(response)
  }
  
  def historyBlack(ip: String, offset: Int, size: Int): HistoryInfo = {
    val latestDay = CommonService.getLatestDay()
    val time0 = System.currentTimeMillis()
    val response = client.execute(
      search(s"dns-black-*" / "docs") query {
        boolQuery()
          .must(termQuery("client", ip))
      } sortBy (
        fieldSort("timeStamp") order SortOrder.DESC) from offset limit size).await
    getHistory2(response)
  }
  
  def historyBlack2(ip: String, offset: Int, size: Int): Array[Array[String]] = {
    val latestDay = CommonService.getLatestDay()
    val time0 = System.currentTimeMillis()
    val response = client.execute(
      search(s"dns-black-*" / "docs") query {
        boolQuery()
          .must(termQuery("client", ip))
      } sortBy (
        fieldSort("timeStamp") order SortOrder.DESC) from offset limit size).await
    val res = response.hits.hits.map(x => {
      val map = x.sourceAsMap
      //println(map)
      val timestamp = getValueAsString(map, "timeStamp", "0") //map.get("timeStamp").getOrElse("0").toString()
      val date = DateTimeUtil.create(timestamp, Parameters.ES_5_DATETIME_FORMAT)
      val day = date.toString(DateTimeUtil.YMD)
      val hour = date.toString("HH")//map.get("hour").getOrElse("").toString()
      val domain = getValueAsString(map, "domain")//map.get("domain").getOrElse("").toString()
      val second = getValueAsString(map, "second")//map.get("second").getOrElse("").toString()
      val label = map.get("label").getOrElse("").toString()
      val queries = getValueAsString(map, "queries")//map.get("queries").getOrElse("0").toString().toInt
      val rCode = getValueAsString(map, "rCodeName")//map.get("rCodeName").getOrElse("-1").toString()
      val malware = getValueAsString(map, "malware")//map.get("malware").getOrElse("null").toString()
      val answers = getValueAsString(map, "answers")//map.get("answers").getOrElse("null").toString()
      
      Array(day, date.toString("HH:mm:SS"), domain, second, malware, rCode, answers.split(",").mkString("\n"))
    })
    res
  }
  
  def historyJson(ip: String, offset: Int, size: Int): HistoryInfo = {
    val latestDay = CommonService.getLatestDay()
    val time0 = System.currentTimeMillis()
    val response = client.execute(
      search(s"dns-history-client-*" / "docs") query {
        boolQuery()
          .must(termQuery("client", ip), termQuery("rCode", "0"))
          .not(termQuery("tld", "null"))
      } sortBy (
        fieldSort(DAY_FIELD) order SortOrder.DESC,
        fieldSort("hour") order SortOrder.DESC) from offset limit size).await
    getHistory(response)
  }

  private def getHistory(response: SearchResponse): HistoryInfo = {
    val res = response.hits.hits.map(x => {
      val map = x.sourceAsMap
      val day = map.get("day").getOrElse("").toString()
      val hour = map.get("hour").getOrElse("").toString()
      val domain = map.get("domain").getOrElse("").toString()
      val second = map.get("second").getOrElse("").toString()
      val label = map.get("label").getOrElse("").toString()
      val queries = map.get("queries").getOrElse("0").toString().toInt
      val rCode = map.get("rCode").getOrElse("-1").toString()
      //println(day)
      HistoryDay(day, Array(HistoryHour(hour, Array(HistoryRow(domain, second, label, queries, rCode)))))
    })
    //println(response.totalHits)
    HistoryInfo(res).group()
  }
  
  private def getHistory2(response: SearchResponse): HistoryInfo = {
    val res = response.hits.hits.map(x => {
      val map = x.sourceAsMap
      //println(map)
      val timestamp = map.get("timeStamp").getOrElse("0").toString()
      val date = DateTimeUtil.create(timestamp, Parameters.ES_5_DATETIME_FORMAT)
      val day = date.toString(DateTimeUtil.YMD)
      val hour = date.toString("HH")//map.get("hour").getOrElse("").toString()
      val domain = getValueAsString(map, "domain")//map.get("domain").getOrElse("").toString()
//      println(map.get("second").get.toString())
      val second = getValueAsString(map, "second")//map.get("second").getOrElse("").toString()
      
      val label = getValueAsString(map, "label")//map.get("label").getOrElse("").toString()
      val queries = getValueAsInt(map, "queries")//map.get("queries").getOrElse("0").toString().toInt
      val rCode = getValueAsString(map, "rCode")//map.get("rCode").getOrElse("-1").toString()
      //println(day)
      HistoryDay(day, Array(HistoryHour(hour, Array(HistoryRow(domain, second, label, queries, rCode)))))
    })
    //println(response.totalHits)
    HistoryInfo(res).group()
  }
}