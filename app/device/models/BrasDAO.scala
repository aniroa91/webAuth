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
import services.domain.CommonService.{formatUTC, getAggregations}
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval
import org.joda.time.DateTimeZone
import com.ftel.bigdata.utils.DateTimeUtil
import org.elasticsearch.search.sort.SortOrder
import service.BrasService.{client, getValueAsInt, getValueAsString}

object BrasDAO {

  val client = Configure.client
  val client_kibana = Configure.client_kibana
  val monthSize = CommonService.monthSize
  val topN = CommonService.SIZE_DEFAULT
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  def getSpliterMudule(userInf_down: String): Future[Seq[(String,String,String,Int)]] = {
    val dt = new DateTime();
    val aDay = dt.minusHours(12);
    val aDayTime = aDay.toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS"))
    dbConfig.db.run(
      sql"""select date_time,host,splitter,sum(lost_signal)
            from dwh_inf_splitter
            where date_time >= $aDayTime::TIMESTAMP AND lost_signal>0
            group by date_time,host,splitter
            order by date_time desc
                  """
        .as[(String,String,String,Int)])
  }

  def confirmLabelInf(host: String,module: String,time: String,bras: String) = {
    dbConfig.db.run(
      sqlu"""UPDATE dwh_inf_module SET confirm = true
            where host =$host and module=$module and bras_id=$bras and date_time=$time::TIMESTAMP
                  """
    )
  }

  def rejectLabelInf(host: String,module: String,time: String,bras: String) = {
    dbConfig.db.run(
      sqlu"""UPDATE dwh_inf_module SET confirm = false
             where host =$host and module=$module and bras_id=$bras and date_time=$time::TIMESTAMP
                  """
    )
  }

  def getSigLogByRegion(month: String): Future[Seq[(String,String,Int,Int)]] = {
    if(month.equals("")) {
      val month = CommonService.get3MonthAgo()+"-01"
      dbConfig.db.run(
        sql"""select month,province,sum(signin) as signin, sum(logoff) as logoff
              from dmt_overview_conn
              where month > $month::TIMESTAMP
              group by month,province
              order by month desc
                  """
          .as[(String,String,Int,Int)])
    } else{
      val query = month + "-01"
      dbConfig.db.run(
        sql"""select month,province,sum(signin) as signin, sum(logoff) as logoff
              from dmt_overview_conn
              where month = $query::TIMESTAMP
              group by month,province
              order by month desc
                  """
          .as[(String,String,Int,Int)])
    }
  }

  def getSflofiMudule(queries: String): Future[Seq[(String,String,String,Int,Int,Boolean,String)]] = {
    val dt = new DateTime();
    val aDay = dt.minusHours(12);
    val aDayTime = aDay.toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS"))
    dbConfig.db.run(
      sql"""select date_time,host,module,sf_error,lofi_error,confirm,bras_id
            from dwh_inf_module
            where date_time >= $aDayTime::TIMESTAMP AND label =1
            order by date_time desc
                  """
        .as[(String,String,String,Int,Int,Boolean,String)])
  }

  def getIndexRougeMudule(userInf_down: String): Future[Seq[(String,String,String,String,Int)]] = {
    val dt = CommonService.getCurrentDay()
    dbConfig.db.run(
      sql"""select date_time,host,module,index,sum(rouge_error)
            from dwh_inf_index
            where date_time >= $dt::TIMESTAMP AND rouge_error >0
            group by date_time,host,module,index
            order by date_time desc
                  """
        .as[(String,String,String,String,Int)])
  }

  def getUserDownMudule(userInf_down: String): Future[Seq[(String,String,String,Int)]] = {
    val dt = new DateTime();
    val aDay = dt.minusHours(12);
    val aDayTime = aDay.toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS"))
    dbConfig.db.run(
      sql"""select date_time,host,module,sum(user_down)
            from dwh_inf_module
            where date_time >= $aDayTime::TIMESTAMP AND user_down>0
            group by date_time,host,module
            order by date_time desc
                  """
        .as[(String,String,String,Int)])
  }

  def getInfDownMudule(userInf_down: String): Future[Seq[(String,String,String,Int)]] = {
    val dt = new DateTime();
    val aDay = dt.minusHours(12);
    val aDayTime = aDay.toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS"))
    dbConfig.db.run(
      sql"""select date_time,host,module,sum(inf_down)
            from dwh_inf_module
            where date_time >= $aDayTime::TIMESTAMP AND inf_down>0
            group by date_time,host,module
            order by date_time desc
                  """
        .as[(String,String,String,Int)])
  }

  def getProvinceOpsview(): Future[Seq[(String,String,String,Int)]] = {
    val month = CommonService.get3MonthAgo()+"-01"
    dbConfig.db.run(
      sql"""select month,province,bras_id,sum(total_opsview) as total_opsview
            from dmt_overview_noc
            where month > $month::TIMESTAMP
            group by month,province,bras_id
            order by month desc, province
                  """
        .as[(String,String,String,Int)])
  }

  def getProvinceContract(): Future[Seq[(String,String,String,Int,Int,Int)]] = {
    val month = CommonService.get3MonthAgo()+"-01"
    dbConfig.db.run(
      sql"""select month,province,host,sum(no_contract) as no_contract,sum(no_device) as no_device,sum(poor_conn) as poor_conn
            from dmt_overview_device
            where month > $month::TIMESTAMP
            group by month,province,host
            order by month desc, province
                  """
        .as[(String,String,String,Int,Int,Int)])
  }

  def getProvinceKibana(): Future[Seq[(String,String,String,Int)]] = {
    val month = CommonService.get3MonthAgo()+"-01"
    dbConfig.db.run(
      sql"""select month,province,bras_id,sum(total_kibana) as total_kibana
            from dmt_overview_noc
            where month > $month::TIMESTAMP
            group by month,province,bras_id
            order by month desc, province
                  """
        .as[(String,String,String,Int)])
  }

  def getProvinceSigLogoff(): Future[Seq[(String,String,Double,Double)]] = {
    val month = CommonService.get3MonthAgo()+"-01"
    dbConfig.db.run(
      sql"""select month,province,sum(signin) as signin,sum(logoff) as logoff
            from dmt_overview_conn
            where month > $month::TIMESTAMP
            group by month,province
            order by month desc
                  """
        .as[(String,String,Double,Double)])
  }

  def getProvinceTotalInf(): Future[Seq[(String,String,Double)]] = {
    val month = CommonService.get3MonthAgo()+"-01"
    dbConfig.db.run(
      sql"""select month,province,sum(total_inf) as total_inf
            from dmt_overview_inf
            where month > $month::TIMESTAMP
            group by month,province
            order by month desc
                  """
        .as[(String,String,Double)])
  }

  def getTotalInfbyProvince(id: String): Future[Seq[(String,String,Double)]] = {
    val month = CommonService.get3MonthAgo()+"-01"
    val aMonthago = CommonService.getnumMonthAgo(0)
    val twoMonthago = CommonService.getnumMonthAgo(1)
    dbConfig.db.run(
      sql"""select month, bras_id,sum(total_inf) as total_inf
            from dmt_overview_inf
            where month > $month::TIMESTAMP and province = $id and bras_id IN(
            select tb1.bras_id
            from (
                (select bras_id
                        from dmt_overview_inf
                        where month= $aMonthago::TIMESTAMP and province = $id
                        group by bras_id) tb1
                 join (
                        select bras_id
                        from dmt_overview_inf
                        where month= $twoMonthago::TIMESTAMP and province = $id
                        group by bras_id ) tb2
                        on tb1.bras_id=tb2.bras_id
            )
            )
            group by month,bras_id
            order by month desc
                  """
        .as[(String,String,Double)])
  }

  def getSigLogconnbyProvince(id: String): Future[Seq[(String,String,Double,Double)]] = {
    val month = CommonService.get3MonthAgo()+"-01"
    dbConfig.db.run(
      sql"""select month,bras_id,sum(signin) as signin,sum(logoff) as logoff
            from dmt_overview_conn
            where month > $month::TIMESTAMP and province = $id
            group by month,bras_id
            order by month desc
            limit 30
                  """
        .as[(String,String,Double,Double)])
  }

  def getSigLogconnbyBras(id: String): Future[Seq[(String,String,Double,Double)]] = {
    val month = CommonService.get3MonthAgo()+"-01"
    dbConfig.db.run(
      sql"""select tbO.month,tbO.host,sum(tbO.signin),sum(tbO.logoff)
                                   from(
                                      select row_number() OVER (PARTITION BY conn.month ORDER BY conn.signin desc) AS r,
                                       conn.*
                                       from (
                                           select month,host,sum(signin) as signin,sum(logoff) as logoff
                                           from dmt_overview_conn
                                           where month > $month::TIMESTAMP and bras_id = $id
                                           group by month,host
                                           order by month desc
                                       ) conn
                                   ) tbO
                                   where tbO.r<=10
                                   group by tbO.month,tbO.host
                                   order by tbO.month desc
                                limit 30
                  """
        .as[(String,String,Double,Double)])
  }

  def getTotalInfbyBras(id: String): Future[Seq[(String,String,Double)]] = {
    val month = CommonService.get3MonthAgo()+"-01"
    val aMonthago = CommonService.getnumMonthAgo(0)
    val twoMonthago = CommonService.getnumMonthAgo(1)
    dbConfig.db.run(
      sql"""select month, host,sum(total_inf) as total_inf
            from dmt_overview_inf
            where month > $month::TIMESTAMP and bras_id = $id and host IN(
            select tb1.host
            from (
                (select host
                        from dmt_overview_inf
                        where month= $aMonthago::TIMESTAMP and bras_id = $id
                        group by host order by sum(total_inf) desc) tb1
                 join (
                        select host
                        from dmt_overview_inf
                        where month= $twoMonthago::TIMESTAMP and bras_id = $id
                        group by host ) tb2
                        on tb1.host=tb2.host
            )
            limit 10
            )
            group by month,host
            order by month desc,sum(total_inf)  desc
                  """
        .as[(String,String,Double)])
  }

  def getProvinceSuyhao(): Future[Seq[(String,String,String,Int)]] = {
    val month = CommonService.get3MonthAgo()+"-01"
    dbConfig.db.run(
      sql"""select month,province,host,sum(not_passed_suyhao) as not_passed_suyhao
            from dmt_overview_suyhao
            where month > $month::TIMESTAMP
            group by month,province,host
            order by month desc, province
                  """
        .as[(String,String,String,Int)])
  }

  def getProvinceCount(month: String): Future[Seq[(String,String,Int,Int,Int,Int,Int,Int)]] = {
    if(month.equals("")) {
      val month = CommonService.get3MonthAgo()+"-01"
      dbConfig.db.run(
        sql"""select province,bras_id,sum(alert_count) as alert_count,sum(crit_count) as crit_count,sum(warning_count) as warning_count,
                      sum(notice_count) as notice_count, sum(err_count) as err_count,sum(emerg_count) as emerg_count
              from dmt_overview_noc
              where month > $month::TIMESTAMP
              group by province,bras_id
              order by province
                  """
          .as[(String,String,Int,Int,Int,Int,Int,Int)])
    } else{
      val query = month + "-01"
      dbConfig.db.run(
        sql"""select province,bras_id,sum(alert_count) as alert_count,sum(crit_count) as crit_count,sum(warning_count) as warning_count,
                      sum(notice_count) as notice_count, sum(err_count) as err_count,sum(emerg_count) as emerg_count
              from dmt_overview_noc
              where month = $query::TIMESTAMP
              group by province,bras_id
              order by province
                  """
          .as[(String,String,Int,Int,Int,Int,Int,Int)])
    }
  }

  def getProvinceOpsviewType(month: String): Future[Seq[(String,String,Int,Int,Int,Int)]] = {
    if(month.equals("")) {
      val month = CommonService.get3MonthAgo()+"-01"
      dbConfig.db.run(
        sql"""select month,province,sum(ok_opsview) as ok_opsview,sum(warn_opsview) as warn_opsview,sum(unknown_opsview) as unknown_opsview,
                      sum(crit_opsview) as crit_opsview
              from dmt_overview_noc
              where month > $month::TIMESTAMP
              group by month,province
              order by month desc
                  """
          .as[(String,String,Int,Int,Int,Int)])
    } else{
      val query = month + "-01"
      dbConfig.db.run(
        sql"""select month,province,sum(ok_opsview) as ok_opsview,sum(warn_opsview) as warn_opsview,sum(unknown_opsview) as unknown_opsview,
                      sum(crit_opsview) as crit_opsview
              from dmt_overview_noc
              where month = $query::TIMESTAMP
              group by month,province
              order by month desc
                  """
          .as[(String,String,Int,Int,Int,Int)])
    }
  }

  def getProvinceInfDownError(month: String): Future[Seq[(String,String,Int,Int,Int,Int)]] = {
    if(month.equals("")) {
      val month = CommonService.get3MonthAgo()+"-01"
      dbConfig.db.run(
        sql"""select province,bras_id,sum(inf_down) as inf_down,sum(user_down) as user_down,sum(rouge_error) as rouge_error,
                                   sum(lost_signal) as lost_signal
                           from dmt_overview_inf
                           where month > $month::TIMESTAMP
                           group by province,bras_id
              order by province,bras_id
                  """
          .as[(String,String,Int,Int,Int,Int)])
    } else{
      val query = month + "-01"
      dbConfig.db.run(
        sql"""select province,bras_id,sum(inf_down) as inf_down,sum(user_down) as user_down,sum(rouge_error) as rouge_error,
                                   sum(lost_signal) as lost_signal
                           from dmt_overview_inf
                           where month =  $query::TIMESTAMP
                           group by province,bras_id
              order by province,bras_id
                  """
          .as[(String,String,Int,Int,Int,Int)])
    }
  }

  def getTopSignin(month: String): Future[Seq[(String,String,Int)]] = {
    if(month.equals("")) {
      val month = CommonService.get3MonthAgo()+"-01"
      dbConfig.db.run(
        sql"""select tbO.bras_id,tbO.host,sum(tbO.signin)
                        from(
                           select row_number() OVER (PARTITION BY conn.bras_id ORDER BY conn.signin desc) AS r,
                            conn.*
                            from (
                                 select bras_id,host,sum(signin) as signin
                                 from dmt_overview_conn
                                 where month > $month::TIMESTAMP
                                 group by bras_id,host
                                 order by sum(signin) desc
                            ) conn
                        ) tbO
                        where tbO.r<=10
                        group by tbO.bras_id,tbO.host
                        order by tbO.bras_id desc
                     limit 100
                  """
          .as[(String,String,Int)])
    } else{
      val query = month + "-01"
      dbConfig.db.run(
        sql"""select tbO.bras_id,tbO.host,sum(tbO.signin)
                        from(
                           select row_number() OVER (PARTITION BY conn.bras_id ORDER BY conn.signin desc) AS r,
                            conn.*
                            from (
                                 select bras_id,host,sum(signin) as signin
                                 from dmt_overview_conn
                                 where month = $query::TIMESTAMP
                                 group by bras_id,host
                                 order by sum(signin) desc
                            ) conn
                        ) tbO
                        where tbO.r<=10
                        group by tbO.bras_id,tbO.host
                        order by tbO.bras_id desc
                     limit 100
                  """
          .as[(String,String,Int)])
    }
  }

  def getTopLogoff(month: String): Future[Seq[(String,String,Int)]] = {
    if(month.equals("")) {
      val month = CommonService.get3MonthAgo()+"-01"
      dbConfig.db.run(
        sql"""select tbO.bras_id,tbO.host,sum(tbO.logoff)
                        from(
                           select row_number() OVER (PARTITION BY conn.bras_id ORDER BY conn.logoff desc) AS r,
                            conn.*
                            from (
                                 select bras_id,host,sum(logoff) as logoff
                                 from dmt_overview_conn
                                 where month > $month::TIMESTAMP
                                 group by bras_id,host
                                 order by sum(logoff) desc
                            ) conn
                        ) tbO
                        where tbO.r<=10
                        group by tbO.bras_id,tbO.host
                        order by tbO.bras_id desc
                     limit 100
                  """
          .as[(String,String,Int)])
    } else{
      val query = month + "-01"
      dbConfig.db.run(
        sql"""select tbO.bras_id,tbO.host,sum(tbO.logoff)
                        from(
                           select row_number() OVER (PARTITION BY conn.bras_id ORDER BY conn.logoff desc) AS r,
                            conn.*
                            from (
                                 select bras_id,host,sum(logoff) as logoff
                                 from dmt_overview_conn
                                 where month = $query::TIMESTAMP
                                 group by bras_id,host
                                 order by sum(logoff) desc
                            ) conn
                        ) tbO
                        where tbO.r<=10
                        group by tbO.bras_id,tbO.host
                        order by tbO.bras_id desc
                     limit 100
                  """
          .as[(String,String,Int)])
    }
  }

  def getTopKibana(month: String): Future[Seq[(String,Int)]] = {
    if(month.equals("")) {
      val month = CommonService.get3MonthAgo()+"-01"
      dbConfig.db.run(
        sql"""select bras_id,sum(total_kibana)
            from dmt_overview_noc
            where month > $month::TIMESTAMP
            group by bras_id
            order by sum(total_kibana) desc
            limit 10
                  """
          .as[(String,Int)])
    } else{
      val query = month + "-01"
      dbConfig.db.run(
        sql"""select bras_id,sum(total_kibana)
            from dmt_overview_noc
            where month = $query::TIMESTAMP
            group by bras_id
            order by sum(total_kibana) desc
            limit 10
                  """
          .as[(String,Int)])
    }
  }

  def getTopOpsview(month: String): Future[Seq[(String,Int)]] = {
    if(month.equals("")) {
      val month = CommonService.get3MonthAgo()+"-01"
      dbConfig.db.run(
        sql"""select bras_id,sum(total_opsview)
            from dmt_overview_noc
            where month > $month::TIMESTAMP
            group by bras_id
            order by sum(total_opsview) desc
            limit 10
                  """
          .as[(String,Int)])
    } else{
      val query = month + "-01"
      dbConfig.db.run(
        sql"""select bras_id,sum(total_opsview)
            from dmt_overview_noc
            where month = $query::TIMESTAMP
            group by bras_id
            order by sum(total_opsview) desc
            limit 10
                  """
          .as[(String,Int)])
    }
  }

  def getTopInf(month: String): Future[Seq[(String,String,Int)]] = {
    if(month.equals("")) {
      val month = CommonService.get3MonthAgo()+"-01"
      dbConfig.db.run(
        sql"""select tbO.bras_id,tbO.host,sum(tbO.total_inf)
                        from(
                           select row_number() OVER (PARTITION BY inf.bras_id ORDER BY inf.total_inf desc) AS r,
                            inf.*
                            from (
                                 select bras_id,host,sum(total_inf) as total_inf
                                 from dmt_overview_inf
                                 where month > $month::TIMESTAMP
                                 group by bras_id,host
                                 order by sum(total_inf) desc
                            ) inf
                        ) tbO
                        where tbO.r<=20
                        group by tbO.bras_id,tbO.host
                        order by tbO.bras_id desc
                     limit 200
                  """
          .as[(String,String,Int)])
    } else{
      val query = month + "-01"
      dbConfig.db.run(
        sql"""select tbO.bras_id,tbO.host,sum(tbO.total_inf)
                        from(
                           select row_number() OVER (PARTITION BY inf.bras_id ORDER BY inf.total_inf desc) AS r,
                            inf.*
                            from (
                                 select bras_id,host,sum(total_inf) as total_inf
                                 from dmt_overview_inf
                                 where month = $query::TIMESTAMP
                                 group by bras_id,host
                                 order by sum(total_inf) desc
                            ) inf
                        ) tbO
                        where tbO.r<=20
                        group by tbO.bras_id,tbO.host
                        order by tbO.bras_id desc
                     limit 200
                  """
          .as[(String,String,Int)])
    }
  }

  def getTopnotSuyhao(month: String): Future[Seq[(String,Int)]] = {
    if(month.equals("")) {
      val month = CommonService.get3MonthAgo()+"-01"
      dbConfig.db.run(
        sql"""select host,sum(not_passed_suyhao)
            from dmt_overview_suyhao
            where month > $month::TIMESTAMP
            group by host
            order by sum(not_passed_suyhao) desc
            limit 10
                  """
          .as[(String,Int)])
    } else{
      val query = month + "-01"
      dbConfig.db.run(
        sql"""select host,sum(not_passed_suyhao)
            from dmt_overview_suyhao
            where month = $query::TIMESTAMP
            group by host
            order by sum(not_passed_suyhao) desc
            limit 10
                  """
          .as[(String,Int)])
    }
  }

  def getTopPoorconn(month: String): Future[Seq[(String,Int)]] = {
    if(month.equals("")) {
      val month = CommonService.get3MonthAgo()+"-01"
      dbConfig.db.run(
        sql"""select host,sum(poor_conn)
            from dmt_overview_device
            where month > $month::TIMESTAMP
            group by host
            order by sum(poor_conn) desc
            limit 10
                  """
          .as[(String,Int)])
    } else{
      val query = month + "-01"
      dbConfig.db.run(
        sql"""select host,sum(poor_conn)
            from dmt_overview_device
            where month = $query::TIMESTAMP
            group by host
            order by sum(poor_conn) desc
            limit 10
                  """
          .as[(String,Int)])
    }
  }

  def getBrasOutlierCurrent(nowDay: String): Future[Seq[(String,String,Int,Int,String)]] = {
    //val sumDay = CommonService.getRangeDay(nowDay).split(",").length
    dbConfig.db.run(
      sql"""select date_time,bras_id,signin,logoff,verified
            from dwh_conn_bras_detail
            where date_time >= $nowDay::TIMESTAMP and label='outlier'
            order by date_time desc
                  """
        .as[(String,String,Int,Int,String)])
  }

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
  def getSigLogCurrent(bras: String, day: String): (Int,Int) = {
    val multiRs = client.execute(
      multi(
        search(s"radius-streaming-*" / "docs")
          query { must(termQuery("type", "con"),termQuery("typeLog", "SignIn"),termQuery("nasName",bras.toLowerCase),rangeQuery("timestamp").gte(CommonService.formatYYmmddToUTC(day.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(day.split("/")(1))))) },
        search(s"radius-streaming-*" / "docs")
          query { must(termQuery("type", "con"),termQuery("typeLog", "LogOff"),termQuery("nasName",bras.toLowerCase),rangeQuery("timestamp").gte(CommonService.formatYYmmddToUTC(day.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(day.split("/")(1))))) }
      )
    ).await
    val signin = multiRs.responses(0).totalHits.toInt
    val logoff = multiRs.responses(1).totalHits.toInt
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
  def getKibanaBytimeES(bras: String,day: String): Array[(Int,Int)] ={
    val rs = client_kibana.execute(
      search(s"infra_dwh_kibana_*" / "docs")
        query { must(termQuery("bras_id",bras),rangeQuery("date_time").gte(CommonService.formatYYmmddToUTC(day.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(day.split("/")(1))))) }
        aggregations(
        dateHistogramAggregation("hourly")
          .field("date_time")
          .interval(DateHistogramInterval.HOUR)
          .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
        )
    ).await
    CommonService.getAggregationsSiglog(rs.aggregations.get("hourly")).map(x=> (CommonService.getHoursFromMiliseconds(x._1.toLong)->x._2.toInt))
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
  def getSigLogBytimeCurrent(bras: String, day: String): SigLogByTime = {
    val mulRes = client.execute(
      multi(
        search(s"radius-streaming-*" / "docs")
          query { must(termQuery("type", "con"),termQuery("nasName",bras.toLowerCase),termQuery("typeLog", "SignIn"),rangeQuery("timestamp").gte(CommonService.formatYYmmddToUTC(day.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(day.split("/")(1))))) }
          aggregations (
          dateHistogramAggregation("hourly")
            .field("timestamp")
            .interval(DateHistogramInterval.HOUR)
            .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
          ),
        search(s"radius-streaming-*" / "con")
          query { must(termQuery("type", "con"),termQuery("nasName",bras.toLowerCase),termQuery("typeLog", "LogOff"),rangeQuery("timestamp").gte(CommonService.formatYYmmddToUTC(day.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(day.split("/")(1))))) }
          aggregations (
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

  def getInfErrorBytimeResponse(bras: String,nowDay: String,hourly:Int): Array[(Int,Int)] = {
    val response = client_kibana.execute(
      search(s"infra_dwh_inf_host_*" / "docs")
        query { must(termQuery("bras_id.keyword",bras),rangeQuery("date_time").gte(CommonService.formatYYmmddToUTC(nowDay.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(nowDay.split("/")(1))))) }
        size 10000
    ).await
    val jsonRs = response.hits.hits.map(x=> x.sourceAsMap)
      .map(x=>(
        CommonService.formatUTCToHour(getValueAsString(x,"date_time")),
        getValueAsInt(x,"sf_error"),
        getValueAsInt(x,"lofi_error")
      ))
    jsonRs.map(x=> (x._1,x._2+x._3)).groupBy(_._1).mapValues(_.map(_._2).sum).toSeq.sorted.map(x=>x._1.toInt -> x._2).toArray

    /*dbConfig.db.run(
      sql"""select extract(hour from  date_time) as hourly, sum(sf_error)+sum(lofi_error) as sumError
            from dwh_inf_host
            where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            group by bras_id,extract(hour from  date_time)
            order by hourly
                  """
        .as[(Int,Int)])*/
  }

  def getInfhostResponse(bras: String,nowDay: String): Array[(String,Long,Long)] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    val rs = client_kibana.execute(
      search(s"infra_dwh_inf_host_*" / "docs")
        query { must(termQuery("bras_id.keyword",bras),rangeQuery("date_time").gte(CommonService.formatYYmmddToUTC(nowDay.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(nowDay.split("/")(1))))) }
        aggregations(
        termsAggregation("host")
          .field("host.keyword")
          .subaggs(
            sumAgg("sum0","sf_error"),
            sumAgg("sum1","lofi_error")
          ) size 100
        )
    ).await
    CommonService.getTermGroupMultiSums(rs, "host", "sum0","sum1")
   /* dbConfig.db.run(
      sql"""select host, sum(sf_error) as cpe,sum(lofi_error) as lostip
            from dwh_inf_host
            where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            group by host
                  """
        .as[(String,Int,Int)])*/
  }

  def getInfModuleResponse(bras: String,nowDay: String): Array[(String,String,Long,Long,Long)] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    val rs = client_kibana.execute(
      search(s"infra_dwh_inf_module_*" / "docs")
        query { must(termQuery("bras_id.keyword",bras),not(termQuery("module.keyword","-1")),rangeQuery("date_time").gte(CommonService.formatYYmmddToUTC(nowDay.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(nowDay.split("/")(1))))) }
        aggregations(
        termsAggregation("host")
          .field("host.keyword")
          .subAggregations(
            termsAggregation("module")
              .field("module.keyword")
              .subaggs(
                sumAgg("sum0","sf_error"),
                sumAgg("sum1","lofi_error")
              ) size 100
        ))
    ).await
    val mapHostModule = CommonService.getSecondAggregationsAndSums(rs.aggregations.get("host"),"module")
    val rsHostMol = mapHostModule.flatMap(x => x._2.map(y => x._1 -> y))
      .map(x => (x._1, x._2._1, x._2._2,x._2._3,x._2._2+x._2._3))
    rsHostMol

/*    dbConfig.db.run(
      sql"""select host, module,sum(sf_error) as cpe,sum(lofi_error) as lostip,sum(sf_error)+sum(lofi_error) as sumAll
            from dwh_inf_module
            where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP and module <> '-1'
            group by host,module
                  """
        .as[(String,String,Int,Int,Int)])*/
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
            where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP and linecard != '-1' and card != '-1' and port != '-1'
            group by linecard, card, port
                  """
        .as[(String,Int,Int)])
  }
  def getLinecardhostCurrent(bras: String,day: String): Array[(String,String)] = {
    val multiRs = client.execute(
      multi(
        search(s"radius-streaming-*" / "docs")
          query { must(termQuery("type", "con"),termQuery("nasName", bras.toLowerCase),termQuery("typeLog", "SignIn"),not(termQuery("card.lineId","-1")),not(termQuery("card.id","-1")),not(termQuery("card.port","-1")),rangeQuery("timestamp").gte(CommonService.formatYYmmddToUTC(day.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(day.split("/")(1))))) }
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
        search(s"radius-streaming-*" / "docs")
          query { must(termQuery("type", "con"),termQuery("nasName", bras.toLowerCase),termQuery("typeLog", "LogOff"),not(termQuery("card.lineId","-1")),not(termQuery("card.id","-1")),not(termQuery("card.port","-1")),rangeQuery("timestamp").gte(CommonService.formatYYmmddToUTC(day.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(day.split("/")(1))))) }
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
  def getErrorSeverityES(bras: String,day: String): Array[(String,Long)] ={
    val rs = client_kibana.execute(
      search(s"infra_dwh_kibana_*" / "docs")
        query { must(termQuery("bras_id.keyword",bras),rangeQuery("date_time").gte(CommonService.formatYYmmddToUTC(day.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(day.split("/")(1))))) }
        aggregations(
        termsAggregation("severity")
          .field("severity.keyword")
        )
    ).await
    CommonService.getAggregationsSiglog(rs.aggregations.get("severity"))
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
  def getErrorTypeES(bras: String,day: String): Array[(String,Long)] ={
    val rs = client_kibana.execute(
      search(s"infra_dwh_kibana_*" / "docs")
        query { must(termQuery("bras_id.keyword",bras),rangeQuery("date_time").gte(CommonService.formatYYmmddToUTC(day.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(day.split("/")(1))))) }
        aggregations(
        termsAggregation("error_type")
          .field("error_type.keyword")
        )
    ).await
    CommonService.getAggregationsSiglog(rs.aggregations.get("error_type"))
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
  def getFacilityES(bras: String,day: String): Array[(String,Long)] ={
    val rs = client_kibana.execute(
      search(s"infra_dwh_kibana_*" / "docs")
        query { must(termQuery("bras_id.keyword",bras),rangeQuery("date_time").gte(CommonService.formatYYmmddToUTC(day.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(day.split("/")(1))))) }
        aggregations(
        termsAggregation("facility")
          .field("facility.keyword")
        )
    ).await

    CommonService.getAggregationsSiglog(rs.aggregations.get("facility"))
  }

  def getDdosResponse(bras: String,nowDay: String): Future[Seq[(String,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select description, sum(value) from dwh_kibana
            where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP and error_type='ddos'
            group by description
                  """
        .as[(String,Int)])
  }
  def getDdosES(bras: String,day: String): Array[(String,Long)] ={
    val rs = client_kibana.execute(
      search(s"infra_dwh_kibana_*" / "docs")
        query { must(termQuery("bras_id.keyword",bras),termQuery("error_type","ddos"),rangeQuery("date_time").gte(CommonService.formatYYmmddToUTC(day.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(day.split("/")(1))))) }
        aggregations(
        termsAggregation("description")
          .field("description.keyword")
          .subaggs(
            sumAgg("sum","value")
          )
        )
    ).await
    CommonService.getTerm(rs, "description", "sum")
  }

  def getSeveValueResponse(bras: String,nowDay: String): Future[Seq[(String,String,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select severity,error_name, count(*) from dwh_kibana
            where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP and severity is not null and error_name is not null
            group by severity,error_name
                  """
        .as[(String,String,Int)])
  }
  def getSeveValueES(bras: String,day: String): Array[((String,String),Long)] ={
    val rs = client_kibana.execute(
      search(s"infra_dwh_kibana_*" / "docs")
        query { must(termQuery("bras_id.keyword",bras),not(termQuery("error_name.keyword","")),rangeQuery("date_time").gte(CommonService.formatYYmmddToUTC(day.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(day.split("/")(1))))) }
        aggregations(
        termsAggregation("severity")
          .field("severity.keyword")
          .subAggregations(
            termsAggregation("error_name")
              .field("error_name.keyword")
          )
        )
    ).await
    val mapSevErr = CommonService.getSecondAggregations(rs.aggregations.get("severity"),"error_name")

    mapSevErr.flatMap(x => x._2.map(y => x._1 -> y))
      .map(x => (x._1, x._2._1) -> x._2._2)
  }

  def getSigLogByHost(bras: String,nowDay: String): SigLogByHost = {
    val arrDay = CommonService.getRangeDay(nowDay).split(",")
    var streamingIndex = s"radius-streaming-"+arrDay(0)
    for(i <- 1 until arrDay.length) {
      streamingIndex += s",radius-streaming-"+arrDay(i)
    }
    val multiRs = client.execute(
      multi(
        search(s"radius-streaming-*" / "docs")
          query { must(termQuery("type", "con"),termQuery("nasName", bras.toLowerCase),termQuery("typeLog", "SignIn"),rangeQuery("timestamp").gte(CommonService.formatYYmmddToUTC(nowDay.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(nowDay.split("/")(1))))) }
          aggregations (
          termsAggregation("olt")
            .field("card.olt")
          ),
        search(s"radius-streaming-*" / "docs")
          query { must(termQuery("type", "con"),termQuery("nasName", bras.toLowerCase),termQuery("typeLog", "LogOff"),rangeQuery("timestamp").gte(CommonService.formatYYmmddToUTC(nowDay.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(nowDay.split("/")(1))))) }
          aggregations (
          termsAggregation("olt")
            .field("card.olt")
          )
      )
    ).await
    if(multiRs.responses(0).hits != null && multiRs.responses(1).hits != null) {
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