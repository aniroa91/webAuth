package service

import slick.driver.PostgresDriver.api._
import services.domain.AbstractService

import scala.concurrent.Future
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.jdbc.{PositionedParameters, SetParameter}

object ProblemService extends AbstractService{
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

  def listProvinceByWeek(week: String, province: String): Future[Seq[(String, String, Long)]] = {
    val arrName = province.split(",").map("'" + _ + "'").mkString(",")
    val startDate = week.split("->")(0).trim
    if(province.equals("")){
      dbConfig.db.run(
        sql"""select province, device_type, sum(n_prob_device) from dmt_weekly_mapping where week = $startDate::TIMESTAMP
            group by province, device_type having sum(n_prob_device) >0
            """
          .as[(String, String, Long)])
    }
    else{
      dbConfig.db.run(
        sql"""select province, device_type, sum(n_prob_device) from dmt_weekly_mapping where week = $startDate::TIMESTAMP and province IN(#$arrName)
            group by province, device_type having sum(n_prob_device) >0
            """
          .as[(String, String, Long)])
    }
  }

  def listProbconnectivity(week: String, prov: Array[String]): Future[Seq[(String, Long, Long)]] = {
    val startDate = week.split("->")(0).trim
    val lstProv = prov.toList
    implicit object SetListString extends SetParameter[List[String]] {
      def apply(vList: List[String], pp: PositionedParameters) {
        vList.foreach(pp.setString)
      }
    }
    dbConfig.db.run(
      sql"""select bras_id, sum(signin) signin, sum(logoff) logoff
            from dmt_weekly_conn
            where week = $startDate::TIMESTAMP AND (flag_signin =1 OR flag_logoff = 1) AND province IN ($lstProv#${",?" * (lstProv.size - 1)})
            group by bras_id
            order by sum(signin) + sum(logoff) desc
            """
        .as[(String, Long, Long)])
  }

  def listProbError(week: String, prov: Array[String], devType: String): Future[Seq[(String, Long)]] = {
    val startDate = week.split("->")(0).trim
    val endDate = week.split("->")(1).trim
    val lstProv = prov.toList
    implicit object SetListString extends SetParameter[List[String]] {
      def apply(vList: List[String], pp: PositionedParameters) {
        vList.foreach(pp.setString)
      }
    }
    if(devType == "*"){
      dbConfig.db.run(
        sql"""select bras_id, sum(err) err from dmt_weekly_kib where week = $startDate::TIMESTAMP AND flag_err =1 AND province IN ($lstProv#${",?" * (lstProv.size - 1)})
            group by bras_id having sum(err)>0
            order by sum(err) desc
            """
          .as[(String, Long)])
    }
    else{
      dbConfig.db.run(
        sql"""select bras_id, sum(err) err from dmt_weekly_kib where week = $startDate::TIMESTAMP AND flag_err =1 AND province IN ($lstProv#${",?" * (lstProv.size - 1)}) AND device_type= $devType
            group by bras_id having sum(err)>0
            order by sum(err) desc
            """
          .as[(String, Long)])
    }
  }

  def listProbWarning(week: String, prov: Array[String], devType: String): Future[Seq[(String, Long)]] = {
    val lstProv = prov.toList
    implicit object SetListString extends SetParameter[List[String]] {
      def apply(vList: List[String], pp: PositionedParameters) {
        vList.foreach(pp.setString)
      }
    }
    val startDate = week.split("->")(0).trim
    if(devType == "*"){
      dbConfig.db.run(
        sql"""select bras_id, sum(warn) warn from dmt_weekly_kib where week = $startDate::TIMESTAMP AND (flag_warn =1 OR flag_cluster = 1) AND province IN ($lstProv#${",?" * (lstProv.size - 1)})
            group by bras_id
            order by sum(warn) desc
            """
          .as[(String, Long)])
    }
    else{
      dbConfig.db.run(
        sql"""select bras_id, sum(warn) warn from dmt_weekly_kib where week = $startDate::TIMESTAMP AND (flag_warn =1 OR flag_cluster = 1) AND province IN ($lstProv#${",?" * (lstProv.size - 1)}) AND device_type= $devType
            group by bras_id
            order by sum(warn) desc
            """
          .as[(String, Long)])
    }
  }

  def listCritAlerts(week: String, prov: Array[String], devType: String): Future[Seq[(String, Long)]] = {
    val startDate = week.split("->")(0).trim
    val lstProv = prov.toList
    implicit object SetListString extends SetParameter[List[String]] {
      def apply(vList: List[String], pp: PositionedParameters) {
        vList.foreach(pp.setString)
      }
    }
    if(devType == "*"){
      dbConfig.db.run(
        sql"""select bras_id, sum(crit) crit from dmt_weekly_ops where week = $startDate::TIMESTAMP AND flag_crit =1 AND province IN ($lstProv#${",?" * (lstProv.size - 1)})
            group by bras_id having sum(crit) >0
            order by sum(crit) desc
            """
          .as[(String, Long)])
    }
    else{
      dbConfig.db.run(
        sql"""select bras_id, sum(crit) crit from dmt_weekly_ops where week = $startDate::TIMESTAMP AND flag_crit =1 AND province IN ($lstProv#${",?" * (lstProv.size - 1)}) AND device_type= $devType
            group by bras_id having sum(crit) >0
            order by sum(crit) desc
            """
          .as[(String, Long)])
    }
  }

  def listWarnAlerts(week: String, prov: Array[String], devType: String): Future[Seq[(String, Long)]] = {
    val startDate = week.split("->")(0).trim
    val lstProv = prov.toList
    implicit object SetListString extends SetParameter[List[String]] {
      def apply(vList: List[String], pp: PositionedParameters) {
        vList.foreach(pp.setString)
      }
    }
    if(devType == "*") {
      dbConfig.db.run(
        sql"""select bras_id, sum(warn) warn from dmt_weekly_ops where week = $startDate::TIMESTAMP AND (flag_warn = 1 OR flag_cluster = 1) AND province IN ($lstProv#${",?" * (lstProv.size - 1)})
            group by bras_id having sum(warn) >0
            order by sum(warn) desc
            """
          .as[(String, Long)])
    }
    else{
      dbConfig.db.run(
        sql"""select bras_id, sum(warn) warn from dmt_weekly_ops where week = $startDate::TIMESTAMP AND (flag_warn = 1 OR flag_cluster = 1) AND province IN ($lstProv#${",?" * (lstProv.size - 1)}) AND device_type= $devType
            group by bras_id having sum(warn) >0
            order by sum(warn) desc
            """
          .as[(String, Long)])
    }
  }

  def listSuyhao(week: String, prov: Array[String]): Future[Seq[(String, Long, Long, Double)]] = {
    val lstProv = prov.toList
    implicit object SetListString extends SetParameter[List[String]] {
      def apply(vList: List[String], pp: PositionedParameters) {
        vList.foreach(pp.setString)
      }
    }
    val startDate = week.split("->")(0).trim
    dbConfig.db.run(
      sql"""select host, sum(pass), sum(not_pass), sum(rate) from dmt_weekly_suyhao where week = $startDate::TIMESTAMP AND flag_not_pass = 1 AND province IN ($lstProv#${",?" * (lstProv.size - 1)})
            group by host
            """
        .as[(String, Long, Long, Double)])
  }

  def listBroken(week: String, prov: Array[String]): Future[Seq[(String, Long, Long)]] = {
    val lstProv = prov.toList
    implicit object SetListString extends SetParameter[List[String]] {
      def apply(vList: List[String], pp: PositionedParameters) {
        vList.foreach(pp.setString)
      }
    }
    val startDate = week.split("->")(0).trim
    dbConfig.db.run(
      sql"""select host, sum(lost_signal), sum(affected_client) from dmt_weekly_splitter where week = $startDate::TIMESTAMP AND flag_top_splitter = 1 AND province IN ($lstProv#${",?" * (lstProv.size - 1)})
            group by host
            """
        .as[(String, Long, Long)])
  }

  def listOLT(week: String, prov: Array[String]): Future[Seq[(String,String, Int, Int, Int, Int, Int, Int)]] = {
    val lstProv = prov.toList
    implicit object SetListString extends SetParameter[List[String]] {
      def apply(vList: List[String], pp: PositionedParameters) {
        vList.foreach(pp.setString)
      }
    }
    val startDate = week.split("->")(0).trim
    dbConfig.db.run(
      sql"""select host, module, sum(user_down), sum(inf_down), sum(lofi_error), sum(sf_error), sum(rogue_error), sum(sf_module)
            from dmt_weekly_inf where week = $startDate::TIMESTAMP AND (flag_sf_error = 1 OR flag_cluster = 1) AND province IN ($lstProv#${",?" * (lstProv.size - 1)})
            group by host, module
            order by sum(lofi_error) desc
            """
        .as[(String,String, Int, Int, Int, Int, Int, Int)])
  }
}