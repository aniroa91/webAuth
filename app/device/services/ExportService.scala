package service

import slick.driver.PostgresDriver.api._
import services.domain.{AbstractService, CommonService}
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.jdbc.GetResult

case class JsonKibana(dateTime: String, brasId: String, errorName: String, severity: String, facility: String, errorType: String, description: String, value: String, deviceType: String)

case class JsonOpsview(dateTime: String, brasId: String, serviceName: String, serviceStatus: String, message: String, deviceType: String)

case class JsonInf(dateTime: String, host: String, user_down: Long, inf_down: Double, sf_error: Long, lofi_error: Long, rouge_error: Long, lost_signal: Long, sf_module: Long)

case class JsonSuyhao(dateTime: String, province: String, host: String, module: String, not_pass_suyhao: String, pass_suyhao: String)

object ExportService extends AbstractService{
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  def exportByKibana(fromDate: String, toDate: String, device: String)= {
    val bras = "%"+device+"%"
    implicit val getUserResult = GetResult(r =>
      JsonKibana(r.nextString, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString))
    dbConfig.db.run(
      sql"""select * from dwh_kibana where bras_id like $bras AND date_time >= $fromDate::TIMESTAMP AND date_time < $toDate::TIMESTAMP order by date_time desc
            """
        .as[JsonKibana])
  }

  def exportByOpsview(fromDate: String, toDate: String, device: String)= {
    val bras = "%"+device+"%"
    implicit val getUserResult = GetResult(r =>
      JsonOpsview(r.nextString, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString))
    dbConfig.db.run(
      sql"""select * from dwh_opsview where bras_id like $bras AND date_time >= $fromDate::TIMESTAMP AND date_time < $toDate::TIMESTAMP order by date_time desc
            """
        .as[JsonOpsview])
  }

  def exportByInf(fromDate: String, toDate: String, device: String)= {
    val bras = "%"+device+"%"
    implicit val getUserResult = GetResult(r =>
      JsonInf(r.nextString, r.nextString, r.nextLong, r.nextDouble,r.nextLong,r.nextLong,r.nextLong,r.nextLong,r.nextLong))
    dbConfig.db.run(
      sql"""select date_trunc('day', date_time) as date_time, host,
                   sum(user_down) user_down,
                   sum(inf_down) / 2 inf_down,
                   sum(sf_error) sf_error,
                   sum(lofi_error) lofi_error,
                   sum(rouge_error) rouge_error,
                   sum(lost_signal) lost_signal,
                   sum(jumper_error) sf_module
            from dwh_inf_host where host like $bras AND date_time >= $fromDate::TIMESTAMP AND date_time < $toDate::TIMESTAMP
            group by date_trunc('day', date_time), host
            order by date_trunc('day', date_time), host
            """
        .as[JsonInf])
  }

  def exportBySuyhao(fromDate: String, toDate: String, device: String)= {
    val bras = "%"+device+"%"
    implicit val getUserResult = GetResult(r =>
      JsonSuyhao(r.nextString, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString))
    dbConfig.db.run(
      sql"""select date, province, host, module, passed_false as not_pass_suyhao, passed_true as pass_suyhao
            from dmt_portpon_suyhao where host like $bras AND date >= $fromDate::TIMESTAMP AND date < $toDate::TIMESTAMP order by date desc
            """
        .as[JsonSuyhao])
  }

}