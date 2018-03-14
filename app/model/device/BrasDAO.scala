package model.device

import play.api.Play
import play.api.data.Form
import play.api.data.Forms._
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future
import slick.driver.JdbcProfile
import slick.driver.PostgresDriver.api._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import services.domain.CommonService

object BrasDAO {

  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  def getSigLogResponse(bras: String,nowDay: String): Future[Seq[(Int,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    val sumDay = CommonService.getRangeDay(nowDay).split(",").length
    dbConfig.db.run(
      sql"""select sum(signin_total_count) / $sumDay,sum(logoff_total_count) / $sumDay
            from bras_count
            where bras_id=$bras and time >= $fromDay::TIMESTAMP and time < $nextDay::TIMESTAMP
            group by bras_id
                  """
        .as[(Int,Int)])
  }

  def getNoOutlierResponse(bras: String,nowDay: String): Future[Seq[(Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    val sumDay = CommonService.getRangeDay(nowDay).split(",").length
    dbConfig.db.run(
      sql"""select count(*) / $sumDay
            from dwh_radius_bras_detail
            where bras_id=$bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP and label = 'outlier'
            group by bras_id
                  """
        .as[(Int)])
  }

  def getOpviewBytimeResponse(bras: String,nowDay: String,hourly: Int): Future[Seq[(Int,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select extract(hour from  date_time) as hourly, count(service_name)
            from dwh_opsview
            where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            group by bras_id,extract(hour from  date_time)
            having extract(hour from  date_time) = $hourly
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
            having extract(hour from  date_time) = $hourly
                  """
        .as[(Int,Int)])
  }

  def getSigLogBytimeResponse(bras: String,nowDay: String,hourly:Int): Future[Seq[(Int,Int,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select extract(hour from  time) as hourly, sum(signin_total_count),sum(logoff_total_count)
            from bras_count
            where bras_id= $bras and time >= $fromDay::TIMESTAMP and time < $nextDay::TIMESTAMP
            group by bras_id,extract(hour from  time)
            having extract(hour from  time) = $hourly
                  """
        .as[(Int,Int,Int)])
  }

  def getInfErrorBytimeResponse(bras: String,nowDay: String,hourly:Int): Future[Seq[(Int,Int)]] = {
    val fromDay = nowDay.split("/")(0)
    val nextDay = CommonService.getNextDay(nowDay.split("/")(1))
    dbConfig.db.run(
      sql"""select extract(hour from  date_time) as hourly, sum(cpe_error)+sum(lostip_error) as sumError
            from dwh_inf_host
            where bras_id= $bras and date_time >= $fromDay::TIMESTAMP and date_time < $nextDay::TIMESTAMP
            group by bras_id,extract(hour from  date_time)
            having extract(hour from  date_time) = $hourly
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
      sql"""select line_ol || '-' || card_ol || '-' || port_ol as linecard_card_port,sum(signin_total_count_by_port) as signin,
            sum(logoff_total_count_by_port) as logoff
            from bras_count_by_port
            where bras_id= $bras and time >= $fromDay::TIMESTAMP and time < $nextDay::TIMESTAMP
            group by line_ol, card_ol, port_ol
                  """
        .as[(String,Int,Int)])
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
}