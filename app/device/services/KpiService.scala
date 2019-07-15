package service

import slick.driver.PostgresDriver.api._
import services.domain.AbstractService

import scala.concurrent.Future
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile

object KpiService extends AbstractService{
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  def listWeekly(): Future[Seq[(String, String)]] = {
    dbConfig.db.run(
      sql"""select replace(tb.weekly, 'm', 'w'), tb.date
            from (select distinct to_char(week::date, 'IYYY_mIW') AS weekly,
            		      cast(date_trunc('week', week::date) as date) + 0 || ' -> ' ||  cast(date_trunc('week', week::date) as date) + 6 as date
            	   from dmt_weekly_mapping order by weekly desc) tb
            """
        .as[(String, String)])
  }

  def listKpi(week: String, prov: String): Future[Seq[(String, Double, Double)]] = {
    val startDate = week.split("->")(0).trim
    if(prov == ""){
      dbConfig.db.run(
        sql"""select  week.kpi_index, week.vlWeek, quarter.vlQuarter
            from (select kpi_index, sum(value) vlWeek from dmt_weekly_kpi where week = $startDate::TIMESTAMP
            group by kpi_index) week
            join (select kpi_index, sum(value) vlQuarter from dmt_weekly_kpi_thres where quarter = cast(date_trunc('quarter', $startDate::date) as date)
            group by kpi_index) quarter on week.kpi_index = quarter.kpi_index
            order by week.kpi_index
            """
          .as[(String, Double, Double)])
    }
    else{
      val arrName = prov.split(",").map("'" + _ + "'").mkString(",")
      dbConfig.db.run(
        sql"""select  week.kpi_index, week.vlWeek, quarter.vlQuarter
            from (select kpi_index, sum(value) vlWeek from dmt_weekly_kpi where week = $startDate::TIMESTAMP AND province IN(#$arrName)
            group by kpi_index) week
            join (select kpi_index, sum(value) vlQuarter from dmt_weekly_kpi_thres where quarter = cast(date_trunc('quarter', $startDate::date) as date) AND province IN(#$arrName)
            group by kpi_index) quarter on week.kpi_index = quarter.kpi_index
            order by week.kpi_index
            """
          .as[(String, Double, Double)])
    }
  }

  /*def listKpiJson(week: String, prov: String): Future[Seq[(String, Double, Double)]] = {
    val startDate = week.split("->")(0).trim
    dbConfig.db.run(
      sql"""select week.kpi_index, week.vlWeek, quarter.vlQuarter
            from (select kpi_index, sum(value) vlWeek from dmt_weekly_kpi where week = $startDate::TIMESTAMP AND province = $prov
            group by kpi_index) week
            join (select kpi_index, sum(value) vlQuarter from dmt_weekly_kpi_thres where quarter = cast(date_trunc('quarter', $startDate::date) as date) AND province = $prov
            group by kpi_index) quarter on week.kpi_index = quarter.kpi_index
            """
        .as[(String, Double, Double)])
  }*/

  def listKpiTimeSeries(week: String, prov: String, index: String): Future[Seq[(String, Double)]] = {
    val startDate = week.split("->")(0).trim
    if(prov == ""){
      dbConfig.db.run(
        sql"""select week, sum(value) from dmt_weekly_kpi where week <= $startDate::TIMESTAMP AND kpi_index = $index
              group by week
              order by week
            """
          .as[(String, Double)])
    }
    else{
      val arrName = prov.split(",").map("'" + _ + "'").mkString(",")
      dbConfig.db.run(
        sql"""select week, sum(value) from dmt_weekly_kpi where week <= $startDate::TIMESTAMP AND province IN(#$arrName) AND kpi_index = $index
              group by week
              order by week
            """
          .as[(String, Double)])
    }
  }

}