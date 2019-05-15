package model.device

import play.api.Play
import play.api.data.Form
import play.api.data.Forms._
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future
import slick.driver.JdbcProfile
import com.sksamuel.elastic4s.http.ElasticDsl._
import slick.driver.PostgresDriver.api._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import services.Configure
import services.domain.CommonService
import services.domain.CommonService.getAggregations
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval
import org.joda.time.DateTimeZone
import com.ftel.bigdata.utils.DateTimeUtil
import org.elasticsearch.search.sort.SortOrder
import service.BrasService.{client, getValueAsInt, getValueAsString}

object InfDAO {

  val client = Configure.client

  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  def getInfHostDailyResponse(host: String,nowDay: String): Array[(String,Int,Int,Int,Int,Int,Int,Int)] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))

    val res = client.execute(
      search(s"infra_dwh_inf_index_*" / "docs")
        query { must(termQuery("host.keyword",host),rangeQuery("date_time").gte(CommonService.formatYYmmddToUTC(nowDay.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(nowDay.split("/")(1))))) }
        aggregations (
        dateHistogramAggregation("daily")
          .field("date_time")
          .interval(DateHistogramInterval.DAY)
          .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
          .subaggs(
            sumAgg("sum0","sf_error"),
            sumAgg("sum1","lofi_error"),
            sumAgg("sum2","user_down"),
            sumAgg("sum3","inf_down"),
            sumAgg("sum4","rouge_error"),
            sumAgg("sum5","lost_signal"),
            sumAgg("sum6", "jumper_error")
          )
        )  size 1000
    ).await

    val arrHostDaily = CommonService.getAggregationsKeyStringAndMultiSum(res.aggregations.get("daily"))
    arrHostDaily.map(x=> (CommonService.formatUTC(x._1),x._2,x._3,x._4,x._5,x._6,x._7, x._8))

    /*dbConfig.db.run(
      sql"""select date_trunc('day' ,  date_time) as daily,sum(sf_error),sum(lofi_error),sum(user_down),sum(inf_down),sum(rouge_error),sum(lost_signal)
            from dwh_inf_index
            where host= $host and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            group by date_trunc('day' ,  date_time)
            order by daily
                  """
        .as[(String,Int,Int,Int,Int,Int,Int)])*/
  }

  def getNoOutlierInfByHost(host: String,nowDay: String): Int = {
    val response = client.execute(
      search(s"infra_dwh_inf_module_*" / "docs")
        query { must(termQuery("host.keyword",host),termQuery("label",1),rangeQuery("date_time").gte(CommonService.formatYYmmddToUTC(nowDay.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(nowDay.split("/")(1))))) }
        size 10000
    ).await
    response.hits.total

    /*dbConfig.db.run(
      sql"""select count(label)
            from dwh_inf_module
            where host= $host and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP and label =1
            group by host
                  """
        .as[(Int)])*/
  }

  def getNoOutlierInfByBras(bras: String,nowDay: String): Int = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))

    val rs = client.execute(
      search(s"infra_dwh_inf_module_*" / "docs")
        query { must(termQuery("bras_id.keyword",bras),termQuery("label",1),rangeQuery("date_time").gte(CommonService.formatYYmmddToUTC(nowDay.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(nowDay.split("/")(1))))) }
        size 1000
    ).await
    rs.totalHits
    /*dbConfig.db.run(
      sql"""select count(label)
            from dwh_inf_module
            where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP and label =1
            group by bras_id
                  """
        .as[(Int)])*/
  }

  def getSuyhaobyModule(host: String,nowDay: String): Future[Seq[(String,Double,Double,Double)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select 'Module ' || module as module, sum(passed_true),sum(passed_false),avg(rate)
            from dmt_portpon_suyhao
            where host= $host and date >= $fromDay::TIMESTAMP and date < $nextDay::TIMESTAMP
            group by module
                  """
        .as[(String,Double,Double,Double)])
  }

  def getErrorHostbyHourly(host: String,nowDay: String): Array[(Int,Int,Int,Int,Int,Int,Int,Int)] = {
    val res = client.execute(
      search(s"infra_dwh_inf_host_*" / "docs")
        query { must(termQuery("host.keyword",host),rangeQuery("date_time").gte(CommonService.formatYYmmddToUTC(nowDay.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(nowDay.split("/")(1))))) }
        aggregations (
        dateHistogramAggregation("hourly")
          .field("date_time")
          .interval(DateHistogramInterval.HOUR)
          .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
          .subaggs(
            sumAgg("sum0","user_down"),
            sumAgg("sum1","inf_down"),
            sumAgg("sum2","sf_error"),
            sumAgg("sum3","lofi_error"),
            sumAgg("sum4","rouge_error"),
            sumAgg("sum5","lost_signal"),
            sumAgg("sum6", "jumper_error")
          )
        )  size 1000
    ).await
    val arrHostHourly = CommonService.getAggregationsKeyStringAndMultiSum(res.aggregations.get("hourly")).map(x=> (CommonService.getHourFromES5(x._1),x._2,x._3,x._4,x._5,x._6,x._7,x._8))
    val errorRes = arrHostHourly.groupBy(_._1).map{case (k,v) => k -> (v.map(x=> x._2).sum,v.map(x=> x._3).sum,v.map(x=> x._4).sum,v.map(x=> x._5).sum,v.map(x=> x._6).sum,v.map(x=> x._7).sum,v.map(x=> x._7).sum)}
    errorRes.map(x=> (x._1,x._2._1,x._2._2,x._2._3,x._2._4,x._2._5,x._2._6,x._2._7)).toArray.sorted

   /* dbConfig.db.run(
      sql"""select  extract(hour from  date_time) as hourly,sum(user_down),sum(inf_down),sum(sf_error),sum(lofi_error),sum(rouge_error),sum(lost_signal)
            from dwh_inf_host
            where host= $host and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            group by  extract(hour from  date_time)
            order by hourly
                  """
        .as[(String,Int,Int,Int,Int,Int,Int)])*/
  }

  def getSigLogbyModuleIndex(host: String,day: String): Array[((String,String),String)] = {
    // cable.ontId: module, card.indexId: index
    val mulRes = client.execute(
      multi(
        search(s"radius-streaming-*" / "docs")
          query { must(termQuery("type", "con"),termQuery("card.olt",host),termQuery("typeLog", "SignIn"),rangeQuery("timestamp").gte(CommonService.formatYYmmddToUTC(day.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(day.split("/")(1))))) }
          aggregations(
          termsAggregation("module")
            .field("cable.ontId")
            .subAggregations(
              termsAggregation("index")
                .field("cable.indexId")
            )
          ),
        search(s"radius-streaming-*" / "docs")
          query { must(termQuery("type", "con"),termQuery("card.olt",host),termQuery("typeLog", "LogOff"),rangeQuery("timestamp").gte(CommonService.formatYYmmddToUTC(day.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(day.split("/")(1))))) }
          aggregations(
          termsAggregation("module")
            .field("cable.ontId")
            .subAggregations(
              termsAggregation("index")
                .field("cable.indexId")
            )
          )
      )
    ).await
    val mapSigin = CommonService.getSecondAggregations(mulRes.responses(0).aggregations.get("module"),"index")
    val mapLogoff = CommonService.getSecondAggregations(mulRes.responses(1).aggregations.get("module"),"index")

    val sig = mapSigin.flatMap(x => x._2.map(y => x._1 -> y))
      .map(x => (x._1, x._2._1) -> x._2._2)
    val log = mapLogoff.flatMap(x => x._2.map(y => x._1 -> y))
      .map(x => (x._1, x._2._1) -> x._2._2)
    (sig++log).groupBy(_._1).map{case (k,v) => k -> v.map(x=> x._2.toString).mkString("_")}.toArray
  }

  def getSiglogByHourly(host: String,day: String): SigLogByTime = {
    val mulRes = client.execute(
      multi(
        search(s"radius-streaming-*" / "docs")
          query { must(termQuery("type", "con"),termQuery("card.olt",host),termQuery("typeLog", "SignIn"),rangeQuery("timestamp").gte(CommonService.formatYYmmddToUTC(day.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(day.split("/")(1))))) }
          aggregations(
          dateHistogramAggregation("hourly")
            .field("timestamp")
            .interval(DateHistogramInterval.HOUR)
            .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
          ),
        search(s"radius-streaming-*" / "docs")
          query { must(termQuery("type", "con"),termQuery("card.olt",host),termQuery("typeLog", "LogOff"),rangeQuery("timestamp").gte(CommonService.formatYYmmddToUTC(day.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(day.split("/")(1))))) }
          aggregations(
          dateHistogramAggregation("hourly")
            .field("timestamp")
            .interval(DateHistogramInterval.HOUR)
            .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
          )
      )
    ).await
    val arrSigin = CommonService.getAggregationsSiglog(mulRes.responses(0).aggregations.get("hourly")).map(x=> CommonService.getHoursFromMiliseconds(x._1.toLong)-> x._2).groupBy(_._1).mapValues(_.map(_._2).sum).toSeq.sortBy(_._1).map(x=>x._2).toArray
    val arrLogoff = CommonService.getAggregationsSiglog(mulRes.responses(1).aggregations.get("hourly")).map(x=> CommonService.getHoursFromMiliseconds(x._1.toLong)-> x._2).groupBy(_._1).mapValues(_.map(_._2).sum).toSeq.sortBy(_._1).map(x=>x._2).toArray
    SigLogByTime(arrSigin,arrLogoff)
  }

  def getSplitterByHost(host: String,nowDay: String): Future[Seq[(String,String,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select date_time,splitter, sum(lost_signal)
            from dwh_inf_splitter
            where host= $host and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            group by date_time,splitter
            order by date_time desc
                  """
        .as[(String,String,Int)])
  }

  def getErrorTableModuleIndex(host: String,nowDay: String): Array[(String,Int,Int)] = {
    val response = client.execute(
      search(s"infra_dwh_inf_index_*" / "docs")
        query { must(termQuery("host.keyword",host),rangeQuery("date_time").gte(CommonService.formatYYmmddToUTC(nowDay.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(nowDay.split("/")(1))))) }
        aggregations(
        termsAggregation("module")
          .field("module.keyword")
          .subAggregations(
            termsAggregation("index")
              .field("index")
              .subaggs(
                sumAgg("sum0","user_down"),
                sumAgg("sum1","inf_down"),
                sumAgg("sum2","sf_error"),
                sumAgg("sum3","lofi_error"),
                sumAgg("sum4","rouge_error"),
                sumAgg("sum5","lost_signal")
              ) size 1000
          )) size 0
    ).await
    val mapModule = CommonService.getSecondAggregationsAndSumInfError(response.aggregations.get("module"),"index")
    val rsModuleInex = mapModule.flatMap(x => x._2.map(y => x._1 -> y))
      .map(x => (x._1, x._2._1.toInt, (x._2._2+x._2._3+x._2._4+x._2._5+x._2._6+x._2._7).toInt))
    rsModuleInex

    /*dbConfig.db.run(
      sql"""select module ,index,sum(user_down)+sum(inf_down)+sum(sf_error)+sum(lofi_error)+sum(rouge_error)+sum(lost_signal) as sumS
            from dwh_inf_index
            where host= $host and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            group by module,index
                  """
        .as[(String,Int,Int)])*/
  }

  def getPortPonDown(host: String,nowDay: String): Future[Seq[(String,String,String)]] = {
   /* val response = client.execute(
      search(s"infra_dwh_inf_index_*" / "docs")
        query { must(termQuery("host.keyword",host),rangeQuery("date_time").gte(CommonService.formatYYmmddToUTC(nowDay.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(nowDay.split("/")(1))))) }
        aggregations(
        termsAggregation("module")
          .field("module")
          .subAggregations(
            termsAggregation("index")
              .field("index")
              .subaggs(
                sumAgg("sum0","sf_error"),
                sumAgg("sum1","sign_in"),
                sumAgg("sum2","log_off")
              ) size 1000
          ))
    ).await

    val mapContractSf = CommonService.getSecondAggregationsAndSumContractSf(response.aggregations.get("module"),"index")
    val rsContract = mapContractSf.flatMap(x => x._2.map(y => x._1 -> y))
      .map(x => (x._1, x._2._1.toInt, x._2._2.toInt,x._2._3.toInt,x._2._4.toInt)).filter(x=> x._3>300)
    rsContract*/
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select date_time, module,result
            from dwh_inf_port_pon
            where host= $host and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            order by date_time desc
                  """
        .as[(String,String,String)])
  }

  def getTicketOutlierByHost(host: String,nowDay: String): Future[Seq[(String,String)]] = {
    val hostName = s"%$host%"
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select tb.* from
            ((select 'ticket' device_type, created_date as time from dwh_ticket
              where device_name like $hostName AND created_date >= $fromDay::TIMESTAMP AND created_date < $nextDay::TIMESTAMP)
            union all
             (select 'outlier' device_type, date_time as time from dwh_inf_module
              where host= $host AND date_time >= $fromDay::TIMESTAMP AND date_time < $nextDay::TIMESTAMP AND label =1)
             ) as tb
             order by tb.time desc
                  """
        .as[(String,String)])
  }

}