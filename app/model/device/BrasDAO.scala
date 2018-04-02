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

object BrasDAO {

  val client = Configure.client

  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  def getSigLogResponse(bras: String,fromDay: String,nextDay: String): Future[Seq[(Int,Int)]] = {
    //val sumDay = CommonService.getRangeDay(nowDay).split(",").length
    dbConfig.db.run(
      sql"""select sum(signin),sum(logoff)
            from dwh_conn_bras
            where bras_id=$bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            group by bras_id
                  """
        .as[(Int,Int)])
  }
  def getSigLogCurrent(bras: String, nowDay: String): (Int,Int) = {
    val multiRs = client.execute(
      multi(
        search(s"radius-streaming-$nowDay" / "con")
          query { must(termQuery("typeLog", "SignIn"),termQuery("nasName",bras.toLowerCase)) },
        search(s"radius-streaming-$nowDay" / "con")
          query { must(termQuery("typeLog", "LogOff"),termQuery("nasName",bras.toLowerCase)) }
      )
    ).await
    val signin = multiRs.responses(0).totalHits.toInt
    val logoff = multiRs.responses(0).totalHits.toInt
    (signin,logoff)
  }

  def getNoOutlierResponse(bras: String,nowDay: String): Future[Seq[(Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = nowDay.split("/")(1)
   // val sumDay = CommonService.getRangeDay(nowDay).split(",").length
    dbConfig.db.run(
      sql"""select count(*)
            from dwh_conn_bras_detail
            where bras_id=$bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP and label = 'outlier'
            group by bras_id
                  """
        .as[(Int)])
  }
  def getNoOutlierCurrent(bras: String,nowDay: String) : Int = {
    val response = client.execute(
        search(s"monitor-radius-$nowDay" / "docs")
          query { must(termQuery("bras_id", bras),termQuery("label","outlier")) }
    ).await
    response.totalHits.toInt
  }

  def getOpviewBytimeResponse(bras: String,nowDay: String,hourly: Int): Future[Seq[(Int,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select extract(hour from  date_time) as hourly, count(service_name)
            from dwh_opsview
            where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            group by bras_id,extract(hour from  date_time)
            order by hourly
                  """
        .as[(Int,Int)])
  }

  def getKibanaBytimeResponse(bras: String,nowDay: String,hourly:Int): Future[Seq[(Int,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select extract(hour from  date_time) as hourly, count(error_name)
            from dwh_kibana
            where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            group by bras_id,extract(hour from  date_time)
            order by hourly
                  """
        .as[(Int,Int)])
  }

  def getSigLogBytimeResponse(bras: String,nowDay: String,hourly:Int): Future[Seq[(Int,Int,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select extract(hour from  date_time) as hourly, sum(signin),sum(logoff)
            from dwh_conn_bras
            where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            group by bras_id,extract(hour from  date_time)
            having extract(hour from  date_time) = $hourly
                  """
        .as[(Int,Int,Int)])
  }
  def getSigLogBytimeCurrent(bras: String, nowDay: String): SigLogByTime = {
    val mulRes = client.execute(
      multi(
        search(s"radius-streaming-$nowDay" / "con")
          query { must(termQuery("nasName",bras.toLowerCase),termQuery("typeLog", "SignIn")) }
          aggregations (
            dateHistogramAggregation("hourly")
            .field("timestamp")
            .interval(DateHistogramInterval.HOUR)
            .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
          ),
        search(s"radius-streaming-$nowDay" / "con")
          query { must(termQuery("nasName",bras.toLowerCase),termQuery("typeLog", "LogOff")) }
          aggregations (
          dateHistogramAggregation("hourly")
            .field("timestamp")
            .interval(DateHistogramInterval.HOUR)
            .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
          )
      )
    ).await
/*    println(client.show(
      search(s"radius-streaming-$nowDay" / "con")
        query { must(termQuery("nasName",bras.toLowerCase),termQuery("typeLog", "SignIn")) }
        aggregations (
        dateHistogramAggregation("hourly")
          .field("timestamp")
          .interval(DateHistogramInterval.HOUR)
          .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
        )
    ))*/
    val arrSigin = CommonService.getAggregationsSiglog(mulRes.responses(0).aggregations.get("hourly")).map(x=>x._2.toInt)
    val arrLogoff = CommonService.getAggregationsSiglog(mulRes.responses(1).aggregations.get("hourly")).map(x=>x._2.toInt)
    SigLogByTime(arrSigin,arrLogoff)
  }

  def getInfErrorBytimeResponse(bras: String,nowDay: String,hourly:Int): Future[Seq[(Int,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select extract(hour from  date_time) as hourly, sum(cpe_error)+sum(lostip_error) as sumError
            from dwh_inf_host
            where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            group by bras_id,extract(hour from  date_time)
            order by hourly
                  """
        .as[(Int,Int)])
  }

  def getInfhostResponse(bras: String,nowDay: String): Future[Seq[(String,Int,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select host, sum(cpe_error) as cpe,sum(lostip_error) as lostip
            from dwh_inf_host
            where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            group by host
                  """
        .as[(String,Int,Int)])
  }

  def getInfModuleResponse(bras: String,nowDay: String): Future[Seq[(String,String,Int,Int,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select host, module,sum(cpe_error) as cpe,sum(lostip_error) as lostip,sum(cpe_error)+sum(lostip_error) as sumAll
            from dwh_inf_module
            where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            group by host,module
                  """
        .as[(String,String,Int,Int,Int)])
  }

  def getOpsviewServiceNameResponse(bras: String,nowDay: String): Future[Seq[(String,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select service_name,count(*) from dwh_opsview
             where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
             group by service_name
                  """
        .as[(String,Int)])
  }

  def getOpsviewServiceSttResponse(bras: String,nowDay: String): Future[Seq[(String,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select service_status,count(*) from dwh_opsview
             where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
             group by service_status
                  """
        .as[(String,Int)])
  }

  def getOpServByStatusResponse(bras: String,nowDay: String): Future[Seq[(String,String,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select service_name,service_status,count(*) from dwh_opsview
             where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
             group by service_name,service_status
                  """
        .as[(String,String,Int)])
  }

  def getLinecardhostResponse(bras: String,nowDay: String): Future[Seq[(String,Int,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select linecard || '_' || card || '_' || port as linecard_card_port,sum(signin) as signin,
            sum(logoff) as logoff
            from dwh_conn_port
            where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            group by linecard, card, port
                  """
        .as[(String,Int,Int)])
  }
  def getLinecardhostCurrent(bras: String,nowDay: String): Array[(String,String)] = {
    val multiRs = client.execute(
      multi(
       search(s"radius-streaming-$nowDay" / "con")
        query { must(termQuery("nasName", bras.toLowerCase),termQuery("typeLog", "SignIn")) }
        aggregations (
        termsAggregation("linecard")
          .field("card.lineId")
          .subAggregations(
            termsAggregation("card")
              .field("card.id")
              .subAggregations(
                termsAggregation("port")
                  .field("card.port")
              )
          )
        ),
        search(s"radius-streaming-$nowDay" / "con")
          query { must(termQuery("nasName", bras.toLowerCase),termQuery("typeLog", "LogOff")) }
          aggregations (
          termsAggregation("linecard")
            .field("card.lineId")
            .subAggregations(
              termsAggregation("card")
                .field("card.id")
                .subAggregations(
                  termsAggregation("port")
                    .field("card.port")
                )
            )
          )
      )
    ).await

    val mapSigin = CommonService.getMultiAggregations(multiRs.responses(0).aggregations.get("linecard"))
    val mapLogoff = CommonService.getMultiAggregations(multiRs.responses(1).aggregations.get("linecard"))

    val arrSigin =  mapSigin.flatMap(x => x._2.map(y => x._1 -> y))
      .map(x => (x._1 + "_" + x._2._1) -> x._2._2)
      .flatMap(x => x._2.map(y => x._1 -> y))
      .map(x => (x._1 + "_" + x._2._1) -> x._2._2)

    val arrLogoff = mapLogoff.flatMap(x => x._2.map(y => x._1 -> y))
      .map(x => (x._1 + "_" + x._2._1) -> x._2._2)
      .flatMap(x => x._2.map(y => x._1 -> y))
      .map(x => (x._1 + "_" + x._2._1) -> x._2._2)

    (arrSigin++arrLogoff).groupBy(_._1).map{case (k,v) => k -> v.map(x=> x._2.toString).mkString("-")}.toArray
  }

  def getErrorSeverityResponse(bras: String,nowDay: String): Future[Seq[(String,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select severity, count(error_name) from dwh_kibana
            where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            group by severity
                  """
        .as[(String,Int)])
  }

  def getErrorTypeResponse(bras: String,nowDay: String): Future[Seq[(String,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select error_type, count(error_name) from dwh_kibana
            where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            group by error_type
                  """
        .as[(String,Int)])
  }

  def getFacilityResponse(bras: String,nowDay: String): Future[Seq[(String,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select facility, count(error_name) from dwh_kibana
            where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            group by facility
                  """
        .as[(String,Int)])
  }

  def getDdosResponse(bras: String,nowDay: String): Future[Seq[(String,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select error_name, sum(value) from dwh_kibana
            where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP and error_type='ddos'
            group by error_name
                  """
        .as[(String,Int)])
  }

  def getSeveValueResponse(bras: String,nowDay: String): Future[Seq[(String,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select severity, sum(value) from dwh_kibana
            where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            group by severity
                  """
        .as[(String,Int)])
  }

  def getSigLogByHost(bras: String,nowDay: String): SigLogByHost = {
    val arrDay = CommonService.getRangeDay(nowDay).split(",")
    var streamingIndex = s"radius-streaming-"+arrDay(0)
    for(i <- 1 until arrDay.length) {
      streamingIndex += s",radius-streaming-"+arrDay(i)
    }
    val multiRs = client.execute(
      multi(
        search(s"$streamingIndex" / "con")
          query { must(termQuery("nasName", bras.toLowerCase),termQuery("typeLog", "SignIn")) }
          aggregations (
          termsAggregation("olt")
            .field("card.olt")
          ),
        search(s"$streamingIndex" / "con")
          query { must(termQuery("nasName", bras.toLowerCase),termQuery("typeLog", "LogOff")) }
          aggregations (
          termsAggregation("olt")
            .field("card.olt")
          )
      )
    ).await
    if(multiRs.responses(0).hits != null && multiRs.responses(1) != null) {
      val arrSigin = CommonService.getAggregationsSiglog(multiRs.responses(0).aggregations.get("olt"))
      val arrLogoff = CommonService.getAggregationsSiglog(multiRs.responses(1).aggregations.get("olt"))
      //val rs = (arrSigin++arrLogoff).groupBy(_._1).map{case (k,v) => k -> v.map(x=> x._2.toString).mkString("-")}.filter(x=> x._1 != "").filter(x=> x._1 != "N/A").toArray
      val cates = (arrSigin ++ arrLogoff).groupBy(_._1).filter(x => x._1 != "").filter(x => x._1 != "N/A").map(x => x._1).toArray
      val sigByHost = cates.map(x => CommonService.getLongValueByKey(arrSigin, x))
      val logByHost = cates.map(x => CommonService.getLongValueByKey(arrLogoff, x))
      SigLogByHost(cates, sigByHost, logByHost)
    }
    else null
  }

}