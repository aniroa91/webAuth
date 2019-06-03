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
import services.domain.CommonService.{formatUTC, getAggregations}
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval
import org.joda.time.DateTimeZone
import com.ftel.bigdata.utils.DateTimeUtil
import org.elasticsearch.search.sort.SortOrder
import service.BrasService.{client, getValueAsInt, getValueAsString}

object BrasDAO {

  val client = Configure.client
  val monthSize = CommonService.monthSize
  val topN = CommonService.SIZE_DEFAULT
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  def getSigLogByMonth(month: String): Future[Seq[(String, Int, Int)]] = {
    val currMonth = month+"-01"
    val prevMonth = CommonService.getPreviousMonth(month)+"-01"
    dbConfig.db.run(
      sql"""select month, sum(signin), sum(logoff)
            from dmt_overview_conn
            where month = $currMonth::TIMESTAMP OR month = $prevMonth::TIMESTAMP
            group by month
            order by month desc
                  """
        .as[(String, Int, Int)])
  }

  def getTopBrasOutMonthly() = {
    dbConfig.db.run(
      sql"""select month, sum(no_outliers)
            from dmt_overview_bras_outlier where month >='2018-06-01'::TIMESTAMP
            group by month
            order by month
                  """
        .as[(String, Long)])
  }

  def getTopConnectMonthly(province: String) = {
    dbConfig.db.run(
      sql"""select month, sum(signin), sum(logoff)
            from dmt_overview_conn where month >='2018-06-01'::TIMESTAMP and province ~* $province
            group by month
            order by month
                  """
        .as[(String, Long, Long)])
  }

  def getTopInfErrMonthly(province: String) = {
    dbConfig.db.run(
      sql"""select month, sum(total_inf)
            from dmt_overview_inf where month >='2018-06-01'::TIMESTAMP and province ~* $province
            group by month
            order by month
                  """
        .as[(String, Long)])
  }

  def getTopOltMonthly() = {
    dbConfig.db.run(
      sql"""select month, sum(total_outliers)
            from dmt_overview_inf_outlier where month >='2018-06-01'::TIMESTAMP
            group by month
            order by month
                  """
        .as[(String, Long)])
  }

  def getTopServBrasErrMonthly() = {
    dbConfig.db.run(
      sql"""select month, sum(alert_count),sum(crit_count),sum(emerg_count),sum(err_count),sum(notice_count),sum(warning_count)
            from dmt_overview_noc
            group by month
            order by month
                  """
        .as[(String, Long, Long, Long, Long, Long, Long)])
  }

  def getTopServOpsviewMonthly() = {
    dbConfig.db.run(
      sql"""select month, sum(crit_opsview),sum(ok_opsview),sum(warn_opsview),sum(unknown_opsview)
            from dmt_overview_noc
            group by month
            order by month
                  """
        .as[(String, Long, Long, Long, Long)])
  }

  def getTopServInfErrMonthly(province: String) = {
    dbConfig.db.run(
      sql"""select month, sum(inf_down),sum(user_down),sum(sf_error),sum(lofi_error), sum(lost_signal), sum(rouge_error)
            from dmt_overview_inf where province ~* $province
            group by month
            order by month
                  """
        .as[(String, Long, Long, Long, Long, Long, Long)])
  }

  def getTopOverviewNocMonthly(col: String) = {
    dbConfig.db.run(
      sql"""select month, sum(#$col)
            from dmt_overview_noc where month >='2018-06-01'::TIMESTAMP
            group by month
            order by month
                  """
        .as[(String, Long)])
  }

  def getSuyhaoByMonth(month: String, province: String): Future[Seq[(String, Int, Int)]] = {
    val currMonth = month+"-01"
    val prevMonth = CommonService.getPreviousMonth(month)+"-01"
    dbConfig.db.run(
      sql"""select month, sum(passed_suyhao), sum(not_passed_suyhao)
            from dmt_overview_suyhao
            where month = $currMonth::TIMESTAMP OR month = $prevMonth::TIMESTAMP and province ~* $province
            group by month
            order by month desc
                  """
        .as[(String, Int, Int)])
  }

  def getBrasOutlierByMonth(month: String): Future[Seq[(String, Int, Int)]] = {
    val currMonth = month+"-01"
    val prevMonth = CommonService.getPreviousMonth(month)+"-01"
    dbConfig.db.run(
      sql"""select month, sum(no_outliers), sum(affected_clients)
            from dmt_overview_bras_outlier
            where month = $currMonth::TIMESTAMP OR month = $prevMonth::TIMESTAMP
            group by month
            order by month desc
                  """
        .as[(String, Int, Int)])
  }

  def getInfOutlierByMonth(month: String,province: String): Future[Seq[(String, Int, Int, Int, Int,Int, Int, Int, Int, Int, Int)]] = {
    val currMonth = month+"-01"
    val prevMonth = CommonService.getPreviousMonth(month)+"-01"
    dbConfig.db.run(
      sql"""select month, sum(total_outliers), sum(total_clients), sum(inf_down_clients), sum(inf_down_outliers), sum(user_down_clients), sum(user_down_outliers), sum(sf_clients), sum(sf_outliers), sum(lost_signal_clients), sum(lost_signal_outliers)
            from dmt_overview_inf_outlier
            where month = $currMonth::TIMESTAMP OR month = $prevMonth::TIMESTAMP and province ~* $province
            group by month
            order by month desc
                  """
        .as[(String, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)])
  }

  def getDeviceByMonth(month: String,province: String): Future[Seq[(String, Int, Int, Int, Int)]] = {
    val currMonth = month+"-01"
    val prevMonth = CommonService.getPreviousMonth(month)+"-01"
    dbConfig.db.run(
      sql"""select month, sum(no_contract), sum(no_device), sum(not_poor_conn), sum(poor_conn)
            from dmt_overview_device
            where month = $currMonth::TIMESTAMP OR month = $prevMonth::TIMESTAMP and province ~* $province
            group by month
            order by month desc
                  """
        .as[(String, Int, Int, Int, Int)])
  }

  def getInfErrorByMonth(month: String,province: String): Future[Seq[(String, Int, Int, Int, Int, Int, Int, Int)]] = {
    val currMonth = month+"-01"
    val prevMonth = CommonService.getPreviousMonth(month)+"-01"
    dbConfig.db.run(
      sql"""select month, sum(total_inf), sum(inf_down), sum(user_down), sum(lost_signal), sum(sf_error), sum(lofi_error), sum(rouge_error)
            from dmt_overview_inf
            where month = $currMonth::TIMESTAMP OR month = $prevMonth::TIMESTAMP and province ~* $province
            group by month
            order by month desc
                  """
        .as[(String, Int, Int, Int, Int,Int, Int, Int)])
  }

  def getKibaOpsByMonth(month: String): Future[Seq[(String, Int, Int)]] = {
    val currMonth = month+"-01"
    val prevMonth = CommonService.getPreviousMonth(month)+"-01"
    dbConfig.db.run(
      sql"""select month, sum(total_kibana), sum(total_opsview)
            from dmt_overview_noc
            where month = $currMonth::TIMESTAMP OR month = $prevMonth::TIMESTAMP
            group by month
            order by month desc
                  """
        .as[(String, Int, Int)])
  }

  def getSigninUnique(id: String, time: String): Future[Seq[(Int, Int)]] = {
    dbConfig.db.run(
      sql"""select sum(signin_unique), sum(logoff_unique)
            from dwh_conn_bras_detail
            where bras_id= $id and date_time =$time::TIMESTAMP
                  """
        .as[(Int, Int)])
  }

  def getErrorMetric(id: String, time: String): Future[Seq[(Int)]] = {
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
    val dateTime = DateTime.parse(time, formatter)
    val oldTime  = dateTime.minusMinutes(30).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))
    dbConfig.db.run(
      sql"""select sum(error)
            from dwh_inf_module
            where bras_id= $id and date_time <= $time::TIMESTAMP and date_time >= $oldTime::TIMESTAMP
                  """
        .as[(Int)])
  }

  def getTrackingUser(id: String, time: String): Future[Seq[(Int, Double)]] = {
    dbConfig.db.run(
      sql"""select n_user, percentage
            from dwh_tracking_user
            where bras_id= $id and date_time =$time::TIMESTAMP
            order by minutes DESC
            limit 1
                  """
        .as[(Int, Double)])
  }

  def getHostMonitor(id: String): Future[Seq[(String,String,String)]] = {
    dbConfig.db.run(
      sql"""select module,splitter,name
            from dwh_user_info
            where host= $id and module is not null and splitter is not null and name is not null
            group by module,splitter,name
                  """
        .as[(String,String,String)])
  }

  def getSpliterMudule(province: String): Future[Seq[(String,String,String,Int)]] = {
    val dt = new DateTime();
    val aDay = dt.minusHours(12);
    val aDayTime = aDay.toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS"))
    dbConfig.db.run(
      sql"""select date_time,host,splitter,sum(lost_signal)
            from dwh_inf_splitter
            where date_time >= $aDayTime::TIMESTAMP AND lost_signal>0 and bras_id ~* $province
            group by date_time,host,splitter
            order by date_time desc
                  """
        .as[(String,String,String,Int)])
  }

  def confirmLabelInf(host: String,module: String,time: String) = {
    val dt = new DateTime();
    val nowDate  = dt.toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))
    dbConfig.db.run(
      sqlu"""UPDATE dwh_inf_module SET confirm = true, confirm_date_time = $nowDate::TIMESTAMP
            where host =$host and module=$module and date_time=$time::TIMESTAMP
                  """
    )
  }

  def rejectLabelInf(host: String,module: String,time: String) = {
    val dt = new DateTime();
    val nowDate  = dt.toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))
    dbConfig.db.run(
      sqlu"""UPDATE dwh_inf_module SET confirm = false, confirm_date_time = $nowDate::TIMESTAMP
             where host =$host and module=$module and date_time=$time::TIMESTAMP
                  """
    )
  }

  def getErrorHistory(id: String, province: String): Future[Seq[(String,Int)]] = {
    val time = id.split("/")(0)
    val host = id.split("/")(1)
    val module = id.split("/")(2)
    val nowDay = CommonService.getCurrentDay()
    dbConfig.db.run(
      sql"""select date_time,(sum(user_down)+sum(inf_down)+sum(sf_error)+sum(lofi_error)) errors
            from dwh_inf_module
            where host = $host and module = $module and date_time <$time::TIMESTAMP and date_time >= $nowDay::TIMESTAMP and label = 1 and bras_id ~* $province
            group by date_time
            order by date_time desc
                  """
        .as[(String,Int)])
  }

  def getSigLogByRegion(month: String): Future[Seq[(String,Int,Int,Int,Int)]] = {
    dbConfig.db.run(
      sql"""select province,sum(signin) as signin, sum(logoff) as logoff,sum(signin_clients) as signin_clients, sum(logoff_clients) as logoff_clients
              from dmt_overview_conn
              where month = $month::TIMESTAMP
              group by province
                  """
        .as[(String,Int,Int,Int,Int)])
  }

  def getSigLogByProvince(month: String,province: String,lastMonth: String): Future[Seq[(String,Int,Int,Int,Int)]] = {
    dbConfig.db.run(
      sql"""select bras_id,sum(signin) as signin, sum(logoff) as logoff,sum(signin_clients) as signin_clients, sum(logoff_clients) as logoff_clients
              from dmt_overview_conn
              where month = $month::TIMESTAMP and province = $province and bras_id IN(
                 select bras_id
                 from dmt_overview_conn
                 where month = $lastMonth::TIMESTAMP and province = $province
                 group by bras_id
              )
              group by bras_id
              order by bras_id
                  """
        .as[(String,Int,Int,Int,Int)])
  }

  def getDistinctBrasbyProvince(month: String,province: String): Future[Seq[(String)]] = {
    dbConfig.db.run(
      sql"""select bras_id
            from dmt_overview_inf
            where month = $month::TIMESTAMP and province = $province
            group by bras_id
                  """
        .as[(String)])
  }

  def getDistinctHostbyBras(month: String,bras: String): Future[Seq[(String)]] = {
    dbConfig.db.run(
      sql"""select host
            from dmt_overview_inf
            where month = $month::TIMESTAMP and bras_id = $bras
            group by host
                  """
        .as[(String)])
  }

  def getTop10HostId(month: String,lstHost: Array[(String)]): Future[Seq[(String)]] = {
    val arrId = lstHost.map("'"+ _ + "'").mkString(",")
    dbConfig.db.run(
      sql"""select host
            from dmt_overview_inf
            where month = $month::TIMESTAMP and host IN (#$arrId)
            group by host
            order by sum(total_inf) desc
            limit 10
                  """
        .as[(String)])
  }

  def getSigLogByBras(month: String,bras: String,lastMonth: String): Future[Seq[(String,Int,Int,Int,Int)]] = {
    dbConfig.db.run(
      sql"""select host,sum(signin) as signin, sum(logoff) as logoff, sum(signin_clients) as signin_clients, sum(logoff_clients) as logoff_clients
              from dmt_overview_conn
              where month = $month::TIMESTAMP and bras_id = $bras and host IN(
                select host
                from dmt_overview_conn
                where month = $lastMonth::TIMESTAMP and bras_id = $bras
                group by host
                order by sum(signin) desc
                limit 10
              )
              group by host
                  """
        .as[(String,Int,Int,Int,Int)])
  }

  def getTotalOutlier(province: String): Future[Seq[(Int)]] = {
    val dt = new DateTime();
    val currentTime = dt.toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS"))
    val nowDay = CommonService.getCurrentDay()
    dbConfig.db.run(
      sql"""select count(*)
            from dwh_inf_module
            where date_time >= $nowDay::TIMESTAMP and date_time <= $currentTime::TIMESTAMP AND label =1 and bras_id ~* $province
                  """
        .as[(Int)])
  }

  def getSflofiMudule(queries: String, province: String): Future[Seq[(String,String,String,Int,Int,Int,Int,Int,Int,String)]] = {
    val dt = new DateTime();
    var currentTime = dt.toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS"))
    if(!queries.equals("*")) currentTime = CommonService.getNextDay(queries)
    val nowDay = if(queries.equals("*")) CommonService.getCurrentDay() else queries
    dbConfig.db.run(
      sql"""select tb.date_time,tb.host,tb.module,sum(tb.sf_error),sum(tb.lofi_error),sum(tb.confirm),sum(tb.user_down),sum(tb.inf_down),sum(tb.rouge_error), string_agg(tb.splitter,', ')
            from (
                  select A.*,B.splitter
                  from
                       (select date_time,host,module,sum(sf_error) sf_error,sum(lofi_error) lofi_error,sum(cast(confirm as int)) confirm,sum(user_down) user_down,sum(inf_down) inf_down,sum(rouge_error) rouge_error
                        from dwh_inf_module
                        where date_time >= $nowDay::TIMESTAMP and date_time < $currentTime::TIMESTAMP AND label =1 and bras_id ~* $province
                        group by date_time,host,module
                        order by date_time desc) A
                   left join
                   (select distinct x.host, x.splitter, x.module, y.date_time from dwh_user_info x, dwh_inf_splitter y
                    where date_time >= $nowDay::TIMESTAMP and date_time <= $currentTime::TIMESTAMP and bras_id ~* $province and x.host = y.host and x.splitter = y.splitter) B
                    on A.host = B.host and A.module = B.module and B.date_time between A.date_time - interval '15 minutes' and A.date_time
            ) tb
            group by tb.date_time,tb.host,tb.module
            order by tb.date_time desc
                  """
        .as[(String,String,String,Int,Int,Int,Int,Int,Int,String)])

    /*dbConfig.db.run(
      sql"""select date_time,host,module,sum(sf_error) sf_error,sum(lofi_error) lofi_error,sum(cast(confirm as int)) confirm,sum(user_down) user_down,sum(inf_down) inf_down
            from dwh_inf_module
            where date_time >= $nowDay::TIMESTAMP and date_time <= $currentTime::TIMESTAMP AND label =1
            group by date_time,host,module
            order by date_time desc
                  """
        .as[(String,String,String,Int,Int,Int,Int,Int)])*/
  }

  def getIndexRougeMudule(province: String): Array[(String,String,String,String,Int)] = {
    val dt = CommonService.getCurrentDay()
    val rs = client.execute(
      search(s"infra_dwh_inf_index_*" / "docs")
        query { must(rangeQuery("rouge_error").gt(0),rangeQuery("date_time").gte(CommonService.formatYYmmddToUTC(dt)), prefixQuery("bras_id", s"$province")) }
        aggregations (
        termsAggregation("date_time")
          .field("date_time")
          .subAggregations(
            termsAggregation("host")
              .field("host.keyword")
              .subAggregations(
                termsAggregation("module")
                  .field("module.keyword")
                  .subAggregations(
                    termsAggregation("index")
                      .field("index")
                      .subaggs(
                        sumAgg("sum","rouge_error")
                      ) size 100
                  )
            )
         )
      ) size 0
    ).await

    val mapRouge = CommonService.getMultiAggregationsAndSum(rs.aggregations.get("date_time"),"host","module","index")
    mapRouge.flatMap(x => x._2.map(y => x._1 -> y))
      .map(x => (x._1 , x._2._1,x._2._2))
      .flatMap(x => x._3.map(y => (x._1,x._2) -> y))
      .map(x => (x._1._1,x._1._2 ,  x._2._1, x._2._2))
      .flatMap(x => x._4.map(y => (x._1,x._2,x._3) -> y))
      .map(x => (CommonService.formatUTC(x._1._1), x._1._2, x._1._3 , x._2._1, x._2._2.toInt))
      .sortWith((x,y)=> x._1>y._1)
  }

  def getUserDownMudule(province: String): Future[Seq[(String,String,String,Int)]] = {
    val dt = new DateTime();
    val aDay = dt.minusHours(12);
    val aDayTime = aDay.toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS"))
    dbConfig.db.run(
      sql"""select date_time,host,module,sum(user_down)
            from dwh_inf_module
            where date_time >= $aDayTime::TIMESTAMP AND user_down>0 and bras_id ~* $province
            group by date_time,host,module
            order by date_time desc
                  """
        .as[(String,String,String,Int)])
  }

  def getInfDownMudule(province: String): Future[Seq[(String,String,String,Int)]] = {
    val dt = new DateTime();
    val aDay = dt.minusHours(12);
    val aDayTime = aDay.toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS"))
    dbConfig.db.run(
      sql"""select date_time,host,module,sum(inf_down)
            from dwh_inf_module
            where date_time >= $aDayTime::TIMESTAMP AND inf_down>0 and bras_id ~* $province
            group by date_time,host,module
            order by date_time desc
                  """
        .as[(String,String,String,Int)])
  }

  def getProvinceOpsview(fromMonth: String,toMonth: String): Future[Seq[(String,String,String,Int)]] = {
    dbConfig.db.run(
      sql"""select month,province,bras_id,sum(total_opsview) as total_opsview
            from dmt_overview_noc
            where month >= $fromMonth::TIMESTAMP and month <= $toMonth::TIMESTAMP
            group by month,province,bras_id
            order by month desc, province
                  """
        .as[(String,String,String,Int)])
  }

  def getOutlierMonthly(fromMonth: String,toMonth: String, name: String,province: String) = {
    val colSum = if(name == "bras") "no_outliers" else "total_outliers"
    val db = s"dmt_overview_${name}_outlier"
    dbConfig.db.run(
      sql"""select bras_id, sum(#$colSum)
            from #$db
            where month >= $fromMonth::TIMESTAMP and month <= $toMonth::TIMESTAMP AND province ~* $province
            group by bras_id
            """
        .as[(String, Int)]
    )
  }

  def getProvinceContract(fromMonth: String,toMonth: String,province: String): Future[Seq[(String,String,String,Int,Int,Int)]] = {
    dbConfig.db.run(
      sql"""select month,province,host,sum(no_contract) as no_contract,sum(no_device) as no_device,sum(poor_conn) as poor_conn
            from dmt_overview_device
            where month >= $fromMonth::TIMESTAMP and month <= $toMonth::TIMESTAMP and province ~* $province
            group by month,province,host
            order by month desc, province
                  """
        .as[(String,String,String,Int,Int,Int)])
  }

  def getProvinceKibana(fromMonth: String,toMonth: String): Future[Seq[(String,String,String,Int)]] = {
    dbConfig.db.run(
      sql"""select month,province,bras_id,sum(total_kibana) as total_kibana
            from dmt_overview_noc
            where month >= $fromMonth::TIMESTAMP and month <= $toMonth::TIMESTAMP
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

  def getProvinceTotalInf(fromMonth: String,toMonth: String, province: String): Future[Seq[(String,String,Double)]] = {
    dbConfig.db.run(
      sql"""select month,province,sum(total_inf) as total_inf
            from dmt_overview_inf
            where month >= $fromMonth::TIMESTAMP and month <= $toMonth::TIMESTAMP AND province ~* $province
            group by month,province
            order by month desc
                  """
        .as[(String,String,Double)])
  }

  // for daily data
  def getInfErrorsDaily(day: String, province: String) = {
    val nextDay = CommonService.getNextDay(day)
    dbConfig.db.run(
      sql"""select bras_id,host, sum(user_down),sum(inf_down),sum(sf_error),sum(lofi_error), sum(rouge_error), sum(lost_signal)
            from dwh_inf_host
            where date_time >= $day::TIMESTAMP and date_time < $nextDay::TIMESTAMP and bras_id ~* $province
            group by bras_id,host
            """
        .as[(String, String, Double, Double, Double, Double,Double, Double)]
    )
  }

  def getServiceNoticeDaily(day: String): Future[Seq[(String, Double, Double,Double, Double)]] ={
    val nextDay = CommonService.getNextDay(day)
    dbConfig.db.run(
      sql"""select bras_id, sum(warn_opsview),sum(unknown_opsview),sum(ok_opsview),sum(crit_opsview)
            from dwh_opsview_status
            where date_time >= $day::TIMESTAMP and date_time < $nextDay::TIMESTAMP and device_type='bras'
            group by bras_id
            """
        .as[(String, Double, Double,Double, Double)]
    )
  }

  def getInfAccessOutlierDaily(day: String, province: String) = {
    val nextDay = CommonService.getNextDay(day)
    dbConfig.db.run(
      sql"""select bras_id, host,count(*)
            from dwh_inf_module
            where date_time >= $day::TIMESTAMP and date_time < $nextDay::TIMESTAMP and label = 1 and bras_id ~* $province
            group by bras_id,host
            """
        .as[(String, String, Int)]
    )
  }

  def getTicketIssue(day: String) = {
    dbConfig.db.run(
      sql"""select issue_group, province, issue, count(*) from dwh_ticket
            where date_trunc('day', created_date) = $day::TIMESTAMP
            and issue_group in ('Hệ thống Ngoại vi', 'Hệ thống Core IP', 'Hệ Thống Access') and province <> ''
            group by issue_group, province, issue
            order by issue_group, province, issue
            """
        .as[(String, String, String, Int)]
    )
  }

  def getBrasOutlierDaily(day: String) = {
    val nextDay = CommonService.getNextDay(day)
      dbConfig.db.run(
        sql"""select bras_id,count(*)
            from dwh_conn_bras_detail
            where date_time >= $day::TIMESTAMP and date_time < $nextDay::TIMESTAMP and label = 'outlier'
            group by bras_id
            """
          .as[(String, Int)]
      )
  }

  def getErrorHostdaily(bras: String, day: String, province: String) = {
    val nextDay = CommonService.getNextDay(day)
    if(bras.equals("*")){
      dbConfig.db.run(
        sql"""select extract(hour from  date_time) as hourly, sum(user_down),sum(inf_down),sum(sf_error),sum(lofi_error),sum(rouge_error),sum(lost_signal),sum(jumper_error)
            from dwh_inf_host
            where date_time >= $day::TIMESTAMP and date_time < $nextDay::TIMESTAMP and bras_id ~* $province
            group by hourly
            order by hourly
            """
          .as[(String, Double, Double,Double, Double,Double,Double,Double)]
      )
    }
    else{
      dbConfig.db.run(
        sql"""select extract(hour from  date_time) as hourly, sum(user_down),sum(inf_down),sum(sf_error),sum(lofi_error),sum(rouge_error),sum(lost_signal),sum(jumper_error)
            from dwh_inf_host
            where date_time >= $day::TIMESTAMP and date_time < $nextDay::TIMESTAMP and bras_id ~* $province and bras_id = $bras
            group by hourly
            order by hourly
            """
          .as[(String, Double, Double,Double, Double,Double,Double,Double)]
      )
    }
  }

  def getDeviceErrorsDaily(day: String): Future[Seq[(String, Double, Double,Double, Double,Double,Double)]] ={
    val nextDay = CommonService.getNextDay(day)
    dbConfig.db.run(
      sql"""select bras_id, sum(alert_count),sum(crit_count),sum(emerg_count),sum(err_count),sum(notice_count),sum(warning_count)
            from dwh_kibana_agg
            where date_time >= $day::TIMESTAMP and date_time < $nextDay::TIMESTAMP AND device_type='bras'
            group by bras_id
            """
         .as[(String, Double, Double,Double, Double,Double,Double)]
    )
  }

  def getTotalInfbyProvince(month: String,id: String,lstBrasId: Array[(String)]): Future[Seq[(String,String,Double)]] = {
    val lstBras = lstBrasId.map("'" + _ + "'").mkString(",")
    val fromMonth = month.split("/")(0)+"-01"
    val toMonth = month.split("/")(1)+"-01"

    dbConfig.db.run(
      sql"""select month, bras_id,sum(total_inf) as total_inf
            from dmt_overview_inf
            where month >= $fromMonth::TIMESTAMP and month <= $toMonth::TIMESTAMP and province = $id and bras_id IN (#$lstBras)
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

  def getTotalInfbyBras(month: String,id: String,lstHost: Array[(String)]): Future[Seq[(String,String,Double)]] = {
    val arrId = lstHost.map("'" + _ + "'").mkString(",")
    val fromMonth = month.split("/")(0)+"-01"
    val toMonth = month.split("/")(1)+"-01"
    dbConfig.db.run(
      sql"""select month, host,sum(total_inf) as total_inf
            from dmt_overview_inf
            where month >= $fromMonth::TIMESTAMP and month <= $toMonth::TIMESTAMP and bras_id = $id and host IN(#$arrId)
            group by month,host
            order by month desc,sum(total_inf)  desc
                  """
        .as[(String,String,Double)])
  }

  def getProvinceSuyhao(fromMonth: String,toMonth: String, province: String): Future[Seq[(String,String,String,Int,Int)]] = {
    dbConfig.db.run(
      sql"""select month,province,host,sum(not_passed_suyhao) as not_passed_suyhao,sum(not_passed_suyhao_clients) as not_passed_suyhao_clients
            from dmt_overview_suyhao
            where month >= $fromMonth::TIMESTAMP and month <= $toMonth::TIMESTAMP AND province ~* $province
            group by month,province,host
            order by month desc, province
                  """
        .as[(String,String,String,Int,Int)])
  }

  def getProvinceCount(month: String): Future[Seq[(String,String,Int,Int,Int,Int,Int,Int)]] = {
    if(month.indexOf("/")>=0) {
      val fromMonth = month.split("/")(0)
      val toMonth = month.split("/")(1)
      dbConfig.db.run(
        sql"""select province,bras_id,sum(alert_count) as alert_count,sum(crit_count) as crit_count,sum(warning_count) as warning_count,
                      sum(notice_count) as notice_count, sum(err_count) as err_count,sum(emerg_count) as emerg_count
              from dmt_overview_noc
              where month >= $fromMonth::TIMESTAMP and month <= $toMonth::TIMESTAMP
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

  def getBrasOpsviewType(month: String,province: String): Future[Seq[(String,Int,Int,Int,Int)]] = {
    if(month.indexOf("/")>=0) {
      val fromMonth = month.split("/")(0)
      val toMonth = month.split("/")(1)
      dbConfig.db.run(
        sql"""select bras_id,sum(ok_opsview) as ok_opsview,sum(warn_opsview) as warn_opsview,sum(unknown_opsview) as unknown_opsview,
                      sum(crit_opsview) as crit_opsview
              from dmt_overview_noc
              where month >= $fromMonth::TIMESTAMP and month <= $toMonth::TIMESTAMP and province = $province
              group by bras_id
                  """
          .as[(String,Int,Int,Int,Int)])
    } else{
      val query = month + "-01"
      dbConfig.db.run(
        sql"""select bras_id,sum(ok_opsview) as ok_opsview,sum(warn_opsview) as warn_opsview,sum(unknown_opsview) as unknown_opsview,
                      sum(crit_opsview) as crit_opsview
              from dmt_overview_noc
              where month = $query::TIMESTAMP and province = $province
              group by bras_id
                  """
          .as[(String,Int,Int,Int,Int)])
    }
  }

  def getProvinceOpsviewType(month: String): Future[Seq[(String,Int,Int,Int,Int)]] = {
    if(month.indexOf("/")>=0) {
      val fromMonth = month.split("/")(0)
      val toMonth = month.split("/")(1)
      dbConfig.db.run(
        sql"""select province,sum(ok_opsview) as ok_opsview,sum(warn_opsview) as warn_opsview,sum(unknown_opsview) as unknown_opsview,
                      sum(crit_opsview) as crit_opsview
              from dmt_overview_noc
              where month >= $fromMonth::TIMESTAMP and month <= $toMonth::TIMESTAMP
              group by province
                  """
          .as[(String,Int,Int,Int,Int)])
    } else{
      val query = month + "-01"
      dbConfig.db.run(
        sql"""select province,sum(ok_opsview) as ok_opsview,sum(warn_opsview) as warn_opsview,sum(unknown_opsview) as unknown_opsview,
                      sum(crit_opsview) as crit_opsview
              from dmt_overview_noc
              where month = $query::TIMESTAMP
              group by province
                  """
          .as[(String,Int,Int,Int,Int)])
    }
  }

  def getProvinceInfDownError(month: String,province:String): Future[Seq[(String,String,Int,Int,Int,Int)]] = {
    if(month.indexOf("/")>=0) {
      val fromMonth = month.split("/")(0)
      val toMonth = month.split("/")(1)
      dbConfig.db.run(
        sql"""select province,bras_id,sum(inf_down) as inf_down,sum(user_down) as user_down,sum(rouge_error) as rouge_error, sum(lost_signal) as lost_signal
              from dmt_overview_inf
              where month >= $fromMonth::TIMESTAMP and month <= $toMonth::TIMESTAMP AND province ~* $province
              group by province,bras_id
              order by province,bras_id
                  """
          .as[(String,String,Int,Int,Int,Int)])
    } else{
      val query = month + "-01"
      dbConfig.db.run(
        sql"""select province,bras_id,sum(inf_down) as inf_down,sum(user_down) as user_down,sum(rouge_error) as rouge_error,sum(lost_signal) as lost_signal
              from dmt_overview_inf
              where month =  $query::TIMESTAMP AND province ~* $province
              group by province,bras_id
              order by province,bras_id
                  """
          .as[(String,String,Int,Int,Int,Int)])
    }
  }

  def getTopSignin(month: String): Future[Seq[(String,String,Int,Int)]] = {
    val query = month + "-01"
    dbConfig.db.run(
      sql"""select d.province,d.bras_id,d.signin,d.signin_clients from(
                  select row_number() OVER (PARTITION BY c.province ORDER BY c.signin desc) AS r , c.*
                  from(
                       select b.province,a.bras_id, sum(signin) signin,sum(signin_clients) signin_clients
                       from dmt_overview_conn a,
                            (select province
                             from dmt_overview_conn
                             where month = $query::TIMESTAMP
                             group by province
                             having sum(signin) >0
                             order by sum(signin) desc
                             limit 10) b
                        where a.province = b.province and month = $query::TIMESTAMP
                        group by b.province, a.bras_id
                        order by b.province, a.bras_id) c) d
              where d.r <= 10
                  """
        .as[(String,String,Int,Int)])
  }

  def getTopLogoff(month: String): Future[Seq[(String,String,Int,Int)]] = {
    val query = month + "-01"
    dbConfig.db.run(
      sql"""select d.province,d.bras_id,d.logoff,d.logoff_clients from(
                  select row_number() OVER (PARTITION BY c.province ORDER BY c.logoff desc) AS r , c.*
                  from(
                       select b.province, a.bras_id, sum(logoff) logoff,sum(logoff_clients) logoff_clients
                       from dmt_overview_conn a,
                            (select province
                             from dmt_overview_conn
                             where month = $query::TIMESTAMP
                             group by province
                             having sum(logoff) >0
                             order by sum(logoff) desc
                             limit 10) b
                        where a.province = b.province and month = $query::TIMESTAMP
                        group by b.province, a.bras_id
                        order by b.province, a.bras_id) c) d
              where d.r <= 10
                  """
        .as[(String,String,Int,Int)])
  }

  def topInfOut(month: String, province: String): Future[Seq[(String,String,Int)]] = {
    val query = month + "-01"
    dbConfig.db.run(
      sql"""select d.province,d.bras_id,d.total_outliers from(
                  select row_number() OVER (PARTITION BY c.province ORDER BY c.total_outliers desc) AS r , c.*
                  from(
                       select b.province, a.bras_id, sum(total_outliers) total_outliers
                       from dmt_overview_inf_outlier a,
                            (select province
                             from dmt_overview_inf_outlier
                             where month = $query::TIMESTAMP and province ~* $province
                             group by province
                             having sum(total_outliers) >0
                             order by sum(total_outliers) desc
                             limit 10) b
                        where a.province = b.province and month = $query::TIMESTAMP
                        group by b.province, a.bras_id
                        order by b.province, a.bras_id) c) d
              where d.r <= 10
                  """
        .as[(String,String,Int)])
  }

  def topBrasOut(month: String): Future[Seq[(String,String,Int)]] = {
    val query = month + "-01"
    dbConfig.db.run(
      sql"""select d.province,d.bras_id,d.no_outliers from(
                  select row_number() OVER (PARTITION BY c.province ORDER BY c.no_outliers desc) AS r , c.*
                  from(
                       select b.province, a.bras_id, sum(no_outliers) no_outliers
                       from dmt_overview_bras_outlier a,
                            (select province
                             from dmt_overview_bras_outlier
                             where month = $query::TIMESTAMP
                             group by province
                             having sum(no_outliers) >0
                             order by sum(no_outliers) desc
                             limit 10) b
                        where a.province = b.province and month = $query::TIMESTAMP
                        group by b.province, a.bras_id
                        order by b.province, a.bras_id) c) d
              where d.r <= 10
                  """
        .as[(String,String,Int)])
  }

  def getTopKibana(month: String,typeError: String): Future[Seq[(String,String,Int)]] = {
    val query = month + "-01"
    dbConfig.db.run(
      sql"""select d.province,d.bras_id,d.error from(
                  select row_number() OVER (PARTITION BY c.province ORDER BY c.error desc) AS r , c.*
                  from(
                       select b.province, a.bras_id, sum(#$typeError) error
                       from dmt_overview_noc a,
                            (select province
                             from dmt_overview_noc
                             where month = $query::TIMESTAMP
                             group by province
                             having sum(#$typeError) >0
                             order by sum(#$typeError) desc
                             limit 10) b
                        where a.province = b.province and month = $query::TIMESTAMP
                        group by b.province, a.bras_id
                        order by b.province, a.bras_id) c) d
              where d.r <= 10
                  """
        .as[(String,String,Int)])
  }

  def getTopOpsview(month: String,typeOpsview: String): Future[Seq[(String,String,Int)]] = {
    val query = month + "-01"
    dbConfig.db.run(
      sql"""select d.province,d.bras_id,d.ops from(
                  select row_number() OVER (PARTITION BY c.province ORDER BY c.ops desc) AS r , c.*
                  from(
                       select b.province, a.bras_id, sum(#$typeOpsview) ops
                       from dmt_overview_noc a,
                            (select province
                             from dmt_overview_noc
                             where month = $query::TIMESTAMP
                             group by province
                             having sum(#$typeOpsview) >0
                             order by sum(#$typeOpsview) desc
                             limit 10) b
                        where a.province = b.province and month = $query::TIMESTAMP
                        group by b.province, a.bras_id
                        order by b.province, a.bras_id) c) d
              where d.r <= 10
                  """
        .as[(String,String,Int)])
  }

  def getTopInf(month: String,typeInferr: String, province: String): Future[Seq[(String,String, String,Int)]] = {
    val query = month + "-01"
    dbConfig.db.run(
      sql"""select d.province,d.bras_id,d.host,d.total_inf from(
                  select row_number() OVER (PARTITION BY c.province ORDER BY c.total_inf desc) AS r , c.*
                  from(
                       select b.province,a.bras_id, a.host, sum(#$typeInferr) as total_inf
                       from dmt_overview_inf a,
                            (select province
                             from dmt_overview_inf
                             where month = $query::TIMESTAMP and province ~* $province
                             group by province
                             order by sum(#$typeInferr) desc
                             limit 20) b
                        where a.province = b.province and month = $query::TIMESTAMP
                        group by b.province,a.bras_id, a.host
                        order by b.province,a.bras_id, a.host) c) d
              where d.r <= 20
              order by d.province,d.bras_id, d.host
                  """
        .as[(String,String,String,Int)])
  }

  def getTopnotSuyhao(month: String, province: String): Future[Seq[(String,String,Int,Int)]] = {
    val query = month + "-01"
    dbConfig.db.run(
      sql"""select d.province,d.host,d.suyhao,d.suy_clients from(
                  select row_number() OVER (PARTITION BY c.province ORDER BY c.suyhao desc) AS r , c.*
                  from(
                       select b.province, a.host, sum(not_passed_suyhao) suyhao,sum(not_passed_suyhao_clients) suy_clients
                       from dmt_overview_suyhao a,
                            (select province
                             from dmt_overview_suyhao
                             where month = $query::TIMESTAMP and province ~* $province
                             group by province
                             having sum(not_passed_suyhao) >0
                             order by sum(not_passed_suyhao) desc
                             limit 10) b
                        where a.province = b.province and month = $query::TIMESTAMP
                        group by b.province, a.host
                        order by b.province, a.host) c) d
              where d.r <= 10
                  """
        .as[(String,String,Int,Int)])
  }

  def getTopPoorconn(month: String,typeOLTpoor:String,province: String): Future[Seq[(String,String,Int)]] = {
    val query = month + "-01"
    dbConfig.db.run(
      sql"""select d.province,d.host,d.conn from(
                  select row_number() OVER (PARTITION BY c.province ORDER BY c.conn desc) AS r , c.*
                  from(
                       select b.province, a.host, sum(#$typeOLTpoor) conn
                       from dmt_overview_device a,
                            (select province
                             from dmt_overview_device
                             where month = $query::TIMESTAMP and province ~* $province
                             group by province
                             having sum(#$typeOLTpoor) >0
                             order by sum(#$typeOLTpoor) desc
                             limit 10) b
                        where a.province = b.province and month = $query::TIMESTAMP
                        group by b.province, a.host
                        order by b.province, a.host) c) d
              where d.r <= 10
                  """
        .as[(String,String,Int)])
  }

  def get3MonthLastest(): Future[Seq[(String)]] = {
    dbConfig.db.run(
      sql"""select distinct month
            from dmt_overview_noc
            order by month desc
            limit 3
                  """
        .as[(String)])
  }

  def getMinMaxMonth(): Future[Seq[(String,String)]] = {
    dbConfig.db.run(
      sql"""select min(month),max(month)
            from dmt_overview_noc
                  """
        .as[(String,String)])
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
    val signin = multiRs.responses(0).totalHits
    val logoff = multiRs.responses(1).totalHits
    (signin,logoff)
  }

  def getSiglogClients(bras: String, day: String): (Int,Int) = {
    val rs = client.execute(
      multi(
        search(s"radius-streaming-*" / "docs")
          query { must(termQuery("type", "con"),termQuery("nasName",bras.toLowerCase),termQuery("typeLog","SignIn"),rangeQuery("timestamp").gte(CommonService.formatYYmmddToUTC(day.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(day.split("/")(1))))) }
          aggregations(
          cardinalityAgg("contract","name")
          ),
        search(s"radius-streaming-*" / "docs")
          query { must(termQuery("type", "con"),termQuery("nasName",bras.toLowerCase),termQuery("typeLog","LogOff"),rangeQuery("timestamp").gte(CommonService.formatYYmmddToUTC(day.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(day.split("/")(1))))) }
          aggregations(
          cardinalityAgg("contract","name")
          )
      )
    ).await
    val signin =  rs.responses(0).aggregations.get("contract").get.asInstanceOf[Map[String, Int]].get("value").get
    val logoff =  rs.responses(1).aggregations.get("contract").get.asInstanceOf[Map[String, Int]].get("value").get
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
    response.totalHits
  }

  def getOpviewBytimeResponse(bras: String,nowDay: String,hourly: Int): Array[(Int,Int)] = {
    if(bras.equals("*")){
      val res = client.execute(
        search(s"infra_dwh_opsview_*" / "docs")
          query { must(rangeQuery("date_time").gte(CommonService.formatYYmmddToUTC(nowDay)).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(nowDay)))) }
          aggregations (
          dateHistogramAggregation("hourly")
            .field("date_time")
            .interval(DateHistogramInterval.HOUR)
            .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
          )  size 1000
      ).await
      CommonService.getAggregationsKeyString(res.aggregations.get("hourly")).map(x=> (CommonService.getHourFromES5(x._1),x._2))
        .groupBy(_._1).mapValues(_.map(x=>x._2.toInt).sum).toArray.sorted
    }
    else {
      val res = client.execute(
        search(s"infra_dwh_opsview_*" / "docs")
          query { must(termQuery("bras_id.keyword",bras),rangeQuery("date_time").gte(CommonService.formatYYmmddToUTC(nowDay.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(nowDay.split("/")(1))))) }
          aggregations (
          dateHistogramAggregation("hourly")
            .field("date_time")
            .interval(DateHistogramInterval.HOUR)
            .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
          )  size 1000
      ).await
      CommonService.getAggregationsKeyString(res.aggregations.get("hourly")).map(x=> (CommonService.getHourFromES5(x._1),x._2))
        .groupBy(_._1).mapValues(_.map(x=>x._2.toInt).sum).toArray.sorted
    }
   /* dbConfig.db.run(
      sql"""select extract(hour from  date_time) as hourly, count(service_name)
            from dwh_opsview
            where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            group by bras_id,extract(hour from  date_time)
            order by hourly
                  """
        .as[(Int,Int)])*/
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
    if(bras.equals("*")) {
      val rs = client.execute(
        search(s"infra_dwh_kibana_*" / "docs")
          query {
          must(rangeQuery("date_time").gte(CommonService.formatYYmmddToUTC(day)).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(day))))
        }
          aggregations (
          dateHistogramAggregation("hourly")
            .field("date_time")
            .interval(DateHistogramInterval.HOUR)
            .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
          )
      ).await
      CommonService.getAggregationsSiglog(rs.aggregations.get("hourly")).map(x => (CommonService.getHoursFromMiliseconds(x._1.toLong) -> x._2.toInt))
    }
    else{
      val rs = client.execute(
        search(s"infra_dwh_kibana_*" / "docs")
          query {
          must(termQuery("bras_id.keyword", bras), rangeQuery("date_time").gte(CommonService.formatYYmmddToUTC(day.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(day.split("/")(1)))))
        }
          aggregations (
          dateHistogramAggregation("hourly")
            .field("date_time")
            .interval(DateHistogramInterval.HOUR)
            .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
          )
      ).await
      CommonService.getAggregationsSiglog(rs.aggregations.get("hourly")).map(x => (CommonService.getHoursFromMiliseconds(x._1.toLong) -> x._2.toInt))
    }
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
    try {
      val mulRes = client.execute(
        multi(
          search(s"radius-streaming-*" / "docs")
            query {
            must(termQuery("type", "con"), termQuery("nasName", bras.toLowerCase), termQuery("typeLog", "SignIn"), rangeQuery("timestamp").gte(CommonService.formatYYmmddToUTC(day.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(day.split("/")(1)))))
          }
            aggregations (
            dateHistogramAggregation("hourly")
              .field("timestamp")
              .interval(DateHistogramInterval.HOUR)
              .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
            ),
          search(s"radius-streaming-*" / "docs")
            query {
            must(termQuery("type", "con"), termQuery("nasName", bras.toLowerCase), termQuery("typeLog", "LogOff"), rangeQuery("timestamp").gte(CommonService.formatYYmmddToUTC(day.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(day.split("/")(1)))))
          }
            aggregations (
            dateHistogramAggregation("hourly")
              .field("timestamp")
              .interval(DateHistogramInterval.HOUR)
              .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
            )
        )
      ).await
      val arrSigin = CommonService.getAggregationsSiglog(mulRes.responses(0).aggregations.get("hourly")).map(x => CommonService.getHoursFromMiliseconds(x._1.toLong) -> x._2).groupBy(_._1).mapValues(_.map(_._2).sum).toSeq.sortBy(_._1).map(x => x._2).toArray
      val arrLogoff = CommonService.getAggregationsSiglog(mulRes.responses(1).aggregations.get("hourly")).map(x => CommonService.getHoursFromMiliseconds(x._1.toLong) -> x._2).groupBy(_._1).mapValues(_.map(_._2).sum).toSeq.sortBy(_._1).map(x => x._2).toArray
      SigLogByTime(arrSigin, arrLogoff)
    }
    catch{
      case e: Exception => SigLogByTime(Array[(Long)](), Array[(Long)]())
    }
  }

  def getInfErrorBytimeResponse(bras: String,nowDay: String,hourly:Int): Array[(Int,Int, Int,Int, Int,Int,Int,Int)] = {
    val res = client.execute(
      search(s"infra_dwh_inf_host_*" / "docs")
        query { must(termQuery("bras_id.keyword",bras),rangeQuery("date_time").gte(CommonService.formatYYmmddToUTC(nowDay.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(nowDay.split("/")(1))))) }
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
            sumAgg("sum6","jumper_error")
          )
        )  size 0
    ).await
    val arrHourly = CommonService.getAggregationsKeyStringAndMultiSum(res.aggregations.get("hourly")).map(x=> (CommonService.getHourFromES5(x._1),x._2,x._3,x._4,x._5,x._6,x._7,x._8))
    val errorRes = arrHourly.groupBy(_._1).map{case (k,v) => k -> (v.map(x=> x._2).sum,v.map(x=> x._3).sum,v.map(x=> x._4).sum,v.map(x=> x._5).sum,v.map(x=> x._6).sum,v.map(x=> x._7).sum,v.map(x=> x._8).sum)}
    errorRes.map(x=> (x._1,x._2._1,x._2._2,x._2._3,x._2._4,x._2._5,x._2._6,x._2._7)).toArray.sorted

    /*dbConfig.db.run(
      sql"""select extract(hour from  date_time) as hourly, sum(sf_error)+sum(lofi_error) as sumError
            from dwh_inf_host
            where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            group by bras_id,extract(hour from  date_time)
            order by hourly
                  """
        .as[(Int,Int)])*/
  }

  def getSankeyService(bras: String, nowDay: String) = {
    val brasId = s"%$bras%"
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select bras_id, service_name, count(*)
            from dwh_opsview
            where date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP AND device_type='switch'
            	  AND bras_id IN(
            			select bras_id
                        from dwh_opsview where bras_id like $brasId and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP AND device_type='switch'
            			group by bras_id
            			order by count(*) desc limit 10
            		)
            group by bras_id, service_name
                  """
        .as[(String, String, Int)])
  }

  def getServiceNameStt(bras: String, nowDay: String) = {
    val brasId = s"%$bras%"
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select service_name, service_status, count(*)
            from dwh_opsview
            where bras_id like $brasId and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP AND device_type='switch'
            group by service_name, service_status
                  """
        .as[(String, String, Int)])
  }

  def getInfModuleResponse(bras: String,nowDay: String): Array[(String,String,Long,Long,Long)] = {
    val rs = client.execute(
      search(s"infra_dwh_inf_module_*" / "docs")
        query { must(termQuery("bras_id",bras),not(termQuery("module.keyword","-1")),rangeQuery("date_time").gte(CommonService.formatYYmmddToUTC(nowDay.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(nowDay.split("/")(1))))) }
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

  def getTicketOutlierByBrasId(bras: String,nowDay: String): Future[Seq[(String,String)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select tb.* from
            ((select 'ticket_Code: ' || ticket_code || '_Issue: ' || issue || '_Reason: ' || reason_name device_type, created_date as time from dwh_ticket
              where device_name= $bras AND created_date >= $fromDay::TIMESTAMP AND created_date < $nextDay::TIMESTAMP)
            union all
             (select 'outlier' device_type, date_time as time from dwh_conn_bras_detail
              where bras_id= $bras AND date_time >= $fromDay::TIMESTAMP AND date_time < $nextDay::TIMESTAMP)
             ) as tb
             order by tb.time desc
                  """
        .as[(String,String)])
  }

  def getOpsviewServiceNameResponse(bras: String,nowDay: String): Future[Seq[(String,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select service_name,count(*) from dwh_opsview
             where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP and device_type='bras'
             group by service_name
                  """
        .as[(String,Int)])
  }

  def getOpsviewServiceSttResponse(bras: String,nowDay: String): Future[Seq[(String,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select service_status,count(*) from dwh_opsview
             where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP and device_type='bras'
             group by service_status
                  """
        .as[(String,Int)])
  }

  def getDeviceServStatus(bras: String, nowDay: String): Future[Seq[(String,String,String,Int)]] = {
    val brasId = s"%$bras%"
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
     dbConfig.db.run(
       sql"""select bras_id, service_name,service_status,count(*)
             from dwh_opsview
             where bras_id like $brasId and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP and device_type ='power'
             group by bras_id, service_name,service_status
                   """
         .as[(String,String, String,Int)])
  }

  def getOpServByStatusResponse(bras: String,nowDay: String): Array[(String,String,Int)] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    val response = client.execute(
      search(s"infra_dwh_opsview_*" / "docs")
        query { must( termQuery("bras_id.keyword", bras),rangeQuery("date_time").gte(CommonService.formatYYmmddToUTC(fromDay)).lt(CommonService.formatYYmmddToUTC(nextDay)))}
        aggregations (
        termsAggregation("service_name")
          .field("service_name.keyword")
          .subAggregations(
            termsAggregation("service_status")
              .field("service_status.keyword")
          ) size 1000
        )
    ).await
    val mapHeat = CommonService.getSecondAggregations(response.aggregations.get("service_name"),"service_status")
    mapHeat.flatMap(x => x._2.map(y => x._1 -> y))
      .map(x => (x._1, x._2._1, x._2._2.toInt))
   /* dbConfig.db.run(
      sql"""select service_name,service_status,count(*)
            from dwh_opsview
            where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            group by service_name,service_status
                  """
        .as[(String,String,Int)])*/
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
    val rs = client.execute(
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
    val rs = client.execute(
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
    val rs = client.execute(
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
    val rs = client.execute(
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
  def getSeveValueES(bras: String,day: String): Array[(String,String,String,Long)] ={
    /*val rs = client.execute(
      search(s"infra_dwh_kibana_*" / "docs")
        query { must(termQuery("bras_id.keyword",bras),not(termQuery("error_name.keyword","")),rangeQuery("date_time").gte(CommonService.formatYYmmddToUTC(day.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(day.split("/")(1))))) }
        aggregations(
        termsAggregation("severity")
          .field("severity.keyword")
          .subAggregations(
            termsAggregation("error_name")
              .field("error_name.keyword") size 1000
          )
        size 1000
        ) size 0
    ).await*/
    val rs = client.execute(search(s"infra_dwh_kibana_*" / "docs")
      query { must(termQuery("bras_id.keyword",bras),not(termQuery("error_name.keyword","")),rangeQuery("date_time").gte(CommonService.formatYYmmddToUTC(day.split("/")(0))).lt(CommonService.formatYYmmddToUTC(CommonService.getNextDay(day.split("/")(1))))) }
      aggregations(
      termsAggregation("type")
        .field("error_type.keyword")
        .subAggregations(
          termsAggregation("severity")
            .field("severity.keyword")
            .subAggregations(
              termsAggregation("error_name").field("error_name.keyword") size 1000
            ) size 1000
        )
        size 1000
      )).await
    val mapSevErr = CommonService.getThirdAggregations(rs.aggregations.get("type"), "severity", "error_name")
    mapSevErr.flatMap(x => x._2.map(y => x._1 -> y))
      .map(x => (x._1 , x._2._1) -> x._2._2)
      .flatMap(x => x._2.map(y => x._1 -> y))
      .map(x => (x._1._1, x._1._2 , x._2._1, x._2._2))
    /*val mapSevErr = CommonService.getSecondAggregations(rs.aggregations.get("severity"),"error_name")

    mapSevErr.flatMap(x => x._2.map(y => x._1 -> y))
      .map(x => (x._1, x._2._1) -> x._2._2)*/
  }

  def getSigLogByHost(bras: String,nowDay: String): SigLogByHost = {
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