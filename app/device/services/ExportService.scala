package service

import slick.driver.PostgresDriver.api._
import services.domain.{AbstractService, CommonService}
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.jdbc.GetResult

case class JsonKibana(dateTime: String, brasId: String, errorName: String, severity: String, facility: String, errorType: String, description: String, value: String, deviceType: String)

case class JsonOpsview(dateTime: String, brasId: String, serviceName: String, serviceStatus: String, message: String, deviceType: String)

case class JsonInf(dateTime: String, errorName: String, host: String, module: Int, index: Int, text: String)

case class JsonSuyhao(dateTime: String, province: String, host: String, module: String, not_pass_suyhao: String, pass_suyhao: String)

case class JsonPortPonDown(dateTime: String, errorName: String, host: String, module: String, index: String, text: String)

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
      JsonInf(r.nextString, r.nextString, r.nextString, r.nextInt, r.nextInt, r.nextString))
    dbConfig.db.run(
      sql"""select *
            from dwh_inf_error where host like $bras AND date_time >= $fromDate::TIMESTAMP AND date_time < $toDate::TIMESTAMP
            order by date_time desc
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

  def exportByportPon(fromDate: String, toDate: String, device: String, module: Int)= {
    val bras = "%"+device+"%"
    implicit val getUserResult = GetResult(r =>
      JsonPortPonDown(r.nextString, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString))
    dbConfig.db.run(
      sql"""select * from dwh_inf_error where host like $bras AND module =$module AND date_time >= $fromDate::TIMESTAMP AND date_time < $toDate::TIMESTAMP order by date_time desc
            """
        .as[JsonPortPonDown])
  }

}