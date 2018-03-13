package profile.services.internet

import services.Configure
import com.sksamuel.elastic4s.http.ElasticDsl._
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval
import com.ftel.bigdata.utils.DateTimeUtil
import profile.controllers.internet.InternetReponse
import com.ftel.bigdata.utils.Parameters
import profile.controllers.internet.InternetReponseContract
import org.joda.time.Days
import com.ftel.bigdata.utils.StringUtil
import com.ftel.bigdata.utils.ESUtil

object HistoryService {
  
  val client = Configure.client
  
  def main(args: Array[String]) {
    //get("2018-02-01/2018-03-12")
    get("2018-02-07/2018-02-07", "SGH209147".toLowerCase())
    //get("2018-02-07/2018-02-07")
    

    //println(getIndexString("radius-load", "2018-03-01/2018-03-12"))
    client.close()
  }
  
  private def getIndexString(indexParttern: String, date: String): String = {
    if (StringUtil.isNullOrEmpty(date)) {
      s"${indexParttern}-2018-02-07"
    } else {
    val arr = date.split("/")
    val days = (0 until Days.daysBetween(DateTimeUtil.create(arr(0), DateTimeUtil.YMD), DateTimeUtil.create(arr(1), DateTimeUtil.YMD)).getDays + 1)
      .map(x => DateTimeUtil.create(arr(0), DateTimeUtil.YMD).plusDays(x).toString(DateTimeUtil.YMD))
      days.map(x => s"${indexParttern}-${x}").filter(x => client.execute(indexExists(x)).await.isExists).mkString(",")
    }
  }
  
  def get(date: String): InternetReponse = {
    val indexString = getIndexString("radius-load", date)
    println(indexString)
    if (StringUtil.isNullOrEmpty(indexString)) {
      null
    } else {
    val req = search(indexString / "docs") aggregations (
        cardinalityAgg("numberOfcontract", "name"),
        dateHistogramAggregation("hourly")
              .field("timestamp")
              .interval(DateHistogramInterval.HOUR)
              .subAggregations(
                  cardinalityAgg("contract", "name"),
                  sumAgg("download", "download"),
                  sumAgg("upload", "upload")
              ),
        termsAggregation("contract")
              .field("contract")
              .subAggregations(
                  sumAgg("download", "download"),
                  sumAgg("upload", "upload")
              ) size 20,
        termsAggregation("province")
              .field("province")
              .subAggregations(
                  sumAgg("download", "download"),
                  sumAgg("upload", "upload"),
                  sumAgg("duration", "sessiontime"),
                  cardinalityAgg("contract", "name")
              ) size 20,
        termsAggregation("region")
              .field("region")
              .subAggregations(
                  sumAgg("download", "download"),
                  sumAgg("upload", "upload"),
                  sumAgg("duration", "sessiontime"),
                  cardinalityAgg("contract", "name")
              ) size 20
      )
    //val req = if (name == null) reqTemp else reqTemp.query(boolQuery().must(termQuery("name", name)))
    println(client.show(req))
    val response = client.execute(req).await
    
//    response.hits.hits.foreach(println)
    println("RESULT")
    response.aggregations.foreach(println)
    
    val numberOfContract = response.aggregations.get("numberOfcontract").get.asInstanceOf[Map[String, Integer]].get("value").get.toLong
    val numberOfSession = response.totalHits.toLong
    val hourly = response.aggregations.get("hourly")
      .getOrElse("buckets", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]]
      .getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
      .map(x => x.asInstanceOf[Map[String, AnyRef]])
      .map(x => {
        val key = x.getOrElse("key", 0L).toString.toLong
        val count = x.getOrElse("doc_count", 0L).toString().toLong
        val contract = x.get("contract").get.asInstanceOf[Map[String, Integer]].get("value").get.toLong
        val download = x.get("download").get.asInstanceOf[Map[String, Double]].get("value").get.toLong
        val upload = x.get("upload").get.asInstanceOf[Map[String, Double]].get("value").get.toLong
        
        //val value = x.getOrElse(nameSubTerm, Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].getOrElse("value", "0").toString().toDouble
        //BucketDouble(key, count, value)
        //(DateTimeUtil.create(key / 1000).toString("HH").toInt, count, contract, download, upload)
        (key, count, contract, download, upload)
      })
    val contractHourly = hourly.map(x => x._1 -> x._3).sortBy(x => x._1).toArray
    val sessionHourly = hourly.map(x => DateTimeUtil.create(x._1 / 1000).toString("HH").toLong -> x._2)
      .groupBy(x => x._1)
      .map(x => x._1 -> x._2.map(y => y._2).sum)
      .toArray.sortBy(x => x._1)
    val downloadHourly = hourly.map(x => DateTimeUtil.create(x._1 / 1000).toString("HH").toLong -> x._4)
      .groupBy(x => x._1)
      .map(x => x._1 -> x._2.map(y => y._2).sum)
      .toArray.sortBy(x => x._1)
    val uploadHourly = hourly.map(x => DateTimeUtil.create(x._1 / 1000).toString("HH").toLong -> x._5)
      .groupBy(x => x._1)
      .map(x => x._1 -> x._2.map(y => y._2).sum)
      .toArray.sortBy(x => x._1)
    
    val status = Array(
        "ACTLIVE" -> 80,
        "ACTLOFF" -> 20)
    val contract = response.aggregations.get("contract")
      .getOrElse("buckets", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]]
      .getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
      .map(x => x.asInstanceOf[Map[String, AnyRef]])
      .map(x => {
        val key = x.getOrElse("key", "NA").toString
        //val count = x.getOrElse("doc_count", 0L).toString().toLong
        //val contract = x.get("contract").get.asInstanceOf[Map[String, Integer]].get("value").get.toLong
        val download = x.get("download").get.asInstanceOf[Map[String, Double]].get("value").get.toLong
        val upload = x.get("upload").get.asInstanceOf[Map[String, Double]].get("value").get.toLong
        
        //val value = x.getOrElse(nameSubTerm, Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].getOrElse("value", "0").toString().toDouble
        //BucketDouble(key, count, value)
        //(DateTimeUtil.create(key / 1000).toString("HH").toInt, count, contract, download, upload)
        (key, download, upload)
      })
    val province = response.aggregations.get("province")
      .getOrElse("buckets", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]]
      .getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
      .map(x => x.asInstanceOf[Map[String, AnyRef]])
      .map(x => {
        val key = x.getOrElse("key", "NA").toString
        //val count = x.getOrElse("doc_count", 0L).toString().toLong
        val contract = x.get("contract").get.asInstanceOf[Map[String, Integer]].get("value").get.toLong
        val download = x.get("download").get.asInstanceOf[Map[String, Double]].get("value").get.toLong
        val upload = x.get("upload").get.asInstanceOf[Map[String, Double]].get("value").get.toLong
        val duration = x.get("duration").get.asInstanceOf[Map[String, Double]].get("value").get.toLong
        //val value = x.getOrElse(nameSubTerm, Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].getOrElse("value", "0").toString().toDouble
        //BucketDouble(key, count, value)
        //(DateTimeUtil.create(key / 1000).toString("HH").toInt, count, contract, download, upload)
        (key, contract, download, upload, duration)
      }).toArray
    val region = response.aggregations.get("region")
      .getOrElse("buckets", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]]
      .getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
      .map(x => x.asInstanceOf[Map[String, AnyRef]])
      .map(x => {
        val key = x.getOrElse("key", "NA").toString
        val contract = x.get("contract").get.asInstanceOf[Map[String, Integer]].get("value").get.toLong
        val download = x.get("download").get.asInstanceOf[Map[String, Double]].get("value").get.toLong
        val upload = x.get("upload").get.asInstanceOf[Map[String, Double]].get("value").get.toLong
        val duration = x.get("duration").get.asInstanceOf[Map[String, Double]].get("value").get.toLong
        (key, contract, download, upload, duration)
      }).toArray
    val topContract = contract.toArray
    topContract.foreach(println)
    //response.aggregations.foreach(println)
//        
//    val topContract = Array(
//        ("bpfdl-150820-434", 100L, 20L, 20L),
//        ("bpfdl-150820-434", 100L, 20L, 20L),
//        ("bpfdl-150820-434", 100L, 20L, 20L),
//        ("bpfdl-150820-434", 100L, 20L, 20L),
//        ("bpfdl-150820-434", 100L, 20L, 20L),
//        ("bpfdl-150820-434", 100L, 20L, 20L),
//        ("bpfdl-150820-434", 100L, 20L, 20L),
//        ("bpfdl-150820-434", 100L, 20L, 20L),
//        ("bpfdl-150820-434", 100L, 20L, 20L),
//        ("bpfdl-150820-434", 100L, 20L, 20L),
//        ("bpfdl-150820-434", 100L, 20L, 20L),
//        ("bpfdl-150820-434", 100L, 20L, 20L),
//        ("bpfdl-150820-434", 100L, 20L, 20L),
//        ("bpfdl-150820-434", 100L, 20L, 20L),
//        ("bpfdl-150820-434", 100L, 20L, 20L),
//        ("bpfdl-150820-434", 100L, 20L, 20L),
//        ("bpfdl-150820-434", 100L, 20L, 20L),
//        ("bpfdl-150820-434", 100L, 20L, 20L),
//        ("bpfdl-150820-434", 100L, 20L, 20L),
//        ("bpfdl-150820-434", 100L, 20L, 20L))
    val topProvince = Array(
        ("A", 100L, 200L, 100L, 50L),
        ("B", 100L, 200L, 100L, 50L),
        ("C", 100L, 200L, 100L, 50L),
        ("D", 100L, 200L, 100L, 50L),
        ("E", 100L, 200L, 100L, 50L),
        ("F", 100L, 200L, 100L, 50L),
        ("G", 100L, 200L, 100L, 50L),
        ("E", 100L, 200L, 100L, 50L),
        ("F", 100L, 200L, 100L, 50L),
        ("G", 100L, 200L, 100L, 50L))
        
    val topRegion = topProvince.take(7)
        
    InternetReponse(
        "all",
        numberOfContract,
        numberOfSession,
        contractHourly,
        sessionHourly,
        downloadHourly,
        uploadHourly,
        status,
        topContract.filter(x => x._1 != "??").sortBy(x => x._2).reverse,
        province.filter(x => x._1 != "??"),
        region.filter(x => Array("Vung 1", "Vung 2", "Vung 3", "Vung 4", "Vung 5", "Vung 6", "Vung 7").contains(x._1)))
        
    //client.close()
        
    }
  }
  
  def get(date: String, contract: String): InternetReponseContract = {
    
    val indexString = getIndexString("radius-rawlog", date)
    println(indexString)
    if (StringUtil.isNullOrEmpty(indexString)) {
      null
    } else {
    val req = search(indexString / "load") query (
        boolQuery()
          .must(termQuery("contract", contract))
      ) size 10
//      aggregations (
//        cardinalityAgg("contract", "name"),
//        dateHistogramAggregation("hourly")
//              .field("timestamp")
//              .interval(DateHistogramInterval.HOUR)
//              .subAggregations(
//                  cardinalityAgg("contract", "name"),
//                  sumAgg("download", "download"),
//                  sumAgg("upload", "upload")
//              )
//      )
    //val req = if (name == null) reqTemp else reqTemp.query(boolQuery().must(termQuery("name", name)))
    println(client.show(req))
    val response = client.execute(req).await
    
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
    loadLogs.foreach(println)
    val numberOfContract = loadLogs.size
    val numberOfSession = loadLogs.map(x => x.sessionID).distinct.size
    val contractHourly = loadLogs.map(x => x.timestamp -> (if (x.status == "ACTALIVE") x.sessionTime else -x.sessionTime))
    //val contractHourly = loadLogs.map(x => x.timestamp -> (if (x.status == "ACTALIVE") 1 else -1))
    val sessionHourly = loadLogs.map(x => x.timestamp -> x.sessionTime)
    
    val topSesion = loadLogs.map(x => (x.name + x.sessionID) -> x)
            .groupBy(x => x._1)
            .map(x => x._2.map(y => y._2).reduce((a,b) => if (a.timestamp > b.timestamp) a else b))
            .map(x => (x.sessionID, x.download, x.upload, x.sessionTime) )
            .toArray
            
   
    
    val downloadHourly = loadLogs.map(x => x.timestamp -> x.download)
    val uploadHourly = loadLogs.map(x => x.timestamp -> x.upload)
    //println(Common.humanReadableByteCount(topSesion.map(x => x._2).sum) )
    //val downloadRate = downloadHourly.map(x => x._2).sum / topSesion.map(x => x._2).sum 
    //val uploadRate = uploadHourly.map(x => x._2).sum / topSesion.map(x => x._3).sum
    
    val status = loadLogs.map(x => x.status -> 1L)
      .groupBy(x => x._1)
      .map(x => x._1 -> x._2.map(y => y._2).sum.toInt)
      .toArray
//    val status = if (statusTemp.size == 2) statusTemp else {
//      
//    }
      //status.foreach(println)
    //loadLogs.map(x => x.status -> 1L).foreach(println)
    
    
    
//    val topSesion = Array(
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L),
//        ("bpfdl-150820-434", 100L, 20L))
//    val topProvince = Array(
//        ("A", 100L, 200L, 100L, 50L),
//        ("B", 100L, 200L, 100L, 50L),
//        ("C", 100L, 200L, 100L, 50L),
//        ("D", 100L, 200L, 100L, 50L),
//        ("E", 100L, 200L, 100L, 50L),
//        ("F", 100L, 200L, 100L, 50L),
//        ("G", 100L, 200L, 100L, 50L),
//        ("E", 100L, 200L, 100L, 50L),
//        ("F", 100L, 200L, 100L, 50L),
//        ("G", 100L, 200L, 100L, 50L))
        
    //val topRegion = topProvince.take(7)
    
    //val stat
//    println(numberOfContract)
    
//    println(numberOfSession)
    
//    contractHourly.foreach(println)

    val logs = loadLogs.sortBy(x => x.timestamp).reverse.map(x => (DateTimeUtil.create(x.timestamp/1000).toString("yyyy-MM-dd HH:mm:ss"), x.sessionID, x.download, x.upload, x.sessionTime, x.status) )
    //val ipList = loadLogs.map(x => x.ipAddress -> x.callerID).distinct.toArray
    
    val macList = loadLogs.map(x => (x.callerID, x.sessionID) -> x)
            .groupBy(x => x._1)
            .map(x => x._2.map(y => y._2).reduce((a,b) => if (a.timestamp > b.timestamp) a else b))
            .map(x => x.callerID -> (x.download, x.upload))
            .groupBy(x => x._1)
            .map(x => (x._1, x._2.map(y => y._2._1).sum, x._2.map(y => y._2._2).sum))
            .toArray
            
    InternetReponseContract(
        "contract",
        macList.size,
        numberOfSession,
        contractHourly,
        sessionHourly,
        downloadHourly,//.map(x => x._1 -> (x._2 / downloadRate).toLong),
        uploadHourly,//.map(x => x._1 -> (x._2 / uploadRate).toLong),
        status,
        topSesion,
        logs,
        macList)
//    null
    }
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