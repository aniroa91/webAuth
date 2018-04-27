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
import profile.services.internet.HistoryService.client
import services.Configure
import services.domain.CommonService
import services.domain.CommonService.getAggregations
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval
import org.joda.time.DateTimeZone
import com.ftel.bigdata.utils.DateTimeUtil
import org.elasticsearch.search.sort.SortOrder
import service.BrasService.client

object InfDAO {

  val client = Configure.client
  val client_kibana = Configure.client_kibana

  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  def getInfHostDailyResponse(host: String,nowDay: String): Future[Seq[(String,Int,Int,Int,Int,Int,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select date_trunc('day' ,  date_time) as daily,sum(user_down),sum(inf_down),sum(sf_error),sum(lofi_error),sum(rouge_error),sum(lost_signal)
            from dwh_inf_index
            where host= $host and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            group by date_trunc('day' ,  date_time)
            order by daily
                  """
        .as[(String,Int,Int,Int,Int,Int,Int)])
  }

  def getSuyhaobyModule(host: String,nowDay: String): Future[Seq[(String,Double,Double,Double)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select module, sum(passed_true),sum(passed_false),sum(rate)
            from dmt_portpon_suyhao
            where host= $host and date >= $fromDay::TIMESTAMP and date < $nextDay::TIMESTAMP
            group by module
                  """
        .as[(String,Double,Double,Double)])
  }

  def getErrorHostbyHourly(host: String,nowDay: String): Future[Seq[(String,Int,Int,Int,Int,Int,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select  extract(hour from  date_time) as hourly,sum(user_down),sum(inf_down),sum(sf_error),sum(lofi_error),sum(rouge_error),sum(lost_signal)
            from dwh_inf_host
            where host= $host and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            group by  extract(hour from  date_time)
            order by hourly
                  """
        .as[(String,Int,Int,Int,Int,Int,Int)])
  }

  def getSigLogbyModuleIndex(host: String,day: String): Array[((String,String),String)] = {
    // cable.ontId: module, card.indexId: index
    val mulRes = client.execute(
      multi(
        search(s"radius-streaming-*" / "con")
          query { must(termQuery("card.olt",host),termQuery("typeLog", "SignIn"),rangeQuery("timestamp").gte(CommonService.formatYYmmddToUTC(day.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(day.split("/")(1))))) }
          aggregations(
          termsAggregation("module")
            .field("cable.ontId")
            .subAggregations(
              termsAggregation("index")
                .field("cable.indexId")
            )
          ),
        search(s"radius-streaming-*" / "con")
          query { must(termQuery("card.olt",host),termQuery("typeLog", "LogOff"),rangeQuery("timestamp").gte(CommonService.formatYYmmddToUTC(day.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(day.split("/")(1))))) }
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
        search(s"radius-streaming-*" / "con")
          query { must(termQuery("card.olt",host),termQuery("typeLog", "SignIn"),rangeQuery("timestamp").gte(CommonService.formatYYmmddToUTC(day.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(day.split("/")(1))))) }
          aggregations(
          dateHistogramAggregation("hourly")
            .field("timestamp")
            .interval(DateHistogramInterval.HOUR)
            .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
          ),
        search(s"radius-streaming-*" / "con")
          query { must(termQuery("card.olt",host),termQuery("typeLog", "LogOff"),rangeQuery("timestamp").gte(CommonService.formatYYmmddToUTC(day.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(day.split("/")(1))))) }
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

  def getSplitterByHost(host: String,nowDay: String): Future[Seq[(String,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select splitter, sum(lost_signal)
            from dwh_inf_splitter
            where host= $host and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            group by splitter
                  """
        .as[(String,Int)])
  }

  def getErrorTableModuleIndex(host: String,nowDay: String): Future[Seq[(String,Int,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select module ,index,sum(user_down)+sum(inf_down)+sum(sf_error)+sum(lofi_error)+sum(rouge_error)+sum(lost_signal) as sumS
            from dwh_inf_index
            where host= $host and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            group by module,index
                  """
        .as[(String,Int,Int)])
  }

  def getContractwithSf(host: String,nowDay: String): Future[Seq[(String,Int,Int,Int,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select module,index,sum(sf_error) as sf_error,0 as sigin,0 as logoff
            from dwh_inf_index
            where host= $host and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            group by module,index
            having sum(sf_error)>0
                  """
        .as[(String,Int,Int,Int,Int)])
  }

}