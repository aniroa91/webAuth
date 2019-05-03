package services.domain

import java.text.DecimalFormat
import java.time.format.DateTimeFormatter

import com.ftel.bigdata.utils.DateTimeUtil
import org.elasticsearch.search.sort.SortOrder
import org.joda.time.{DateTime, Days}

import scala.collection.mutable
import scala.util.control.Breaks.{break, breakable}

//import com.ftel.bigdata.dns.parameters.Label
import com.ftel.bigdata.utils.DateTimeUtil
import com.ftel.bigdata.utils.WhoisUtil
import com.ftel.bigdata.whois.Whois
import com.sksamuel.elastic4s.http.ElasticDsl.IndexHttpExecutable
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.SearchResponse

import scala.util.Try
import services.Configure
import services.Bucket2
import com.ftel.bigdata.utils.FileUtil
import com.ftel.bigdata.utils.HttpUtil
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.apache.http.HttpHost
import scalaj.http.Http
import play.api.libs.json.Json
import scala.concurrent.duration.Duration
import scala.concurrent.duration._

import java.util.concurrent.TimeUnit
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import play.api.libs.json.JsObject
import com.ftel.bigdata.utils.StringUtil
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime
import org.joda.time.Months;
import java.time.format.DateTimeFormatter

object CommonService extends AbstractService {

  val monthSize = 20*3
  val SIZE_DEFAULT = 20
  val RANK_HOURLY = "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23"
  /**
   * Service for Get Information about day
   */

  def getSigLogByNameContract(name: String, arr: Array[(String,String,String)]): String = {
    val rs = arr.filter(x=> x._1 == name.toLowerCase)
    val status = rs.groupBy(_._1).mapValues(_.max).map(_._2).toList.sortBy(_._2).toArray
    return if(status.length>0) status(0)._2 else "none"
  }

  def getLongValueByKey(arr: Array[(String,Long)], key:String):Int = {
    var value = 0;
    breakable{for(i <- 0 until arr.length){
      if(arr(i)._1 == key) {
        value = arr(i)._2.toInt
        break
      }
    }}
    return value;
  }
  def getIntValueByKey(arr: Array[(Int,Int)], key:Int):Int = {
    var value = 0;
    breakable{for(i <- 0 until arr.length){
      if(arr(i)._1 == key) {
        value = arr(i)._2
        break
      }
    }}
    return value;
  }

  def getLatestDay(): String = {
    val response = client.execute(
      search("dns-marker" / "docs") sortBy { fieldSort("day") order SortOrder.DESC } limit 1).await(Duration(30, SECONDS))
    response.hits.hits.head.id//.sourceAsMap.getOrElse("day", "").toString()
  }

  def getPreviousMonth(): String = {
    val prev = new DateTime()
    prev.minusMonths(1).toString("yyyy-MM")
  }

  def getPreviousMonth(month: String): String = {
    val prev = DateTimeUtil.create(month, "yyyy-MM")
    prev.minusMonths(1).toString("yyyy-MM")
  }

  def getPreviousDay(day: String): String = {
    val prev = DateTimeUtil.create(day, DateTimeUtil.YMD)
    prev.minusDays(1).toString(DateTimeUtil.YMD)
  }

  def getNextDay(day: String): String = {
    val prev = DateTimeUtil.create(day, DateTimeUtil.YMD)
    prev.plusDays(1).toString(DateTimeUtil.YMD)
  }

  def getCurrentDay(): String = {
    val date = new DateTime()
    date.toString(DateTimeFormat.forPattern("yyyy-MM-dd"))
  }

  def getAllMonthfromRange(fromMonth: String,toMonth: String): Array[(String)] = {
    val date1 = DateTimeUtil.create(toMonth, DateTimeUtil.YMD)
    val date2 = DateTimeUtil.create(fromMonth, DateTimeUtil.YMD)
    val numberOfMonths = Months.monthsBetween(date2, date1).getMonths()+1
    (0 until numberOfMonths).map(date2.plusMonths(_).toString(DateTimeFormat.forPattern("yyyy-MM"))).toArray

  }

  def getRangeCurrentMonth(): String = {
    val date = new DateTime()
    val fromDate = date.minusMonths(3).toString(DateTimeFormat.forPattern("yyyy-MM"))
    val toDate = date.minusMonths(1).toString(DateTimeFormat.forPattern("yyyy-MM"))
    fromDate+"/"+toDate
  }

  def getEndDate(endDate:String): String = {
    val date1 = new DateTime()
    val date2 = DateTimeUtil.create(endDate, DateTimeUtil.YMD)
    val months = Months.monthsBetween(date2, date1).getMonths()
    "-"+months+"m"
  }

  def getStartDate(startDate:String): String = {
    val date1 = new DateTime()
    val date2 = DateTimeUtil.create(startDate, DateTimeUtil.YMD)
    val months = Months.monthsBetween(date2, date1).getMonths()
    "-"+months+"m"
  }

  def get3MonthAgo(): String = {
    val date = new DateTime()
    date.minusMonths(4).toString(DateTimeFormat.forPattern("yyyy-MM"))
  }

  def getnumMonthAgo(num: Int): String = {
    val date = new DateTime()
    date.minusMonths(num).toString(DateTimeFormat.forPattern("yyyy-MM-01"))
  }

  def getpreviousMinutes(times: Int): String = {
    val date = new DateTime()
    date.minusMinutes(times).toString()
  }

  def getAggregations(aggr: Option[AnyRef], hasContract: Boolean): Array[(String, Long, Long, Long, Long, Long)] = {
    aggr.getOrElse("buckets", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]]
      .getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
      .map(x => x.asInstanceOf[Map[String, AnyRef]])
      .map(x => {
        val key = x.getOrElse("key", "0L").toString
        val count = x.getOrElse("doc_count", 0L).toString().toLong
        val contract = if (hasContract) x.get("contract").get.asInstanceOf[Map[String, Integer]].get("value").get.toLong else 0L
        val download = x.get("download").get.asInstanceOf[Map[String, Double]].get("value").get.toLong
        val upload = x.get("upload").get.asInstanceOf[Map[String, Double]].get("value").get.toLong
        val duration = x.get("duration").get.asInstanceOf[Map[String, Double]].get("value").get.toLong
        (key, contract, count, download, upload, duration)
      })
      .toArray
  }

  def getAggregationsSiglog(aggr: Option[AnyRef]): Array[(String, Long)] = {
    aggr.getOrElse("buckets", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]]
      .getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
      .map(x => x.asInstanceOf[Map[String, AnyRef]])
      .map(x => {
        val key = x.getOrElse("key", "0L").toString
        val count = x.getOrElse("doc_count", 0L).toString().toLong
        (key, count)
      })
      .toArray
  }

  def getAggregationsKeyString(aggr: Option[AnyRef]): Array[(String, Long)] = {
    aggr.getOrElse("buckets", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]]
      .getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
      .map(x => x.asInstanceOf[Map[String, AnyRef]])
      .map(x => {
        val key = x.getOrElse("key_as_string", "0L").toString
        val count = x.getOrElse("doc_count", 0L).toString().toLong
        (key, count)
      })
      .toArray
  }

  def getThirdAggregations(aggr: Option[AnyRef], secondField: String, thirdField: String):  Array[(String, Array[(String, Array[(String, Long)])])] = {
    aggr.getOrElse("buckets", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]]
      .getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
      .map(x => x.asInstanceOf[Map[String, AnyRef]])
      .map(x => {
        val key = x.getOrElse("key", "0L").toString
        val map = x.getOrElse(s"$secondField",Map[String,AnyRef]()).asInstanceOf[Map[String,AnyRef]]
          .getOrElse("buckets",List).asInstanceOf[List[AnyRef]]
          .map(x => x.asInstanceOf[Map[String,AnyRef]])
          .map(x => {
            val keyCard = x.getOrElse("key","0L").toString
            val map = x.getOrElse(s"$thirdField",Map[String,AnyRef]()).asInstanceOf[Map[String,AnyRef]]
              .getOrElse("buckets",List).asInstanceOf[List[AnyRef]]
              .map(x=> x.asInstanceOf[Map[String,AnyRef]])
              .map(x=> {
                val keyPort = x.getOrElse("key","0L").toString
                val count = x.getOrElse("doc_count",0L).toString.toLong
                (keyPort,count)
              }).toArray
            (keyCard,map)
          }).toArray
        (key, map)
      })
      .toArray
  }

  def getMultiAggregations(aggr: Option[AnyRef]):  Array[(String, Array[(String, Array[(String, Long)])])] = {
    aggr.getOrElse("buckets", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]]
      .getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
      .map(x => x.asInstanceOf[Map[String, AnyRef]])
      .map(x => {
        val key = x.getOrElse("key", "0L").toString
        val map = x.getOrElse("card",Map[String,AnyRef]()).asInstanceOf[Map[String,AnyRef]]
           .getOrElse("buckets",List).asInstanceOf[List[AnyRef]]
             .map(x => x.asInstanceOf[Map[String,AnyRef]])
               .map(x => {
                 val keyCard = x.getOrElse("key","0L").toString
                 val map = x.getOrElse("port",Map[String,AnyRef]()).asInstanceOf[Map[String,AnyRef]]
                             .getOrElse("buckets",List).asInstanceOf[List[AnyRef]]
                               .map(x=> x.asInstanceOf[Map[String,AnyRef]])
                                  .map(x=> {
                                    val keyPort = x.getOrElse("key","0L").toString
                                    val count = x.getOrElse("doc_count",0L).toString.toLong
                                    (keyPort,count)
                                  }).toArray
                 (keyCard,map)
               }).toArray
        (key, map)
      })
      .toArray
  }

  def getMultiAggregationsAndSum(aggr: Option[AnyRef],secondField: String,threeField: String,fourField: String):  Array[(String, Array[(String, Array[(String, Array[(String, Long)])])])] = {
    aggr.getOrElse("buckets", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]]
      .getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
      .map(x => x.asInstanceOf[Map[String, AnyRef]])
      .map(x => {
        val key = x.getOrElse("key_as_string", "0L").toString
        val map = x.getOrElse(s"$secondField",Map[String,AnyRef]()).asInstanceOf[Map[String,AnyRef]]
          .getOrElse("buckets",List).asInstanceOf[List[AnyRef]]
          .map(x => x.asInstanceOf[Map[String,AnyRef]])
          .map(x => {
            val keySecond = x.getOrElse("key","0L").toString
            val map = x.getOrElse(s"$threeField",Map[String,AnyRef]()).asInstanceOf[Map[String,AnyRef]]
              .getOrElse("buckets",List).asInstanceOf[List[AnyRef]]
              .map(x=> x.asInstanceOf[Map[String,AnyRef]])
              .map(x=> {
                val keyThree = x.getOrElse("key","0L").toString
                val map = x.getOrElse(s"$fourField",Map[String,AnyRef]()).asInstanceOf[Map[String,AnyRef]]
                  .getOrElse("buckets",List).asInstanceOf[List[AnyRef]]
                  .map(x => x.asInstanceOf[Map[String,AnyRef]])
                  .map(x => {
                    val keyFour = x.getOrElse("key","0L").toString
                    val sum = x.getOrElse("sum", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].get("value").getOrElse("0").asInstanceOf[Double].toLong
                    (keyFour,sum)
                  }).toArray
                (keyThree,map)
              }).toArray
            (keySecond,map)
          }).toArray
        (key, map)
      })
      .toArray
  }

  def getSecondAggregations(aggr: Option[AnyRef],secondField: String):  Array[(String, Array[(String, Long)])] = {
    aggr.getOrElse("buckets", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]]
      .getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
      .map(x => x.asInstanceOf[Map[String, AnyRef]])
      .map(x => {
        val key = x.getOrElse("key", "0L").toString
        val map = x.getOrElse(s"$secondField",Map[String,AnyRef]()).asInstanceOf[Map[String,AnyRef]]
          .getOrElse("buckets",List).asInstanceOf[List[AnyRef]]
          .map(x => x.asInstanceOf[Map[String,AnyRef]])
          .map(x => {
            val keyCard = x.getOrElse("key","0L").toString
            val count = x.getOrElse("doc_count",0L).toString.toLong
            (keyCard,count)
          }).toArray
        (key, map)
      })
      .toArray
  }

  def getSecondAggregationsAndSums(aggr: Option[AnyRef],secondField: String):  Array[(String, Array[(String, Long, Long)])] = {
    aggr.getOrElse("buckets", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]]
      .getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
      .map(x => x.asInstanceOf[Map[String, AnyRef]])
      .map(x => {
        val key = x.getOrElse("key", "0L").toString
        val map = x.getOrElse(s"$secondField",Map[String,AnyRef]()).asInstanceOf[Map[String,AnyRef]]
          .getOrElse("buckets",List).asInstanceOf[List[AnyRef]]
          .map(x => x.asInstanceOf[Map[String,AnyRef]])
          .map(x => {
            val key = x.getOrElse("key","0L").toString
            val sum0 = x.getOrElse("sum0", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].get("value").getOrElse("0").asInstanceOf[Double].toLong
            val sum1 = x.getOrElse("sum1", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].get("value").getOrElse("0").asInstanceOf[Double].toLong
            (key,sum0,sum1)
          }).toArray
        (key, map)
      })
      .toArray
  }

  def getAggregationsAndCountDistinct(aggr: Option[AnyRef]):  Array[(String, Long, Long)] = {
    aggr.getOrElse("buckets", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]]
      .getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
      .map(x => x.asInstanceOf[Map[String, AnyRef]])
      .map(x => {
        val key = x.getOrElse("key", "0L").toString
        val count = x.getOrElse("doc_count",0L).toString.toLong
        val countDist = x.getOrElse("name", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].get("value").getOrElse("0").asInstanceOf[Int].toLong
        (key, count,countDist)
      })
      .toArray
  }

  def getSecondAggregationsAndSumInfError(aggr: Option[AnyRef],secondField: String):  Array[(String, Array[(String, Long,Long,Long,Long,Long,Long)])] = {
    aggr.getOrElse("buckets", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]]
      .getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
      .map(x => x.asInstanceOf[Map[String, AnyRef]])
      .map(x => {
        val key = x.getOrElse("key", "0L").toString
        val map = x.getOrElse(s"$secondField",Map[String,AnyRef]()).asInstanceOf[Map[String,AnyRef]]
          .getOrElse("buckets",List).asInstanceOf[List[AnyRef]]
          .map(x => x.asInstanceOf[Map[String,AnyRef]])
          .map(x => {
            val key = x.getOrElse("key","0L").toString
            val sum0 = x.getOrElse("sum0", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].get("value").getOrElse("0").asInstanceOf[Double].toLong
            val sum1 = x.getOrElse("sum1", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].get("value").getOrElse("0").asInstanceOf[Double].toLong
            val sum2 = x.getOrElse("sum2", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].get("value").getOrElse("0").asInstanceOf[Double].toLong
            val sum3 = x.getOrElse("sum3", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].get("value").getOrElse("0").asInstanceOf[Double].toLong
            val sum4 = x.getOrElse("sum4", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].get("value").getOrElse("0").asInstanceOf[Double].toLong
            val sum5 = x.getOrElse("sum5", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].get("value").getOrElse("0").asInstanceOf[Double].toLong
            (key,sum0,sum1,sum2,sum3,sum4,sum5)
          }).toArray
        (key, map)
      })
      .toArray
  }

  def getSecondAggregationsAndSumContractSf(aggr: Option[AnyRef],secondField: String):  Array[(String, Array[(String, Long,Long,Long)])] = {
    aggr.getOrElse("buckets", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]]
      .getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
      .map(x => x.asInstanceOf[Map[String, AnyRef]])
      .map(x => {
        val key = x.getOrElse("key", "0L").toString
        val map = x.getOrElse(s"$secondField",Map[String,AnyRef]()).asInstanceOf[Map[String,AnyRef]]
          .getOrElse("buckets",List).asInstanceOf[List[AnyRef]]
          .map(x => x.asInstanceOf[Map[String,AnyRef]])
          .map(x => {
            val key = x.getOrElse("key","0L").toString
            val sum0 = x.getOrElse("sum0", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].get("value").getOrElse("0").asInstanceOf[Double].toLong
            val sum1 = x.getOrElse("sum1", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].get("value").getOrElse("0").asInstanceOf[Double].toLong
            val sum2 = x.getOrElse("sum2", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].get("value").getOrElse("0").asInstanceOf[Double].toLong
            (key,sum0,sum1,sum2)
          }).toArray
        (key, map)
      })
      .toArray
  }

  def getAggregationsKeyStringAndMultiSum(aggr: Option[AnyRef]): Array[(String, Int,Int,Int,Int,Int,Int,Int)] = {
    aggr.getOrElse("buckets", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]]
      .getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
      .map(x => x.asInstanceOf[Map[String, AnyRef]])
      .map(x => {
        val key = x.getOrElse("key_as_string", "0L").toString
        val sum0 = x.getOrElse("sum0", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].get("value").getOrElse("0").asInstanceOf[Double].toInt
        val sum1 = x.getOrElse("sum1", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].get("value").getOrElse("0").asInstanceOf[Double].toInt
        val sum2 = x.getOrElse("sum2", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].get("value").getOrElse("0").asInstanceOf[Double].toInt
        val sum3 = x.getOrElse("sum3", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].get("value").getOrElse("0").asInstanceOf[Double].toInt
        val sum4 = x.getOrElse("sum4", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].get("value").getOrElse("0").asInstanceOf[Double].toInt
        val sum5 = x.getOrElse("sum5", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].get("value").getOrElse("0").asInstanceOf[Double].toInt
        val sum6 = x.getOrElse("sum6", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].get("value").getOrElse("0").asInstanceOf[Double].toInt
        (key, sum0,sum1,sum2,sum3,sum4,sum5,sum6)
      })
      .toArray
  }

  def getPreviousDay(day: String, num: Int): String = {
    val prev = DateTimeUtil.create(day, DateTimeUtil.YMD)
    prev.minusDays(num).toString(DateTimeUtil.YMD)
  }

  def getRangeDay(day: String): String = {
    var from = day.split("/")(0)
    val to = day.split("/")(1)
    val prev = DateTimeUtil.create(from, DateTimeUtil.YMD)
    val next = DateTimeUtil.create(to, DateTimeUtil.YMD)
    val numDays = Days.daysBetween(prev, next).getDays()
    for (f<- 1 to numDays) {
      from += ","+prev.plusDays(f).toString(DateTimeUtil.YMD)
    }
    return from
  }

  def isDayValid(day: String): Boolean = {
    Try(DateTimeUtil.create(day, DateTimeUtil.YMD)).isSuccess
  }

  def format2Decimal(number: Double): Double = {
    BigDecimal(number).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
  }

  def formatNumber(number: Long): String = {
    val formatter = java.text.NumberFormat.getIntegerInstance
    if (number > 1000000000) {
      BigDecimal(number / (1000000000 * 1.0)).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble + " B"
    } else if (number > 1000000) {
      BigDecimal(number / (1000000 * 1.0)).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble + " M"
    } else if (number > 1000) {
      BigDecimal(number / (1000 * 1.0)).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble + " K"
    } else {
      number.toString
    }
    //formatter.format(number)

    //BigDecimal(value).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble
  }
  
  def formatNumberDouble(number: Double): String = {
    val formatter = java.text.NumberFormat.getIntegerInstance
    if (number > 1000000000) {
      BigDecimal(number / (1000000000 * 1.0)).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble + " B"
    } else if (number > 1000000) {
      BigDecimal(number / (1000000 * 1.0)).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble + " M"
    } else if (number > 1000) {
      BigDecimal(number / (1000 * 1.0)).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble + " K"
    } else {
      number.toString
    }
  }
  
  @deprecated
  def formatNumberOld(number: Long): String = {
    val formatter = java.text.NumberFormat.getIntegerInstance
    formatter.format(number)
    
    //BigDecimal(value).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble
  }

  def formatPattern(number: Int): String ={
    val frnum = new DecimalFormat("###,###.###");
    frnum.format(number);
  }

  def formatPatternDouble(number: Double): String ={
    val frnum = new DecimalFormat("###,###.###");
    frnum.format(number);
  }

  def format2DecimalDouble(number: Double): String ={
    val frnum = new DecimalFormat("###,###.##");
    frnum.format(number);
  }

  def percentDouble(number: Double, prev: Double): Double = {
    if(prev == 0.0){
      100.0
    }
    else{
      val value = ((number - prev) / (prev * 1.0)) * 100.0
      BigDecimal(value).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
    }
  }

  def percent(number: Long, prev: Long): Double = {
    val value = ((number - prev) / (prev * 1.0)) * 100.0
    BigDecimal(value).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
  }
  
  def percentInfor(number: Long, total: Long): Double = {
    val value = number/ (total * 1.0) * 100.0
    BigDecimal(value).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
  }

  def formatNumHour(number: Double):Double = {
    val value = number/ 3600 * 1.00
    BigDecimal(value).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
  }

  def getOthers(orgs: Array[(String, Int)], total: Long): Int ={

    total.toInt - orgs.map(x=>x._2).sum
    //percentInfor(orgQueries,total)
  }

  def formatDateYYMMDD( date : DateTime) : String = {
    date.toString(DateTimeFormat.forPattern("yyyy/MM/dd"))
  }

  def formatStringYYMMDD( date : String) : String = {
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
    val dateTime = DateTime.parse(date, formatter)
    dateTime.toString(DateTimeFormat.forPattern("yyyy-MM-dd"))
  }

  def formatDateDDMMYY( date : String) : String = {
    val formatter = DateTimeFormat.forPattern("yyyy-mm-dd")
    val dateTime = DateTime.parse(date, formatter)
    dateTime.toString(DateTimeFormat.forPattern("dd/mm/yyyy"))
  }

  def formatUTC(date: String): String = {
    val ES_5_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"
    val formatter = DateTimeFormat.forPattern(ES_5_DATETIME_FORMAT)
    val dateTime = DateTime.parse(date, formatter)
    dateTime.toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))
  }

  def formatMilisecondToDate(second: Long): Int = {
   /* val date = DateTime(second,DateTimeUtil.TIMEZONE_HCM)
    date.getHours()*/
    1
  }

  def formatUTCToHour(date: String):Long = {
    val ES_5_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"
    val formatter = DateTimeFormat.forPattern(ES_5_DATETIME_FORMAT)
    val dateTime = DateTime.parse(date, formatter)
    dateTime.getHourOfDay()
  }

  def formatStringToMillisecond(date: String):Long = {
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
    val dateTime = DateTime.parse(date, formatter)
    dateTime.getMillis()
  }

  def formatYYmmddToUTC(date: String): String = {
    val ES_5_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
    val dateTime = DateTime.parse(date, formatter)
    dateTime.toString(DateTimeFormat.forPattern(ES_5_DATETIME_FORMAT))
  }

  def formatStringToUTC(date: String): String = {
    val ES_5_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
    val dateTime = DateTime.parse(date, formatter)
    dateTime.toString(DateTimeFormat.forPattern(ES_5_DATETIME_FORMAT))
  }

  def formatYYYYmmddHHmmss( date : String) : String = {
    val strTime = date.substring(0,date.indexOf(".")+3)
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS")
    val dateTime = DateTime.parse(strTime, formatter)
    dateTime.toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))
  }
  
  def formatSecond(seconds: Double) : String = {
    val minutes = (seconds.toInt / 60)
    val hours = (minutes / 60)
    val days = (hours / 24)
    s"${days}d ${hours%24}h ${minutes%60}m ${seconds.toInt%60}s"
  }

  def getHoursFromMiliseconds(miliseconds: Long): Int = {
    new DateTime(miliseconds).getHourOfDay()
  }

  def getHourFromES5(date: String): Int = {
    val ES_5_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"
    val formatter = DateTimeFormat.forPattern(ES_5_DATETIME_FORMAT)
    val dateTime = DateTime.parse(date, formatter)
    dateTime.getHourOfDay
  }

  /**
   * Create html tag
   */
  def getImageTag(domain: String): String = {
    val logo = getLogo(domain, false)
    //"<a href=\"/search?q=" + domain + "\"><img src=\"" + logo + "\" width=\"30\" height=\"30\"></a>"
    //<img id="currentPhoto" src="SomeImage.jpg" onerror="this.src='Default.jpg'" width="100" height="120">
    "<a href=\"/search?ct=" + domain + "\"><img src=\"" + logo + "\" onerror=\"this.src='/assets/images/logo/default.png'\" width=\"30\" height=\"30\"></a>"
  }
  
  def getImageTag2(domain: String): String = {
    val logo = getLogo(domain, false)
    //"<a href=\"/search?q=" + domain + "\"><img src=\"" + logo + "\" width=\"30\" height=\"30\"></a>"
    //<img id="currentPhoto" src="SomeImage.jpg" onerror="this.src='Default.jpg'" width="100" height="120">
    "<a href=\"/search?ct=" + domain + "\"><img src=\"" + logo + "\" onerror=\"this.src='/assets/images/logo/default.png'\" width=\"20\" height=\"20\"></a>"
  }
  
  def getLinkTag(domain: String): String = {
    "<a href=\"/search?ct=" + domain + "\" class=\"titDomain\">" + domain + "</a>"
  }

  def getLogo(secondDomain: String, download: Boolean): String = {
    val logoUrl = Configure.LOGO_API_URL + secondDomain
    val path = Configure.LOGO_PATH + secondDomain + ".png"
    val logo = "/extassets/" + secondDomain + ".png"
    if (download) {
      if (!FileUtil.isExist(path)) {
        println("Download logo to " + path)
        Try(HttpUtil.download(logoUrl, path, Configure.PROXY_HOST, Configure.PROXY_PORT))
      }
    }
    if (FileUtil.isExist(path)) {
      logo
    } else {
      Configure.LOGO_DEFAULT
    }
  }


  def backgroupJob(f: => Unit, msg: String) {
    val thread = new Thread {
      override def run {
        val time0 = System.currentTimeMillis()
        println("Start " +  msg)
        //all.map(x => x.name).map(x => CommonService.getLogo(x, true))
        f
        val time1 = System.currentTimeMillis()
        println("End " +  msg + s" [${time1 -time0}]")
        
      }
    }
    thread.start()
  }
}