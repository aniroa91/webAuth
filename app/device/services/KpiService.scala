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

  def listKpi(week: String): Future[Seq[(String, Double, Double)]] = {
    val startDate = week.split("->")(0).trim
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

  def listKpiJson(week: String): Future[Seq[(String, String, Double, Double)]] = {
    val startDate = week.split("->")(0).trim
    dbConfig.db.run(
      sql"""select week.province, week.kpi_index, week.vlWeek, quarter.vlQuarter
            from (select province, kpi_index, sum(value) vlWeek from dmt_weekly_kpi where week = $startDate::TIMESTAMP
            group by province, kpi_index) week
            join (select province, kpi_index, sum(value) vlQuarter from dmt_weekly_kpi_thres where quarter = cast(date_trunc('quarter', $startDate::date) as date)
            group by province,kpi_index) quarter on week.kpi_index = quarter.kpi_index
            """
        .as[(String, String, Double, Double)])
  }
}