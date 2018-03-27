package profile.services.internet

import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval
import com.ftel.bigdata.utils.DateTimeUtil
import com.ftel.bigdata.utils.Parameters
import com.ftel.bigdata.utils.StringUtil
import com.sksamuel.elastic4s.http.ElasticDsl._
import profile.services.internet.response.History
import profile.services.internet.response.HistoryContractDay
import services.Configure
import profile.services.internet.response.Hourly
import profile.services.internet.response.DayOfWeek
import profile.services.internet.response.Daily
import profile.services.internet.response.HistoryContract
import org.joda.time.DateTimeZone
import services.domain.CommonService
import services.domain.CommonService.getAggregations


object HistoryService {
  
  val client = Configure.client
  
  /**
   * Some contract bat thuong:
   * 	HND636422 -> 1 tuan co: 75,365 hits
   * 		Co gia tri am rat nhieu:
   * 
   */
  
  def main(args: Array[String]) {
    //getAll("day", "2018-02-01/2018-03-12")
    //get("2018-02-01/2018-02-01", "SGH209147".toLowerCase())
    //get("2018-02-07/2018-02-07")
    

    //println(getIndexString("radius-load", "2018-03-01/2018-03-12"))
    //println(getRangeDateForWeek("2018-03-10"))
    //getContract("week", "2018-02-09/2018-02-09", "hufd08955")
    
    //val res = getContract("week", "2018-02-08", "HUFD08955")
    val res = getAll("M", "02/2018")
    //val res = getContract("week", "2018-02-08", "HND636422")
    
    //res.
    client.close()
  }
  
  def getAll(_type: String, date: String): History = {
    get(_type, date, null)
  }
  
  
  
  
  
  
  def getContract(_type: String, date: String, contract: String): HistoryContract = {
    val esIndex = _type match {
      case "D" => Common.getIndexString("radius-rawlog", date)
      case "W" => Common.getIndexString("radius-rawlog", Common.getRangeDateForWeek(date))
      case "M" => Common.getIndexString("radius-rawlog", Common.getRangeDateForMonth(date))
    }
    if (StringUtil.isNullOrEmpty(esIndex)) {
      null
    } else {
      val history = get(_type, date, contract)
      getContract(esIndex, contract, history)
    }
  }
  
  
  def getContract(esIndex: String, contract: String, history: History): HistoryContract = {

    val req = search(esIndex / "load") query (
        boolQuery()
          .must(termQuery("contract", contract))
      ) sortByFieldDesc("timestamp") size 1000
    //println(esIndex)
    //println(client.show(req))
    val response = client.execute(req).await
    //response.hits.hits.foreach(println)
    val loadLogs = response.hits.hits
      .map(x => x.sourceAsMap)
      .map(x => {
        LoadLog(x.get("statusType").get.toString,
            DateTimeUtil.create(x.get("timestamp").get.toString, Parameters.ES_5_DATETIME_FORMAT).getMillis,
            x.get("nasName").get.toString,
            x.get("nasPort").get.toString.toInt,
            x.get("name").get.toString,
            x.get("sessionID").get.toString,
            x.get("sessionTime").get.toString.toLong,
            x.get("ipAddress").get.toString,
            x.get("callerID").get.toString,
            x.get("download").get.toString.toLong,
            x.get("upload").get.toString.toLong)
      })
    
    val sessions = history.sessions.map(x => (x._1, x._4, x._5, x._6))
    
    val sessionMap = sessions.map(x => x._1 -> (x._2, x._3, x._4)).toMap

//    val macs = loadLogs.map(x => (x.callerID, x.sessionID) -> x)
//            .groupBy(x => x._1)
//            .map(x => x._2.map(y => y._2).reduce((a,b) => if (a.timestamp > b.timestamp) a else b))
//            .map(x => x.callerID -> (x.download, x.upload))
//            .groupBy(x => x._1)
//            .map(x => (x._1, x._2.map(y => y._2._1).sum, x._2.map(y => y._2._2).sum))
//            .toArray
     val macs = loadLogs.map(x => (x.callerID, x.sessionID) -> x)
            .groupBy(x => x._1)
            .map(x => x._2.map(y => y._2).reduce((a,b) => if (a.timestamp > b.timestamp) a else b))
            .map(x => {
              val a = sessionMap.getOrElse(x.sessionID, (x.download, x.upload, x.sessionTime))
              x.callerID -> (a._1, a._2)
            })
            .groupBy(x => x._1)
            .map(x => (x._1, x._2.map(y => y._2._1).sum, x._2.map(y => y._2._2).sum))
            .toArray
     
    val numberOfMessage = response.totalHits
    
    
    val numberOfSession = sessions.size
    
    val logs = loadLogs.sortBy(x => x.timestamp)
      .reverse
      .map(x => (DateTimeUtil.create(x.timestamp/1000).toString("yyyy-MM-dd HH:mm:ss"), x.sessionID, x.download, x.upload, x.sessionTime, x.status) )
    
    val logByTime = loadLogs.map(x => x.timestamp -> (if (x.status == "ACTALIVE") x.sessionTime else -x.sessionTime))
    //val duration = loadLogs.map(x => DateTimeUtil.create(x.timestamp / 1000).getHourOfDay -> x.sessionTime)
    val download = loadLogs.map(x => DateTimeUtil.create(x.timestamp / 1000).getHourOfDay -> x.download)
    val upload = loadLogs.map(x => DateTimeUtil.create(x.timestamp / 1000).getHourOfDay -> x.upload)
    val duration = loadLogs.map(x => DateTimeUtil.create(x.timestamp / 1000).getHourOfDay -> x.sessionTime)

    val hourly = Hourly(logByTime, duration, download, upload, duration)
    
//    val sessionsDayOfWeek = loadLogs.map(x => (x.sessionID, DateTimeUtil.create(x.timestamp / 1000).getDayOfWeek) -> x)
//            .groupBy(x => x._1)
//            .map(x => x._2.map(y => y._2).reduce((a,b) => if (a.timestamp > b.timestamp) a else b))
//            .map(x => (x.sessionID, x.download, x.upload, x.sessionTime) )
//            .toArray
            
    //val dayOfWeek = DayOfWeek(logByTime, duration, download, upload, duration)
    
    val status = loadLogs.map(x => x.status -> 1L)
      .groupBy(x => x._1)
      .map(x => x._1 -> x._2.map(y => y._2).sum.toInt)
      .toArray
      
    //history.dayOfWeek.download.foreach(println)
    
    HistoryContract(
        numberOfMessage,
        numberOfSession,
        hourly,
        history.dayOfWeek,
        history.daily,
        status,
        sessions,
        macs,
        logs)
  }

  private def get(_type: String, date: String, contract: String): History = {
    val esIndex = _type match {
      case "D" => Common.getIndexString("radius-load", date)
      case "W" => Common.getIndexString("radius-load", Common.getRangeDateForWeek(date))
      case "M" => Common.getIndexString("radius-load", Common.getRangeDateForMonth(date))
    }
    if (StringUtil.isNullOrEmpty(esIndex)) {
      null
    } else get(esIndex, contract)
  }

   def getRealtime(day: String): History = {
     val secondTime = CommonService.getpreviousMinutes(15)
     println(secondTime)
     val response = client.execute(
       search(s"radius-streaming-$day" / "load")
         query { must(rangeQuery("timestamp").gte(secondTime)) }
         aggregations (
           cardinalityAgg("numberOfcontract", "name"),
           dateHistogramAggregation("minute")
             .field("timestamp")

             .interval(DateHistogramInterval.MINUTE)
             .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
             .subAggregations(
               cardinalityAgg("contract", "name"),
               sumAgg("download", "download"),
               sumAgg("upload", "upload"),
               sumAgg("duration", "sessionTime")
             ),
           termsAggregation("contract")
             .field("name")
             .subAggregations(
               sumAgg("download", "download"),
               sumAgg("upload", "upload"),
               sumAgg("duration", "sessionTime")
             ) size 50
          )
     ).await
     val minutes = getAggregations(response.aggregations.get("minute"), true)
     val contracts = getAggregations(response.aggregations.get("contract"), false)
       .filter(x => x._1 != "??")
       .sortBy(x => x._4)
       .reverse.take(20)
     val numberOfContract = response.aggregations.get("numberOfcontract").get.asInstanceOf[Map[String, Integer]].get("value").get.toLong
     val numberOfSession = response.totalHits.toLong
     History("realtime",numberOfContract,numberOfSession,new Hourly(minutes),null,null,null,contracts,null,null,null)
  }

val pointText = 60 * 2
def getBrasRealtime() = {
     val secondTime = CommonService.getpreviousMinutes(pointText * 2)
     val secondTime2 = CommonService.getpreviousMinutes(pointText)
     //println(secondTime)
     val response = client.execute(
       search(s"monitor-radius-*" / "docs")
         query { must(rangeQuery("date_time").gte(secondTime).lt(secondTime2)) }
         aggregations (
           dateHistogramAggregation("minute")
             .field("date_time")
             .interval(DateHistogramInterval.MINUTE)
             .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
             .subAggregations(
               maxAgg("signin", "signIn"),
               maxAgg("logoff", "logOff"),
               avgAgg("au", "active_users"),
               maxAgg("L_rate", "L_rate"),
               maxAgg("S_rate", "S_rate")
             )
          )
     ).await

     
    val result = response.aggregations.get("minute")
      .getOrElse("buckets", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]]
      .getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
      .map(x => x.asInstanceOf[Map[String, AnyRef]])
      .map(x => {
        val key = x.getOrElse("key", "0L").toString
        //val count = x.getOrElse("doc_count", 0L).toString().toLong
        //val contract = if (hasContract) x.get("contract").get.asInstanceOf[Map[String, Integer]].get("value").get.toLong else 0L
        //val download = x.get("download").get.asInstanceOf[Map[String, Double]].get("value").get.toLong
        //val upload = x.get("upload").get.asInstanceOf[Map[String, Double]].get("value").get.toLong
        val signin = x.get("signin").get.asInstanceOf[Map[String, Double]].get("value").get
        val logoff = x.get("logoff").get.asInstanceOf[Map[String, Double]].get("value").get
        val au = x.get("au").get.asInstanceOf[Map[String, Double]].get("value").get
        val l = x.get("L_rate").get.asInstanceOf[Map[String, Double]].get("value").get
        val s = x.get("S_rate").get.asInstanceOf[Map[String, Double]].get("value").get
        //(key, signin / au, logoff/au)
        (key, s, l)
      }).toArray.map(x => x._1 -> x._3)
  
    //result.foreach(println)
     //val minutes = getAggregations(response.aggregations.get("minute"), true)
     //response.aggregations.get("minute").foreach(println)
     result
  }

  def getBrasRealtime2(unit: Int) = {
     val secondTime = CommonService.getpreviousMinutes(pointText + 4 - unit)
     val secondTime2 = CommonService.getpreviousMinutes(pointText + 2 - unit)
     println(secondTime)
     val response = client.execute(
       search(s"monitor-radius-*" / "docs")
         query { must(rangeQuery("date_time").gte(secondTime).lt(secondTime2)) }
         aggregations (
           dateHistogramAggregation("minute")
             .field("date_time")
             .interval(DateHistogramInterval.MINUTE)
             .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
             .subAggregations(
               maxAgg("signin", "signIn"),
               maxAgg("logoff", "logOff"),
               avgAgg("au", "active_users"),
               maxAgg("L_rate", "L_rate"),
               maxAgg("S_rate", "S_rate")
             )
          )
     ).await

     
    val result = response.aggregations.get("minute")
      .getOrElse("buckets", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]]
      .getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
      .map(x => x.asInstanceOf[Map[String, AnyRef]])
      .map(x => {
        val key = x.getOrElse("key", "0L").toString
        //val count = x.getOrElse("doc_count", 0L).toString().toLong
        //val contract = if (hasContract) x.get("contract").get.asInstanceOf[Map[String, Integer]].get("value").get.toLong else 0L
        //val download = x.get("download").get.asInstanceOf[Map[String, Double]].get("value").get.toLong
        //val upload = x.get("upload").get.asInstanceOf[Map[String, Double]].get("value").get.toLong
        val signin = x.get("signin").get.asInstanceOf[Map[String, Double]].get("value").get
        val logoff = x.get("logoff").get.asInstanceOf[Map[String, Double]].get("value").get
        val au = x.get("au").get.asInstanceOf[Map[String, Double]].get("value").get
        val l = x.get("L_rate").get.asInstanceOf[Map[String, Double]].get("value").get
        val s = x.get("S_rate").get.asInstanceOf[Map[String, Double]].get("value").get
        //(key, signin / au, logoff/au)
        (key, s, l)
      }).toArray.map(x => x._1 -> x._3)
  
    //result.foreach(println)
     //val minutes = getAggregations(response.aggregations.get("minute"), true)
     //response.aggregations.get("minute").foreach(println)
     result
  }

  private def get(esIndex: String, contract: String): History = {
    val req = search(esIndex / "docs") aggregations (
        cardinalityAgg("numberOfcontract", "name"),
        dateHistogramAggregation("hourly")
              .field("timestamp")
              .interval(DateHistogramInterval.HOUR)
              .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
              .subAggregations(
                  cardinalityAgg("contract", "name"),
                  sumAgg("download", "download"),
                  sumAgg("upload", "upload"),
                  sumAgg("duration", "sessionTime")
              ),
        termsAggregation("contract")
              .field("contract")
              .subAggregations(
                  sumAgg("download", "download"),
                  sumAgg("upload", "upload"),
                  sumAgg("duration", "sessionTime")
              ) size 50,
        termsAggregation("province")
              .field("province")
              .subAggregations(
                  sumAgg("download", "download"),
                  sumAgg("upload", "upload"),
                  sumAgg("duration", "sessionTime"),
                  cardinalityAgg("contract", "name")
              ) size 20,
        termsAggregation("region")
              .field("region")
              .subAggregations(
                  sumAgg("download", "download"),
                  sumAgg("upload", "upload"),
                  sumAgg("duration", "sessionTime"),
                  cardinalityAgg("contract", "name")
              ) size 20,
         dateHistogramAggregation("daily")
              .field("timestamp")
              .interval(DateHistogramInterval.DAY)
              .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
              .subAggregations(
                  cardinalityAgg("contract", "name"),
                  sumAgg("download", "download"),
                  sumAgg("upload", "upload"),
                  sumAgg("duration", "sessionTime")
              ),
        termsAggregation("sessions")
                .field("sessionId")
                .subAggregations(
                    sumAgg("download", "download"),
                    sumAgg("upload", "upload"),
                    sumAgg("duration", "sessionTime")
              )
      )
      
    val response = if (contract == null) {
      client.execute(req).await 
    } else {
      //println(client.show(req.query(boolQuery().must(termQuery("contract", contract)))))
      client.execute(
          req.query(boolQuery().must(termQuery("contract", contract)))
              
      ).await
    }
    
    //val response = client.execute(req).await
    
    val numberOfContract = response.aggregations.get("numberOfcontract").get.asInstanceOf[Map[String, Integer]].get("value").get.toLong
    val numberOfSession = response.totalHits.toLong

    //response.aggregations.foreach(println)
    val hourly = getAggregations(response.aggregations.get("hourly"), true)
    val daily = getAggregations(response.aggregations.get("daily"), true)
    val provinces = getAggregations(response.aggregations.get("province"), true)
      .filter(x => x._1 != "??")
      .take(10)
    val regions = getAggregations(response.aggregations.get("region"), true)
      .filter(x => Array("Vung 1", "Vung 2", "Vung 3", "Vung 4", "Vung 5", "Vung 6", "Vung 7").contains(x._1))
    val contracts = getAggregations(response.aggregations.get("contract"), false)
      .filter(x => x._1 != "??")
      .sortBy(x => x._4)
      .reverse.take(20)

    val sessions = getAggregations(response.aggregations.get("sessions"), false)
      .sortBy(x => x._4)
    //sessions.foreach(println)
    val status = Array(
        "ACTLIVE" -> 80,
        "ACTLOFF" -> 20)
        
    History(
        "all",
        numberOfContract,
        numberOfSession,
        new Hourly(hourly),
        new DayOfWeek(daily),
        new Daily(daily),
        status,
        contracts,
        provinces,
        regions,
        sessions)
  }
}

case class LoadLog(
    status: String,
    timestamp: Long, // unit is millis
    nasName: String, // bras Name
    nasPort: Int,
    name: String, // customer Name
    sessionID: String,
    //input: Long, // (Upload)
    //output: Long, // (Download)
    //termCode: Int,
    sessionTime: Long,
    ipAddress: String,
    callerID: String, // MacAdddress
    //ipv6Address: String,
    //inputG: Int, // So Vong Upload
    //outputG: Int, // So Vong Download
    //inputIPv6: Long,
    //inputIPv6G: Int,
    //outputIPv6: Long,
    //outputIPv6G: Int,
    download: Long,
    upload: Long)
